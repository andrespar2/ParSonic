package com.andrespar2.parsonic.data.media

import kotlinx.coroutines.flow.StateFlow

/**
 * Fuente de verdad sobre lo que esta sonando en el dispositivo.
 *
 * La implementacion de Android se apoya en `MediaSessionManager` y en un
 * `NotificationListenerService` habilitado. Todas las actualizaciones llegan
 * por *callbacks* del sistema: ParSonic nunca sondea el estado de forma
 * periodica.
 */
interface MediaSessionRepository {

    /** Estado actual, o `null` si no hay ninguna sesion multimedia activa. */
    val nowPlaying: StateFlow<NowPlaying?>

    /**
     * `true` si la app puede leer las sesiones (acceso a notificaciones
     * concedido). Si es `false`, [nowPlaying] siempre sera `null`.
     */
    val isAvailable: StateFlow<Boolean>

    /** Empieza a observar las sesiones activas. Debe llamarse en el hilo principal. */
    fun start()

    /** Deja de observar y libera los callbacks registrados. */
    fun stop()

    /** Desconecta el controlador actual y vuelve a evaluar las sesiones. */
    fun refresh()

    // --- Comandos de transporte ---------------------------------------------

    fun play()

    fun pause()

    fun togglePlayPause()

    fun skipToNext()

    fun skipToPrevious()

    fun stopPlayback()

    /** Adelanta la pista la cantidad indicada en milisegundos. */
    fun seekForward(deltaMs: Long)

    /** Retrocede la pista la cantidad indicada en milisegundos. */
    fun seekBackward(deltaMs: Long)
}
