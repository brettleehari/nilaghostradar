package com.nila.ghostradar.scare

import kotlin.random.Random

/**
 * Decides, each time the radar locks on something, whether this one is haunted.
 * Rarity is the prank: most toys are "faint traces" and nothing happens; now and then one is not.
 *
 * Rules, all in [Config]:
 *  - nothing is haunted during the first [Config.warmUpMs] of a session;
 *  - never two scares closer than [Config.minGapMs];
 *  - otherwise each lock is haunted with probability [Config.chance];
 *  - but after [Config.guaranteeAfterSkips] consecutive skipped locks the next eligible lock is
 *    haunted for sure, so a patient kid always gets one;
 *  - at most [Config.maxPerSession] scares per session (0 = unlimited).
 *
 * Pure Kotlin; the clock and the dice are injected so it is unit-tested.
 */
class HauntPolicy(
    private val config: Config = Config(),
    private val random: Random = Random.Default,
) {
    data class Config(
        val warmUpMs: Long = 15_000L,
        val minGapMs: Long = 45_000L,
        val chance: Float = 0.3f,
        val guaranteeAfterSkips: Int = 3,
        val maxPerSession: Int = 0,
    )

    private var sessionStartMs = Long.MIN_VALUE
    private var lastScareMs = Long.MIN_VALUE
    private var skippedInARow = 0
    private var scares = 0
    private var forceNext = false

    /** Call once when the hunt screen opens. */
    fun startSession(nowMs: Long) {
        sessionStartMs = nowMs
        lastScareMs = Long.MIN_VALUE
        skippedInARow = 0
        scares = 0
        forceNext = false
    }

    /** Debug/tuning: the next lock is haunted no matter what. */
    fun forceNextHaunt() {
        forceNext = true
    }

    /** Decide for a lock that just happened at [nowMs]. Records the decision. */
    fun decide(nowMs: Long): Boolean {
        if (sessionStartMs == Long.MIN_VALUE) sessionStartMs = nowMs
        if (forceNext) {
            forceNext = false
            return haunt(nowMs)
        }
        if (config.maxPerSession > 0 && scares >= config.maxPerSession) return skip()
        if (nowMs - sessionStartMs < config.warmUpMs) return skip()
        if (lastScareMs != Long.MIN_VALUE && nowMs - lastScareMs < config.minGapMs) return skip()
        if (skippedInARow >= config.guaranteeAfterSkips) return haunt(nowMs)
        return if (random.nextFloat() < config.chance) haunt(nowMs) else skip()
    }

    private fun haunt(nowMs: Long): Boolean {
        lastScareMs = nowMs
        skippedInARow = 0
        scares++
        return true
    }

    private fun skip(): Boolean {
        skippedInARow++
        return false
    }
}
