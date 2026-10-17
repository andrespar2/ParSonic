package com.andrespar2.parsonic.service.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureZoneCalculatorTest {

    private fun zones(
        width: Int = 1080,
        height: Int = 2400,
        edgeWidthDp: Int = 44,
        bottomHeightDp: Int = 56,
        includeBottom: Boolean = true,
        leftInset: Int = 0,
        topInset: Int = 0,
        rightInset: Int = 0,
        bottomInset: Int = 0,
        density: Float = 2.75f
    ) = GestureZoneCalculator.zones(
        displayWidthPx = width,
        displayHeightPx = height,
        edgeZoneWidthDp = edgeWidthDp,
        bottomZoneHeightDp = bottomHeightDp,
        includeBottom = includeBottom,
        leftInset = leftInset,
        topInset = topInset,
        rightInset = rightInset,
        bottomInset = bottomInset,
        density = density
    )

    @Test
    fun `una pantalla invalida no genera zonas`() {
        assertTrue(zones(width = 0).isEmpty())
        assertTrue(zones(height = 0).isEmpty())
    }

    @Test
    fun `genera las tres zonas por defecto`() {
        val result = zones()
        assertEquals(3, result.size)
        assertEquals(GestureZoneSide.LEFT, result[0].side)
        assertEquals(GestureZoneSide.RIGHT, result[1].side)
        assertEquals(GestureZoneSide.BOTTOM, result[2].side)
    }

    @Test
    fun `la zona izquierda empieza en el borde`() {
        val left = zones().first { it.side == GestureZoneSide.LEFT }
        assertEquals(0, left.left)
        assertEquals(121, left.right) // 44 dp * 2.75
        assertEquals(0, left.top)
        assertEquals(2400, left.bottom)
    }

    @Test
    fun `la zona derecha termina en el borde`() {
        val right = zones().first { it.side == GestureZoneSide.RIGHT }
        assertEquals(1080, right.right)
        assertEquals(1080 - 121, right.left)
    }

    @Test
    fun `las zonas laterales no se solapan`() {
        val result = zones()
        val left = result.first { it.side == GestureZoneSide.LEFT }
        val right = result.first { it.side == GestureZoneSide.RIGHT }
        assertTrue(left.right <= right.left)
    }

    @Test
    fun `la zona inferior queda centrada entre las laterales`() {
        val result = zones()
        val left = result.first { it.side == GestureZoneSide.LEFT }
        val right = result.first { it.side == GestureZoneSide.RIGHT }
        val bottom = result.first { it.side == GestureZoneSide.BOTTOM }
        assertEquals(left.right, bottom.left)
        assertEquals(right.left, bottom.right)
        assertEquals(2400 - 154, bottom.top) // 56 dp * 2.75
        assertEquals(2400, bottom.bottom)
    }

    @Test
    fun `el ancho de la zona se limita a un tercio de la pantalla`() {
        val result = zones(width = 300, edgeWidthDp = 96, density = 3f)
        val left = result.first { it.side == GestureZoneSide.LEFT }
        assertEquals(100, left.width) // 300 / 3
    }

    @Test
    fun `el ancho minimo es de un pixel`() {
        val result = zones(edgeWidthDp = 1, density = 0.1f)
        val left = result.first { it.side == GestureZoneSide.LEFT }
        assertEquals(1, left.width)
    }

    @Test
    fun `respeta los insets del sistema`() {
        val result = zones(leftInset = 30, topInset = 40, rightInset = 25, bottomInset = 60)
        val left = result.first { it.side == GestureZoneSide.LEFT }
        val right = result.first { it.side == GestureZoneSide.RIGHT }
        assertEquals(30, left.left)
        assertEquals(40, left.top)
        assertEquals(2400 - 60, left.bottom)
        assertEquals(1080 - 25, right.right)
    }

    @Test
    fun `sin zona inferior solo se generan las laterales`() {
        val result = zones(includeBottom = false)
        assertEquals(2, result.size)
        assertFalse(result.any { it.side == GestureZoneSide.BOTTOM })
    }
}
