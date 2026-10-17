package com.andrespar2.parsonic.ui.gestures

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureBinding
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import com.andrespar2.parsonic.di.AppContainer
import com.andrespar2.parsonic.ui.ParSonicViewModelFactory
import com.andrespar2.parsonic.ui.components.InfoBanner
import com.andrespar2.parsonic.ui.components.ParSonicCard
import com.andrespar2.parsonic.ui.components.SectionTitle
import com.andrespar2.parsonic.ui.components.SettingRow
import com.andrespar2.parsonic.ui.theme.ParGreyDim
import com.andrespar2.parsonic.ui.theme.ParRed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning

@Composable
fun GesturesScreen(container: AppContainer) {
    val viewModel: GesturesViewModel = viewModel(
        factory = remember(container) { ParSonicViewModelFactory(container) }
    )
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var actionTarget by remember { mutableStateOf<GestureType?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        SectionTitle("Reconocimiento")
        ParSonicCard {
            SettingRow(
                title = "Gestos tactiles",
                subtitle = "Zonas en los bordes de la pantalla, encendida."
            ) {
                Switch(
                    checked = settings.touchGesturesEnabled,
                    onCheckedChange = viewModel::setTouchGesturesEnabled
                )
            }
            SettingRow(
                title = "Gestos de sensores",
                subtitle = "Agitar o girar el dispositivo."
            ) {
                Switch(
                    checked = settings.sensorGesturesEnabled,
                    onCheckedChange = viewModel::setSensorGesturesEnabled
                )
            }
            SettingRow(
                title = "Zona inferior",
                subtitle = "Anade una franja centrada en el borde inferior."
            ) {
                Switch(
                    checked = settings.bottomZoneEnabled,
                    onCheckedChange = viewModel::setBottomZoneEnabled
                )
            }
            SettingRow(
                title = "Pistas visuales",
                subtitle = "Un destello breve al reconocer un gesto."
            ) {
                Switch(
                    checked = settings.showGestureHints,
                    onCheckedChange = viewModel::setShowGestureHints
                )
            }
            SettingRow(
                title = "Vibracion",
                subtitle = "Respuesta haptica al reconocer un gesto."
            ) {
                Switch(
                    checked = settings.hapticFeedback,
                    onCheckedChange = viewModel::setHapticFeedback
                )
            }
        }

        SectionTitle("Sensibilidad")
        ParSonicCard {
            Text(
                text = "Sensibilidad global: ${settings.sensitivity.label}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Una sensibilidad mas alta exige menos recorrido y menos tiempo para " +
                    "reconocer un gesto.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            SensitivitySelector(
                current = settings.sensitivity,
                onSelect = viewModel::setSensitivity
            )
        }

        SectionTitle("Ancho de las zonas laterales")
        ParSonicCard {
            Text(
                text = "${settings.edgeZoneWidthDp} dp",
                style = MaterialTheme.typography.titleMedium,
                color = ParRed,
                fontWeight = FontWeight.Bold
            )
            Slider(
                value = settings.edgeZoneWidthDp.toFloat(),
                onValueChange = { viewModel.setEdgeZoneWidth(it.toInt()) },
                valueRange = ParSonicConstants.MIN_EDGE_ZONE_WIDTH_DP.toFloat()..
                    ParSonicConstants.MAX_EDGE_ZONE_WIDTH_DP.toFloat(),
                steps = (ParSonicConstants.MAX_EDGE_ZONE_WIDTH_DP -
                    ParSonicConstants.MIN_EDGE_ZONE_WIDTH_DP) / 4 - 1
            )
            Text(
                text = "Zonas mas anchas son mas faciles de acertar, pero bloquean mas pantalla.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionTitle("Gestos tactiles")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            settings.touchBindings.forEach { binding ->
                GestureCard(
                    binding = binding,
                    onEnabledChange = { viewModel.setGestureEnabled(binding.type, it) },
                    onEditAction = { actionTarget = binding.type }
                )
            }
        }

        SectionTitle("Gestos de sensor")
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            settings.sensorBindings.forEach { binding ->
                GestureCard(
                    binding = binding,
                    onEnabledChange = { viewModel.setGestureEnabled(binding.type, it) },
                    onEditAction = { actionTarget = binding.type }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        InfoBanner(
            text = "Desliza el dedo desde el borde dentro de la zona de gesto. El gesto \"atras\" " +
                "del sistema tambien usa los bordes: por eso el reconocimiento solo se activa " +
                "cuando hay musica sonando.",
            tone = ParGreyDim,
            icon = Icons.Filled.Warning
        )

        Spacer(Modifier.height(10.dp))
        TextButton(onClick = viewModel::resetGestures, modifier = Modifier.fillMaxWidth()) {
            Text("Restaurar gestos por defecto", color = ParRed)
        }

        Spacer(Modifier.height(24.dp))
    }

    actionTarget?.let { type ->
        ActionPickerDialog(
            type = type,
            current = settings.bindingOf(type).action,
            onDismiss = { actionTarget = null },
            onPick = { action ->
                viewModel.setGestureAction(type, action)
                actionTarget = null
            }
        )
    }
}

@Composable
private fun SensitivitySelector(current: Sensitivity, onSelect: (Sensitivity) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Sensitivity.entries.forEach { level ->
            val selected = level == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (selected) ParRed else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = { onSelect(level) }) {
                    Text(
                        text = level.label,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun GestureCard(
    binding: GestureBinding,
    onEnabledChange: (Boolean) -> Unit,
    onEditAction: () -> Unit
) {
    ParSonicCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = binding.type.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = binding.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = binding.enabled, onCheckedChange = onEnabledChange)
        }
        if (binding.enabled) {
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accion",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onEditAction) {
                        Text(binding.action.label, color = ParRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (binding.type.availableWithScreenOff) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Disponible con la pantalla apagada si el dispositivo tiene el sensor " +
                    "de movimiento significativo.",
                style = MaterialTheme.typography.labelSmall,
                color = ParGreyDim
            )
        }
    }
}

@Composable
private fun ActionPickerDialog(
    type: GestureType,
    current: GestureAction,
    onDismiss: () -> Unit,
    onPick: (GestureAction) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text("Accion para \"${type.label}\"", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 420.dp)
            ) {
                GestureAction.entries.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = action == current,
                            onClick = { onPick(action) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}
