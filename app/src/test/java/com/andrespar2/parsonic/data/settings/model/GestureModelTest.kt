package com.andrespar2.parsonic.data.settings.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariantes del modelo de gestos.
 *
 * Estos tests protegen una promesa de producto: ParSonic nunca debe declarar
 * que un gesto tactil funciona con la pantalla apagada.
 */
class GestureModelTest {

    @Test
    fun `todos los gestos tactiles y de sensor cubren el enum completo`() {
        val sum = GestureType.touchGestures.size + GestureType.sensorGestures.size
        assertEquals(GestureType.entries.size, sum)
    }

    @Test
    fun `ningun gesto aparece a la vez como tactil y como sensor`() {
        val touchIds = GestureType.touchGestures.map { it.id }.toSet()
        val sensorIds = GestureType.sensorGestures.map { it.id }.toSet()
        assertTrue(touchIds.intersect(sensorIds).isEmpty())
    }

    @Test
    fun `solo los gestos de sensor pueden funcionar con la pantalla apagada`() {
        GestureType.entries.filter { it.availableWithScreenOff }.forEach { type ->
            assertEquals(
                "El gesto ${type.id} declara pantalla apagada pero no es de sensor",
                GestureSource.SENSOR,
                type.source
            )
        }
    }

    @Test
    fun `ningun gesto tactil declara soporte con pantalla apagada`() {
        GestureType.touchGestures.forEach { type ->
            assertFalse(
                "Android no entrega eventos tactiles a las apps con la pantalla apagada",
                type.availableWithScreenOff
            )
        }
    }

    @Test
    fun `los identificadores de gesto son unicos`() {
        val ids = GestureType.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `fromId recupera el gesto correcto y devuelve null si no existe`() {
        assertEquals(GestureType.DOUBLE_TAP, GestureType.fromId("double_tap"))
        assertNull(GestureType.fromId("no_existe"))
        assertNull(GestureType.fromId(null))
    }

    @Test
    fun `los identificadores de accion son unicos`() {
        val ids = GestureAction.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `fromId de accion devuelve NONE ante valores desconocidos`() {
        assertEquals(GestureAction.NONE, GestureAction.fromId("desconocida"))
        assertEquals(GestureAction.NONE, GestureAction.fromId(null))
        assertEquals(GestureAction.NEXT, GestureAction.fromId("next"))
    }

    @Test
    fun `una sensibilidad mayor exige menos recorrido`() {
        assertTrue(Sensitivity.HIGH.swipeThresholdDp < Sensitivity.MEDIUM.swipeThresholdDp)
        assertTrue(Sensitivity.MEDIUM.swipeThresholdDp < Sensitivity.LOW.swipeThresholdDp)
    }

    @Test
    fun `fromId de sensibilidad devuelve MEDIUM ante valores desconocidos`() {
        assertEquals(Sensitivity.MEDIUM, Sensitivity.fromId("rara"))
        assertEquals(Sensitivity.MEDIUM, Sensitivity.fromId(null))
        assertEquals(Sensitivity.HIGH, Sensitivity.fromId("high"))
    }

    @Test
    fun `todos los gestos tienen etiqueta y descripcion`() {
        GestureType.entries.forEach { type ->
            assertTrue(type.label.isNotBlank())
            assertTrue(type.description.isNotBlank())
        }
        GestureAction.entries.forEach { action ->
            assertTrue(action.label.isNotBlank())
            assertTrue(action.description.isNotBlank())
        }
    }

    @Test
    fun `el gesto de doble toque viene activo con pausar y reproducir`() {
        val binding = AppSettings.defaultBinding(GestureType.DOUBLE_TAP)
        assertTrue(binding.enabled)
        assertEquals(GestureAction.TOGGLE_PLAY_PAUSE, binding.action)
    }

    @Test
    fun `la sensibilidad por defecto es media y es global`() {
        assertEquals(Sensitivity.MEDIUM, AppSettings.DEFAULT.sensitivity)
    }
}
