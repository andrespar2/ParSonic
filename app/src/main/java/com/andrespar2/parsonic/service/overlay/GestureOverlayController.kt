package com.andrespar2.parsonic.service.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.core.util.Logg
import com.andrespar2.parsonic.core.util.ScreenGeometry
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import com.andrespar2.parsonic.service.gesture.GestureInterpreter
import com.andrespar2.parsonic.service.gesture.GestureZone
import com.andrespar2.parsonic.service.gesture.GestureZoneCalculator
import com.andrespar2.parsonic.service.gesture.GestureZoneSide

/**
 * Crea y destruye las ventanas de superposicion de las zonas de gesto.
 *
 * Se usa mas de una ventana a proposito: cada franja es pequena y transparente,
 * y como se marcan como "no modales" los toques fuera de ellas siguen llegando
 * con normalidad a la aplicacion que haya debajo. Asi ParSonic no secuestra toda
 * la pantalla, solo sus bordes.
 *
 * Toda la clase debe usarse desde el hilo principal (`WindowManager` lo exige).
 */
class GestureOverlayController(
    context: Context,
    private val onGesture: (GestureType) -> Unit
) {

    /**
     * Configuracion deseada de la captura tactil.
     *
     * Se compara por igualdad para no reconstruir las ventanas si nada ha
     * cambiado: quitar y volver a poner vistas en cada emision provocaria
     * parpadeos y trabajo inutil.
     */
    data class Config(
        val active: Boolean,
        val edgeZoneWidthDp: Int,
        val bottomZoneEnabled: Boolean,
        val sensitivity: Sensitivity,
        val showHints: Boolean,
        val geometry: ScreenGeometry
    )

    private val appContext = context.applicationContext
    private val windowManager =
        appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
    private val views = mutableMapOf<GestureZoneSide, EdgeZoneView>()
    private var applied: Config? = null

    /** Aplica la configuracion; reconstruye las ventanas solo si ha cambiado. */
    fun update(config: Config) {
        if (config == applied) return
        applied = config
        removeAll()
        if (!config.active) return

        val manager = windowManager ?: return
        val zones = GestureZoneCalculator.zones(
            displayWidthPx = config.geometry.widthPx,
            displayHeightPx = config.geometry.heightPx,
            edgeZoneWidthDp = config.edgeZoneWidthDp,
            bottomZoneHeightDp = ParSonicConstants.DEFAULT_BOTTOM_ZONE_HEIGHT_DP,
            includeBottom = config.bottomZoneEnabled,
            leftInset = config.geometry.leftInset,
            topInset = config.geometry.topInset,
            rightInset = config.geometry.rightInset,
            bottomInset = config.geometry.bottomInset,
            density = config.geometry.density
        )
        zones.forEach { zone -> addZone(manager, zone, config) }
    }

    /** Quita todas las ventanas y olvida la configuracion aplicada. */
    fun release() {
        applied = null
        removeAll()
    }

    private fun addZone(manager: WindowManager, zone: GestureZone, config: Config) {
        val interpreter = GestureInterpreter(
            GestureInterpreter.configFor(config.sensitivity, config.geometry.density)
        )
        val view = EdgeZoneView(appContext, interpreter, config.showHints).apply {
            onGesture = this@GestureOverlayController.onGesture
        }
        runCatching { manager.addView(view, layoutParams(zone)) }
            .onSuccess { views[zone.side] = view }
            .onFailure { Logg.w(TAG, "No se pudo anadir la zona ${zone.side}", it) }
    }

    private fun removeAll() {
        val manager = windowManager
        if (manager != null) {
            views.values.forEach { view ->
                runCatching { manager.removeView(view) }
                    .onFailure { Logg.w(TAG, "No se pudo quitar una zona de gesto", it) }
            }
        }
        views.clear()
    }

    private fun layoutParams(zone: GestureZone): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            zone.width,
            zone.height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = zone.left
            y = zone.top
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

    private companion object {
        const val TAG = "GestureOverlay"
    }
}
