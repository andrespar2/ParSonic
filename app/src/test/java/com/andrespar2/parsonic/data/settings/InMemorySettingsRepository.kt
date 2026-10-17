package com.andrespar2.parsonic.data.settings

import com.andrespar2.parsonic.data.settings.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Implementacion en memoria de [SettingsRepository] para pruebas y vistas
 * previas. No toca disco ni el framework de Android.
 */
class InMemorySettingsRepository(
    initial: AppSettings = AppSettings.DEFAULT
) : SettingsRepository {

    private val state = MutableStateFlow(initial)

    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.update(transform)
    }
}
