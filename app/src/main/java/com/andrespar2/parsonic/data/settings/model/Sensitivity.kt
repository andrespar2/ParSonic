package com.andrespar2.parsonic.data.settings.model

/**
 * Nivel de sensibilidad de un gesto.
 *
 * Se traduce directamente en los umbrales del reconocedor: una sensibilidad
 * alta exige menos recorrido y menos tiempo para disparar el gesto.
 */
enum class Sensitivity(
    val id: String,
    val label: String,
    /** Distancia minima que debe recorrer el dedo (en dp). */
    val swipeThresholdDp: Int,
    /** Tiempo maximo permitido para completar el deslizamiento (ms). */
    val swipeMaxDurationMs: Long,
    /** Movimiento maximo tolerado para que un toque siga siendo un toque (dp). */
    val tapSlopDp: Int
) {
    LOW(
        id = "low",
        label = "Baja",
        swipeThresholdDp = 72,
        swipeMaxDurationMs = 900L,
        tapSlopDp = 24
    ),
    MEDIUM(
        id = "medium",
        label = "Media",
        swipeThresholdDp = 48,
        swipeMaxDurationMs = 700L,
        tapSlopDp = 16
    ),
    HIGH(
        id = "high",
        label = "Alta",
        swipeThresholdDp = 30,
        swipeMaxDurationMs = 550L,
        tapSlopDp = 12
    );

    companion object {
        /** Recupera una sensibilidad por su [id], con [MEDIUM] como valor seguro. */
        fun fromId(id: String?): Sensitivity =
            entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}
