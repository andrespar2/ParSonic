package com.andrespar2.parsonic.service.volume

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.andrespar2.parsonic.core.util.Haptics
import com.andrespar2.parsonic.core.util.Logg
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.service.dispatcher.GestureActionDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio de accesibilidad con el **alcance minimo posible**.
 *
 * Android no ofrece ninguna API publica para que una app en segundo plano
 * intercepte y consuma los botones de volumen (ni `MediaSession`, ni
 * `AudioManager`, ni broadcasts). La unica via es un `AccessibilityService` con
 * [android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS].
 *
 * Este servicio:
 *  - **Solo** filtra las teclas de volumen. Su configuracion declara
 *    `canRetrieveWindowContent=false` y `canPerformGestures=false`, asi que no
 *    puede leer la pantalla ni el contenido de otras aplicaciones.
 *  - No observa eventos de accesibilidad: `onAccessibilityEvent` esta vacio.
 *  - Solo actua si el usuario lo activa en la app y hay una sesion multimedia
 *    activa ("se inicia con los reproductores"). Si no, deja pasar las teclas y
 *    el volumen funciona con normalidad.
 *
 * En la practica, el filtrado de teclas necesita la pantalla encendida en muchos
 * dispositivos: con la pantalla apagada el sistema (o el fabricante) puede no
 * entregar las pulsaciones al servicio, asi que no se promete como universal.
 */
class VolumeKeyAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())

    private var container: AppContainer? = null
    private var dispatcher: GestureActionDispatcher? = null
    private val interpreter = VolumeKeyInterpreter()

    @Volatile
    private var settings: AppSettings = AppSettings.DEFAULT

    /** Teclas cuyo DOWN hemos consumido y cuyo UP tambien debemos consumir. */
    private val filteredKeyCodes = mutableSetOf<Int>()

    private val timeoutRunnable = Runnable {
        val actions = interpreter.onTimeout(SystemClock.uptimeMillis())
        actions.forEach(::perform)
        scheduleTimeout()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val appContainer = AppContainer.get(this)
        container = appContainer
        dispatcher = GestureActionDispatcher(appContainer.mediaRepository, appContainer.volumeController)
        // Mantiene registrado el listener de sesiones mientras el servicio esta
        // activo (opt-in del usuario) para saber si hay un reproductor sonando.
        appContainer.acquireMedia()
        scope.launch {
            appContainer.settingsRepository.settings.collect { settings = it }
        }
        Logg.i(TAG, "Servicio de botones de volumen conectado")
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val key = event.toVolumeKey() ?: return false

        return when (event.action) {
            KeyEvent.ACTION_DOWN -> handleDown(event, key)
            KeyEvent.ACTION_UP -> filteredKeyCodes.remove(event.keyCode)
            // Repeticiones por mantener pulsado: se consumen para no dejar el
            // flujo de eventos inconsistente (DOWN sin UP).
            else -> event.keyCode in filteredKeyCodes
        }
    }

    private fun handleDown(event: KeyEvent, key: VolumeKey): Boolean {
        // Las repeticiones (mantener pulsado) solo se consumen si ya estamos
        // filtrando esa tecla.
        if (event.repeatCount > 0) return event.keyCode in filteredKeyCodes

        if (!shouldHandleKeys()) return false

        filteredKeyCodes += event.keyCode
        val actions = interpreter.onKeyPress(key, SystemClock.uptimeMillis())
        actions.forEach(::perform)
        scheduleTimeout()
        return true
    }

    /**
     * `true` si ParSonic debe quedarse con las teclas de volumen en este
     * instante: el usuario lo activo y hay una sesion multimedia activa.
     */
    private fun shouldHandleKeys(): Boolean {
        if (!settings.volumeKeysEnabled) return false
        return container?.mediaRepository?.nowPlaying?.value != null
    }

    private fun perform(action: VolumeKeyAction) {
        val target = when (action) {
            VolumeKeyAction.NEXT_TRACK -> GestureAction.NEXT
            VolumeKeyAction.PREVIOUS_TRACK -> GestureAction.PREVIOUS
            VolumeKeyAction.VOLUME_UP -> GestureAction.VOLUME_UP
            VolumeKeyAction.VOLUME_DOWN -> GestureAction.VOLUME_DOWN
        }

        val isTrackChange = action == VolumeKeyAction.NEXT_TRACK ||
            action == VolumeKeyAction.PREVIOUS_TRACK
        if (isTrackChange && settings.hapticFeedback) Haptics.confirm(this)

        dispatcher?.dispatch(target)
        Logg.d(TAG, "Boton de volumen -> '${action.name}'")
    }

    private fun scheduleTimeout() {
        handler.removeCallbacks(timeoutRunnable)
        val at = interpreter.nextTimeoutAtMs() ?: return
        val delay = (at - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        handler.postDelayed(timeoutRunnable, delay)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Deliberadamente vacio: ParSonic no inspecciona la interfaz.
    }

    override fun onInterrupt() {
        // Sin trabajo que interrumpir.
    }

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        interpreter.reset()
        filteredKeyCodes.clear()
        container?.releaseMedia()
        container = null
        dispatcher = null
        scope.cancel()
        Logg.i(TAG, "Servicio de botones de volumen desconectado")
        super.onDestroy()
    }

    private fun KeyEvent.toVolumeKey(): VolumeKey? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> VolumeKey.UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> VolumeKey.DOWN
        else -> null
    }

    private companion object {
        const val TAG = "VolumeKeys"
    }
}
