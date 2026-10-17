package com.andrespar2.parsonic.core.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Consulta del estado de los permisos y accesos especiales que necesita
 * ParSonic.
 *
 * Ninguno de estos accesos es un permiso de runtime clasico: son "accesos
 * especiales" que el usuario concede desde Ajustes. Se consultan siempre antes
 * de mostrar la interfaz correspondiente para no prometer funciones que el
 * sistema no va a permitir.
 */
object ParSonicPermissions {

    /**
     * `true` si ParSonic tiene concedido el acceso a notificaciones.
     *
     * Se comprueba el componente exacto en `enabled_notification_listeners`
     * (y no solo el paquete) para no dar falsos positivos si el usuario revoco
     * el acceso a medias.
     */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val flattened = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        if (flattened.isBlank()) return false

        return flattened.split(':').any { entry ->
            val component = ComponentName.unflattenFromString(entry.trim())
            component?.packageName == context.packageName
        }
    }

    /**
     * `true` si la app puede dibujar sobre otras aplicaciones.
     *
     * Necesario para capturar gestos tactiles sobre otros reproductores.
     */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * `true` si la app puede publicar la notificacion del servicio.
     * En Android 12 y anteriores siempre devuelve `true` (no hay permiso).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** `true` si el sistema soporta zonas de gesto (comprobacion de API). */
    fun supportsOverlayGestures(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}
