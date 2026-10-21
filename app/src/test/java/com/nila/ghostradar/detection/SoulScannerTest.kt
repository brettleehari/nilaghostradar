package com.nila.ghostradar.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoulScannerTest {
    private fun obj(label: String, score: Float) = DetectedObject(label, score, Box(0f, 0f, 10f, 10f))

    @Test
    fun `reports the best everyday object`() {
        val s = SoulScanner()
        assertEquals("chair", s.scan(listOf(obj("cup", 0.6f), obj("chair", 0.8f)), 0))
    }

    @Test
    fun `ignores weak, trigger and skipped labels`() {
        val s = SoulScanner(SoulScanner.Config(minScore = 0.55f))
        assertNull(s.scan(listOf(obj("cup", 0.5f)), 0))
        assertNull(s.scan(listOf(obj("teddy bear", 0.9f)), 0))
        assertNull(s.scan(listOf(obj("person", 0.9f)), 0))
        assertEquals("cup", s.scan(listOf(obj("teddy bear", 0.9f), obj("cup", 0.6f)), 0))
    }

    @Test
    fun `one readout per gap`() {
        val s = SoulScanner(SoulScanner.Config(minGapMs = 4_000, repeatLabelMs = 0))
        assertEquals("cup", s.scan(listOf(obj("cup", 0.9f)), 0))
        assertNull(s.scan(listOf(obj("bed", 0.9f)), 3_999))
        assertEquals("bed", s.scan(listOf(obj("bed", 0.9f)), 4_000))
    }

    @Test
    fun `same label not repeated too soon, another label can take its place`() {
        val s = SoulScanner(SoulScanner.Config(minGapMs = 0, repeatLabelMs = 20_000))
        assertEquals("cup", s.scan(listOf(obj("cup", 0.9f)), 0))
        assertNull(s.scan(listOf(obj("cup", 0.9f)), 10_000))
        assertEquals("bed", s.scan(listOf(obj("cup", 0.9f), obj("bed", 0.6f)), 10_001))
        assertEquals("cup", s.scan(listOf(obj("cup", 0.9f)), 20_000))
    }

    @Test
    fun `reset forgets everything`() {
        val s = SoulScanner(SoulScanner.Config(minGapMs = 60_000, repeatLabelMs = 60_000))
        assertEquals("cup", s.scan(listOf(obj("cup", 0.9f)), 0))
        s.reset()
        assertEquals("cup", s.scan(listOf(obj("cup", 0.9f)), 1))
    }
}
