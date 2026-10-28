package com.nila.ghostradar.scare

/**
 * The hunt, step by step, every number in one place (Hari, 2026-10-28). Same table in README.md
 * and in the iOS `HuntSequence`; change both together.
 *
 * | Step | Setting | Default |
 * |---|---|---|
 * | 1. After the hunt opens (or restarts), nothing can lock for | [noLockAfterOpenMs] | 10 s |
 * | 2. A toy is found (lock): "I can see you" plays once in full | [nearLine] | on |
 * | 3. At the end of the line, if the toy is still in view: it wakes, says "Bloody Mary" | — | — |
 * |    and the lunge + stinger fire this long after the line starts | [bangAfterLineMs] | 1.5 s |
 * |    (if the toy left during the line, nothing happens and the hunt goes on) | [scareOnlyIfStillSeen] | on |
 * | 4. After the scare has played out, nothing can lock for | [noLockAfterScareMs] | 10 s |
 * | Leaving the app this long restarts the hunt (step 1 again) | [sessionResetAfterMs] | 60 s |
 *
 * "Still in view" is the detection gate's rule: not seen for 500 ms means gone.
 */
data class HuntSequence(
    val noLockAfterOpenMs: Long = 10_000L,
    val nearLine: Boolean = true,
    val scareOnlyIfStillSeen: Boolean = true,
    val bangAfterLineMs: Long = 1_500L,
    val noLockAfterScareMs: Long = 10_000L,
    val sessionResetAfterMs: Long = 60_000L,
)
