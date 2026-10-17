package com.andrespar2.parsonic.service.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.service.gesture.GestureInterpreter
import com.andrespar2.parsonic.service.gesture.TouchPhase

/**
 * Ventana de superposicion transparente que captura los toques de una franja de
 * gesto y los traduce a un [GestureType].
 *
 * Es deliberadamente una vista minima: no dibuja nada salvo una pista
 * translucida opcional cuando se reconoce un gesto. No hace vibrar: la respuesta
 * haptica la centraliza el servicio para que sea igual en gestos tactiles y de
 * sensor.
 *
 * El reconocimiento lo hace [GestureInterpreter] (sin dependencias de Android),
 * de modo que los umbrales estan cubiertos por pruebas unitarias. Aqui solo se
 * adapta `MotionEvent` a ese modelo y se mantiene **un unico** temporizador
 * activo para resolver el doble toque y la pulsacion larga.
 */
class EdgeZoneView(
    context: Context,
    private val interpreter: GestureInterpreter,
    private var showHints: Boolean
) : View(context) {

    /** Se invoca cada vez que se reconoce un gesto en esta zona. */
    var onGesture: ((GestureType) -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FLASH_COLOR }
    private var flashAnimator: ValueAnimator? = null
    private var flashAlpha: Float = 0f

    private val timeoutRunnable = Runnable {
        deliver(interpreter.onTimeout(SystemClock.uptimeMillis()))
        scheduleTimeout()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val phase = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> TouchPhase.DOWN
            MotionEvent.ACTION_MOVE -> TouchPhase.MOVE
            MotionEvent.ACTION_UP -> TouchPhase.UP
            MotionEvent.ACTION_CANCEL -> TouchPhase.CANCEL
            else -> return false
        }

        deliver(interpreter.onTouch(phase, event.x, event.y, event.eventTime))
        scheduleTimeout()
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (flashAlpha <= 0f || width == 0 || height == 0) return
        flashPaint.alpha = (flashAlpha * 255).toInt().coerceIn(0, 255)
        val radius = resources.displayMetrics.density * 12f
        canvas.drawRoundRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            radius,
            radius,
            flashPaint
        )
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(timeoutRunnable)
        flashAnimator?.cancel()
        flashAnimator = null
        super.onDetachedFromWindow()
    }

    private fun deliver(gesture: GestureType?) {
        if (gesture == null) return
        if (showHints) flash()
        onGesture?.invoke(gesture)
    }

    private fun scheduleTimeout() {
        handler.removeCallbacks(timeoutRunnable)
        val at = interpreter.nextTimeoutAtMs() ?: return
        val delay = (at - SystemClock.uptimeMillis()).coerceAtLeast(0L)
        handler.postDelayed(timeoutRunnable, delay)
    }

    private fun flash() {
        flashAnimator?.cancel()
        flashAnimator = ValueAnimator.ofFloat(FLASH_START_ALPHA, 0f).apply {
            duration = FLASH_DURATION_MS
            addUpdateListener { animator ->
                flashAlpha = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private companion object {
        const val FLASH_COLOR = 0xFFFF1744.toInt()
        const val FLASH_START_ALPHA = 0.28f
        const val FLASH_DURATION_MS = 320L
    }
}
