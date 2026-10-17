package com.andrespar2.parsonic.data.media

/**
 * Traduce las constantes de `android.media.session.PlaybackState` a
 * [PlaybackStatus].
 *
 * Los valores se repiten aqui como constantes propias porque son parte
 * estable y documentada de la API publica de Android. Esto permite probar el
 * mapeo en la maquina virtual de Java, sin emulador ni Robolectric.
 */
object PlaybackStatusMapper {

    // Espejo de android.media.session.PlaybackState (API 21+, valores estables).
    const val STATE_NONE = 0
    const val STATE_STOPPED = 1
    const val STATE_PAUSED = 2
    const val STATE_PLAYING = 3
    const val STATE_FAST_FORWARDING = 4
    const val STATE_REWINDING = 5
    const val STATE_BUFFERING = 6
    const val STATE_ERROR = 7
    const val STATE_CONNECTING = 8
    const val STATE_SKIPPING_TO_PREVIOUS = 9
    const val STATE_SKIPPING_TO_NEXT = 10
    const val STATE_SKIPPING_TO_QUEUE_ITEM = 11

    /** Convierte el codigo entero de estado en un [PlaybackStatus]. */
    fun fromPlaybackStateCode(code: Int): PlaybackStatus = when (code) {
        STATE_PLAYING -> PlaybackStatus.PLAYING
        STATE_PAUSED -> PlaybackStatus.PAUSED
        STATE_STOPPED -> PlaybackStatus.STOPPED
        STATE_BUFFERING, STATE_CONNECTING -> PlaybackStatus.BUFFERING
        STATE_FAST_FORWARDING -> PlaybackStatus.FAST_FORWARDING
        STATE_REWINDING -> PlaybackStatus.REWINDING
        STATE_SKIPPING_TO_PREVIOUS, STATE_SKIPPING_TO_NEXT, STATE_SKIPPING_TO_QUEUE_ITEM ->
            PlaybackStatus.SKIPPING
        STATE_ERROR -> PlaybackStatus.ERROR
        STATE_NONE -> PlaybackStatus.UNKNOWN
        else -> PlaybackStatus.UNKNOWN
    }

    /**
     * `true` si la sesion esta sonando ahora mismo. Es la senal que usa el
     * servicio para decidir si mantener activo el reconocimiento de gestos.
     */
    fun isPlaying(code: Int): Boolean = code == STATE_PLAYING

    /**
     * Formatea una posicion en milisegundos como `m:ss`. Devuelve `--:--` si la
     * duracion es desconocida ([android.media.MediaMetadata.METADATA_KEY_DURATION]
     * suele ser 0 o -1 en emisiones en directo).
     */
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0L) return "--:--"
        val totalSeconds = durationMs / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}
