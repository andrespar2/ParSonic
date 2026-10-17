package com.andrespar2.parsonic.service

import android.content.ComponentName
import android.content.Context
import com.andrespar2.parsonic.data.media.AndroidMediaSessionRepository
import com.andrespar2.parsonic.data.media.MediaSessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Puente de comunicacion entre [MediaNotificationListenerService] y el resto
 * de la app.
 *
 * Android solo concede acceso a `MediaSessionManager.getActiveSessions()` a
 * quien tenga un `NotificationListenerService` habilitado. Cuando el sistema
 * conecta ese servicio (por ejemplo justo despues de conceder el acceso) hay
 * que volver a consultar las sesiones; este puente avisa de ese momento sin
 * sondeos ni `BroadcastReceiver` globales.
 */
object MediaListenerBridge {

    private val _connectionEvents = MutableSharedFlow<Unit>(
        replay = 1,
        extraBufferCapacity = 1
    )

    /** Emite un valor cada vez que el sistema conecta el listener. */
    val connectionEvents: SharedFlow<Unit> = _connectionEvents.asSharedFlow()

    /** Notifica que el listener se ha conectado. */
    fun notifyConnected() {
        _connectionEvents.tryEmit(Unit)
    }

    /**
     * Componente del `NotificationListenerService` de ParSonic.
     *
     * Se usa para identificarse ante `MediaSessionManager`, para abrir los
     * ajustes correctos y como credencial en
     * [MediaSessionRepository].
     */
    fun componentName(context: Context): ComponentName =
        ComponentName(context, MediaNotificationListenerService::class.java)

    /** Crea el repositorio multimedia de la plataforma. */
    fun createRepository(context: Context): MediaSessionRepository =
        AndroidMediaSessionRepository(context.applicationContext, componentName(context))
}
