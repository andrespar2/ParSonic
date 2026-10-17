package com.andrespar2.parsonic.core

/**
 * Constantes globales de ParSonic.
 *
 * Se centralizan aqui para evitar numeros y cadenas magicas repartidos por el
 * codigo y para que los tests puedan referenciarlas.
 */
object ParSonicConstants {

    /** Nombre del archivo de DataStore con las preferencias del usuario. */
    const val DATASTORE_NAME = "parsonic_settings"

    /** Canal de notificacion del servicio de gestos. */
    const val NOTIFICATION_CHANNEL_ID = "parsonic_gesture_service"

    /** Identificador fijo de la notificacion persistente del servicio. */
    const val GESTURE_NOTIFICATION_ID = 2001

    /** Ancho por defecto de cada zona de gesto en los bordes (dp). */
    const val DEFAULT_EDGE_ZONE_WIDTH_DP = 44

    /** Limites configurables del ancho de zona. */
    const val MIN_EDGE_ZONE_WIDTH_DP = 24
    const val MAX_EDGE_ZONE_WIDTH_DP = 96

    /** Altura de la zona inferior reservada a gestos (dp). */
    const val DEFAULT_BOTTOM_ZONE_HEIGHT_DP = 56

    /** Distancia (dp) que debe recorrer un dedo para considerarse un deslizamiento. */
    const val DEFAULT_SWIPE_THRESHOLD_DP = 48

    /** Milisegundos maximos para completar un deslizamiento. */
    const val DEFAULT_SWIPE_MAX_DURATION_MS = 700L

    /** Milisegundos maximos entre dos toques para contar como doble toque. */
    const val DEFAULT_DOUBLE_TAP_TIMEOUT_MS = 320L

    /** Milisegundos minimos de pulsacion para un toque largo. */
    const val DEFAULT_LONG_PRESS_MIN_MS = 500L

    /** Milisegundos maximos que dura un toque simple (por encima es largo). */
    const val DEFAULT_TAP_MAX_DURATION_MS = 300L

    /** Movimiento maximo (dp) tolerado para que un toque siga siendo un toque. */
    const val DEFAULT_TAP_SLOP_DP = 16

    /** Ventana (ms) para agrupar eventos de sensores y evitar disparos dobles. */
    const val SENSOR_DEBOUNCE_MS = 800L

    /** Accion por defecto cuando no hay reproduccion activa conocida. */
    const val UNKNOWN_MEDIA_LABEL = "Sin reproductor activo"
}
