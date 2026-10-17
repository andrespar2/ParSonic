package com.andrespar2.parsonic.service.volume

import com.andrespar2.parsonic.core.ParSonicConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeKeyInterpreterTest {

    private val window = ParSonicConstants.VOLUME_DOUBLE_PRESS_WINDOW_MS
    private val timeout = ParSonicConstants.VOLUME_MODE_TIMEOUT_MS

    private fun interpreter() = VolumeKeyInterpreter()

    // --- Pulsacion simple ---------------------------------------------------

    @Test
    fun `una pulsacion de subir cambia a la siguiente pista`() {
        val interpreter = interpreter()

        val immediate = interpreter.onKeyPress(VolumeKey.UP, 0L)
        assertEquals(emptyList<VolumeKeyAction>(), immediate)

        assertEquals(listOf(VolumeKeyAction.NEXT_TRACK), interpreter.onTimeout(window))
    }

    @Test
    fun `una pulsacion de bajar vuelve a la pista anterior`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.DOWN, 0L)

        assertEquals(listOf(VolumeKeyAction.PREVIOUS_TRACK), interpreter.onTimeout(window))
    }

    @Test
    fun `antes de agotarse la ventana no hay accion inmediata`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.UP, 0L)

        assertEquals(emptyList<VolumeKeyAction>(), interpreter.onTimeout(window - 1))
    }

    // --- Doble pulsacion y modo volumen -------------------------------------

    @Test
    fun `doble pulsacion rapida de subir entra en modo volumen`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.UP, 0L)
        val second = interpreter.onKeyPress(VolumeKey.UP, 200L)

        assertEquals(listOf(VolumeKeyAction.VOLUME_UP), second)
        assertTrue(interpreter.isVolumeModeActive(200L))
    }

    @Test
    fun `doble pulsacion rapida de bajar ajusta el volumen a la baja`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.DOWN, 0L)
        val second = interpreter.onKeyPress(VolumeKey.DOWN, 200L)

        assertEquals(listOf(VolumeKeyAction.VOLUME_DOWN), second)
    }

    @Test
    fun `la doble pulsacion en el limite exacto de la ventana cuenta`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.UP, 0L)
        val second = interpreter.onKeyPress(VolumeKey.UP, window)

        assertEquals(listOf(VolumeKeyAction.VOLUME_UP), second)
    }

    @Test
    fun `en modo volumen cada pulsacion ajusta el volumen`() {
        val interpreter = interpreter()
        interpreter.onKeyPress(VolumeKey.UP, 0L)
        interpreter.onKeyPress(VolumeKey.UP, 200L) // entra en modo volumen

        assertEquals(listOf(VolumeKeyAction.VOLUME_UP), interpreter.onKeyPress(VolumeKey.UP, 1_000L))
        assertEquals(listOf(VolumeKeyAction.VOLUME_DOWN), interpreter.onKeyPress(VolumeKey.DOWN, 1_500L))
    }

    @Test
    fun `no hay temporizador pendiente en modo volumen`() {
        val interpreter = interpreter()
        interpreter.onKeyPress(VolumeKey.UP, 0L)
        interpreter.onKeyPress(VolumeKey.UP, 200L)

        assertNull(interpreter.nextTimeoutAtMs())
    }

    @Test
    fun `tras el tiempo de espera vuelve a cambiar de pista`() {
        val interpreter = interpreter()
        interpreter.onKeyPress(VolumeKey.UP, 0L)
        interpreter.onKeyPress(VolumeKey.UP, 200L) // modo volumen

        val afterTimeout = 200L + timeout + 1L
        assertFalse(interpreter.isVolumeModeActive(afterTimeout))

        // La primera pulsacion despues del tiempo de espera ya vuelve a ser simple.
        assertEquals(emptyList<VolumeKeyAction>(), interpreter.onKeyPress(VolumeKey.UP, afterTimeout))
        assertEquals(listOf(VolumeKeyAction.NEXT_TRACK), interpreter.onTimeout(afterTimeout + window))
    }

    // --- Botones distintos y ventanas ---------------------------------------

    @Test
    fun `botones distintos no forman doble pulsacion`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.UP, 0L)
        // Bajar llega dentro de la ventana: resuelve la pendiente como simple.
        assertEquals(listOf(VolumeKeyAction.NEXT_TRACK), interpreter.onKeyPress(VolumeKey.DOWN, 100L))
        // Y la nueva queda pendiente, resolviendose como anterior.
        assertEquals(listOf(VolumeKeyAction.PREVIOUS_TRACK), interpreter.onTimeout(100L + window))
    }

    @Test
    fun `una segunda pulsacion fuera de la ventana resuelve la primera`() {
        val interpreter = interpreter()

        interpreter.onKeyPress(VolumeKey.UP, 0L)
        val late = interpreter.onKeyPress(VolumeKey.UP, window + 50L)

        assertEquals(listOf(VolumeKeyAction.NEXT_TRACK), late)
        assertEquals(listOf(VolumeKeyAction.NEXT_TRACK), interpreter.onTimeout(window + 50L + window))
    }

    // --- Temporizador -------------------------------------------------------

    @Test
    fun `nextTimeoutAtMs devuelve la ventana pendiente y luego null`() {
        val interpreter = interpreter()

        assertNull(interpreter.nextTimeoutAtMs())
        interpreter.onKeyPress(VolumeKey.UP, 1_000L)
        assertEquals(1_000L + window, interpreter.nextTimeoutAtMs())
        interpreter.onTimeout(1_000L + window)
        assertNull(interpreter.nextTimeoutAtMs())
    }

    // --- Limpieza y validacion ----------------------------------------------

    @Test
    fun `reset limpia el estado interno`() {
        val interpreter = interpreter()
        interpreter.onKeyPress(VolumeKey.UP, 0L)
        interpreter.onKeyPress(VolumeKey.UP, 200L) // modo volumen

        interpreter.reset()

        assertFalse(interpreter.isVolumeModeActive(200L))
        assertNull(interpreter.nextTimeoutAtMs())
    }

    @Test
    fun `una configuracion invalida se rechaza`() {
        assertThrows(IllegalArgumentException::class.java) {
            VolumeKeyInterpreter(doublePressWindowMs = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            VolumeKeyInterpreter(volumeModeTimeoutMs = 0L)
        }
    }
}
