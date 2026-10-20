package com.nila.ghostradar.detection

/**
 * Debounces raw detections into a stable "locked on a target" signal.
 *
 * Rules (from PROJECT.md):
 *  - A frame counts as a hit when it contains a trigger label with score >= [Config.minScore].
 *  - [Config.framesToLock] consecutive hits are needed before the gate locks (kills false scares).
 *  - Once locked, the gate stays locked until no hit has been seen for [Config.lostTimeoutMs]
 *    (this is the "pointed away = normal" rule).
 *
 * Pure Kotlin, no Android types, so it is unit-tested on the JVM.
 */
class DetectionGate(private val config: Config = Config()) {

    data class Config(
        val minScore: Float = 0.5f,
        val framesToLock: Int = 3,
        val lostTimeoutMs: Long = 500L,
        val triggerLabels: Set<String> = TRIGGER_LABELS,
    )

    data class Output(
        val locked: Boolean,
        /** Best trigger detection in this frame, or the last one seen while still locked. */
        val target: DetectedObject?,
        /** True only on the frame where the gate flips from unlocked to locked. */
        val justLocked: Boolean,
        /** True only on the frame where the gate flips from locked to unlocked. */
        val justLost: Boolean,
    )

    private var consecutiveHits = 0
    private var locked = false
    private var lastHitAtMs = Long.MIN_VALUE
    private var lastTarget: DetectedObject? = null

    val isLocked: Boolean get() = locked

    fun onFrame(objects: List<DetectedObject>, nowMs: Long): Output {
        val hit = objects
            .filter { it.label in config.triggerLabels && it.score >= config.minScore }
            .maxByOrNull { it.score }

        val wasLocked = locked
        if (hit != null) {
            consecutiveHits++
            lastHitAtMs = nowMs
            lastTarget = hit
            if (!locked && consecutiveHits >= config.framesToLock) locked = true
        } else {
            consecutiveHits = 0
            if (locked && nowMs - lastHitAtMs >= config.lostTimeoutMs) {
                locked = false
                lastTarget = null
            }
        }

        return Output(
            locked = locked,
            target = if (locked) lastTarget else hit,
            justLocked = locked && !wasLocked,
            justLost = !locked && wasLocked,
        )
    }

    fun reset() {
        consecutiveHits = 0
        locked = false
        lastHitAtMs = Long.MIN_VALUE
        lastTarget = null
    }
}
