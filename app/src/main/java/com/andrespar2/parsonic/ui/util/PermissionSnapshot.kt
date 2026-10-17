package com.andrespar2.parsonic.ui.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.andrespar2.parsonic.core.util.ParSonicPermissions

/** Fotografia del estado de los accesos especiales que necesita ParSonic. */
data class PermissionSnapshot(
    val notificationAccess: Boolean,
    val overlay: Boolean,
    val notifications: Boolean
) {
    /** `true` si estan concedidos todos los accesos obligatorios. */
    val allGranted: Boolean get() = notificationAccess && overlay

    companion object {
        fun of(context: Context): PermissionSnapshot = PermissionSnapshot(
            notificationAccess = ParSonicPermissions.isNotificationAccessGranted(context),
            overlay = ParSonicPermissions.canDrawOverlays(context),
            notifications = ParSonicPermissions.hasNotificationPermission(context)
        )
    }
}

/**
 * Observa el estado de los permisos y lo refresca cada vez que la pantalla vuelve
 * al primer plano (por ejemplo, al regresar de los ajustes del sistema).
 */
@Composable
fun rememberPermissionSnapshot(): PermissionSnapshot {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(PermissionSnapshot.of(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        snapshot = PermissionSnapshot.of(context)
    }
    return snapshot
}
