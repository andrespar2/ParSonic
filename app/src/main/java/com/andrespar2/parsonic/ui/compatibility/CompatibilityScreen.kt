package com.andrespar2.parsonic.ui.compatibility

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrespar2.parsonic.ui.components.InfoBanner
import com.andrespar2.parsonic.ui.components.KeyValueRow
import com.andrespar2.parsonic.ui.components.ParDivider
import com.andrespar2.parsonic.ui.components.ParSonicCard
import com.andrespar2.parsonic.ui.components.ScreenHeader
import com.andrespar2.parsonic.ui.components.SectionTitle
import com.andrespar2.parsonic.ui.components.StatusPill
import com.andrespar2.parsonic.ui.theme.ParAmber
import com.andrespar2.parsonic.ui.theme.ParGreen
import com.andrespar2.parsonic.ui.theme.ParGreyDim

@Composable
fun CompatibilityScreen() {
    val context = LocalContext.current
    val hardware = remember(context) { HardwareInfo.of(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader(
            title = "Compatibilidad",
            subtitle = "Que puede hacer ParSonic en tu dispositivo y que limita Android."
        )

        SectionTitle("Tu dispositivo")
        ParSonicCard {
            KeyValueRow("Fabricante", hardware.manufacturer)
            ParDivider()
            KeyValueRow("Modelo", hardware.model)
            ParDivider()
            KeyValueRow("Android", "${hardware.androidRelease} (API ${hardware.sdkInt})")
            ParDivider()
            KeyValueRow(
                "Acelerometro",
                if (hardware.hasAccelerometer) "Disponible" else "No disponible"
            )
            ParDivider()
            KeyValueRow(
                "Movimiento significativo",
                if (hardware.hasSignificantMotion) "Disponible" else "No disponible"
            )
        }

        SectionTitle("Lo que ParSonic hace")
        ParSonicCard {
            Feature("Controla cualquier reproductor", ParGreen)
            Text(
                text = "Funciona con Spotify, YouTube Music, VLC, reproductores locales y " +
                    "cualquier app que publique una sesion multimedia compatible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Feature(
                text = "Gestos tactiles",
                tone = if (ParSonicCapabilities.TOUCH_SUPPORTED) ParGreen else ParAmber
            )
            Text(
                text = "Zonas en los bordes de la pantalla para deslizar, tocar o mantener. " +
                    "Requieren la pantalla encendida.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Feature(
                text = "Gestos de sensor",
                tone = if (hardware.hasAccelerometer) ParGreen else ParGreyDim
            )
            Text(
                text = if (hardware.hasAccelerometer) {
                    "Agitar el dispositivo y girarlo boca abajo. Solo con la pantalla encendida, " +
                        "para no gastar bateria."
                } else {
                    "Tu dispositivo no tiene acelerometro, asi que estos gestos no estan " +
                        "disponibles."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Feature(
                text = "Golpe en pantalla apagada",
                tone = if (hardware.hasSignificantMotion) ParGreen else ParGreyDim
            )
            Text(
                text = if (hardware.hasSignificantMotion) {
                    "Usa el sensor de movimiento significativo, el unico que Android mantiene " +
                        "activo con el dispositivo suspendido sin coste apreciable de bateria."
                } else {
                    "Tu dispositivo no expone el sensor de movimiento significativo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SectionTitle("Limites de Android")
        ParSonicCard {
            Limitation(
                "La pantalla apagada no entrega toques",
                "Android no envia eventos tactiles a ninguna aplicacion mientras la pantalla " +
                    "esta apagada. No es una limitacion de ParSonic: es del sistema. Por eso los " +
                    "gestos tactiles exigen la pantalla encendida. La alternativa honesta es el " +
                    "sensor de movimiento significativo o los botones del propio reproductor."
            )
            Spacer(Modifier.height(14.dp))
            Limitation(
                "El gesto \"atras\" del sistema",
                "Desde Android 10 el borde de la pantalla hace la funcion \"atras\". Las zonas de " +
                    "gesto comparten ese borde, asi que pueden entrar en conflicto. ParSonic las " +
                    "activa solo mientras suena musica para molestar lo minimo; puedes reducir su " +
                    "ancho o desactivar la zona inferior en Ajustes."
            )
            Spacer(Modifier.height(14.dp))
            Limitation(
                "Superposicion sobre la pantalla de bloqueo",
                "Que una app pueda dibujar sobre la pantalla de bloqueo depende del fabricante. " +
                    "En algunos dispositivos el sistema la oculta por seguridad."
            )
            Spacer(Modifier.height(14.dp))
            Limitation(
                "Solo con reproduccion activa",
                "Con la opcion de ahorro activada, el reconocimiento se apaga cuando no hay " +
                    "musica sonando. Si el reproductor esta en pausa, los gestos tampoco responden."
            )
        }

        SectionTitle("Fabricantes")
        ParSonicCard {
            Text(
                text = "Algunos fabricantes (Xiaomi/MIUI, Huawei/EMUI, Samsung, OPPO...) limitan el " +
                    "trabajo en segundo plano y los servicios en primer plano. Para que ParSonic " +
                    "sea fiable:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "· Permite que ParSonic se ejecute en segundo plano.\n" +
                    "· Desactiva la optimizacion de bateria para ParSonic.\n" +
                    "· Activa el inicio automatico en los ajustes del fabricante.\n" +
                    "· Bloquea la app en recientes para que no la mate el sistema.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(12.dp))
        InfoBanner(
            text = "ParSonic nunca finge una compatibilidad que el sistema no permite. Todo lo que " +
                "aqui figura como disponible se apoya en APIs publicas de Android.",
            tone = ParGreyDim
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Feature(text: String, tone: androidx.compose.ui.graphics.Color) {
    StatusPill(text = text, color = tone)
}

@Composable
private fun Limitation(title: String, description: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(Modifier.height(3.dp))
    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Capacidades que dependen de la version de Android, no del hardware. */
private object ParSonicCapabilities {
    /** Las ventanas de superposicion necesitan Android 8 (API 26). */
    val TOUCH_SUPPORTED: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}

/** Informacion de hardware y sistema relevante para la compatibilidad. */
private data class HardwareInfo(
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val sdkInt: Int,
    val hasAccelerometer: Boolean,
    val hasSignificantMotion: Boolean
) {
    companion object {
        fun of(context: Context): HardwareInfo {
            val sensorManager =
                context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            return HardwareInfo(
                manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                model = Build.MODEL,
                androidRelease = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                hasAccelerometer =
                    sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null,
                hasSignificantMotion =
                    sensorManager?.getDefaultSensor(Sensor.TYPE_SIGNIFICANT_MOTION) != null
            )
        }
    }
}
