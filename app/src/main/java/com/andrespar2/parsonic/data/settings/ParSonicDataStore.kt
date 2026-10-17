package com.andrespar2.parsonic.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.andrespar2.parsonic.core.ParSonicConstants

/**
 * Instancia unica de DataStore para las preferencias de ParSonic.
 *
 * Se declara a nivel de archivo (no dentro de una clase) porque
 * `preferencesDataStore` debe evaluarse una sola vez por proceso.
 */
val Context.parSonicDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ParSonicConstants.DATASTORE_NAME
)
