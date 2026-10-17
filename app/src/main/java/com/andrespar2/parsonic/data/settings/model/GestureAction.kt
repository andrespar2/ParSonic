package com.andrespar2.parsonic.data.settings.model

/**
 * Accion que ParSonic puede ejecutar sobre la sesion multimedia activa.
 *
 * [PLAY], [PAUSE], [TOGGLE_PLAY_PAUSE], [NEXT], [PREVIOUS], [SEEK_FORWARD],
 * [SEEK_BACKWARD] y [STOP] se envian al `MediaController` del reproductor
 * activo. Las acciones de volumen se aplican con `AudioManager`, porque muchos
 * reproductores no exponen el control de volumen a traves de su sesion.
 */
enum class GestureAction(
    val id: String,
    val label: String,
    val description: String
) {
    NONE(
        id = "none",
        label = "Sin accion",
        description = "El gesto se reconoce pero no hace nada."
    ),
    TOGGLE_PLAY_PAUSE(
        id = "toggle_play_pause",
        label = "Reproducir / Pausar",
        description = "Alterna entre reproduccion y pausa."
    ),
    PLAY(
        id = "play",
        label = "Reproducir",
        description = "Reanuda la reproduccion."
    ),
    PAUSE(
        id = "pause",
        label = "Pausar",
        description = "Pausa la reproduccion."
    ),
    NEXT(
        id = "next",
        label = "Siguiente pista",
        description = "Salta a la pista siguiente."
    ),
    PREVIOUS(
        id = "previous",
        label = "Pista anterior",
        description = "Vuelve a la pista anterior."
    ),
    SEEK_FORWARD(
        id = "seek_forward",
        label = "Avanzar 10 s",
        description = "Adelanta diez segundos dentro de la pista."
    ),
    SEEK_BACKWARD(
        id = "seek_backward",
        label = "Retroceder 10 s",
        description = "Retrocede diez segundos dentro de la pista."
    ),
    STOP(
        id = "stop",
        label = "Detener",
        description = "Detiene la reproduccion."
    ),
    VOLUME_UP(
        id = "volume_up",
        label = "Subir volumen",
        description = "Sube el volumen multimedia del dispositivo."
    ),
    VOLUME_DOWN(
        id = "volume_down",
        label = "Bajar volumen",
        description = "Baja el volumen multimedia del dispositivo."
    );

    companion object {
        /** Recupera una accion por su [id], con [NONE] como valor seguro. */
        fun fromId(id: String?): GestureAction =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}
