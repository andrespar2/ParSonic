package com.andrespar2.parsonic.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStatusMapperTest {

    @Test
    fun `traduce todos los codigos conocidos de PlaybackState`() {
        assertEquals(PlaybackStatus.PLAYING, PlaybackStatusMapper.fromPlaybackStateCode(3))
        assertEquals(PlaybackStatus.PAUSED, PlaybackStatusMapper.fromPlaybackStateCode(2))
        assertEquals(PlaybackStatus.STOPPED, PlaybackStatusMapper.fromPlaybackStateCode(1))
        assertEquals(PlaybackStatus.BUFFERING, PlaybackStatusMapper.fromPlaybackStateCode(6))
        assertEquals(PlaybackStatus.BUFFERING, PlaybackStatusMapper.fromPlaybackStateCode(8))
        assertEquals(PlaybackStatus.FAST_FORWARDING, PlaybackStatusMapper.fromPlaybackStateCode(4))
        assertEquals(PlaybackStatus.REWINDING, PlaybackStatusMapper.fromPlaybackStateCode(5))
        assertEquals(PlaybackStatus.ERROR, PlaybackStatusMapper.fromPlaybackStateCode(7))
        assertEquals(PlaybackStatus.UNKNOWN, PlaybackStatusMapper.fromPlaybackStateCode(0))
    }

    @Test
    fun `los estados de salto de pista se agrupan como SKIPPING`() {
        assertEquals(PlaybackStatus.SKIPPING, PlaybackStatusMapper.fromPlaybackStateCode(9))
        assertEquals(PlaybackStatus.SKIPPING, PlaybackStatusMapper.fromPlaybackStateCode(10))
        assertEquals(PlaybackStatus.SKIPPING, PlaybackStatusMapper.fromPlaybackStateCode(11))
    }

    @Test
    fun `un codigo desconocido no rompe la app`() {
        assertEquals(PlaybackStatus.UNKNOWN, PlaybackStatusMapper.fromPlaybackStateCode(-42))
        assertEquals(PlaybackStatus.UNKNOWN, PlaybackStatusMapper.fromPlaybackStateCode(9999))
    }

    @Test
    fun `isPlaying solo es cierto para el estado de reproduccion`() {
        assertTrue(PlaybackStatusMapper.isPlaying(3))
        assertFalse(PlaybackStatusMapper.isPlaying(2))
        assertFalse(PlaybackStatusMapper.isPlaying(6))
    }

    @Test
    fun `isActive agrupa reproduccion, buffering y saltos`() {
        assertTrue(PlaybackStatus.PLAYING.isActive)
        assertTrue(PlaybackStatus.BUFFERING.isActive)
        assertTrue(PlaybackStatus.FAST_FORWARDING.isActive)
        assertTrue(PlaybackStatus.REWINDING.isActive)
        assertFalse(PlaybackStatus.PAUSED.isActive)
        assertFalse(PlaybackStatus.STOPPED.isActive)
        assertFalse(PlaybackStatus.UNKNOWN.isActive)
    }

    @Test
    fun `formatDuration devuelve guiones cuando la duracion es desconocida`() {
        assertEquals("--:--", PlaybackStatusMapper.formatDuration(0L))
        assertEquals("--:--", PlaybackStatusMapper.formatDuration(-1L))
    }

    @Test
    fun `formatDuration formatea minutos y segundos`() {
        assertEquals("0:05", PlaybackStatusMapper.formatDuration(5_000L))
        assertEquals("0:59", PlaybackStatusMapper.formatDuration(59_999L))
        assertEquals("1:05", PlaybackStatusMapper.formatDuration(65_000L))
        assertEquals("60:00", PlaybackStatusMapper.formatDuration(3_600_000L))
    }
}
