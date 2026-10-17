package com.andrespar2.parsonic.service.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.TriggerEvent
import android.hardware.TriggerEventListener
import android.os.PowerManager
import com.andrespar2.parsonic.core.util.Logg
import com.andrespar2.parsonic.data.settings.model.GestureType

/**
 * Registra y libera los sensores que ParSonic usa para reconocer gestos.
 *
 * Se apoya en dos sensores, cada uno por un motivo concreto:
 *
 *  - **Acelerometro**: mide agitados y giros boca abajo. Consume bateria si se
 *    deja encendido, asi que **solo se registra con la pantalla encendida** y
 *    mientras el usuario tiene los gestos de sensor activos. Con la pantalla
 *    apagada se retira de inmediato.
 *  - **Movimiento significativo** (`TYPE_SIGNIFICANT_MOTION`): es el unico
 *    sensor que Android mantiene con el dispositivo suspendido a coste
 *    practicamente nulo, porque lo gestiona el propio *hardware*. Se usa para
 *    el gesto "golpe en pantalla apagada". Es un disparador de un solo uso: tras
 *    cada aviso se vuelve a armar.
 *
 * Todo llega por *callbacks* del sistema; no hay ningun sondeo periodico.
 */
class SensorGestureController(
    context: Context,
    private val onGesture: (GestureType) -> Unit
) : TriggerEventListener(), SensorEventListener {

    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val powerManager =
        appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val significantMotion = sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION)

    private val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())

    private var active = false
    private var accelerometerRegistered = false
    private var significantMotionArmed = false
    private var screenReceiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = syncAccelerometer()
    }

    /** `true` si el dispositivo tiene el acelerometro necesario para agitar/girar. */
    val supportsMotion: Boolean
        get() = accelerometer != null

    /** `true` si el dispositivo soporta el sensor de movimiento significativo. */
    val supportsSignificantMotion: Boolean
        get() = significantMotion != null

    fun setActive(active: Boolean) {
        if (this.active == active) {
            if (active) syncAccelerometer()
            return
        }
        this.active = active
        if (active) {
            registerScreenReceiver()
            evaluator.reset()
            syncAccelerometer()
            syncSignificantMotion()
        } else {
            unregisterAll()
        }
    }

    fun release() = unregisterAll().also { active = false }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val timeMs = event.timestamp / NANOS_PER_MILLI
        evaluator.onSample(event.values[0], event.values[1], event.values[2], timeMs)
        // La evaluacion va en el propio callback del sensor: no hace falta
        // ningun temporizador ni sondeo.
        evaluator.evaluate(timeMs)?.let(onGesture)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onTrigger(event: TriggerEvent) {
        if (event.sensor.type != Sensor.TYPE_SIGNIFICANT_MOTION) return
        onGesture(GestureType.DOUBLE_KNOCK)
        // El disparador es de un solo uso: se vuelve a armar para seguir atento.
        significantMotionArmed = false
        if (active) syncSignificantMotion()
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        runCatching {
            appContext.registerReceiver(screenReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            })
            screenReceiverRegistered = true
        }.onFailure { Logg.w(TAG, "No se pudo observar el estado de la pantalla", it) }
    }

    private fun syncAccelerometer() {
        val manager = sensorManager ?: return
        val wantsIt = active && isScreenInteractive() && accelerometer != null
        when {
            wantsIt && !accelerometerRegistered -> {
                accelerometerRegistered =
                    manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
            !wantsIt && accelerometerRegistered -> {
                manager.unregisterListener(this)
                accelerometerRegistered = false
                evaluator.reset()
            }
        }
    }

    private fun syncSignificantMotion() {
        val manager = sensorManager ?: return
        val wantsIt = active && significantMotion != null
        when {
            wantsIt && !significantMotionArmed ->
                significantMotionArmed = manager.requestTriggerSensor(this, significantMotion)
            !wantsIt && significantMotionArmed -> {
                manager.cancelTriggerSensor(this, significantMotion)
                significantMotionArmed = false
            }
        }
    }

    private fun unregisterAll() {
        val manager = sensorManager
        if (manager != null) {
            if (accelerometerRegistered) {
                manager.unregisterListener(this)
                accelerometerRegistered = false
            }
            if (significantMotionArmed) {
                manager.cancelTriggerSensor(this, significantMotion)
                significantMotionArmed = false
            }
        }
        if (screenReceiverRegistered) {
            runCatching { appContext.unregisterReceiver(screenReceiver) }
            screenReceiverRegistered = false
        }
        evaluator.reset()
    }

    private fun isScreenInteractive(): Boolean = powerManager?.isInteractive ?: true

    private companion object {
        const val TAG = "SensorGestures"
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
