package com.andrespar2.parsonic.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingTest {

    private fun sample(
        title: String = "Cancion",
        artist: String = "Artista",
        album: String = "Album",
        appLabel: String = "Spotify",
        status: PlaybackStatus = PlaybackStatus.PLAYING
    ) = NowPlaying(
        packageName = "com.spotify.music",
        appLabel = appLabel,
        title = title,
        artist = artist,
        album = album,
        status = status
    )

    @Test
    fun `subtitle combina artista y album cuando son distintos`() {
        assertEquals("Artista · Album", sample().subtitle)
    }

    @Test
    fun `subtitle no repite el album si coincide con el artista`() {
        assertEquals("Artista", sample(artist = "Artista", album = "Artista").subtitle)
    }

    @Test
    fun `subtitle cae al nombre de la app si no hay metadatos`() {
        assertEquals("Spotify", sample(artist = "", album = "").subtitle)
    }

    @Test
    fun `isPlaying depende del estado`() {
        assertTrue(sample().isPlaying)
        assertFalse(sample(status = PlaybackStatus.PAUSED).isPlaying)
    }

    @Test
    fun `hasMetadata es falso si todo esta vacio`() {
        assertFalse(sample(title = "", artist = "").hasMetadata)
        assertTrue(sample(title = "").hasMetadata)
    }

    @Test
    fun `los valores por defecto describen una sesion sin capacidades`() {
        val nowPlaying = NowPlaying(packageName = "x", appLabel = "X")
        assertFalse(nowPlaying.canSkipNext)
        assertFalse(nowPlaying.canSkipPrevious)
        assertFalse(nowPlaying.canSeek)
        assertFalse(nowPlaying.canTogglePlayback)
        assertEquals(0L, nowPlaying.durationMs)
        assertEquals(PlaybackStatus.UNKNOWN, nowPlaying.status)
    }
}
