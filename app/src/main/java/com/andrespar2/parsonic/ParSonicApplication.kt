package com.andrespar2.parsonic

import android.app.Application

/**
 * Clase Application de ParSonic.
 *
 * Se mantiene deliberadamente ligera: el contenedor de dependencias se crea de
 * forma perezosa y no se arranca ningun servicio en `onCreate`. Arrancar
 * trabajo en segundo plano al abrir la app seria un gasto de bateria inutil.
 */
class ParSonicApplication : Application()
