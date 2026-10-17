package com.andrespar2.parsonic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.andrespar2.parsonic.MainActivity
import com.andrespar2.parsonic.R
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.data.media.NowPlaying

/**
 * Construye la notificacion persistente del servicio de gestos.
 *
 * La notificacion es obligatoria mientras el servicio esta en primer plano, pero
 * aqui se aprovecha para dar informacion util: que ParSonic esta activo y, si es
 * posible, que reproductor se esta controlando. El boton "Desactivar" detiene el
 * servicio sin tener que abrir la aplicacion.
 */
class NotificationHelper(private val context: Context) {

    private val manager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    /** Crea el canal una sola vez (Android 8+). */
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = manager ?: return
        if (notificationManager.getNotificationChannel(ParSonicConstants.NOTIFICATION_CHANNEL_ID) != null) {
            return
        }
        val channel = NotificationChannel(
            ParSonicConstants.NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.gesture_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.gesture_channel_description)
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    /** Construye la notificacion segun el estado de reproduccion actual. */
    fun build(nowPlaying: NowPlaying?): Notification {
        ensureChannel()

        val playing = nowPlaying?.isPlaying == true
        val text = if (playing) {
            context.getString(R.string.gesture_notification_text_playing, nowPlaying?.appLabel.orEmpty())
        } else {
            context.getString(R.string.gesture_notification_text_idle)
        }

        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopService = PendingIntent.getService(
            context,
            REQUEST_STOP,
            Intent(context, GestureService::class.java).setAction(GestureService.ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, ParSonicConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.gesture_notification_title))
            .setContentText(text)
            .setColor(ContextCompat.getColor(context, R.color.parsonic_red))
            .setContentIntent(openApp)
            .addAction(0, context.getString(R.string.gesture_notification_stop), stopService)
            .setOngoing(true)
            .setShowWhen(false)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private companion object {
        const val REQUEST_OPEN = 100
        const val REQUEST_STOP = 101
    }
}
