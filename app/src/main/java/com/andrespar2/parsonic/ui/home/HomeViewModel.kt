package com.andrespar2.parsonic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Intent
import android.provider.Settings
import com.andrespar2.parsonic.data.media.NowPlaying
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.service.GestureRuntimeState
import com.andrespar2.parsonic.service.GestureService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de inicio: preferencias, reproduccion actual y estado en
 * vivo del reconocimiento.
 */
class HomeViewModel(private val container: AppContainer) : ViewModel() {

    data class UiState(
        val settings: AppSettings = AppSettings.DEFAULT,
        val nowPlaying: NowPlaying? = null,
        val runtime: GestureRuntimeState.Status = GestureRuntimeState.Status()
    )

    val uiState: StateFlow<UiState> = combine(
        container.settingsRepository.settings,
        container.mediaRepository.nowPlaying,
        GestureRuntimeState.status
    ) { settings, nowPlaying, runtime ->
        UiState(settings = settings, nowPlaying = nowPlaying, runtime = runtime)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState()
    )

    /** Activa o desactiva el control por gestos. */
    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setServiceEnabled(enabled)
            if (enabled) {
                GestureService.start(container.appContext)
            }
        }
    }

    /** Activa o desactiva el control con los botones de volumen. */
    fun setVolumeKeysEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setVolumeKeysEnabled(enabled) }
    }

    /** Abre los ajustes de accesibilidad del sistema. */
    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { container.appContext.startActivity(intent) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
