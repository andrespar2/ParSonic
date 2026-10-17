package com.andrespar2.parsonic.data.settings

import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureBinding
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import kotlinx.coroutines.flow.Flow

/**
 * Acceso a las preferencias del usuario.
 *
 * Se define como interfaz para poder sustituirla en las pruebas por una
 * implementacion en memoria, sin depender de DataStore ni del framework.
 */
interface SettingsRepository {

    /** Flujo con el estado actual de las preferencias. Nunca lanza excepciones. */
    val settings: Flow<AppSettings>

    /** Aplica una transformacion atomica sobre las preferencias. */
    suspend fun update(transform: (AppSettings) -> AppSettings)

    /** Aplica una transformacion a la configuracion de un gesto. */
    suspend fun updateBinding(type: GestureType, transform: (GestureBinding) -> GestureBinding)
        = update { current ->
            val updated = transform(current.bindingOf(type))
            current.copy(bindings = current.bindings + (type to updated))
        }

    /** Activa o desactiva el servicio de gestos (interruptor maestro). */
    suspend fun setServiceEnabled(enabled: Boolean) =
        update { it.copy(serviceEnabled = enabled) }

    /** Activa o desactiva el arranque automatico tras reiniciar. */
    suspend fun setAutostartOnBoot(enabled: Boolean) =
        update { it.copy(autostartOnBoot = enabled) }

    /** Activa o desactiva la optimizacion "solo mientras suena musica". */
    suspend fun setOnlyWhilePlaying(enabled: Boolean) =
        update { it.copy(onlyWhilePlaying = enabled) }

    /** Activa o desactiva los gestos tactiles. */
    suspend fun setTouchGesturesEnabled(enabled: Boolean) =
        update { it.copy(touchGesturesEnabled = enabled) }

    /** Activa o desactiva los gestos de sensores. */
    suspend fun setSensorGesturesEnabled(enabled: Boolean) =
        update { it.copy(sensorGesturesEnabled = enabled) }

    /** Cambia el ancho de las zonas laterales de gesto. */
    suspend fun setEdgeZoneWidth(widthDp: Int) =
        update { it.copy(edgeZoneWidthDp = widthDp) }

    /** Activa o desactiva la zona inferior. */
    suspend fun setBottomZoneEnabled(enabled: Boolean) =
        update { it.copy(bottomZoneEnabled = enabled) }

    /** Activa o desactiva las pistas visuales. */
    suspend fun setShowGestureHints(enabled: Boolean) =
        update { it.copy(showGestureHints = enabled) }

    /** Activa o desactiva la respuesta haptica. */
    suspend fun setHapticFeedback(enabled: Boolean) =
        update { it.copy(hapticFeedback = enabled) }

    /** Cambia la sensibilidad global del reconocimiento. */
    suspend fun setSensitivity(sensitivity: Sensitivity) =
        update { it.copy(sensitivity = sensitivity) }

    /** Activa o desactiva un gesto concreto. */
    suspend fun setGestureEnabled(type: GestureType, enabled: Boolean) =
        updateBinding(type) { it.copy(enabled = enabled) }

    /** Cambia la accion asignada a un gesto. */
    suspend fun setGestureAction(type: GestureType, action: GestureAction) =
        updateBinding(type) { it.copy(action = action) }

    /** Devuelve todos los gestos a su configuracion de fabrica. */
    suspend fun resetGestures() =
        update { it.copy(bindings = AppSettings.defaultBindings(), sensitivity = AppSettings.DEFAULT.sensitivity) }
}
