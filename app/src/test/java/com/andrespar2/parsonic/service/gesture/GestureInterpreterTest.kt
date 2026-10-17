package com.andrespar2.parsonic.service.gesture

import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del reconocedor de gestos.
 *
 * Los umbrales de [Sensitivity.MEDIUM] con densidad 1 son:
 * deslizamiento >= 48 px en <= 700 ms, toque <= 300 ms con <= 16 px de
 * movimiento, doble toque en <= 320 ms y pulsacion larga >= 500 ms.
 */
class GestureInterpreterTest {

    private fun interpreter(
        sensitivity: Sensitivity = Sensitivity.MEDIUM,
        density: Float = 1f
    ) = GestureInterpreter(GestureInterpreter.configFor(sensitivity, density))

    private fun GestureInterpreter.tap(x: Float, y: Float, timeMs: Long) {
        assertNull(onTouch(TouchPhase.DOWN, x, y, timeMs))
        assertNull(onTouch(TouchPhase.UP, x, y, timeMs + 60L))
    }

    // --- Toques -------------------------------------------------------------

    @Test
    fun `un toque corto se resuelve como toque simple cuando caduca la ventana`() {
        val interpreter = interpreter()
        interpreter.tap(10f, 10f, 0L)

        assertTrue(interpreter.hasPendingTap())
        assertNull("Antes de caducar no se decide", interpreter.onTimeout(200L))
        assertEquals(GestureType.SINGLE_TAP, interpreter.onTimeout(320L))
        assertFalse(interpreter.hasPendingTap())
    }

    @Test
    fun `dos toques seguidos se resuelven como doble toque`() {
        val interpreter = interpreter()
        interpreter.tap(10f, 10f, 0L)

        assertNull(
            "El segundo contacto no se decide hasta levantar el dedo",
            interpreter.onTouch(TouchPhase.DOWN, 12f, 12f, 150L)
        )
        assertEquals(
            GestureType.DOUBLE_TAP,
            interpreter.onTouch(TouchPhase.UP, 12f, 12f, 200L)
        )
        assertFalse(interpreter.hasPendingTap())
    }

    @Test
    fun `el segundo contacto no confirma el doble toque si resulta un deslizamiento`() {
        val interpreter = interpreter()
        interpreter.tap(10f, 10f, 0L)

        interpreter.onTouch(TouchPhase.DOWN, 10f, 10f, 150L)
        assertEquals(
            "El deslizamiento gana al doble toque",
            GestureType.SWIPE_RIGHT,
            interpreter.onTouch(TouchPhase.UP, 160f, 10f, 220L)
        )
    }

    @Test
    fun `dos toques separados mas que la ventana son dos toques simples`() {
        val interpreter = interpreter()
        interpreter.tap(10f, 10f, 0L)
        assertEquals(GestureType.SINGLE_TAP, interpreter.onTimeout(500L))

        interpreter.tap(10f, 10f, 600L)
        assertNull("No es doble toque: la ventana ya cerro", interpreter.onTimeout(800L))
        assertEquals(GestureType.SINGLE_TAP, interpreter.onTimeout(920L))
    }

