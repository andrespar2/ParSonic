package com.andrespar2.parsonic.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andrespar2.parsonic.core.util.Logg
import com.andrespar2.parsonic.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Rearranca el servicio de gestos tras reiniciar el dispositivo.
 *
 * Solo actua si el usuario ha activado tanto el interruptor maestro como la
 * opcion de inicio automatico. Es un extra de comodidad, no una obligacion: si
 * el sistema bloquea el arranque en segundo plano, se registra el motivo y no
 * pasa nada mas.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != QUICKBOOT_POWERON) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = AppContainer.get(appContext).settingsRepository.settings.first()
                if (settings.serviceEnabled && settings.autostartOnBoot) {
                    Logg.i(TAG, "Inicio automatico: arrancando el servicio de gestos")
                    GestureService.start(appContext)
                }
            } catch (throwable: Throwable) {
                Logg.w(TAG, "No se pudo evaluar el inicio automatico", throwable)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
        const val QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
