package com.andrespar2.parsonic.di

import android.content.Context
import com.andrespar2.parsonic.data.media.AndroidVolumeController
import com.andrespar2.parsonic.data.media.MediaSessionRepository
import com.andrespar2.parsonic.data.media.VolumeController
import com.andrespar2.parsonic.data.settings.DataStoreSettingsRepository
import com.andrespar2.parsonic.data.settings.SettingsRepository
import com.andrespar2.parsonic.data.settings.parSonicDataStore
import com.andrespar2.parsonic.service.MediaListenerBridge

/**
 * Contenedor de dependencias de ParSonic.
 *
 * Se escribe a mano (sin Hilt ni KSP) para no anadir procesadores de anotaciones
 * al build y para mantener el grafo de dependencias explicito y facil de leer.
 *
 * El contenedor vive tanto tiempo como el proceso: la actividad y el servicio de
 * gestos comparten la misma instancia, de modo que el estado multimedia se
 * observa una sola vez.
 */
class AppContainer private constructor(val appContext: Context) {

    /** Preferencias persistentes del usuario. */
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(appContext.parSonicDataStore)

    /** Control del volumen multimedia del dispositivo. */
    val volumeController: VolumeController = AndroidVolumeController(appContext)

    /** Fuente de verdad sobre lo que esta sonando. */
    val mediaRepository: MediaSessionRepository = MediaListenerBridge.createRepository(appContext)

    /**
     * Numero de consumidores activos de [mediaRepository].
     *
     * Observar las sesiones multimedia mantiene registrado un callback del
     * sistema, asi que solo debe hacerse mientras alguien lo necesita: la
     * interfaz mientras esta visible y el servicio mientras captura gestos. Con
     * el recuento a cero se libera todo y no se consume bateria.
     */
    private var mediaReferences: Int = 0

    /** Registra un consumidor del estado multimedia (hilo principal). */
    fun acquireMedia() {
        mediaReferences += 1
        if (mediaReferences == 1) mediaRepository.start()
    }

    /** Libera un consumidor del estado multimedia (hilo principal). */
    fun releaseMedia() {
        if (mediaReferences == 0) return
        mediaReferences -= 1
        if (mediaReferences == 0) mediaRepository.stop()
    }

    companion object {

        @Volatile
        private var instance: AppContainer? = null

        /** Devuelve la instancia unica del contenedor. */
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
