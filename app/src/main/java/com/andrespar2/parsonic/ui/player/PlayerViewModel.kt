package com.andrespar2.parsonic.ui.player

import androidx.lifecycle.ViewModel
import com.andrespar2.parsonic.data.media.MediaSessionRepository
import com.andrespar2.parsonic.data.media.NowPlaying
import com.andrespar2.parsonic.data.media.VolumeController
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.service.dispatcher.GestureActionDispatcher
import kotlinx.coroutines.flow.StateFlow

/**
 * Pantalla del reproductor: muestra lo que suena y ofrece los mismos comandos
 * que los gestos, para poder probarlos sin configurar nada.
 */
class PlayerViewModel(private val container: AppContainer) : ViewModel() {

    private val mediaRepository: MediaSessionRepository = container.mediaRepository
    private val volumeController: VolumeController = container.volumeController

    val nowPlaying: StateFlow<NowPlaying?> = mediaRepository.nowPlaying

    fun togglePlayPause() = mediaRepository.togglePlayPause()

    fun next() = mediaRepository.skipToNext()

    fun previous() = mediaRepository.skipToPrevious()

    fun seekForward() = mediaRepository.seekForward(GestureActionDispatcher.DEFAULT_SEEK_DELTA_MS)

    fun seekBackward() = mediaRepository.seekBackward(GestureActionDispatcher.DEFAULT_SEEK_DELTA_MS)

    fun volumeUp() = volumeController.raise()

    fun volumeDown() = volumeController.lower()
}
