package com.andrespar2.parsonic.service.gesture

/**
 * Fase de un evento tactil, desacoplada de `android.view.MotionEvent` para que
 * el reconocedor de gestos se pueda probar en la maquina virtual de Java.
 */
enum class TouchPhase {
    /** El dedo toca la pantalla. */
    DOWN,

    /** El dedo se mueve. */
    MOVE,

    /** El dedo se levanta. */
    UP,

    /** El gesto se cancela (por ejemplo, otra ventana se lleva el toque). */
    CANCEL
}
