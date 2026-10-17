package com.andrespar2.parsonic.service.gesture

import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Reconocedor de gestos puro y determinista.
 *
 * No depende de `MotionEvent` ni del framework: recibe posiciones y tiempos, y
 * devuelve el gesto reconocido. Esa separacion permite cubrir con pruebas
 * unitarias todos los umbrales y casos limite, que es justo donde este tipo de
 * funcionalidad suele fallar.
 *
 * Uso tipico (desde la vista de superposicion):
 * ```
 * val gesture = interpreter.onTouch(TouchPhase.DOWN, x, y, time)
 * ...
 * val gesture = interpreter.onTouch(TouchPhase.UP, x, y, time)
 * // programar un unico temporizador en interpreter.nextTimeoutAtMs()
 * val gesture = interpreter.onTimeout(now)
 * ```
 *
 * El doble toque y la pulsacion larga se resuelven con [onTimeout] porque no se
 * pueden decidir en el instante del contacto: hay que esperar a que caduque la
 * ventana correspondiente. `nextTimeoutAtMs()` indica cuando hay que volver a
 * llamar, de modo que solo existe un temporizador activo a la vez.
 */
class GestureInterpreter(
    private val config: Config
) {

    /**
     * Umbrales del reconocedor, ya convertidos a pixeles.
     */
    data class Config(
        val swipeThresholdPx: Float,
        val swipeMaxDurationMs: Long,
        val doubleTapTimeoutMs: Long,
        val longPressMinMs: Long,
        val tapMaxDurationMs: Long,
        val tapSlopPx: Float
    ) {
        init {
            require(swipeThresholdPx > 0f) { "swipeThresholdPx debe ser positivo" }
            require(swipeMaxDurationMs > 0L) { "swipeMaxDurationMs debe ser positivo" }
            require(doubleTapTimeoutMs > 0L) { "doubleTapTimeoutMs debe ser positivo" }
            require(longPressMinMs > 0L) { "longPressMinMs debe ser positivo" }
            require(tapMaxDurationMs > 0L) { "tapMaxDurationMs debe ser positivo" }
            require(tapSlopPx >= 0f) { "tapSlopPx no puede ser negativo" }
        }
    }

    private var pointerDown = false
    private var startX = 0f
    private var startY = 0f
    private var downTimeMs = 0L
    private var maxTravelPx = 0f
    private var longPressFired = false

    /** Instante del DOWN del ultimo toque a la espera de resolverse como doble toque. */
    private var pendingTapDownTimeMs = -1L

    /** `true` si el contacto en curso es el segundo toque de un posible doble toque. */
    private var currentDownExtendsTap = false

    /**
     * Procesa un evento tactil y devuelve el gesto reconocido en ese instante,
     * o `null` si todavia no se puede decidir.
     */
    fun onTouch(phase: TouchPhase, x: Float, y: Float, eventTimeMs: Long): GestureType? {
        when (phase) {
            TouchPhase.DOWN -> {
                // Se ignora un segundo dedo: ParSonic reconoce gestos de un dedo.
                if (pointerDown) return null
                pointerDown = true
                startX = x
                startY = y
                downTimeMs = eventTimeMs
                maxTravelPx = 0f
                longPressFired = false
                // Se decide aqui, pero el doble toque solo se confirma en el UP:
                // si el segundo contacto resulta ser un deslizamiento, no debe
                // haberse disparado nada.
                currentDownExtendsTap = pendingTapDownTimeMs >= 0L &&
                    (eventTimeMs - pendingTapDownTimeMs) <= config.doubleTapTimeoutMs
                return null
            }

            TouchPhase.MOVE -> {
                if (!pointerDown) return null
                val travel = distanceFromStart(x, y)
                if (travel > maxTravelPx) maxTravelPx = travel
                return null
            }

            TouchPhase.UP -> {
                if (!pointerDown) return null
                pointerDown = false
                val travel = max(maxTravelPx, distanceFromStart(x, y))
                val durationMs = (eventTimeMs - downTimeMs).coerceAtLeast(0L)

                // 1. Deslizamiento: recorrido suficiente y rapido.
                if (travel >= config.swipeThresholdPx && durationMs <= config.swipeMaxDurationMs) {
                    clearPendingTap()
                    return swipeDirection(startX, startY, x, y)
                }

                if (longPressFired) {
                    clearPendingTap()
                    return null
                }

                // 2. Pulsacion larga lenta (por si el temporizador no llego a dispararse).
                if (durationMs >= config.longPressMinMs && travel <= config.tapSlopPx) {
                    clearPendingTap()
                    return GestureType.LONG_PRESS
                }

                // 3. Toque: puede ser simple o el segundo de un doble toque.
                val isTap = durationMs <= config.tapMaxDurationMs && travel <= config.tapSlopPx
                if (!isTap) {
                    clearPendingTap()
                    return null
                }

                if (currentDownExtendsTap) {
                    clearPendingTap()
                    return GestureType.DOUBLE_TAP
                }

                pendingTapDownTimeMs = downTimeMs
                currentDownExtendsTap = false
                return null
            }

            TouchPhase.CANCEL -> {
                pointerDown = false
                clearPendingTap()
                return null
            }
        }
    }

    /**
     * Comprueba si ha caducado alguna ventana temporal. Devuelve el gesto
     * resultante ([GestureType.LONG_PRESS] o [GestureType.SINGLE_TAP]) o `null`.
     */
    fun onTimeout(nowMs: Long): GestureType? {
        if (pointerDown && !longPressFired &&
            ((nowMs - downTimeMs) >= config.longPressMinMs) &&
            maxTravelPx <= config.tapSlopPx
        ) {
            longPressFired = true
            return GestureType.LONG_PRESS
        }

        if (!pointerDown && pendingTapDownTimeMs >= 0L &&
            (nowMs - pendingTapDownTimeMs) >= config.doubleTapTimeoutMs
        ) {
            clearPendingTap()
            return GestureType.SINGLE_TAP
        }

        return null
    }

    /**
     * Instante en el que conviene llamar a [onTimeout], o `null` si no hay
     * ninguna ventana abierta. Asi la vista mantiene como maximo un
     * temporizador activo en lugar de sondear continuamente.
     */
    fun nextTimeoutAtMs(): Long? {
        if (pointerDown && !longPressFired && maxTravelPx <= config.tapSlopPx) {
            return downTimeMs + config.longPressMinMs
        }
        if (!pointerDown && pendingTapDownTimeMs >= 0L) {
            return pendingTapDownTimeMs + config.doubleTapTimeoutMs
        }
        return null
    }

    /** Descarta cualquier estado en curso. */
    fun reset() {
        pointerDown = false
        maxTravelPx = 0f
        longPressFired = false
        clearPendingTap()
    }

    /** `true` si hay un toque a la espera de resolverse como doble toque. */
    fun hasPendingTap(): Boolean = pendingTapDownTimeMs >= 0L

    private fun clearPendingTap() {
        pendingTapDownTimeMs = -1L
        currentDownExtendsTap = false
    }

    private fun distanceFromStart(x: Float, y: Float): Float {
        val dx = x - startX
        val dy = y - startY
        return sqrt(dx * dx + dy * dy)
    }

    private fun swipeDirection(startX: Float, startY: Float, endX: Float, endY: Float): GestureType {
        val dx = endX - startX
        val dy = endY - startY
        return if (abs(dx) >= abs(dy)) {
            if (dx > 0f) GestureType.SWIPE_RIGHT else GestureType.SWIPE_LEFT
        } else {
            if (dy > 0f) GestureType.SWIPE_DOWN else GestureType.SWIPE_UP
        }
    }

    companion object {

        /**
         * Construye la configuracion a partir de una [Sensitivity] y de la
         * densidad de pantalla, convirtiendo los dp a pixeles.
         */
        fun configFor(sensitivity: Sensitivity, density: Float): Config = Config(
            swipeThresholdPx = sensitivity.swipeThresholdDp * density,
            swipeMaxDurationMs = sensitivity.swipeMaxDurationMs,
            doubleTapTimeoutMs = ParSonicConstants.DEFAULT_DOUBLE_TAP_TIMEOUT_MS,
            longPressMinMs = ParSonicConstants.DEFAULT_LONG_PRESS_MIN_MS,
            tapMaxDurationMs = ParSonicConstants.DEFAULT_TAP_MAX_DURATION_MS,
            tapSlopPx = sensitivity.tapSlopDp * density
        )
    }
}
