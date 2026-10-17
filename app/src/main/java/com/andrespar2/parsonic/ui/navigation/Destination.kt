package com.andrespar2.parsonic.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de navegacion de ParSonic.
 *
 * Los cuatro primeros son las pestanas inferiores; el resto se abre desde
 * enlaces dentro de las pantallas, por eso se marcan con [topLevel] en falso.
 */
enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val topLevel: Boolean
) {
    HOME("home", "Inicio", Icons.Filled.Home, true),
    GESTURES("gestures", "Gestos", Icons.Filled.Build, true),
    PLAYER("player", "Reproductor", Icons.Filled.PlayArrow, true),
    SETTINGS("settings", "Ajustes", Icons.Filled.Settings, true),
    PERMISSIONS("permissions", "Permisos", Icons.Filled.Lock, false),
    COMPATIBILITY("compatibility", "Compatibilidad", Icons.Filled.Info, false),
    CREDITS("credits", "Creditos", Icons.Filled.Star, false);

    companion object {
        /** Pestanas que aparecen en la barra inferior. */
        val topLevelDestinations: List<Destination> = entries.filter { it.topLevel }

        fun fromRoute(route: String?): Destination? = entries.firstOrNull { it.route == route }
    }
}
