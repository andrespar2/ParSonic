package com.andrespar2.parsonic.service.dispatcher

import com.andrespar2.parsonic.data.media.MediaSessionRepository
import com.andrespar2.parsonic.data.media.VolumeController
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureAction.NONE
import com.andrespar2.parsonic.data.settings.model.GestureAction.NEXT
import com.andrespar2.parsonic.data.settings.model.GestureAction.PAUSE
import com.andrespar2.parsonic.data.settings.model.GestureAction.PLAY
import com.andrespar2.parsonic.data.settings.model.GestureAction.PREVIOUS
import com.andrespar2.parsonic.data.settings.model.GestureAction.SEEK_BACKWARD
import com.andrespar2.parsonic.data.settings.model.GestureAction.SEEK_FORWARD
import com.andrespar2.parsonic.data.settings.model.GestureAction.STOP
import com.andrespar2.parsonic.data.settings.model.GestureAction.TOGGLE_PLAY_PAUSE
import com.andrespar2.parsonic.data.settings.model.GestureAction.VOLUME_DOWN
import com.andrespar2.parsonic.data.settings.model.GestureAction.VOLUME_UP

/**
 * Traduce [GestureAction] en comandos concretos sobre el reproductor o el
 * volumen del dispositivo.
 *
 * No accede directamente a AudioManager ni a MediaSessionManager: depende de
 * las abstracciones del propio proyecto. Esto la hace completamente testeable
 * con dobles en memoria.
 */
class GestureActionDispatcher(
    private val mediaRepository: MediaSessionRepository,
    private val volumeController: VolumeController,
    private val seekDeltaMs: Long = DEFAULT_SEEK_DELTA_MS
) {

    fun dispatch(action: GestureAction) {
        when (action) {
            NONE -> Unit
            PLAY -> mediaRepository.play()
            PAUSE -> mediaRepository.pause()
            TOGGLE_PLAY_PAUSE -> mediaRepository.togglePlayPause()
            NEXT -> mediaRepository.skipToNext()
            PREVIOUS -> mediaRepository.skipToPrevious()
            SEEK_FORWARD -> mediaRepository.seekForward(seekDeltaMs)
            SEEK_BACKWARD -> mediaRepository.seekBackward(seekDeltaMs)
            STOP -> mediaRepository.stopPlayback()
            VOLUME_UP -> volumeController.raise()
            VOLUME_DOWN -> volumeController.lower()
        }
    }

    companion object {
        const val DEFAULT_SEEK_DELTA_MS = 10_000L
    }
}
