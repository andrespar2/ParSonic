package com.andrespar2.parsonic.ui.gestures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsonic.data.settings.SettingsRepository
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import com.andrespar2.parsonic.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Expone las preferencias de gestos y las acciones para modificarlas.
 *
 * Todas las escrituras pasan por [AppContainer.settingsRepository], de modo que
 * el servicio de gestos reacciona a los cambios de forma automatica.
 */
class GesturesViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppSettings.DEFAULT
    )

    fun setTouchGesturesEnabled(enabled: Boolean) = update { it.setTouchGesturesEnabled(enabled) }

    fun setSensorGesturesEnabled(enabled: Boolean) = update { it.setSensorGesturesEnabled(enabled) }

    fun setBottomZoneEnabled(enabled: Boolean) = update { it.setBottomZoneEnabled(enabled) }

    fun setShowGestureHints(enabled: Boolean) = update { it.setShowGestureHints(enabled) }

    fun setHapticFeedback(enabled: Boolean) = update { it.setHapticFeedback(enabled) }

    fun setSensitivity(sensitivity: Sensitivity) = update { it.setSensitivity(sensitivity) }

    fun setEdgeZoneWidth(widthDp: Int) = update { it.setEdgeZoneWidth(widthDp) }

    fun setGestureEnabled(type: GestureType, enabled: Boolean) =
        update { it.setGestureEnabled(type, enabled) }

    fun setGestureAction(type: GestureType, action: GestureAction) =
        update { it.setGestureAction(type, action) }

    fun resetGestures() = update { it.resetGestures() }

    private fun update(block: suspend (SettingsRepository) -> Unit) {
        viewModelScope.launch { block(container.settingsRepository) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
