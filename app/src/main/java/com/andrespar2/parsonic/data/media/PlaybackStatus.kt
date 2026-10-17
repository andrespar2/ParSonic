package com.andrespar2.parsonic.data.media

/**
 * Estado de reproduccion normalizado, independiente del framework.
 *
 * Se usa un enum propio para que el resto de la app (y las pruebas) no dependa
 * de las constantes enteras de `android.media.session.PlaybackState`.
 */
enum class PlaybackStatus(val label: String) {
    PLAYING("Reproduciendo"),
    PAUSED("En pausa"),
    STOPPED("Detenido"),
    BUFFERING("Cargando"),
    FAST_FORWARDING("Avanzando"),
    REWINDING("Retrocediendo"),
    SKIPPING("Cambiando de pista"),
    ERROR("Error"),
    UNKNOWN("Sin estado")
    ;

    /** `true` si la sesion esta consumiendo audio en este momento. */
    val isActive: Boolean
        get() = this == PLAYING || this == FAST_FORWARDING || this == REWINDING || this == BUFFERING
}
