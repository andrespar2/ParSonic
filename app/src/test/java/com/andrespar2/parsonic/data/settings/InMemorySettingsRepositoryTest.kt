package com.andrespar2.parsonic.data.settings

import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemorySettingsRepositoryTest {

    @Test
    fun `aplica los cambios de forma inmediata`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setServiceEnabled(true)
        assertTrue(repository.settings.first().serviceEnabled)
    }

    @Test
    fun `updateBinding delega en update`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setGestureAction(GestureType.DOUBLE_TAP, GestureAction.PLAY)
        assertEquals(
            GestureAction.PLAY,
            repository.settings.first().bindingOf(GestureType.DOUBLE_TAP).action
        )
    }

    @Test
    fun `resetGestures restaura los valores de fabrica`() = runTest {
        val repository = InMemorySettingsRepository()
        repository.setGestureEnabled(GestureType.SWIPE_LEFT, false)
        assertFalse(repository.settings.first().bindingOf(GestureType.SWIPE_LEFT).enabled)
        repository.resetGestures()
        assertTrue(repository.settings.first().bindingOf(GestureType.SWIPE_LEFT).enabled)
    }
}
