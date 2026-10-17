package com.andrespar2.parsonic.data.media

import android.content.Context
import android.media.AudioManager

/**
 * Control del volumen multimedia del dispositivo.
 *
 * No se usa el volumen de la sesion (`MediaController.getPlaybackInfo`) porque
 * la mayoria de reproductores no lo exponen a terceros, mientras que
 * `AudioManager` funciona siempre y respeta la interfaz de volumen del sistema.
 */
interface VolumeController {
    /** `true` si el dispositivo permite ajustar el volumen multimedia. */
    val isAvailable: Boolean

    /** Sube un escalon el volumen multimedia. */
    fun raise()

    /** Baja un escalon el volumen multimedia. */
    fun lower()

    /** Volumen multimedia actual en porcentaje (0-100). */
    fun currentPercent(): Int
}

/** Implementacion sobre `AudioManager`. */
class AndroidVolumeController(context: Context) : VolumeController {

    private val audioManager: AudioManager? =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    override val isAvailable: Boolean
        get() = audioManager != null

    override fun raise() = adjust(direction = AudioManager.ADJUST_RAISE)

    override fun lower() = adjust(direction = AudioManager.ADJUST_LOWER)

    override fun currentPercent(): Int {
        val manager = audioManager ?: return 0
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0
        val current = manager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (current * 100) / max
    }

    private fun adjust(direction: Int) {
        val manager = audioManager ?: return
        runCatching {
            manager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
        }
    }
}
