package com.nila.ghostradar.detection

import com.nila.ghostradar.radar.Blips
import com.nila.ghostradar.radar.fakeDistanceMetres
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RadarMathTest {
    private val eps = 1e-4f

    @Test
    fun `blip is brightest just behind the sweep and dark a revolution later`() {
        assertEquals(1f, Blips.brightness(blipAngle = 90f, sweepAngle = 90f), eps)
        assertEquals(0.5f, Blips.brightness(blipAngle = 90f, sweepAngle = 240f), eps)
        assertEquals(0f, Blips.brightness(blipAngle = 90f, sweepAngle = 30f), eps)   // 300° behind
        assertEquals(0f, Blips.brightness(blipAngle = 90f, sweepAngle = 89f), eps)   // not yet reached
    }

    @Test
    fun `brightness wraps around 360`() {
        assertEquals(Blips.brightness(350f, 10f), Blips.brightness(-10f, 370f), eps)
    }

    @Test
    fun `random blips stay inside the dish`() {
        val rng = Random(7)
        repeat(200) {
            val b = Blips.random(rng)
            assertTrue(b.angle in 0f..360f)
            assertTrue(b.radius in 0.1f..1f)
        }
    }

    @Test
    fun `shuffle keeps the count`() {
        val blips = Blips.random(7, Random(1))
        assertEquals(7, Blips.shuffle(blips, 3, Random(2)).size)
        assertEquals(emptyList<Any>(), Blips.shuffle(emptyList(), 3))
    }

    @Test
    fun `fake distance shrinks as the box grows and is clamped`() {
        assertTrue(fakeDistanceMetres(0.1f) > fakeDistanceMetres(0.5f))
        assertEquals(5f, fakeDistanceMetres(0.0f), eps)
        assertEquals(0.45f, fakeDistanceMetres(1f), eps)
        assertTrue(fakeDistanceMetres(0.9f) >= 0.3f)
    }
}
