package com.nila.ghostradar.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoulHeatTest {
    private val eps = 1e-4f
    private fun bear(score: Float, heightFrac: Float) =
        DetectedObject("teddy bear", score, Box(0f, 0f, 100f, 640f * heightFrac))

    @Test
    fun `raw heat grows with score and size and is clamped`() {
        val h = SoulHeat()
        assertEquals(0f, h.raw(0.3f, 0f), eps)
        assertTrue(h.raw(0.6f, 0.1f) > h.raw(0.4f, 0.1f))
        assertTrue(h.raw(0.6f, 0.3f) > h.raw(0.6f, 0.1f))
        assertEquals(1f, h.raw(0.99f, 0.9f), eps)
    }

    @Test
    fun `non-trigger objects add no heat`() {
        val h = SoulHeat()
        assertEquals(0f, h.onFrame(listOf(DetectedObject("chair", 0.95f, Box(0f, 0f, 480f, 640f))), 640), eps)
    }

    @Test
    fun `rises quickly, falls slowly, reaches zero`() {
        val h = SoulHeat(SoulHeat.Config(riseFraction = 0.5f, fallFraction = 0.2f))
        val up = h.onFrame(listOf(bear(0.9f, 0.5f)), 640)
        assertEquals(0.5f, up, eps)
        h.onFrame(listOf(bear(0.9f, 0.5f)), 640)
        assertEquals(0.75f, h.value, eps)
        val down = h.onFrame(emptyList(), 640)
        assertEquals(0.6f, down, eps)
        repeat(60) { h.onFrame(emptyList(), 640) }
        assertEquals(0f, h.value, eps)
    }

    @Test
    fun `bands`() {
        assertEquals(SoulHeat.Band.COLD, SoulHeat.band(0f))
        assertEquals(SoulHeat.Band.COLD, SoulHeat.band(0.19f))
        assertEquals(SoulHeat.Band.WARM, SoulHeat.band(0.3f))
        assertEquals(SoulHeat.Band.HOT, SoulHeat.band(0.9f))
    }
}
