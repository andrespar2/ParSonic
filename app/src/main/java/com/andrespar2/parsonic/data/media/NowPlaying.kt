package com.andrespar2.parsonic.data.media

import android.graphics.Bitmap

/**
 * Fotografia inmutable de lo que esta sonando en el dispositivo.
 *
 * Se publica como un unico objeto (en lugar de varios flujos sueltos) para que
 * la interfaz no pueda mostrar un estado incoherente, por ejemplo la portada de
 * una cancion con el titulo de otra.
 */
data class NowPlaying(
    /** Paquete de la aplicacion que publica la sesion, por ejemplo `com.spotify.music`. */
    val packageName: String,
    /** Nombre legible de la aplicacion, por ejemplo "Spotify". */
    val appLabel: String,
    /** Titulo de la pista; cadena vacia si el reproductor no lo publica. */
    val title: String = "",
    /** Artista o interprete. */
    val artist: String = "",
    /** Album. */
    val album: String = "",
    /** Estado normalizado de reproduccion. */
    val status: PlaybackStatus = PlaybackStatus.UNKNOWN,
    /** Posicion conocida en milisegundos. */
    val positionMs: Long = 0L,
    /** Duracion total en milisegundos; 0 si es desconocida. */
    val durationMs: Long = 0L,
    /**
     * Instante (`SystemClock.elapsedRealtime`) en el que se conocio [positionMs].
     * Permite a la interfaz extrapolar el progreso sin sondear el reproductor.
     */
    val positionUpdatedAtElapsedMs: Long = 0L,
    /** Portada incrustada en los metadatos, si el reproductor la incluye. */
    val artwork: Bitmap? = null,
    /** URI de la portada, para cargarla de forma diferida si no viene incrustada. */
    val artworkUri: String? = null,
    /** `true` si la sesion permite saltar a la pista siguiente. */
    val canSkipNext: Boolean = false,
    /** `true` si la sesion permite volver a la pista anterior. */
    val canSkipPrevious: Boolean = false,
    /** `true` si la sesion permite desplazarse dentro de la pista. */
    val canSeek: Boolean = false,
    /** `true` si la sesion acepta la orden de pausa o reproduccion. */
    val canTogglePlayback: Boolean = false
) {
    /** `true` si hay una pista con titulo conocido. */
    val hasMetadata: Boolean
        get() = title.isNotBlank() || artist.isNotBlank()

    /** `true` si la sesion esta sonando. */
    val isPlaying: Boolean
        get() = status == PlaybackStatus.PLAYING

    /** Linea secundaria lista para mostrar: artista y album sin duplicar datos. */
    val subtitle: String
        get() = when {
            artist.isNotBlank() && album.isNotBlank() && album != artist -> "$artist · $album"
            artist.isNotBlank() -> artist
            album.isNotBlank() -> album
            else -> appLabel
        }
}
