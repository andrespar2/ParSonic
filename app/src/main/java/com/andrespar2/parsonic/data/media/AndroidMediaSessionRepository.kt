package com.andrespar2.parsonic.data.media

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.andrespar2.parsonic.core.ParSonicConstants
import com.andrespar2.parsonic.core.util.Logg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Implementacion real basada en `MediaSessionManager`.
 *
 * Como funciona:
 *  1. Se registra un `OnActiveSessionsChangedListener` (callback, sin sondeo)
 *     usando como credencial el `NotificationListenerService` de ParSonic.
 *  2. De entre las sesiones activas se elige la que esta sonando; si ninguna
 *     suena, la primera que publique metadatos.
 *  3. Se conecta un `MediaController` a esa sesion y se reciben sus cambios de
 *     estado y de metadatos por callback.
 *
 * Toda la API se invoca desde el hilo principal porque `MediaSessionManager`
 * exige un `Looper` para registrar sus listeners.
 */
class AndroidMediaSessionRepository(
    context: Context,
    private val listenerComponent: ComponentName
) : MediaSessionRepository {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mediaSessionManager: MediaSessionManager? =
        appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    override val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _isAvailable = MutableStateFlow(false)
    override val isAvailable: StateFlow<Boolean> = _isAvailable.asStateFlow()

    private var controller: MediaController? = null
    private var started: Boolean = false
    private val labelCache = mutableMapOf<String, String>()

    private val controllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()

        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()

        override fun onSessionDestroyed() {
            Logg.d(TAG, "La sesion activa se ha cerrado")
            detachController()
            refresh()
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachBest(controllers)
        }

    override fun start() {
        if (started) return
        started = true

        val manager = mediaSessionManager
        if (manager == null) {
            Logg.w(TAG, "Este dispositivo no expone MediaSessionManager")
            _isAvailable.value = false
            return
        }

        mainHandler.post {
            if (!started) return@post
            try {
                manager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent)
                _isAvailable.value = true
                attachBest(manager.getActiveSessions(listenerComponent))
            } catch (security: SecurityException) {
                // Falta el acceso a notificaciones, o el usuario lo acaba de revocar.
                Logg.w(TAG, "Sin acceso a las sesiones multimedia: ${security.message}")
                _isAvailable.value = false
                _nowPlaying.value = null
            }
        }
    }

    override fun stop() {
        if (!started) return
        started = false
        mainHandler.post {
            runCatching {
                mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            }
            detachController()
            _nowPlaying.value = null
            _isAvailable.value = false
        }
    }

    override fun refresh() {
        val manager = mediaSessionManager ?: return
        mainHandler.post {
            if (!started) return@post
            try {
                attachBest(manager.getActiveSessions(listenerComponent))
                _isAvailable.value = true
            } catch (security: SecurityException) {
                Logg.w(TAG, "Sin acceso a las sesiones multimedia: ${security.message}")
                _isAvailable.value = false
                _nowPlaying.value = null
            }
        }
    }

    // --- Seleccion y conexion de la sesion -----------------------------------

    /**
     * Elige la sesion mas relevante y conecta el controlador si ha cambiado.
     *
     * Prioridad: lo que esta sonando > lo que publica metadatos > la primera
     * sesion disponible.
     */
    private fun attachBest(controllers: List<MediaController>?) {
        val candidates = controllers.orEmpty()
        val best = selectBest(candidates)

        if (best == null) {
            detachController()
            _nowPlaying.value = null
            return
        }

        val current = controller
        if (current != null && current.sessionToken == best.sessionToken) {
            // La misma sesion: basta con refrescar la informacion publicada.
            controller = best
            publish()
            return
        }

        detachController()
        controller = best
        best.registerCallback(controllerCallback, mainHandler)
        Logg.i(TAG, "Sesion conectada: ${best.packageName}")
        publish()
    }

    private fun selectBest(candidates: List<MediaController>): MediaController? {
        if (candidates.isEmpty()) return null
        return candidates.firstOrNull { playingOrBuffering(it) }
            ?: candidates.firstOrNull { hasMetadata(it) }
            ?: candidates.first()
    }

    private fun playingOrBuffering(controller: MediaController): Boolean {
        val state = controller.playbackState?.state ?: return false
        return PlaybackStatusMapper.fromPlaybackStateCode(state).isActive
    }

    private fun hasMetadata(controller: MediaController): Boolean {
        val metadata = controller.metadata ?: return false
        return !metadata.getString(MediaMetadata.METADATA_KEY_TITLE).isNullOrBlank()
    }

    private fun detachController() {
        controller?.let { current ->
            runCatching { current.unregisterCallback(controllerCallback) }
        }
        controller = null
    }

    // --- Publicacion del estado ---------------------------------------------

    private fun publish() {
        val controller = this.controller
        if (controller == null) {
            _nowPlaying.value = null
            return
        }
        _nowPlaying.value = runCatching { controller.toNowPlaying() }
            .onFailure { Logg.w(TAG, "No se pudo leer el estado de la sesion", it) }
            .getOrNull()
    }

    private fun MediaController.toNowPlaying(): NowPlaying {
        val metadata = this.metadata
        val playback = this.playbackState
        val status = PlaybackStatusMapper.fromPlaybackStateCode(
            playback?.state ?: PlaybackStatusMapper.STATE_NONE
        )
        val actions = playback?.actions ?: 0L
        val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val packageName = this.packageName ?: ""

        return NowPlaying(
            packageName = packageName,
            appLabel = resolveAppLabel(packageName),
            title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
            artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?.takeIf { it.isNotBlank() }
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
            album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM).orEmpty(),
            status = status,
            positionMs = playback?.position ?: 0L,
            durationMs = duration.coerceAtLeast(0L),
            positionUpdatedAtElapsedMs = SystemClock.elapsedRealtime(),
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            artworkUri = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata?.getString(MediaMetadata.METADATA_KEY_ART_URI),
            canSkipNext = hasAction(actions, PlaybackState.ACTION_SKIP_TO_NEXT),
            canSkipPrevious = hasAction(actions, PlaybackState.ACTION_SKIP_TO_PREVIOUS),
            canSeek = hasAction(actions, PlaybackState.ACTION_SEEK_TO),
            canTogglePlayback = hasAnyAction(
                actions,
                PlaybackState.ACTION_PLAY_PAUSE,
                PlaybackState.ACTION_PLAY,
                PlaybackState.ACTION_PAUSE
            )
        )
    }

    private fun hasAction(actions: Long, action: Long): Boolean = (actions and action) != 0L

    private fun hasAnyAction(actions: Long, vararg candidates: Long): Boolean =
        candidates.any { hasAction(actions, it) }

    // --- Etiquetas legibles --------------------------------------------------

    /**
     * Nombre visible del reproductor.
     *
     * Desde Android 11 el acceso a los paquetes esta filtrado, por lo que la
     * consulta puede fallar; en ese caso se usa un nombre amable conocido o,
     * como ultimo recurso, el ultimo segmento del paquete.
     */
    private fun resolveAppLabel(packageName: String): String {
        if (packageName.isBlank()) return ParSonicConstants.UNKNOWN_MEDIA_LABEL
        labelCache[packageName]?.let { return it }

        val packageManager = appContext.packageManager
        val label = runCatching {
            @Suppress("DEPRECATION")
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: knownAppLabel(packageName)

        labelCache[packageName] = label
        return label
    }

    private fun knownAppLabel(packageName: String): String = when (packageName) {
        "com.spotify.music" -> "Spotify"
        "com.google.android.apps.youtube.music" -> "YouTube Music"
        "com.google.android.youtube" -> "YouTube"
        "org.videolan.vlc" -> "VLC"
        "com.amazon.mp3" -> "Amazon Music"
        "deezer.android.app" -> "Deezer"
        "com.soundcloud.android" -> "SoundCloud"
        "com.apple.android.music" -> "Apple Music"
        "com.miui.player" -> "Mi Music"
        "com.sec.android.app.music" -> "Samsung Music"
        "com.android.music" -> "Reproductor de Android"
        "com.google.android.music" -> "Google Play Music"
        "tv.danmaku.bili" -> "Bilibili"
        else -> packageName
            .substringAfterLast('.')
            .replaceFirstChar { it.uppercase() }
            .ifBlank { ParSonicConstants.UNKNOWN_MEDIA_LABEL }
    }

    // --- Comandos ------------------------------------------------------------

    override fun play() = withController("play") { it.transportControls.play() }

    override fun pause() = withController("pause") { it.transportControls.pause() }

    override fun togglePlayPause() = withController("toggle") { controller ->
        val isPlaying = controller.playbackState?.state == PlaybackStatusMapper.STATE_PLAYING
        if (isPlaying) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    override fun skipToNext() = withController("next") { it.transportControls.skipToNext() }

    override fun skipToPrevious() =
        withController("previous") { it.transportControls.skipToPrevious() }

    override fun stopPlayback() = withController("stop") { it.transportControls.stop() }

    override fun seekForward(deltaMs: Long) = seekBy(deltaMs)

    override fun seekBackward(deltaMs: Long) = seekBy(-deltaMs)

    private fun seekBy(deltaMs: Long) {
        withController("seek") { controller ->
            val playback = controller.playbackState ?: return@withController
            val duration = controller.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
            var target = (playback.position + deltaMs).coerceAtLeast(0L)
            if (duration > 0L) target = target.coerceAtMost(duration)
            controller.transportControls.seekTo(target)
        }
    }

    private inline fun withController(command: String, block: (MediaController) -> Unit) {
        val controller = this.controller
        if (controller == null) {
            Logg.d(TAG, "Comando '$command' ignorado: no hay sesion activa")
            return
        }
        runCatching { block(controller) }
            .onFailure { Logg.w(TAG, "El reproductor rechazo el comando '$command'", it) }
    }

    private companion object {
        const val TAG = "MediaSession"
    }
}
