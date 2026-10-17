package com.andrespar2.parsonic.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrespar2.parsonic.BuildConfig
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.ui.ParSonicViewModelFactory
import com.andrespar2.parsonic.ui.components.InfoBanner
import com.andrespar2.parsonic.ui.components.KeyValueRow
import com.andrespar2.parsonic.ui.components.ParDivider
import com.andrespar2.parsonic.ui.components.ParSonicCard
import com.andrespar2.parsonic.ui.components.SectionTitle
import com.andrespar2.parsonic.ui.components.SettingRow
import com.andrespar2.parsonic.ui.navigation.Destination
import com.andrespar2.parsonic.ui.theme.ParGreyDim
import com.andrespar2.parsonic.ui.theme.ParRed

@Composable
fun SettingsScreen(
    container: AppContainer,
    onNavigate: (Destination) -> Unit
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = remember(container) { ParSonicViewModelFactory(container) }
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        SectionTitle("Servicio")
        ParSonicCard {
            SettingRow(
                title = "Control por gestos",
                subtitle = "Interruptor maestro. Al desactivarlo se liberan permisos y sensores."
            ) {
                Switch(
                    checked = settings.serviceEnabled,
                    onCheckedChange = viewModel::setServiceEnabled
                )
            }
            ParDivider()
            SettingRow(
                title = "Iniciar al encender el dispositivo",
                subtitle = "Requiere que el control por gestos este activado."
            ) {
                Switch(
                    checked = settings.autostartOnBoot,
                    onCheckedChange = viewModel::setAutostartOnBoot
                )
            }
        }

        SectionTitle("Bateria")
        ParSonicCard {
            SettingRow(
                title = "Solo mientras suena musica",
                subtitle = "Recomendado. Mantiene el reconocimiento apagado cuando no hay " +
                    "reproduccion activa."
            ) {
                Switch(
                    checked = settings.onlyWhilePlaying,
                    onCheckedChange = viewModel::setOnlyWhilePlaying
                )
            }
        }

        SectionTitle("Informacion")
        ParSonicCard {
            KeyValueRow("Version", BuildConfig.VERSION_NAME)
            ParDivider()
            KeyValueRow("Paquete", BuildConfig.APPLICATION_ID)
        }

        SectionTitle("Mas")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ParSonicCard(onClick = { onNavigate(Destination.PERMISSIONS) }) {
                LinkRow(Icons.Filled.Lock, "Permisos", "Revisa los accesos concedidos")
            }
            ParSonicCard(onClick = { onNavigate(Destination.COMPATIBILITY) }) {
                LinkRow(Icons.Filled.Info, "Compatibilidad", "Que funciona en tu dispositivo")
            }
            ParSonicCard(onClick = { onNavigate(Destination.CREDITS) }) {
                LinkRow(Icons.Filled.Star, "Creditos", "Desarrollado por andrespar2")
            }
        }

        Spacer(Modifier.height(12.dp))
        InfoBanner(
            text = "ParSonic no usa servicios de accesibilidad, ni root, ni APIs privadas. Toda la " +
                "informacion sobre reproduccion se queda en tu dispositivo.",
            tone = ParGreyDim
        )

        Spacer(Modifier.height(10.dp))
        TextButton(onClick = viewModel::openBatterySettings, modifier = Modifier.fillMaxWidth()) {
            Text("Abrir ajustes de bateria del sistema", color = ParRed)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = ParRed, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
