package com.andrespar2.parsonic.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.core.util.Haptics
import com.andrespar2.parsonic.core.util.Logg
import com.andrespar2.parsonic.core.util.ParSonicPermissions
import com.andrespar2.parsonic.core.util.ScreenGeometry
import com.andrespar2.parsonic.data.media.NowPlaying
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.service.dispatcher.GestureActionDispatcher
import com.andrespar2.parsonic.service.overlay.GestureOverlayController
import com.andrespar2.parsonic.service.sensors.SensorGestureController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que mantiene vivo el reconocimiento de gestos.
 *
 * Responsabilidades:
 *  - Observar las preferencias y el estado de reproduccion **por flujos**, sin
 *    ningun sondeo.
 *  - Decidir si toca capturar gestos tactiles, de sensor, ambos o ninguno.
 *  - Aplicar la optimizacion de bateria: si el usuario lo pide y no hay musica
 *    sonando, se retiran las ventanas y se liberan los sensores.
 *  - Traducir cada gesto reconocido a una accion sobre el reproductor.
 *
 * El servicio se detiene solo cuando el interruptor maestro se apaga, de modo
 * que la interfaz nunca tiene que matarlo a mano.
 */
class GestureService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var container: AppContainer
    private lateinit var notifications: NotificationHelper
    private lateinit var dispatcher: GestureActionDispatcher
    private var overlay: GestureOverlayController? = null
    private var sensors: SensorGestureController? = null

    @Volatile
    private var latestSettings: AppSettings = AppSettings.DEFAULT
    private var observing = false
    private var foregroundStarted = false
    private var lastNotificationSignature: String? = null

    override fun onCreate() {
        super.onCreate()
        container = AppContainer.get(this)
        notifications = NotificationHelper(this)
        dispatcher = GestureActionDispatcher(container.mediaRepository, container.volumeController)
        overlay = GestureOverlayController(this, ::handleGesture)
        sensors = SensorGestureController(this, ::handleGesture)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // Se apaga el interruptor maestro y se espera a que la preferencia
            // quede guardada antes de cerrar. Asi la interfaz ve el cambio y el
            // usuario no encuentra el servicio "encendido" al volver.
            scope.launch {
                container.settingsRepository.setServiceEnabled(false)
                stopForegroundCompat()
                stopSelf()
            }
            return START_NOT_STICKY
        }

        startForegroundWithNotification(container.mediaRepository.nowPlaying.value)

        if (!observing) {
            observing = true
            container.acquireMedia()
            GestureRuntimeState.setServiceRunning(true)
            observe()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observing = false
        scope.cancel()
        overlay?.release()
        sensors?.release()
        container.releaseMedia()
        GestureRuntimeState.setServiceRunning(false)
        foregroundStarted = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observe() {
        scope.launch {
            combine(
                container.settingsRepository.settings,
                container.mediaRepository.nowPlaying
            ) { settings, nowPlaying -> settings to nowPlaying }
                .collect { (settings, nowPlaying) -> applyState(settings, nowPlaying) }
        }
    }

    private fun applyState(settings: AppSettings, nowPlaying: NowPlaying?) {
        latestSettings = settings

        if (!settings.serviceEnabled) {
            overlay?.release()
            sensors?.release()
            GestureRuntimeState.setServiceRunning(false)
            stopForegroundCompat()
            stopSelf()
            return
        }

        val overlayGranted = ParSonicPermissions.canDrawOverlays(this)
        val playing = nowPlaying?.isPlaying == true
        // Optimizacion de bateria: con "solo mientras suena" activo, si no hay
        // reproduccion no se mantiene ningun reconocimiento encendido.
        val gating = !settings.onlyWhilePlaying || playing

        val touchActive = gating &&
            settings.touchGesturesEnabled &&
            settings.hasAnyActiveTouchGesture &&
            overlayGranted &&
            ParSonicPermissions.supportsOverlayGestures()

        overlay?.update(
            GestureOverlayController.Config(
                active = touchActive,
                edgeZoneWidthDp = settings.edgeZoneWidthDp,
                bottomZoneEnabled = settings.bottomZoneEnabled,
                sensitivity = settings.sensitivity,
                showHints = settings.showGestureHints,
                geometry = ScreenGeometry.of(this)
            )
        )

        val sensorActive = gating &&
            settings.sensorGesturesEnabled &&
            settings.hasAnyActiveSensorGesture
        sensors?.setActive(sensorActive)

        GestureRuntimeState.update { current ->
            current.copy(
                serviceRunning = true,
                touchActive = touchActive,
                sensorActive = sensorActive,
                overlayGranted = overlayGranted,
                playing = playing
            )
        }

        updateNotification(nowPlaying, playing)
    }

    private fun handleGesture(type: GestureType) {
        val settings = latestSettings
        val binding = settings.bindingOf(type)
        if (!binding.isEffective) return

        if (settings.hapticFeedback) Haptics.confirm(this)
        dispatcher.dispatch(binding.action)
        GestureRuntimeState.recordGesture(type)
        Logg.d(TAG, "Gesto '${type.id}' -> accion '${binding.action.id}'")
    }

    private fun updateNotification(nowPlaying: NowPlaying?, playing: Boolean) {
        if (!foregroundStarted) return
        val signature = "$playing:${nowPlaying?.appLabel.orEmpty()}"
        if (signature == lastNotificationSignature) return
        lastNotificationSignature = signature
        runCatching {
            NotificationManagerCompat.from(this).notify(
                ParSonicConstants.GESTURE_NOTIFICATION_ID,
                notifications.build(nowPlaying)
            )
        }.onFailure { Logg.w(TAG, "No se pudo actualizar la notificacion", it) }
    }

    private fun startForegroundWithNotification(nowPlaying: NowPlaying?) {
        val notification = notifications.build(nowPlaying)
        ServiceCompat.startForeground(
            this,
            ParSonicConstants.GESTURE_NOTIFICATION_ID,
            notification,
            foregroundServiceType()
        )
        foregroundStarted = true
        lastNotificationSignature = "${nowPlaying?.isPlaying == true}:${nowPlaying?.appLabel.orEmpty()}"
    }

    private fun stopForegroundCompat() {
        if (!foregroundStarted) return
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        foregroundStarted = false
        lastNotificationSignature = null
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    companion object {

        const val ACTION_START = "com.andrespar2.parsonic.action.START"
        const val ACTION_STOP = "com.andrespar2.parsonic.action.STOP"

        private const val TAG = "GestureService"

        /** Arranca el servicio en primer plano. */
        fun start(context: Context) {
            val intent = Intent(context, GestureService::class.java).setAction(ACTION_START)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Logg.w(TAG, "No se pudo iniciar el servicio de gestos", it) }
        }

        /** Pide al servicio que se detenga guardando antes la preferencia. */
        fun stop(context: Context) {
            val intent = Intent(context, GestureService::class.java).setAction(ACTION_STOP)
            runCatching { context.startService(intent) }
                .onFailure { Logg.w(TAG, "No se pudo detener el servicio de gestos", it) }
        }
    }
}
