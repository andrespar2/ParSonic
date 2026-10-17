package com.andrespar2.parsonic.service.dispatcher

import com.andrespar2.parsonic.data.media.MediaSessionRepository
import com.andrespar2.parsonic.data.media.NowPlaying
import com.andrespar2.parsonic.data.media.VolumeController
import com.andrespar2.parsonic.data.settings.model.GestureAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureActionDispatcherTest {

    private class FakeMediaRepository : MediaSessionRepository {
        val calls = mutableListOf<String>()
        override val nowPlaying: StateFlow<NowPlaying?> = MutableStateFlow(null)
        override val isAvailable: StateFlow<Boolean> = MutableStateFlow(true)
        override fun start() = Unit
        override fun stop() = Unit
        override fun refresh() = Unit
        override fun play() { calls.add("play") }
        override fun pause() { calls.add("pause") }
        override fun togglePlayPause() { calls.add("toggle") }
        override fun skipToNext() { calls.add("next") }
        override fun skipToPrevious() { calls.add("previous") }
        override fun stopPlayback() { calls.add("stop") }
        override fun seekForward(deltaMs: Long) { calls.add("seekForward:$deltaMs") }
        override fun seekBackward(deltaMs: Long) { calls.add("seekBackward:$deltaMs") }
    }

    private class FakeVolumeController : VolumeController {
        val calls = mutableListOf<String>()
        override val isAvailable: Boolean = true
        override fun raise() { calls.add("raise") }
        override fun lower() { calls.add("lower") }
        override fun currentPercent(): Int = 50
    }

    private val media = FakeMediaRepository()
    private val volume = FakeVolumeController()
    private val dispatcher = GestureActionDispatcher(media, volume)

    @Test
    fun `ninguna accion no hace nada`() {
        dispatcher.dispatch(GestureAction.NONE)
        assertEquals(emptyList<String>(), media.calls)
        assertEquals(emptyList<String>(), volume.calls)
    }

    @Test
    fun `las acciones de transporte llegan al reproductor`() {
        dispatcher.dispatch(GestureAction.PLAY)
        dispatcher.dispatch(GestureAction.PAUSE)
        dispatcher.dispatch(GestureAction.TOGGLE_PLAY_PAUSE)
        dispatcher.dispatch(GestureAction.NEXT)
        dispatcher.dispatch(GestureAction.PREVIOUS)
        dispatcher.dispatch(GestureAction.STOP)

        assertEquals(
            listOf("play", "pause", "toggle", "next", "previous", "stop"),
            media.calls
        )
        assertEquals(emptyList<String>(), volume.calls)
    }

    @Test
    fun `las acciones de volumen llegan al controlador de volumen`() {
        dispatcher.dispatch(GestureAction.VOLUME_UP)
        dispatcher.dispatch(GestureAction.VOLUME_DOWN)

        assertEquals(listOf("raise", "lower"), volume.calls)
        assertEquals(emptyList<String>(), media.calls)
    }

    @Test
    fun `el desplazamiento usa el delta configurado`() {
        GestureActionDispatcher(media, volume, seekDeltaMs = 500L)
            .dispatch(GestureAction.SEEK_FORWARD)
        GestureActionDispatcher(media, volume, seekDeltaMs = 500L)
            .dispatch(GestureAction.SEEK_BACKWARD)

        assertTrue(media.calls.containsAll(listOf("seekForward:500", "seekBackward:500")))
    }

    @Test
    fun `el desplazamiento por defecto es de diez segundos`() {
        dispatcher.dispatch(GestureAction.SEEK_FORWARD)
        assertEquals(listOf("seekForward:10000"), media.calls)
    }
}
