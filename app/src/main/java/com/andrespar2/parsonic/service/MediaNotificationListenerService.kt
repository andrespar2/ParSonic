package com.andrespar2.parsonic.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.andrespar2.parsonic.core.util.Logg

/**
 * Servicio que habilita el acceso a `MediaSessionManager`.
 *
 * Importante para la privacidad: **no lee el contenido de las notificaciones**.
 * Android exige tener un `NotificationListenerService` habilitado para poder
 * consultar las sesiones multimedia de otras aplicaciones, asi que este
 * servicio cumple esa funcion como "llave" y descarta por completo el texto,
 * el titulo y las acciones de cada notificacion.
 *
 * No se usa `AccessibilityService`, root ni APIs privadas.
 */
class MediaNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Logg.i(TAG, "Listener conectado: ParSonic ya puede leer las sesiones multimedia")
        MediaListenerBridge.notifyConnected()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Logg.w(TAG, "Listener desconectado: el acceso a las sesiones no estara disponible")
    }

    /**
     * Se sobrescribe para dejar constancia explicita de que el contenido se
     * ignora. El sistema sigue llamando a este metodo, pero ParSonic no lee
     * `notification.extras` ni publica nada.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Deliberadamente vacio: ParSonic nunca inspecciona notificaciones.
        // La deteccion de reproduccion se hace con callbacks de MediaSessionManager.
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Deliberadamente vacio, por el mismo motivo.
    }

    private companion object {
        const val TAG = "MediaListener"
    }
}
