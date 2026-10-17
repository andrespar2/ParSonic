package com.andrespar2.parsonic.core.util

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager

/**
 * Geometria aproximada de la pantalla fisica, con los *insets* de las barras de
 * sistema.
 *
 * Un servicio no tiene ventana propia, asi que no puede preguntar por sus
 * insets como haria una actividad. Se usa `WindowManager.getCurrentWindowMetrics`
 * (Android 11+) que si funciona desde cualquier contexto, y se recurre a
 * `getRealMetrics` mas una estimacion de las barras en versiones anteriores.
 *
 * Los *insets* se respetan a proposito: la franja de gestos no debe invadir la
 * barra de estado ni la de navegacion, para no impedir que el usuario despliegue
 * las notificaciones o use los botones del sistema.
 */
data class ScreenGeometry(
    val widthPx: Int,
    val heightPx: Int,
    val density: Float,
    val leftInset: Int,
    val topInset: Int,
    val rightInset: Int,
    val bottomInset: Int
) {
    companion object {

        fun of(context: Context): ScreenGeometry {
            val density = context.resources.displayMetrics.density
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                    ?: return fallback(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return runCatching {
                    val metrics = windowManager.currentWindowMetrics
                    val bounds = metrics.bounds
                    val insets = metrics.windowInsets.getInsetsIgnoringVisibility(
                        WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
                    )
                    ScreenGeometry(
                        widthPx = bounds.width(),
                        heightPx = bounds.height(),
                        density = density,
                        leftInset = insets.left,
                        topInset = insets.top,
                        rightInset = insets.right,
                        bottomInset = insets.bottom
                    )
                }.getOrElse { fallback(context) }
            }

            return legacy(context, windowManager, density)
        }

        private fun legacy(
            context: Context,
            windowManager: WindowManager,
            density: Float
        ): ScreenGeometry = runCatching {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
            ScreenGeometry(
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                density = density,
                leftInset = 0,
                topInset = systemDimension(context, "status_bar_height"),
                rightInset = 0,
                bottomInset = systemDimension(context, "navigation_bar_height")
            )
        }.getOrElse { fallback(context) }

        private fun fallback(context: Context): ScreenGeometry {
            val metrics = context.resources.displayMetrics
            return ScreenGeometry(
                widthPx = metrics.widthPixels,
                heightPx = metrics.heightPixels,
                density = metrics.density,
                leftInset = 0,
                topInset = 0,
                rightInset = 0,
                bottomInset = 0
            )
        }

        private fun systemDimension(context: Context, name: String): Int {
            val id = context.resources.getIdentifier(name, "dimen", "android")
            return if (id > 0) context.resources.getDimensionPixelSize(id) else 0
        }
    }
}
