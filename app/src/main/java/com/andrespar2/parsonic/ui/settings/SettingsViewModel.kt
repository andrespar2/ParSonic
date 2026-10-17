package com.andrespar2.parsonic.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.service.GestureService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ajustes generales: servicio, inicio automatico y optimizacion de bateria. */
class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = AppSettings.DEFAULT
    )

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.setServiceEnabled(enabled)
            if (enabled) GestureService.start(container.appContext)
        }
    }

    fun setAutostartOnBoot(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setAutostartOnBoot(enabled) }
    }

    fun setOnlyWhilePlaying(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setOnlyWhilePlaying(enabled) }
    }

    /** Abre la pantalla de bateria del sistema, donde esta la optimizacion por app. */
    fun openBatterySettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { container.appContext.startActivity(intent) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
