package com.nila.ghostradar.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionGateTest {

    private val box = Box(10f, 10f, 50f, 60f)
    private fun bear(score: Float = 0.9f) = DetectedObject("teddy bear", score, box)
    private fun cat(score: Float = 0.9f) = DetectedObject("cat", score, box)

    @Test
    fun `locks only after three consecutive hits`() {
        val gate = DetectionGate()
        assertFalse(gate.onFrame(listOf(bear()), 0).locked)
        assertFalse(gate.onFrame(listOf(bear()), 33).locked)
        val third = gate.onFrame(listOf(bear()), 66)
        assertTrue(third.locked)
        assertTrue(third.justLocked)
        assertFalse(gate.onFrame(listOf(bear()), 100).justLocked)
    }

    @Test
    fun `a miss resets the consecutive count`() {
        val gate = DetectionGate()
        gate.onFrame(listOf(bear()), 0)
        gate.onFrame(listOf(bear()), 33)
        gate.onFrame(emptyList(), 66)
        gate.onFrame(listOf(bear()), 100)
        assertFalse(gate.onFrame(listOf(bear()), 133).locked)
        assertTrue(gate.onFrame(listOf(bear()), 166).locked)
    }

    @Test
    fun `non-trigger labels and low scores never count`() {
        val gate = DetectionGate()
        repeat(10) { assertFalse(gate.onFrame(listOf(cat()), it * 33L).locked) }
        repeat(10) { assertFalse(gate.onFrame(listOf(bear(0.49f)), 1000 + it * 33L).locked) }
        assertNull(gate.onFrame(listOf(cat()), 2000).target)
    }

    @Test
    fun `stays locked through a short gap and keeps the last target`() {
        val gate = DetectionGate()
        repeat(3) { gate.onFrame(listOf(bear()), it * 33L) }
        val gap = gate.onFrame(emptyList(), 66 + 400)
        assertTrue(gap.locked)
        assertEquals(box, gap.target?.box)
        assertFalse(gap.justLost)
    }

    @Test
    fun `unlocks once the target is lost for 500 ms`() {
        val gate = DetectionGate()
        repeat(3) { gate.onFrame(listOf(bear()), it * 33L) }
        assertTrue(gate.onFrame(emptyList(), 66 + 499).locked)
        val lost = gate.onFrame(emptyList(), 66 + 500)
        assertFalse(lost.locked)
        assertTrue(lost.justLost)
        assertNull(lost.target)
    }

    @Test
    fun `re-locking after loss needs three fresh hits`() {
        val gate = DetectionGate()
        repeat(3) { gate.onFrame(listOf(bear()), it * 33L) }
        gate.onFrame(emptyList(), 1000)
        assertFalse(gate.isLocked)
        assertFalse(gate.onFrame(listOf(bear()), 1100).locked)
        assertFalse(gate.onFrame(listOf(bear()), 1133).locked)
        assertTrue(gate.onFrame(listOf(bear()), 1166).locked)
    }

    @Test
    fun `picks the highest scoring trigger when several are in frame`() {
        val gate = DetectionGate()
        val strong = DetectedObject("teddy bear", 0.95f, Box(0f, 0f, 1f, 1f))
        val out = gate.onFrame(listOf(bear(0.6f), cat(0.99f), strong), 0)
        assertEquals(strong, out.target)
    }

    @Test
    fun `config is honoured`() {
        val gate = DetectionGate(DetectionGate.Config(minScore = 0.8f, framesToLock = 1, lostTimeoutMs = 100))
        assertFalse(gate.onFrame(listOf(bear(0.79f)), 0).locked)
        assertTrue(gate.onFrame(listOf(bear(0.8f)), 33).locked)
        assertTrue(gate.onFrame(emptyList(), 33 + 99).locked)
        assertFalse(gate.onFrame(emptyList(), 33 + 100).locked)
    }
}
