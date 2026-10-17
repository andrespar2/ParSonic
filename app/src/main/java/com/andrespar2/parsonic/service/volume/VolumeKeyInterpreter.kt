package com.andrespar2.parsonic.service.volume

import com.andrespar2.parsonic.core.ParSonicConstants

/**
 * Interpreta las pulsaciones de los botones de volumen y decide si cambian de
 * pista o si ajustan el volumen.
 *
 * Comportamiento:
 *  - **Una pulsacion** (simple) cambia de pista: subir = siguiente,
 *    bajar = anterior.
 *  - **Doble pulsacion rapida** del mismo boton entra en "modo volumen": ajusta
 *    el volumen un escalon y, a partir de ahi, cada pulsacion ajusta el volumen.
 *  - Al cabo de [volumeModeTimeoutMs] sin pulsar, vuelve solo al modo de cambio
 *    de pista. La caducidad se comprueba de forma perezosa (en la siguiente
 *    pulsacion), asi no hace falta ningun temporizador para ello.
 *
 * Es una clase pura y determinista: recibe tiempos y devuelve acciones. No
 * depende de `KeyEvent` ni de `AudioManager`, de modo que todos los umbrales y
 * casos limite estan cubiertos por pruebas unitarias JVM.
 *
 * Uso tipico (desde el servicio de accesibilidad):
 * ```
 * val actions = interpreter.onKeyPress(VolumeKey.UP, now)
 * // programar un unico temporizador en interpreter.nextTimeoutAtMs()
 * val pending = interpreter.onTimeout(now) // resuelve la pulsacion simple
 * ```
 */
class VolumeKeyInterpreter(
    private val doublePressWindowMs: Long = ParSonicConstants.VOLUME_DOUBLE_PRESS_WINDOW_MS,
    private val volumeModeTimeoutMs: Long = ParSonicConstants.VOLUME_MODE_TIMEOUT_MS
) {

    init {
        require(doublePressWindowMs > 0L) { "doublePressWindowMs debe ser positivo" }
        require(volumeModeTimeoutMs > 0L) { "volumeModeTimeoutMs debe ser positivo" }
    }

    private enum class Mode { TRACK, VOLUME }

    private var mode = Mode.TRACK
    private var volumeDeadlineMs = 0L
    private var pendingKey: VolumeKey? = null
    private var pendingAtMs = 0L

    /**
     * Procesa la pulsacion de un boton y devuelve las acciones inmediatas.
     *
     * Normalmente devuelve cero o una accion. Puede devolver dos cuando llega
     * un boton distinto mientras habia una pulsacion pendiente del otro: la
     * pendiente se resuelve como cambio de pista y la nueva queda a la espera de
     * confirmarse como simple o doble.
     */
    fun onKeyPress(key: VolumeKey, atMs: Long): List<VolumeKeyAction> {
        val actions = mutableListOf<VolumeKeyAction>()

        // Caducidad perezosa del modo volumen.
        if (mode == Mode.VOLUME && atMs >= volumeDeadlineMs) {
            mode = Mode.TRACK
            volumeDeadlineMs = 0L
        }

        when (mode) {
            Mode.VOLUME -> {
                volumeDeadlineMs = atMs + volumeModeTimeoutMs
                actions += volumeAction(key)
            }

            Mode.TRACK -> {
                val pending = pendingKey
                if (pending != null) {
                    val isDouble = pending == key && (atMs - pendingAtMs) <= doublePressWindowMs
                    if (isDouble) {
                        // Doble pulsacion: entra en modo volumen y ajusta un escalon.
                        pendingKey = null
                        mode = Mode.VOLUME
                        volumeDeadlineMs = atMs + volumeModeTimeoutMs
                        actions += volumeAction(key)
                        return actions
                    }
                    // La pendiente no forma doble pulsacion: se resuelve ya.
                    actions += trackAction(pending)
                    pendingKey = null
                }
                pendingKey = key
                pendingAtMs = atMs
            }
        }

        return actions
    }

    /**
     * Comprueba si ha caducado alguna ventana. Resuelve la pulsacion pendiente
     * como cambio de pista o simplemente cierra el modo volumen.
     */
    fun onTimeout(nowMs: Long): List<VolumeKeyAction> {
        val actions = mutableListOf<VolumeKeyAction>()

        if (mode == Mode.VOLUME) {
            if (nowMs >= volumeDeadlineMs) {
                mode = Mode.TRACK
                volumeDeadlineMs = 0L
            }
            return actions
        }

        val pending = pendingKey
        if (pending != null && (nowMs - pendingAtMs) >= doublePressWindowMs) {
            actions += trackAction(pending)
            pendingKey = null
        }
        return actions
    }

    /**
     * Instante en el que conviene llamar a [onTimeout], o `null` si no hay
     * ninguna ventana abierta. Asi el servicio mantiene como maximo un
     * temporizador activo en lugar de sondear continuamente.
     */
    fun nextTimeoutAtMs(): Long? {
        if (mode == Mode.VOLUME) return null
        val pending = pendingKey ?: return null
        return pendingAtMs + doublePressWindowMs
    }

    /** `true` si ahora mismo los botones estan en modo volumen. */
    fun isVolumeModeActive(nowMs: Long): Boolean =
        mode == Mode.VOLUME && nowMs < volumeDeadlineMs

    /** Descarta cualquier estado en curso y vuelve al modo de cambio de pista. */
    fun reset() {
        mode = Mode.TRACK
        volumeDeadlineMs = 0L
        pendingKey = null
        pendingAtMs = 0L
    }

    private fun trackAction(key: VolumeKey): VolumeKeyAction = when (key) {
        VolumeKey.UP -> VolumeKeyAction.NEXT_TRACK
        VolumeKey.DOWN -> VolumeKeyAction.PREVIOUS_TRACK
    }

    private fun volumeAction(key: VolumeKey): VolumeKeyAction = when (key) {
        VolumeKey.UP -> VolumeKeyAction.VOLUME_UP
        VolumeKey.DOWN -> VolumeKeyAction.VOLUME_DOWN
    }
}
