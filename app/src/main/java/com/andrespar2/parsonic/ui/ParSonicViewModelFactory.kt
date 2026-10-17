package com.andrespar2.parsonic.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.ui.gestures.GesturesViewModel
import com.andrespar2.parsonic.ui.home.HomeViewModel
import com.andrespar2.parsonic.ui.player.PlayerViewModel
import com.andrespar2.parsonic.ui.settings.SettingsViewModel

/**
 * Fabrica de ViewModels que inyecta el [AppContainer].
 *
 * Se hace asi en lugar de usar una libreria de inyeccion para no anadir
 * procesadores de anotaciones y mantener el grafo explicito.
 */
class ParSonicViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
        when {
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container)
            modelClass.isAssignableFrom(GesturesViewModel::class.java) -> GesturesViewModel(container)
            modelClass.isAssignableFrom(PlayerViewModel::class.java) -> PlayerViewModel(container)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
            else -> error("ViewModel no soportado: ${modelClass.name}")
        } as T
}
