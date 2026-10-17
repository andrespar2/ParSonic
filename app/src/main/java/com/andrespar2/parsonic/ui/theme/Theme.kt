package com.andrespar2.parsonic.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Esquema oscuro unico de ParSonic.
 *
 * La app no ofrece tema claro a proposito: el negro puro es lo que reduce el
 * consumo en pantallas OLED y forma parte de la identidad visual.
 */
private val ParSonicColorScheme = darkColorScheme(
    primary = ParRed,
    onPrimary = ParWhite,
    primaryContainer = ParRedDark,
    onPrimaryContainer = ParWhite,
    secondary = ParGrey,
    onSecondary = ParBlack,
    secondaryContainer = ParSurfaceElevated,
    onSecondaryContainer = ParWhite,
    tertiary = ParBlue,
    onTertiary = ParBlack,
    background = ParBlack,
    onBackground = ParWhite,
    surface = ParSurface,
    onSurface = ParWhite,
    surfaceVariant = ParSurfaceElevated,
    onSurfaceVariant = ParGrey,
    surfaceContainer = ParSurface,
    surfaceContainerHigh = ParSurfaceElevated,
    surfaceContainerHighest = ParSurfaceElevated,
    surfaceContainerLow = ParSurface,
    surfaceContainerLowest = ParBlack,
    outline = ParBorder,
    outlineVariant = ParBorderStrong,
    error = ParRed,
    onError = ParWhite,
    scrim = ParBlack,
    inverseSurface = ParWhite,
    inverseOnSurface = ParBlack
)

@Composable
fun ParSonicTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val context = LocalContext.current
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                // Edge-to-edge con iconos claros sobre negro.
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = ParSonicColorScheme,
        typography = ParTypography,
        content = content
    )
}
