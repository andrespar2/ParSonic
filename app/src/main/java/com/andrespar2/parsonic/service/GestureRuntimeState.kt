package com.andrespar2.parsonic.service

import android.os.SystemClock
import com.andrespar2.parsonic.data.settings.model.GestureType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Estado en vivo del reconocimiento de gestos.
 *
 * El servicio de gestos lo actualiza y la interfaz lo observa para poder contar
 * con honestidad en que punto esta ParSonic: si el servicio corre, si la captura
 * tactil y la de sensores estan activas ahora mismo y por que.
 */
object GestureRuntimeState {

    data class Status(
        /** `true` si el servicio de gestos esta en ejecucion. */
        val serviceRunning: Boolean = false,
        /** `true` si las ventanas de superposicion estan capturando toques. */
        val touchActive: Boolean = false,
        /** `true` si los sensores estan registrados. */
        val sensorActive: Boolean = false,
        /** `true` si el usuario ha concedido "dibujar sobre otras apps". */
        val overlayGranted: Boolean = false,
        /** `true` si hay una sesion reproduciendo en este momento. */
        val playing: Boolean = false,
        /** Ultimo gesto reconocido, para mostrar la pista en la interfaz. */
        val lastGesture: GestureType? = null,
        /** Instante (`elapsedRealtime`) del ultimo gesto reconocido. */
        val lastGestureAtMs: Long = 0L
    )

    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()

    private val _events = MutableSharedFlow<GestureType>(extraBufferCapacity = 16)
    val events: SharedFlow<GestureType> = _events.asSharedFlow()

    /** Aplica una transformacion atomica al estado. */
    fun update(transform: (Status) -> Status) {
        _status.update(transform)
    }

    /** Marca el servicio como activo o detenido. */
    fun setServiceRunning(running: Boolean) = update { current ->
        current.copy(
            serviceRunning = running,
            touchActive = if (running) current.touchActive else false,
            sensorActive = if (running) current.sensorActive else false
        )
    }

    /** Registra un gesto reconocido y emite su evento. */
    fun recordGesture(type: GestureType) {
        update { it.copy(lastGesture = type, lastGestureAtMs = SystemClock.elapsedRealtime()) }
        _events.tryEmit(type)
    }

    /** Vuelve al estado inicial. */
    fun reset() {
        _status.value = Status()
    }
}
