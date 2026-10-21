package com.nila.ghostradar.detection

import com.nila.ghostradar.scare.HauntPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class HauntPolicyTest {
    private val always = object : Random() { override fun nextBits(bitCount: Int) = 0 }        // nextFloat() = 0 → always below chance
    private val never = object : Random() { override fun nextBits(bitCount: Int) = (1 shl bitCount) - 1 } // nextFloat() ≈ 1 → never below chance

    @Test
    fun `nothing during warm-up`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 10_000, chance = 1f), always)
        p.startSession(0)
        assertFalse(p.decide(5_000))
        assertTrue(p.decide(10_000))
    }

    @Test
    fun `minimum gap between scares`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 0, minGapMs = 30_000, chance = 1f), always)
        p.startSession(0)
        assertTrue(p.decide(1_000))
        assertFalse(p.decide(20_000))
        assertTrue(p.decide(31_000))
    }

    @Test
    fun `chance zero still guarantees after N skips`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 0, minGapMs = 0, chance = 0f, guaranteeAfterSkips = 3), never)
        p.startSession(0)
        assertFalse(p.decide(1)); assertFalse(p.decide(2)); assertFalse(p.decide(3))
        assertTrue(p.decide(4))
        assertFalse(p.decide(5)) // counter reset after the haunt
    }

    @Test
    fun `max per session caps it`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 0, minGapMs = 0, chance = 1f, maxPerSession = 1), always)
        p.startSession(0)
        assertTrue(p.decide(1))
        repeat(10) { assertFalse(p.decide(10L + it)) }
    }

    @Test
    fun `force overrides warm-up and gap`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 60_000, minGapMs = 60_000, chance = 0f), never)
        p.startSession(0)
        p.forceNextHaunt(); assertTrue(p.decide(1))
        p.forceNextHaunt(); assertTrue(p.decide(2))
        assertFalse(p.decide(3))
    }

    @Test
    fun `chance is roughly honoured`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 0, minGapMs = 0, chance = 0.3f, guaranteeAfterSkips = 1_000), Random(42))
        p.startSession(0)
        val hits = (1..2000).count { p.decide(it.toLong()) }
        assertTrue("got $hits", hits in 500..700)
    }

    @Test
    fun `new session resets everything`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 0, minGapMs = 60_000, chance = 1f), always)
        p.startSession(0); assertTrue(p.decide(1))
        p.startSession(100); assertTrue(p.decide(101))
        assertEquals(false, p.decide(102))
    }

    @Test
    fun `short absence keeps the session, long absence restarts it`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 10_000, minGapMs = 0, chance = 1f, sessionResetAfterMs = 60_000), always)
        p.startSession(0)
        p.onPaused(20_000); assertFalse(p.onResumed(50_000))
        assertTrue(p.decide(50_001))                      // warm-up long over, still the same session
        p.onPaused(60_000); assertTrue(p.onResumed(120_000))
        assertFalse(p.decide(125_000))                    // fresh warm-up
        assertTrue(p.decide(130_000))
    }

    @Test
    fun `resume without a pause is a no-op`() {
        val p = HauntPolicy(HauntPolicy.Config(warmUpMs = 0, chance = 1f), always)
        p.startSession(0)
        assertFalse(p.onResumed(1_000_000))
    }
}
