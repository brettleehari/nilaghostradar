package com.nila.ghostradar.detection

import com.nila.ghostradar.scare.GhostState
import com.nila.ghostradar.scare.GhostStateMachine
import com.nila.ghostradar.scare.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GhostStateMachineTest {

    private val target = Target("teddy bear", 0.9f, Box(0f, 0f, 10f, 10f), horizontalOffset = 0.2f, proximity = 0.3f)
    private val other = target.copy(horizontalOffset = -0.5f)

    private fun locked(t: Target = target, just: Boolean = false) =
        DetectionGate.Output(locked = true, target = null, justLocked = just, justLost = false) to t
    private val unlocked = DetectionGate.Output(locked = false, target = null, justLocked = false, justLost = false) to null

    private fun GhostStateMachine.feed(p: Pair<DetectionGate.Output, Target?>) = onGate(p.first, p.second)

    @Test
    fun `stays searching while the gate is open`() {
        val m = GhostStateMachine()
        repeat(5) { assertEquals(GhostState.Searching, m.feed(unlocked)) }
    }

    @Test
    fun `lock then next frame starts the scare`() {
        val m = GhostStateMachine()
        assertEquals(GhostState.Locked(target), m.feed(locked(just = true)))
        assertEquals(GhostState.Scaring(target), m.feed(locked()))
    }

    @Test
    fun `losing the lock right after locking cancels without a scare`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true))
        assertEquals(GhostState.Searching, m.feed(unlocked))
    }

    @Test
    fun `target keeps updating while scaring`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true)); m.feed(locked())
        assertEquals(GhostState.Scaring(other), m.feed(locked(other)))
    }

    @Test
    fun `scare finishing moves to cooldown and keeps the target`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true)); m.feed(locked())
        assertEquals(GhostState.Cooldown(target), m.onScareFinished())
    }

    @Test
    fun `pointing away mid-scare moves to cooldown`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true)); m.feed(locked())
        assertTrue(m.feed(unlocked) is GhostState.Cooldown)
    }

    @Test
    fun `one scare per lock - cooldown holds while the doll stays in frame`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true)); m.feed(locked())
        m.onScareFinished()
        m.onCooldownFinished()
        assertTrue("still locked, must not restart", m.feed(locked()) is GhostState.Cooldown)
        assertEquals(GhostState.Searching, m.feed(unlocked))
    }

    @Test
    fun `cooldown waits for its timer even if the doll is gone`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true)); m.feed(locked())
        m.feed(unlocked) // → Cooldown
        assertTrue(m.feed(unlocked) is GhostState.Cooldown)
        assertEquals(GhostState.Searching, m.onCooldownFinished())
    }

    @Test
    fun `a fresh lock after cooldown scares again`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true)); m.feed(locked()); m.onScareFinished(); m.onCooldownFinished(); m.feed(unlocked)
        assertEquals(GhostState.Locked(target), m.feed(locked(just = true)))
        assertEquals(GhostState.Scaring(target), m.feed(locked()))
    }

    @Test
    fun `hold keeps Locked until released, then scares`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true))
        assertEquals(GhostState.Locked(target), m.onGate(locked().first, target, holdScare = true))
        assertEquals(GhostState.Locked(other), m.onGate(locked().first, other, holdScare = true))
        assertEquals(GhostState.Scaring(other), m.onGate(locked().first, other, holdScare = false))
    }

    @Test
    fun `hold does not keep a lost lock`() {
        val m = GhostStateMachine()
        m.feed(locked(just = true))
        assertEquals(GhostState.Searching, m.onGate(unlocked.first, null, holdScare = true))
    }

    @Test
    fun `an unhaunted lock never scares and ends on release`() {
        val m = GhostStateMachine(decideHaunted = { false })
        assertEquals(GhostState.Locked(target, haunted = false), m.feed(locked(just = true)))
        repeat(50) { assertEquals(GhostState.Locked(target, haunted = false), m.feed(locked())) }
        assertEquals(GhostState.Searching, m.feed(unlocked))
    }

    @Test
    fun `haunted decision is asked once per lock`() {
        var calls = 0
        val m = GhostStateMachine(decideHaunted = { calls++; true })
        m.feed(locked(just = true)); m.feed(locked()); m.feed(locked())
        assertEquals(1, calls)
        m.onScareFinished(); m.onCooldownFinished(); m.feed(unlocked)
        m.feed(locked(just = true))
        assertEquals(2, calls)
    }

    @Test
    fun `scare finished is ignored outside scaring`() {
        val m = GhostStateMachine()
        assertEquals(GhostState.Searching, m.onScareFinished())
        assertEquals(GhostState.Searching, m.onCooldownFinished())
    }

    @Test
    fun `a faint trace can wake up and then scares`() {
        val m = GhostStateMachine(decideHaunted = { false })
        assertEquals(GhostState.Locked(target, haunted = false), m.feed(locked(just = true)))
        assertEquals(GhostState.Locked(target, haunted = false), m.feed(locked()))
        assertEquals(GhostState.Locked(target, haunted = true), m.wakeUp())
        assertTrue(m.feed(locked()) is GhostState.Scaring)
    }

    @Test
    fun `wake up is a no-op outside a faint trace`() {
        val m = GhostStateMachine()
        assertEquals(GhostState.Searching, m.wakeUp())
        m.feed(locked(just = true)); m.feed(locked())
        assertTrue(m.wakeUp() is GhostState.Scaring)
    }
}
