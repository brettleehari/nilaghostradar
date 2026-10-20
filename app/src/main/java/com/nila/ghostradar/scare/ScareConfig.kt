package com.nila.ghostradar.scare

/**
 * Every tunable number for the scare in one place. Defaults are from the brief; tune with Nila on a
 * real device (P2). Durations in milliseconds.
 */
data class ScareConfig(
    /** Doll rises by this fraction of its box height. */
    val floatUpFraction: Float = 0.15f,
    val floatUpMs: Int = 600,
    /** Normal → evil face crossfade. */
    val faceCrossfadeMs: Int = 300,
    /** Grows from 1x to this toward screen centre. */
    val approachScale: Float = 5f,
    val approachMs: Int = 1500,
    /** ± rotation jitter while approaching, degrees. */
    val jitterDegrees: Float = 3f,
    /** Overlay fade when Cooldown starts. */
    val fadeOutMs: Int = 400,
    /** Cooldown ends when the song ends or after this, whichever is shorter. */
    val cooldownMaxMs: Long = 8_000L,
) {
    /** Total scare timeline before the state machine moves on to Cooldown. */
    val scareDurationMs: Long get() = (floatUpMs + approachMs).toLong()
}
