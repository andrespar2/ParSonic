package com.andrespar2.parsonic.core.util

import android.util.Log
import com.andrespar2.parsonic.BuildConfig

/**
 * Registro de eventos centralizado.
 *
 * En compilaciones de release solo se emiten avisos y errores: escribir en
 * logcat constantemente cuesta bateria y no aporta al usuario final.
 */
object Logg {

    private const val PREFIX = "ParSonic"

    fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d("$PREFIX/$tag", message)
    }

    fun i(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.i("$PREFIX/$tag", message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w("$PREFIX/$tag", message, throwable) else Log.w("$PREFIX/$tag", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.e("$PREFIX/$tag", message, throwable) else Log.e("$PREFIX/$tag", message)
    }
}
