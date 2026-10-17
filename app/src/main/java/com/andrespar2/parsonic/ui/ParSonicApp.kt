package com.andrespar2.parsonic.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.ui.compatibility.CompatibilityScreen
import com.andrespar2.parsonic.ui.credits.CreditsScreen
import com.andrespar2.parsonic.ui.gestures.GesturesScreen
import com.andrespar2.parsonic.ui.home.HomeScreen
import com.andrespar2.parsonic.ui.navigation.Destination
import com.andrespar2.parsonic.ui.permissions.PermissionsScreen
import com.andrespar2.parsonic.ui.player.PlayerScreen
import com.andrespar2.parsonic.ui.settings.SettingsScreen

/**
 * Raiz de la interfaz de ParSonic: barra inferior, barra superior contextual y
 * grafo de navegacion.
 *
 * Mientras la interfaz esta visible se registra un consumidor del estado
 * multimedia; al pasar a segundo plano se libera. Asi la app no mantiene el
 * callback del sistema encendido sin necesidad.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParSonicApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentDestination = Destination.fromRoute(currentRoute)

    LifecycleEventEffect(Lifecycle.Event.ON_START) { container.acquireMedia() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { container.releaseMedia() }

    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentDestination?.label ?: "ParSonic",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (currentDestination?.topLevel == false) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background,
                    titleContentColor = colors.onBackground,
                    navigationIconContentColor = colors.onBackground
                )
            )
        },
        bottomBar = {
            if (currentDestination?.topLevel == true) {
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    Destination.topLevelDestinations.forEach { destination ->
                        val selected = currentDestination == destination
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(navController, destination) },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.onPrimary,
                                selectedTextColor = colors.primary,
                                indicatorColor = colors.primary,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HOME.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.HOME.route) {
                HomeScreen(container) { destination -> navController.navigateTo(destination) }
            }
            composable(Destination.GESTURES.route) {
                GesturesScreen(container)
            }
            composable(Destination.PLAYER.route) {
                PlayerScreen(container)
            }
            composable(Destination.SETTINGS.route) {
                SettingsScreen(container) { destination -> navController.navigateTo(destination) }
            }
            composable(Destination.PERMISSIONS.route) {
                PermissionsScreen()
            }
            composable(Destination.COMPATIBILITY.route) {
                CompatibilityScreen()
            }
            composable(Destination.CREDITS.route) {
                CreditsScreen()
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateTo(destination: Destination) {
    if (currentDestination?.route == destination.route) return
    navigate(destination.route) { launchSingleTop = true }
}

private fun navigateToTopLevel(
    navController: androidx.navigation.NavHostController,
    destination: Destination
) {
    if (navController.currentDestination?.route == destination.route) return
    navController.navigate(destination.route) {
        popUpTo(Destination.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
