package com.andrespar2.parsonic.service.volume

/**
 * Boton fisico de volumen reconocido por [VolumeKeyInterpreter].
 */
enum class VolumeKey {
    UP,
    DOWN
}

/**
 * Accion resultante de interpretar las pulsaciones de los botones de volumen.
 *
 * Se mantiene deliberadamente separada de `GestureAction` para que el
 * reconocimiento no dependa de la capa de preferencias ni del despachador.
 */
enum class VolumeKeyAction {
    NEXT_TRACK,
    PREVIOUS_TRACK,
    VOLUME_UP,
    VOLUME_DOWN
}