    @Test
    fun `un toque que se mueve mas que el margen no cuenta como toque`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.onTouch(TouchPhase.MOVE, 30f, 0f, 50L)
        assertNull(interpreter.onTouch(TouchPhase.UP, 30f, 0f, 100L))
        assertNull(interpreter.onTimeout(1_000L))
    }

    @Test
    fun `un toque demasiado lento no cuenta como toque`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.onTouch(TouchPhase.MOVE, 120f, 0f, 400L)
        assertNull(
            "900 ms supera el maximo de deslizamiento y el recorrido impide la pulsacion larga",
            interpreter.onTouch(TouchPhase.UP, 220f, 0f, 900L)
        )
    }

    // --- Deslizamientos -----------------------------------------------------

    @Test
    fun `reconoce el deslizamiento hacia la derecha`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.onTouch(TouchPhase.MOVE, 60f, 4f, 80L)
        assertEquals(
            GestureType.SWIPE_RIGHT,
            interpreter.onTouch(TouchPhase.UP, 120f, 6f, 160L)
        )
    }

    @Test
    fun `reconoce el deslizamiento hacia la izquierda`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 200f, 100f, 0L)
        assertEquals(
            GestureType.SWIPE_LEFT,
            interpreter.onTouch(TouchPhase.UP, 100f, 104f, 150L)
        )
    }

    @Test
    fun `reconoce el deslizamiento hacia arriba`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 100f, 300f, 0L)
        assertEquals(
            GestureType.SWIPE_UP,
            interpreter.onTouch(TouchPhase.UP, 104f, 200f, 150L)
        )
    }

    @Test
    fun `reconoce el deslizamiento hacia abajo`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 100f, 100f, 0L)
        assertEquals(
            GestureType.SWIPE_DOWN,
            interpreter.onTouch(TouchPhase.UP, 96f, 220f, 150L)
        )
    }

    @Test
    fun `en un deslizamiento diagonal gana el eje dominante`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertEquals(
            "Mas recorrido horizontal que vertical",
            GestureType.SWIPE_RIGHT,
            interpreter.onTouch(TouchPhase.UP, 100f, 55f, 150L)
        )

        val other = interpreter()
        other.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertEquals(
            "Mas recorrido vertical que horizontal",
            GestureType.SWIPE_DOWN,
            other.onTouch(TouchPhase.UP, 55f, 100f, 150L)
        )
    }

    @Test
    fun `un deslizamiento demasiado lento no se reconoce`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertNull(
            "800 ms supera el maximo de 700 ms para sensibilidad media",
            interpreter.onTouch(TouchPhase.UP, 200f, 0f, 800L)
        )
    }

    @Test
    fun `el deslizamiento se reconoce justo en el umbral`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertEquals(
            GestureType.SWIPE_RIGHT,
            interpreter.onTouch(TouchPhase.UP, 48f, 0f, 100L)
        )
    }

    @Test
    fun `un movimiento por debajo del umbral no es deslizamiento ni toque`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.onTouch(TouchPhase.MOVE, 30f, 0f, 50L)
        assertNull(interpreter.onTouch(TouchPhase.UP, 30f, 0f, 100L))
    }

    @Test
    fun `el deslizamiento descarta un toque pendiente de doble toque`() {
        val interpreter = interpreter()
        interpreter.tap(0f, 0f, 0L)

        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 60L)
        assertEquals(
            GestureType.SWIPE_RIGHT,
            interpreter.onTouch(TouchPhase.UP, 150f, 0f, 120L)
        )

        // Al haberse descartado el toque pendiente, el siguiente toque no es doble.
        interpreter.tap(0f, 0f, 200L)
        assertEquals(GestureType.SINGLE_TAP, interpreter.onTimeout(600L))
    }

    // --- Pulsacion larga ----------------------------------------------------

    @Test
    fun `mantener pulsado dispara la pulsacion larga por temporizador`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 10f, 10f, 0L)

        assertNull(interpreter.onTimeout(400L))
        assertEquals(GestureType.LONG_PRESS, interpreter.onTimeout(500L))
        assertNull("Solo se dispara una vez", interpreter.onTimeout(900L))
        assertNull("Al levantar el dedo no hay gesto adicional", interpreter.onTouch(TouchPhase.UP, 10f, 10f, 950L))
    }

    @Test
    fun `si el dedo se desplaza no hay pulsacion larga`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.onTouch(TouchPhase.MOVE, 40f, 0f, 100L)
        assertNull(interpreter.onTimeout(600L))
    }

    @Test
    fun `la pulsacion larga lenta se detecta al levantar el dedo`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 5f, 5f, 0L)
        assertEquals(
            GestureType.LONG_PRESS,
            interpreter.onTouch(TouchPhase.UP, 5f, 6f, 700L)
        )
    }

    // --- Temporizadores -----------------------------------------------------

    @Test
    fun `nextTimeoutAtMs apunta al fin de la pulsacion larga`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 1_000L)
        assertEquals(1_000L + ParSonicConstants.DEFAULT_LONG_PRESS_MIN_MS, interpreter.nextTimeoutAtMs())
    }

    @Test
    fun `nextTimeoutAtMs apunta al fin de la ventana de doble toque`() {
        val interpreter = interpreter()
        interpreter.tap(0f, 0f, 2_000L)
        // La ventana se cuenta desde el DOWN del primer toque.
        assertEquals(
            2_000L + ParSonicConstants.DEFAULT_DOUBLE_TAP_TIMEOUT_MS,
            interpreter.nextTimeoutAtMs()
        )
    }

    @Test
    fun `nextTimeoutAtMs es nulo cuando no hay nada pendiente`() {
        val interpreter = interpreter()
        assertNull(interpreter.nextTimeoutAtMs())
    }

    @Test
    fun `tras desplazarse ya no se programa la pulsacion larga`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.onTouch(TouchPhase.MOVE, 60f, 0f, 50L)
        assertNull(interpreter.nextTimeoutAtMs())
    }

    // --- Robustez -----------------------------------------------------------

    @Test
    fun `los eventos huerfanos no alteran el estado`() {
        val interpreter = interpreter()
        assertNull(interpreter.onTouch(TouchPhase.MOVE, 10f, 10f, 0L))
        assertNull(interpreter.onTouch(TouchPhase.UP, 10f, 10f, 10L))
        assertNull(interpreter.nextTimeoutAtMs())
    }

    @Test
    fun `un segundo dedo se ignora mientras hay uno apoyado`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertNull("El segundo DOWN se descarta", interpreter.onTouch(TouchPhase.DOWN, 300f, 300f, 20L))
        assertEquals(
            GestureType.SWIPE_RIGHT,
            interpreter.onTouch(TouchPhase.UP, 120f, 0f, 120L)
        )
    }

    @Test
    fun `cancelar descarta el toque pendiente`() {
        val interpreter = interpreter()
        interpreter.tap(0f, 0f, 0L)
        assertNull(interpreter.onTouch(TouchPhase.CANCEL, 0f, 0f, 50L))
        assertNull(interpreter.onTimeout(5_000L))
        assertFalse(interpreter.hasPendingTap())
    }

    @Test
    fun `reset limpia cualquier estado en curso`() {
        val interpreter = interpreter()
        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        interpreter.reset()
        assertNull(interpreter.nextTimeoutAtMs())
        assertNull(interpreter.onTouch(TouchPhase.UP, 0f, 0f, 10L))
    }

    @Test
    fun `varios gestos seguidos se reconocen de forma independiente`() {
        val interpreter = interpreter()

        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertEquals(GestureType.SWIPE_RIGHT, interpreter.onTouch(TouchPhase.UP, 100f, 0f, 100L))

        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 200L)
        assertEquals(GestureType.SWIPE_LEFT, interpreter.onTouch(TouchPhase.UP, -100f, 0f, 300L))

        interpreter.onTouch(TouchPhase.DOWN, 0f, 0f, 400L)
        assertEquals(GestureType.SWIPE_DOWN, interpreter.onTouch(TouchPhase.UP, 0f, 120f, 500L))
    }

    // --- Sensibilidad y densidad --------------------------------------------

    @Test
    fun `una sensibilidad alta reconoce recorridos mas cortos`() {
        val high = interpreter(sensitivity = Sensitivity.HIGH)
        high.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertEquals(GestureType.SWIPE_RIGHT, high.onTouch(TouchPhase.UP, 35f, 0f, 100L))

        val low = interpreter(sensitivity = Sensitivity.LOW)
        low.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertNull("Con sensibilidad baja 35 px no bastan", low.onTouch(TouchPhase.UP, 35f, 0f, 100L))
    }

    @Test
    fun `una sensibilidad baja permite deslizamientos mas lentos`() {
        val low = interpreter(sensitivity = Sensitivity.LOW)
        low.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertEquals(GestureType.SWIPE_RIGHT, low.onTouch(TouchPhase.UP, 120f, 0f, 800L))

        val high = interpreter(sensitivity = Sensitivity.HIGH)
        high.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertNull("Con sensibilidad alta 800 ms es demasiado lento", high.onTouch(TouchPhase.UP, 120f, 0f, 800L))
    }

    @Test
    fun `la densidad de pantalla escala los umbrales`() {
        // Con densidad 2, el umbral de sensibilidad media son 96 px fisicos.
        val hdpi = interpreter(density = 2f)

        hdpi.onTouch(TouchPhase.DOWN, 0f, 0f, 0L)
        assertNull("95 px quedan por debajo del umbral", hdpi.onTouch(TouchPhase.UP, 95f, 0f, 100L))

        hdpi.onTouch(TouchPhase.DOWN, 0f, 0f, 200L)
        assertEquals(GestureType.SWIPE_RIGHT, hdpi.onTouch(TouchPhase.UP, 97f, 0f, 300L))

        hdpi.onTouch(TouchPhase.DOWN, 0f, 0f, 400L)
        assertEquals("96 px es justo el umbral", GestureType.SWIPE_RIGHT, hdpi.onTouch(TouchPhase.UP, 96f, 0f, 500L))
    }

    @Test
    fun `una configuracion invalida se rechaza al construirla`() {
        assertThrows(IllegalArgumentException::class.java) {
            GestureInterpreter.Config(
                swipeThresholdPx = 0f,
                swipeMaxDurationMs = 700L,
                doubleTapTimeoutMs = 320L,
                longPressMinMs = 500L,
                tapMaxDurationMs = 300L,
                tapSlopPx = 16f
            )
        }
    }
}
