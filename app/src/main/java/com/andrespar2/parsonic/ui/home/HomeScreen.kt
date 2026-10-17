package com.andrespar2.parsonic.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrespar2.parsonic.data.media.NowPlaying
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.service.GestureRuntimeState
import com.andrespar2.parsonic.ui.ParSonicViewModelFactory
import com.andrespar2.parsonic.ui.components.InfoBanner
import com.andrespar2.parsonic.ui.components.KeyValueRow
import com.andrespar2.parsonic.ui.components.ParDivider
import com.andrespar2.parsonic.ui.components.ParSonicCard
import com.andrespar2.parsonic.ui.components.SectionTitle
import com.andrespar2.parsonic.ui.components.StatusPill
import com.andrespar2.parsonic.ui.navigation.Destination
import com.andrespar2.parsonic.ui.theme.ParAmber
import com.andrespar2.parsonic.ui.theme.ParGreen
import com.andrespar2.parsonic.ui.theme.ParGreyDim
import com.andrespar2.parsonic.ui.theme.ParRed
import com.andrespar2.parsonic.ui.util.rememberPermissionSnapshot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star

@Composable
fun HomeScreen(
    container: AppContainer,
    onNavigate: (Destination) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = remember(container) { ParSonicViewModelFactory(container) }
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissions = rememberPermissionSnapshot()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        MasterCard(
            settings = state.settings,
            runtime = state.runtime,
            onToggle = viewModel::setServiceEnabled
        )

        if (state.settings.serviceEnabled && !permissions.overlay && state.settings.touchGesturesEnabled) {
            Spacer(Modifier.height(12.dp))
            InfoBanner(
                text = "Falta el permiso \"Dibujar sobre otras apps\": sin el, ParSonic no puede " +
                    "capturar los gestos tactiles sobre otros reproductores.",
                tone = ParAmber,
                icon = Icons.Filled.Lock
            )
        }

        if (state.settings.serviceEnabled && !permissions.notificationAccess) {
            Spacer(Modifier.height(12.dp))
            InfoBanner(
                text = "Falta el acceso a notificaciones: sin el, Android no deja a ParSonic ver " +
                    "que reproductor esta activo.",
                tone = ParAmber,
                icon = Icons.Filled.Info
            )
        }

        SectionTitle("Botones de volumen")
        VolumeKeysCard(
            enabled = state.settings.volumeKeysEnabled,
            accessibilityGranted = permissions.accessibility,
            onToggle = viewModel::setVolumeKeysEnabled,
            onOpenAccessibility = viewModel::openAccessibilitySettings
        )

        SectionTitle("Ahora suena")
        NowPlayingCard(state.nowPlaying)

        SectionTitle("Estado del reconocimiento")
        RuntimeCard(state.settings, state.runtime)

        SectionTitle("Ajustes rapidos")
        QuickLinksCard(onNavigate)

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MasterCard(
    settings: AppSettings,
    runtime: GestureRuntimeState.Status,
    onToggle: (Boolean) -> Unit
) {
    ParSonicCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Control por gestos",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    text = if (settings.serviceEnabled && runtime.serviceRunning) "Activo" else "Desactivado",
                    color = if (settings.serviceEnabled && runtime.serviceRunning) ParGreen else ParGreyDim
                )
            }
            Switch(checked = settings.serviceEnabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun VolumeKeysCard(
    enabled: Boolean,
    accessibilityGranted: Boolean,
    onToggle: (Boolean) -> Unit,
    onOpenAccessibility: () -> Unit
) {
    ParSonicCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Subir = siguiente · Bajar = anterior",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Doble pulsacion rapida: ajustar el volumen (vuelve a cambiar de " +
                        "cancion a los 7 segundos). Funciona con la pantalla apagada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                StatusPill(
                    text = when {
                        !enabled -> "Desactivado"
                        accessibilityGranted -> "Activo"
                        else -> "Falta la accesibilidad"
                    },
                    color = when {
                        !enabled -> ParGreyDim
                        accessibilityGranted -> ParGreen
                        else -> ParAmber
                    }
                )
            }
            Spacer(Modifier.width(14.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }

        if (enabled && !accessibilityGranted) {
            Spacer(Modifier.height(12.dp))
            InfoBanner(
                text = "Para usar los botones de volumen hay que activar el servicio de " +
                    "accesibilidad de ParSonic (solo filtra las teclas de volumen).",
                tone = ParAmber,
                icon = Icons.Filled.Lock
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir ajustes de accesibilidad")
            }
        }
    }
}

@Composable
private fun NowPlayingCard(nowPlaying: NowPlaying?) {
    ParSonicCard {
        if (nowPlaying == null) {
            Text(
                text = "Sin reproduccion activa",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Abre Spotify, YouTube Music, VLC o cualquier reproductor y da al play.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@ParSonicCard
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Artwork(nowPlaying)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = nowPlaying.title.ifBlank { "Sin titulo" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = nowPlaying.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = nowPlaying.appLabel,
                        color = ParRed
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = nowPlaying.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun Artwork(nowPlaying: NowPlaying) {
    val shape = RoundedCornerShape(14.dp)
    val bitmap = nowPlaying.artwork
    if (bitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(64.dp).clip(shape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nowPlaying.appLabel.take(1).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
                color = ParRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RuntimeCard(settings: AppSettings, runtime: GestureRuntimeState.Status) {
    ParSonicCard {
        KeyValueRow(
            key = "Captura tactil",
            value = when {
                !settings.serviceEnabled -> "Desactivada"
                runtime.touchActive -> "Activa"
                settings.onlyWhilePlaying && !runtime.playing -> "En espera de reproduccion"
                !settings.touchGesturesEnabled -> "Desactivada en ajustes"
                else -> "Inactiva"
            }
        )
        ParDivider()
        KeyValueRow(
            key = "Sensores",
            value = when {
                !settings.serviceEnabled -> "Desactivados"
                runtime.sensorActive -> "Activos"
                settings.onlyWhilePlaying && !runtime.playing -> "En espera de reproduccion"
                !settings.sensorGesturesEnabled -> "Desactivados en ajustes"
                else -> "Inactivos"
            }
        )
        ParDivider()
        KeyValueRow(
            key = "Ahorro de bateria",
            value = if (settings.onlyWhilePlaying) "Solo mientras suena" else "Siempre activo"
        )
        runtime.lastGesture?.let { gesture ->
            ParDivider()
            KeyValueRow(key = "Ultimo gesto", value = gesture.label)
        }
    }
}

@Composable
private fun QuickLinksCard(onNavigate: (Destination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ParSonicCard(onClick = { onNavigate(Destination.PERMISSIONS) }) {
            LinkRow(Icons.Filled.Lock, "Permisos", "Accesos que ParSonic necesita")
        }
        ParSonicCard(onClick = { onNavigate(Destination.COMPATIBILITY) }) {
            LinkRow(Icons.Filled.Info, "Compatibilidad", "Que funciona y que limita Android")
        }
        ParSonicCard(onClick = { onNavigate(Destination.CREDITS) }) {
            LinkRow(Icons.Filled.Star, "Creditos", "Desarrollado por andrespar2")
        }
    }
}

@Composable
private fun LinkRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ParRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
