package com.andrespar2.parsonic.data.settings.model

import com.andrespar2.parsonic.core.ParSonicConstants

/**
 * Configuracion de un gesto concreto: si esta activo, que hace y con que
 * sensibilidad se reconoce.
 */
data class GestureBinding(
    val type: GestureType,
    val enabled: Boolean,
    val action: GestureAction,
    val sensitivity: Sensitivity
) {
    /** `true` si el gesto esta activo y ademas tiene una accion util. */
    val isEffective: Boolean
        get() = enabled && action != GestureAction.NONE
}

/**
 * Estado completo de las preferencias del usuario.
 *
 * Se modela como un unico objeto inmutable para que la interfaz pueda
 * observarlo con un solo `Flow` y para que los tests construyan variantes
 * facilmente con `copy()`.
 */
data class AppSettings(
    /** Interruptor maestro: servicio de gestos activo. */
    val serviceEnabled: Boolean = false,

    /** Arrancar el servicio automaticamente tras reiniciar el dispositivo. */
    val autostartOnBoot: Boolean = false,

    /**
     * Optimizacion de bateria: mantener el reconocimiento activo unicamente
     * mientras hay una sesion multimedia reproduciendo contenido.
     */
    val onlyWhilePlaying: Boolean = true,

    /** Habilitar gestos tactiles sobre otras aplicaciones. */
    val touchGesturesEnabled: Boolean = true,

    /** Habilitar gestos de sensores (agitar, girar). */
    val sensorGesturesEnabled: Boolean = false,

    /** Ancho de las zonas laterales de gesto, en dp. */
    val edgeZoneWidthDp: Int = ParSonicConstants.DEFAULT_EDGE_ZONE_WIDTH_DP,

    /** Mostrar una zona de gesto adicional en el borde inferior. */
    val bottomZoneEnabled: Boolean = true,

    /** Mostrar una pequena pista visual al reconocer un gesto. */
    val showGestureHints: Boolean = true,

    /** Vibracion breve al reconocer un gesto. */
    val hapticFeedback: Boolean = true,

    /** Configuracion individual de cada gesto. */
    val bindings: Map<GestureType, GestureBinding> = defaultBindings()
) {

    /** Devuelve la configuracion de [type], con los valores por defecto si falta. */
    fun bindingOf(type: GestureType): GestureBinding =
        bindings[type] ?: defaultBinding(type)

    /** Gestos tactiles en orden de presentacion. */
    val touchBindings: List<GestureBinding>
        get() = GestureType.touchGestures.map(::bindingOf)

    /** Gestos de sensor en orden de presentacion. */
    val sensorBindings: List<GestureBinding>
        get() = GestureType.sensorGestures.map(::bindingOf)

    /** `true` si algun gesto tactil esta realmente operativo. */
    val hasAnyActiveTouchGesture: Boolean
        get() = touchBindings.any { it.isEffective }

    /** `true` si algun gesto de sensor esta realmente operativo. */
    val hasAnyActiveSensorGesture: Boolean
        get() = sensorBindings.any { it.isEffective }

    companion object {

        /** Configuracion inicial de fabrica. */
        val DEFAULT: AppSettings = AppSettings()

        /** Configuracion por defecto de un gesto. */
        fun defaultBinding(type: GestureType): GestureBinding = GestureBinding(
            type = type,
            enabled = type.defaultAction != GestureAction.NONE,
            action = type.defaultAction,
            sensitivity = Sensitivity.MEDIUM
        )

        /** Mapa con la configuracion por defecto de todos los gestos. */
        fun defaultBindings(): Map<GestureType, GestureBinding> =
            GestureType.entries.associateWith { defaultBinding(it) }
    }
}
