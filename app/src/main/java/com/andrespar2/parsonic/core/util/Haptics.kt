package com.andrespar2.parsonic.core.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Respuesta haptica breve de ParSonic.
 *
 * Se centraliza aqui para que la vibracion sea identica tanto para los gestos
 * tactiles como para los de sensor, y para respetar la preferencia del usuario
 * en un unico punto.
 */
object Haptics {

    /** Duracion en milisegundos del pulso de confirmacion. */
    private const val CONFIRM_DURATION_MS = 22L

    /** Amplitud suave, para no resultar molesto en gestos frecuentes. */
    private const val CONFIRM_AMPLITUDE = 120

    fun confirm(context: Context) {
        val vibrator = resolveVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        runCatching {
            vibrator.vibrate(
                VibrationEffect.createOneShot(CONFIRM_DURATION_MS, CONFIRM_AMPLITUDE)
            )
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
}
