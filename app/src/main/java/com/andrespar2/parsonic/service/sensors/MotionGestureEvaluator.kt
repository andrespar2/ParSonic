package com.andrespar2.parsonic.service.sensors

import com.andrespar2.parsonic.data.settings.model.GestureType
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Evaluador puro de gestos basados en movimiento: SHAKE y FLIP_DOWN.
 *
 * No usa `SensorEvent` ni el framework de sensores. Recibe muestras (x,y,z,t)
 * ya filtradas y devuelve un `GestureType` cuando se cumple la condicion,
 * respetando un periodo de *debounce* para evitar dobles disparos.
 *
 * Ejes de sensores (Android): en general, la gravedad se mide en m/s². Para
 * detectar que el telefono esta boca abajo, buscamos que la componente z de la
 * gravedad sea aproximadamente -g (el dispositivo apuntando hacia la mesa) y
 * que las componentes x/y sean pequeñas.
 */
class MotionGestureEvaluator(
    private val config: Config
) {

    data class Config(
        val shakeMinimumForce: Float = 11.5f,
        val shakeRequiredSpikes: Int = 3,
        val shakeWindowMs: Long = 550L,
        val shakeDebounceMs: Long = 850L,
        val flipMinGravityZ: Float = -7.5f,
        val flipMaxTilt: Float = 3.5f,
        val flipSustainMs: Long = 400L,
        val flipDebounceMs: Long = 1_200L
    ) {
        init {
            require(shakeRequiredSpikes >= 2) { "Se necesitan al menos dos picos para detectar un agitado" }
            require(shakeWindowMs > 0) { "La ventana de agitado debe ser positiva" }
            require(flipSustainMs > 0) { "El tiempo de boca abajo debe ser positivo" }
        }
    }

    private data class ShakeSpike(val timeMs: Long)

    private val shakeSpikes = ArrayList<ShakeSpike>(16)
    private var lastShakeAtMs: Long = -1L
    private var lastSampleTimeMs: Long = 0L
    private var lastGravityMagnitude: Float = 0f
    private var gravityInitialized: Boolean = false

    // Flip tracking.
    private var flipStartMs: Long = -1L
    private var lastFlipAtMs: Long = -1L
    private var isFaceDown: Boolean = false

    fun onSample(x: Float, y: Float, z: Float, timeMs: Long) {
        lastSampleTimeMs = timeMs
        val gravityX = x
        val gravityY = y
        val gravityZ = z
        val gMag = sqrt(gravityX * gravityX + gravityY * gravityY + gravityZ * gravityZ)

        // SHAKE: detectamos picos de aceleracion sobre la gravedad filtrada.
        val deltaG = abs(gMag - lastGravityMagnitude)
        if (deltaG >= config.shakeMinimumForce) {
            recordShake(timeMs)
        }
        lastGravityMagnitude = gMag

        // FLIP: boca abajo (z negativo sustancial) con poca inclinacion en X/Y.
        val isNowFaceDown = (gravityZ <= config.flipMinGravityZ) &&
            abs(gravityX) <= config.flipMaxTilt &&
            abs(gravityY) <= config.flipMaxTilt

        if (isNowFaceDown) {
            if (!isFaceDown || flipStartMs < 0) {
                flipStartMs = timeMs
            }
            isFaceDown = true
        } else {
            flipStartMs = -1L
            isFaceDown = false
        }
    }

    /**
     * Devuelve el gesto detectado en funcion de las muestras acumuladas hasta
     * [nowMs]. Cada gesto solo puede dispararse una vez por periodo de
     * *debounce*.
     */
    fun evaluate(nowMs: Long): GestureType? {
        // Prioriza SHAKE sobre FLIP: un fuerte agitado no suele ser un giro
        // intencional boca abajo.
        val shakeResult = evaluateShake(nowMs)
        if (shakeResult != null) return shakeResult

        return evaluateFlip(nowMs)
    }

    /** Reinicia el estado interno. */
    fun reset() {
        shakeSpikes.clear()
        lastShakeAtMs = -1L
        lastSampleTimeMs = 0L
        lastGravityMagnitude = 0f
        flipStartMs = -1L
        lastFlipAtMs = -1L
        isFaceDown = false
    }

    private fun recordShake(timeMs: Long) {
        // Limpia picos muy antiguos para evitar que la lista crezca indefinidamente.
        val cutoff = timeMs - config.shakeWindowMs
        while (shakeSpikes.isNotEmpty() && shakeSpikes[0].timeMs < cutoff) {
            shakeSpikes.removeAt(0)
        }
        shakeSpikes.add(ShakeSpike(timeMs))
    }

    private fun evaluateShake(nowMs: Long): GestureType? {
        // Descarta si estamos en periodo de debounce.
        if (lastShakeAtMs >= 0 && nowMs - lastShakeAtMs < config.shakeDebounceMs) {
            return null
        }
        val cutoff = nowMs - config.shakeWindowMs
        var recentCount = 0
        for (i in shakeSpikes.indices.reversed()) {
            val spike = shakeSpikes[i]
            if (spike.timeMs < cutoff) break
            recentCount++
            if (recentCount >= config.shakeRequiredSpikes) {
                // Ha habido suficientes picos dentro de la ventana: dispara SHAKE.
                shakeSpikes.clear()
                lastShakeAtMs = nowMs
                return GestureType.SHAKE
            }
        }
        return null
    }

    private fun evaluateFlip(nowMs: Long): GestureType? {
        if (lastFlipAtMs >= 0 && nowMs - lastFlipAtMs < config.flipDebounceMs) {
            return null
        }
        if (isFaceDown && flipStartMs >= 0) {
            val sustained = nowMs - flipStartMs
            if (sustained >= config.flipSustainMs) {
                flipStartMs = -1L
                lastFlipAtMs = nowMs
                return GestureType.FLIP_DOWN
            }
        }
        return null
    }
}
