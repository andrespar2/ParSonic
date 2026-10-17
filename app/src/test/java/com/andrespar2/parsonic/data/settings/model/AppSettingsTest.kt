package com.andrespar2.parsonic.data.settings.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `los valores por defecto definen todos los gestos`() {
        val settings = AppSettings.DEFAULT
        GestureType.entries.forEach { type ->
            assertEquals(type, settings.bindingOf(type).type)
        }
    }

    @Test
    fun `bindingOf devuelve el valor de fabrica cuando falta el gesto`() {
        val settings = AppSettings.DEFAULT.copy(bindings = emptyMap())
        val binding = settings.bindingOf(GestureType.DOUBLE_TAP)
        assertEquals(GestureAction.TOGGLE_PLAY_PAUSE, binding.action)
        assertTrue(binding.enabled)
    }

    @Test
    fun `los gestos sin accion por defecto empiezan desactivados`() {
        assertEquals(GestureAction.NONE, GestureType.SINGLE_TAP.defaultAction)
        assertFalse(AppSettings.defaultBinding(GestureType.SINGLE_TAP).enabled)
        assertFalse(AppSettings.defaultBinding(GestureType.LONG_PRESS).enabled)
    }

    @Test
    fun `isEffective exige gesto activo y accion distinta de NONE`() {
        val base = AppSettings.defaultBinding(GestureType.DOUBLE_TAP)
        assertTrue(base.isEffective)
        assertFalse(base.copy(action = GestureAction.NONE).isEffective)
        assertFalse(base.copy(enabled = false).isEffective)
    }

    @Test
    fun `las vistas de gestos respetan el origen`() {
        val settings = AppSettings.DEFAULT
        assertTrue(settings.touchBindings.all { it.type.source == GestureSource.TOUCH })
        assertTrue(settings.sensorBindings.all { it.type.source == GestureSource.SENSOR })
        assertEquals(GestureType.entries.size, settings.touchBindings.size + settings.sensorBindings.size)
    }

    @Test
    fun `hasAnyActiveTouchGesture refleja el estado real`() {
        val withoutTouch = AppSettings.DEFAULT.copy(
            bindings = AppSettings.defaultBindings().mapValues { (type, binding) ->
                if (type.source == GestureSource.TOUCH) binding.copy(enabled = false) else binding
            }
        )
        assertFalse(withoutTouch.hasAnyActiveTouchGesture)
        assertTrue(AppSettings.DEFAULT.hasAnyActiveTouchGesture)
    }

    @Test
    fun `por defecto la app arranca desactivada y con ahorro de bateria`() {
        val settings = AppSettings.DEFAULT
        assertFalse(settings.serviceEnabled)
        assertFalse(settings.autostartOnBoot)
        assertTrue(settings.onlyWhilePlaying)
        assertFalse("Los sensores son opcionales y de consumo adicional", settings.sensorGesturesEnabled)
        assertFalse("Los botones de volumen son opcionales", settings.volumeKeysEnabled)
    }

    @Test
    fun `copy permite modificar un solo gesto sin afectar al resto`() {
        val original = AppSettings.DEFAULT
        val updated = original.copy(
            bindings = original.bindings + (
                GestureType.SWIPE_LEFT to original.bindingOf(GestureType.SWIPE_LEFT)
                    .copy(action = GestureAction.PAUSE)
                )
        )
        assertEquals(GestureAction.PAUSE, updated.bindingOf(GestureType.SWIPE_LEFT).action)
        assertEquals(
            GestureAction.NEXT,
            updated.bindingOf(GestureType.SWIPE_RIGHT).action
        )
    }
}
