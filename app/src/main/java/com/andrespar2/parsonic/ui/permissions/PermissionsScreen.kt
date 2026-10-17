package com.andrespar2.parsonic.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andrespar2.parsonic.ui.components.InfoBanner
import com.andrespar2.parsonic.ui.components.ParSonicCard
import com.andrespar2.parsonic.ui.components.ScreenHeader
import com.andrespar2.parsonic.ui.components.SectionTitle
import com.andrespar2.parsonic.ui.components.StatusPill
import com.andrespar2.parsonic.ui.theme.ParAmber
import com.andrespar2.parsonic.ui.theme.ParGreen
import com.andrespar2.parsonic.ui.theme.ParGreyDim
import com.andrespar2.parsonic.ui.util.rememberPermissionSnapshot

@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    val permissions = rememberPermissionSnapshot()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* El estado se refresca al volver al primer plano. */ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader(
            title = "Permisos",
            subtitle = "ParSonic pide solo lo imprescindible y explica para que usa cada acceso."
        )

        SectionTitle("Acceso a notificaciones")
        PermissionCard(
            granted = permissions.notificationAccess,
            title = "Deteccion de reproductores",
            description = "Android solo permite leer que reproductor esta sonando a las apps con un " +
                "servicio de escucha de notificaciones habilitado. ParSonic lo usa unicamente " +
                "como llave: no lee el texto, el titulo ni las acciones de tus notificaciones.",
            onOpen = {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )

        SectionTitle("Dibujar sobre otras apps")
        PermissionCard(
            granted = permissions.overlay,
            title = "Zonas de gesto",
            description = "Permite mostrar las franjas transparentes de los bordes sobre otros " +
                "reproductores para capturar los gestos. Sin este permiso, los gestos tactiles " +
                "no funcionan (los de sensor si).",
            onOpen = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )

        SectionTitle("Notificaciones")
        val notificationsRequired = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        PermissionCard(
            granted = !notificationsRequired || permissions.notifications,
            title = "Aviso persistente",
            description = if (notificationsRequired) {
                "Android 13 y posteriores exigen permiso para mostrar la notificacion del " +
                    "servicio. Si lo deniegas, ParSonic sigue funcionando, pero no veras el aviso."
            } else {
                "En esta version de Android no hace falta: el aviso se muestra siempre."
            },
            onOpen = if (notificationsRequired) {
                { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            } else {
                null
            }
        )

        Spacer(Modifier.height(16.dp))
        InfoBanner(
            text = "Privacidad: ParSonic no envia nada a Internet, no tiene cuentas ni " +
                "analitica, y no guarda el contenido de tus notificaciones. Todo el estado de " +
                "reproduccion vive solo en tu dispositivo.",
            tone = ParGreyDim
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    granted: Boolean,
    title: String,
    description: String,
    onOpen: (() -> Unit)?
) {
    ParSonicCard {
        StatusPill(
            text = if (granted) "Concedido" else "Pendiente",
            color = if (granted) ParGreen else ParAmber
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!granted && onOpen != null) {
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Abrir ajustes")
            }
        }
    }
}
