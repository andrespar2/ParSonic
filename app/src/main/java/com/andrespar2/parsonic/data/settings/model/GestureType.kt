package com.andrespar2.parsonic.data.settings.model

/**
 * Origen de un gesto.
 *
 * La distincion es importante para la seccion de compatibilidad: los gestos
 * tactiles dependen de una ventana de superposicion y de que la pantalla este
 * encendida, mientras que los gestos de sensor pueden dispararse con la
 * pantalla apagada si el hardware lo permite.
 */
enum class GestureSource {
    /** Gesto tactil capturado sobre otras aplicaciones (pantalla encendida). */
    TOUCH,

    /** Gesto derivado de un sensor de bajo consumo. */
    SENSOR
}

/**
 * Gestos que ParSonic sabe reconocer.
 *
 * El conjunto de gestos tactiles sigue el modelo de "Music Gesture": zonas en
 * los bordes que no interfieren con el uso normal del telefono.
 */
enum class GestureType(
    val id: String,
    val label: String,
    val description: String,
    val source: GestureSource,
    val defaultAction: GestureAction,
    /**
     * Indica si el gesto esta disponible cuando la pantalla esta apagada.
     * Solo los gestos de sensor pueden serlo (ver limitaciones de Android).
     */
    val availableWithScreenOff: Boolean = false
) {
    SWIPE_LEFT(
        id = "swipe_left",
        label = "Deslizar a la izquierda",
        description = "Desliza el dedo hacia la izquierda desde la zona de gestos.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.PREVIOUS
    ),
    SWIPE_RIGHT(
        id = "swipe_right",
        label = "Deslizar a la derecha",
        description = "Desliza el dedo hacia la derecha desde la zona de gestos.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.NEXT
    ),
    SWIPE_UP(
        id = "swipe_up",
        label = "Deslizar hacia arriba",
        description = "Desliza el dedo hacia arriba desde la zona de gestos.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.VOLUME_UP
    ),
    SWIPE_DOWN(
        id = "swipe_down",
        label = "Deslizar hacia abajo",
        description = "Desliza el dedo hacia abajo desde la zona de gestos.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.VOLUME_DOWN
    ),
    DOUBLE_TAP(
        id = "double_tap",
        label = "Doble toque",
        description = "Toca dos veces seguidas dentro de la zona de gestos.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.TOGGLE_PLAY_PAUSE
    ),
    SINGLE_TAP(
        id = "single_tap",
        label = "Toque simple",
        description = "Toca una vez dentro de la zona de gestos.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.NONE
    ),
    LONG_PRESS(
        id = "long_press",
        label = "Mantener pulsado",
        description = "Mantiene el dedo en la zona de gestos sin desplazarlo.",
        source = GestureSource.TOUCH,
        defaultAction = GestureAction.NONE
    ),
    SHAKE(
        id = "shake",
        label = "Agitar el dispositivo",
        description = "Agita el telefono con la pantalla encendida.",
        source = GestureSource.SENSOR,
        defaultAction = GestureAction.NEXT
    ),
    FLIP_DOWN(
        id = "flip_down",
        label = "Girar boca abajo",
        description = "Coloca el telefono boca abajo sobre una superficie.",
        source = GestureSource.SENSOR,
        defaultAction = GestureAction.PAUSE
    ),
    DOUBLE_KNOCK(
        id = "double_knock",
        label = "Doble golpe en la pantalla apagada",
        description = "Dos golpecitos suaves con la pantalla apagada. Usa el sensor " +
            "de movimiento significativo, el unico sensor que Android mantiene " +
            "activo con el dispositivo suspendido sin coste apreciable de bateria.",
        source = GestureSource.SENSOR,
        defaultAction = GestureAction.TOGGLE_PLAY_PAUSE,
        availableWithScreenOff = true
    );

    companion object {
        /** Recupera un gesto por su [id], o `null` si no existe. */
        fun fromId(id: String?): GestureType? = entries.firstOrNull { it.id == id }

        /** Gestos tactiles, en el orden en que se muestran en la interfaz. */
        val touchGestures: List<GestureType> = entries.filter { it.source == GestureSource.TOUCH }

        /** Gestos basados en sensores. */
        val sensorGestures: List<GestureType> = entries.filter { it.source == GestureSource.SENSOR }
    }
}
