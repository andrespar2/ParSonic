package com.andrespar2.parsonic.service.sensors

import com.andrespar2.parsonic.data.settings.model.GestureType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MotionGestureEvaluatorTest {

    // --- Shake --------------------------------------------------------------

    @Test
    fun `tres picos dentro de la ventana disparan el agitado`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 0f, 0L)
        evaluator.onSample(0f, 0f, 14.7f, 100L)
        evaluator.onSample(0f, 0f, 0f, 200L)
        evaluator.onSample(0f, 0f, 14.7f, 300L)

        assertEquals(GestureType.SHAKE, evaluator.evaluate(400L))
    }

    @Test
    fun `dos picos no bastan para el agitado`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 0f, 0L)
        evaluator.onSample(0f, 0f, 14.7f, 100L)
        evaluator.onSample(0f, 0f, 0f, 200L)

        assertNull(evaluator.evaluate(300L))
    }

    @Test
    fun `picos antiguos fuera de la ventana no cuentan`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 0f, 0L)
        evaluator.onSample(0f, 0f, 14.7f, 100L)
        evaluator.onSample(0f, 0f, 0f, 200L)
        evaluator.onSample(0f, 0f, 14.7f, 300L)

        // A 1000 ms, los picos de 100/200/300 quedan fuera de la ventana de 550 ms.
        assertNull(evaluator.evaluate(1_000L))
    }

    @Test
    fun `el agitado respeta el periodo de rebote`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 0f, 0L)
        evaluator.onSample(0f, 0f, 14.7f, 100L)
        evaluator.onSample(0f, 0f, 0f, 200L)
        evaluator.onSample(0f, 0f, 14.7f, 300L)
        assertEquals(GestureType.SHAKE, evaluator.evaluate(400L))

        // Picos inmediatamente despues, dentro del periodo de rebote de 850 ms.
        evaluator.onSample(0f, 0f, 0f, 500L)
        evaluator.onSample(0f, 0f, 14.7f, 600L)
        evaluator.onSample(0f, 0f, 0f, 700L)
        assertNull(evaluator.evaluate(800L))
    }

    @Test
    fun `tras agotarse el rebote vuelve a detectarse el agitado`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 0f, 0L)
        evaluator.onSample(0f, 0f, 14.7f, 100L)
        evaluator.onSample(0f, 0f, 0f, 200L)
        evaluator.onSample(0f, 0f, 14.7f, 300L)
        evaluator.evaluate(400L)

        evaluator.onSample(0f, 0f, 0f, 1_000L)
        evaluator.onSample(0f, 0f, 14.7f, 1_100L)
        evaluator.onSample(0f, 0f, 0f, 1_200L)
        evaluator.onSample(0f, 0f, 14.7f, 1_300L)
        assertEquals(GestureType.SHAKE, evaluator.evaluate(1_350L))
    }

    // --- Flip (boca abajo) -------------------------------------------------

    @Test
    fun `mantener el telefono boca abajo dispara el gesto`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 9.8f, 0L)
        evaluator.onSample(0f, 0f, -9.8f, 100L)
        evaluator.onSample(0f, 0f, -9.8f, 200L)

        assertEquals(GestureType.FLIP_DOWN, evaluator.evaluate(500L))
    }

    @Test
    fun `una inclinacion lateral invalida el gesto de girar boca abajo`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 9.8f, 0L)
        evaluator.onSample(6.5f, 0f, -9.8f, 100L)

        assertNull(evaluator.evaluate(500L))
    }

    @Test
    fun `no se dispara si el telefono vuelve a la normalidad antes de tiempo`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 9.8f, 0L)
        evaluator.onSample(0f, 0f, -9.8f, 100L)
        evaluator.onSample(0f, 0f, 9.8f, 200L)

        assertNull(evaluator.evaluate(300L))
    }

    // --- Prioridad y limpieza ----------------------------------------------

    @Test
    fun `el agitado tiene prioridad sobre girar boca abajo`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, -9.8f, 0L)
        evaluator.onSample(0f, 0f, -9.8f, 100L)
        evaluator.onSample(0f, 0f, -24.5f, 150L)
        evaluator.onSample(0f, 0f, -9.8f, 200L)
        evaluator.onSample(0f, 0f, -24.5f, 250L)

        assertEquals(GestureType.SHAKE, evaluator.evaluate(260L))
    }

    @Test
    fun `reset limpia el estado interno`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        evaluator.onSample(0f, 0f, 0f, 0L)
        evaluator.onSample(0f, 0f, 14.7f, 100L)
        evaluator.onSample(0f, 0f, 0f, 200L)
        evaluator.onSample(0f, 0f, 14.7f, 300L)
        evaluator.evaluate(400L)

        evaluator.reset()
        assertNull(evaluator.evaluate(500L))
    }

    @Test
    fun `sin muestras no se detecta nada`() {
        val evaluator = MotionGestureEvaluator(MotionGestureEvaluator.Config())
        assertNull(evaluator.evaluate(100L))
    }

    @Test
    fun `una configuracion invalida se rechaza`() {
        assertThrows(IllegalArgumentException::class.java) {
            MotionGestureEvaluator.Config(shakeRequiredSpikes = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MotionGestureEvaluator.Config(shakeWindowMs = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            MotionGestureEvaluator.Config(flipSustainMs = 0L)
        }
    }
}
