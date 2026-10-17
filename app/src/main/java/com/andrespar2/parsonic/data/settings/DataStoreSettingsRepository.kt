package com.andrespar2.parsonic.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.core.util.Logg
import com.andrespar2.parsonic.data.settings.model.AppSettings
import com.andrespar2.parsonic.data.settings.model.GestureAction
import com.andrespar2.parsonic.data.settings.model.GestureBinding
import com.andrespar2.parsonic.data.settings.model.GestureType
import com.andrespar2.parsonic.data.settings.model.Sensitivity
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Implementacion de [SettingsRepository] sobre Preferences DataStore.
 *
 * Cada gesto se guarda con tres claves independientes (`<id>.enabled`,
 * `<id>.action`, `<id>.sensitivity`) en lugar de serializar un unico bloque.
 * Asi, cambiar el conjunto de gestos en el futuro no invalida las preferencias
 * ya guardadas y un valor desconocido se resuelve con el valor por defecto.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settings: Flow<AppSettings> = dataStore.data
        .catch { throwable ->
            // Un fallo de lectura no debe tumbar la app: se usan los valores por defecto.
            if (throwable is IOException) {
                Logg.w(TAG, "No se pudo leer DataStore; se usan valores por defecto", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { preferences -> preferences.toAppSettings() }
        .distinctUntilChanged()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { preferences ->
            val current = preferences.toAppSettings()
            val updated = transform(current)
            preferences.writeFrom(updated)
        }
    }

    private fun Preferences.toAppSettings(): AppSettings = AppSettings(
        serviceEnabled = this[Keys.SERVICE_ENABLED] ?: AppSettings.DEFAULT.serviceEnabled,
        autostartOnBoot = this[Keys.AUTOSTART_ON_BOOT] ?: AppSettings.DEFAULT.autostartOnBoot,
        onlyWhilePlaying = this[Keys.ONLY_WHILE_PLAYING] ?: AppSettings.DEFAULT.onlyWhilePlaying,
        touchGesturesEnabled = this[Keys.TOUCH_GESTURES] ?: AppSettings.DEFAULT.touchGesturesEnabled,
        sensorGesturesEnabled = this[Keys.SENSOR_GESTURES] ?: AppSettings.DEFAULT.sensorGesturesEnabled,
        volumeKeysEnabled = this[Keys.VOLUME_KEYS] ?: AppSettings.DEFAULT.volumeKeysEnabled,
        edgeZoneWidthDp = (this[Keys.EDGE_ZONE_WIDTH] ?: AppSettings.DEFAULT.edgeZoneWidthDp)
            .coerceIn(
                ParSonicConstants.MIN_EDGE_ZONE_WIDTH_DP,
                ParSonicConstants.MAX_EDGE_ZONE_WIDTH_DP
            ),
        sensitivity = Sensitivity.fromId(
            this[Keys.SENSITIVITY] ?: AppSettings.DEFAULT.sensitivity.id
        ),
        bottomZoneEnabled = this[Keys.BOTTOM_ZONE] ?: AppSettings.DEFAULT.bottomZoneEnabled,
        showGestureHints = this[Keys.SHOW_HINTS] ?: AppSettings.DEFAULT.showGestureHints,
        hapticFeedback = this[Keys.HAPTIC] ?: AppSettings.DEFAULT.hapticFeedback,
        bindings = GestureType.entries.associateWith { type ->
            GestureBinding(
                type = type,
                enabled = this[Keys.gestureEnabled(type)]
                    ?: AppSettings.defaultBinding(type).enabled,
                action = GestureAction.fromId(
                    this[Keys.gestureAction(type)] ?: AppSettings.defaultBinding(type).action.id
                )
            )
        }
    )

    private fun androidx.datastore.preferences.core.MutablePreferences.writeFrom(settings: AppSettings) {
        this[Keys.SERVICE_ENABLED] = settings.serviceEnabled
        this[Keys.AUTOSTART_ON_BOOT] = settings.autostartOnBoot
        this[Keys.ONLY_WHILE_PLAYING] = settings.onlyWhilePlaying
        this[Keys.TOUCH_GESTURES] = settings.touchGesturesEnabled
        this[Keys.SENSOR_GESTURES] = settings.sensorGesturesEnabled
        this[Keys.VOLUME_KEYS] = settings.volumeKeysEnabled
        this[Keys.EDGE_ZONE_WIDTH] = settings.edgeZoneWidthDp
        this[Keys.SENSITIVITY] = settings.sensitivity.id
        this[Keys.BOTTOM_ZONE] = settings.bottomZoneEnabled
        this[Keys.SHOW_HINTS] = settings.showGestureHints
        this[Keys.HAPTIC] = settings.hapticFeedback

        GestureType.entries.forEach { type ->
            val binding = settings.bindingOf(type)
            this[Keys.gestureEnabled(type)] = binding.enabled
            this[Keys.gestureAction(type)] = binding.action.id
        }
    }

    /** Claves de Preferences usadas por ParSonic. */
    private object Keys {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val AUTOSTART_ON_BOOT = booleanPreferencesKey("autostart_on_boot")
        val ONLY_WHILE_PLAYING = booleanPreferencesKey("only_while_playing")
        val TOUCH_GESTURES = booleanPreferencesKey("touch_gestures_enabled")
        val SENSOR_GESTURES = booleanPreferencesKey("sensor_gestures_enabled")
        val VOLUME_KEYS = booleanPreferencesKey("volume_keys_enabled")
        val EDGE_ZONE_WIDTH = intPreferencesKey("edge_zone_width_dp")
        val SENSITIVITY = stringPreferencesKey("sensitivity")
        val BOTTOM_ZONE = booleanPreferencesKey("bottom_zone_enabled")
        val SHOW_HINTS = booleanPreferencesKey("show_gesture_hints")
        val HAPTIC = booleanPreferencesKey("haptic_feedback")

        fun gestureEnabled(type: GestureType) =
            booleanPreferencesKey("gesture_${type.id}_enabled")

        fun gestureAction(type: GestureType) =
            stringPreferencesKey("gesture_${type.id}_action")
    }

    private companion object {
        const val TAG = "Settings"
    }
}
