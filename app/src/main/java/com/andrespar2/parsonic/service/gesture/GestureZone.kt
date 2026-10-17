package com.andrespar2.parsonic.service.gesture

import kotlin.math.min

/**
 * Lado de una zona de gesto.
 */
enum class GestureZoneSide {
    LEFT,
    RIGHT,
    BOTTOM
}

/**
 * Zona rectangular que recibe toques para un mismo reconocedor de gestos.
 *
 * Cada zona es una ventana independiente de superposicion (`TYPE_APPLICATION_OVERLAY`)
 * para evitar que el gestor de ventanas robe toques fuera de esa franja.
 */
data class GestureZone(
    val side: GestureZoneSide,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int
        get() = (right - left).coerceAtLeast(0)

    val height: Int
        get() = (bottom - top).coerceAtLeast(0)

    val isValid: Boolean
        get() = width > 0 && height > 0
}

/**
 * Calcula las zonas tactiles de ParSonic partiendo de la geometria de pantalla.
 *
 * Reglas:
 *  - La zona izquierda y derecha no deben solaparse entre si.
 *  - La zona inferior, si existe, no invade las zonas laterales: ocupa el
 *    centro de la pantalla, desde el ancho de zona izquierda hasta la derecha.
 *  - El ancho efectivo se limita a un tercio del ancho de pantalla para no
 *    bloquear demasiada pantalla.
 *  - Se respetan los *insets* (barras de sistema) para no impedir que el
 *    usuario deslice para mostrar notificaciones/control rapido cuando sea
 *    posible.
 */
object GestureZoneCalculator {

    fun zones(
        displayWidthPx: Int,
        displayHeightPx: Int,
        edgeZoneWidthDp: Int,
        bottomZoneHeightDp: Int,
        includeBottom: Boolean,
        leftInset: Int = 0,
        topInset: Int = 0,
        rightInset: Int = 0,
        bottomInset: Int = 0,
        density: Float = 1f
    ): List<GestureZone> {
        if (displayWidthPx <= 0 || displayHeightPx <= 0) return emptyList()
        val zones = mutableListOf<GestureZone>()

        val maxEdge = (displayWidthPx / 3f).toInt()
        val edgeWidth = (edgeZoneWidthDp * density).toInt().coerceIn(1, maxEdge)
        if (edgeWidth <= 0) return emptyList()

        // Zona izquierda.
        val leftSide = GestureZone(
            side = GestureZoneSide.LEFT,
            left = leftInset,
            top = topInset,
            right = leftInset + edgeWidth,
            bottom = displayHeightPx - bottomInset
        )
        if (leftSide.isValid) zones.add(leftSide)

        // Zona derecha.
        val rightSide = GestureZone(
            side = GestureZoneSide.RIGHT,
            left = displayWidthPx - rightInset - edgeWidth,
            top = topInset,
            right = displayWidthPx - rightInset,
            bottom = displayHeightPx - bottomInset
        )
        if (rightSide.isValid) zones.add(rightSide)

        if (includeBottom) {
            val bottomHeight = (bottomZoneHeightDp * density).toInt().coerceAtLeast(1)
            // La zona inferior ocupa el centro, sin invadir las zonas laterales.
            val bottomLeft = min(leftSide.right, rightSide.right)
            val bottomRight = min(displayWidthPx - rightInset, rightSide.left)
            if (bottomRight > bottomLeft) {
                val bottom = GestureZone(
                    side = GestureZoneSide.BOTTOM,
                    left = bottomLeft,
                    top = displayHeightPx - bottomInset - bottomHeight,
                    right = bottomRight,
                    bottom = displayHeightPx - bottomInset
                )
                if (bottom.isValid) zones.add(bottom)
            }
        }

        return zones
    }
}
