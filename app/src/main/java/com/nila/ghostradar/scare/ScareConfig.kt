package com.nila.ghostradar.scare

/**
 * Every tunable number for the scare in one place. Tune with Nila on a real device.
 * Durations in milliseconds, fractions relative to the detection box.
 *
 * The scare: at t=0 the stinger and haptics fire, the doll cutout (a crop of the live camera
 * frame) appears over the doll, its face crossfades to the evil face, and the whole doll lunges
 * toward the viewer, growing [approachScale]x over [approachMs], then holds for [holdMs].
 */
data class ScareConfig(
    /** Extra margin around the detection box when cutting the doll out of the frame. */
    val cutoutMarginFraction: Float = 0.12f,
    /** Where the doll's face sits inside the cutout: centre x/y as fractions of its size. */
    val faceCenterXFraction: Float = 0.50f,
    val faceCenterYFraction: Float = 0.24f,
    /** Face width as a fraction of the cutout width (height follows the PNG's aspect). */
    val faceWidthFraction: Float = 0.52f,
    /** Doll → evil face crossfade. */
    val faceCrossfadeMs: Int = 250,
    /** Small rise before the lunge, as a fraction of box height. */
    val floatUpFraction: Float = 0.06f,
    val floatUpMs: Int = 250,
    /** The whole doll grows to this toward screen centre. */
    val approachScale: Float = 3.2f,
    val approachMs: Int = 800,
    /** Stays huge for this long so it registers before fading. */
    val holdMs: Int = 700,
    /** ± rotation jitter while lunging, degrees. */
    val jitterDegrees: Float = 3f,
    /** Overlay fade when Cooldown starts. */
    val fadeOutMs: Int = 400,
    /** Cooldown ends when the song ends or after this, whichever is shorter. */
    val cooldownMaxMs: Long = 8_000L,
    /** Spoken by the phone the moment it locks, before the bang. Empty disables it. */
    val voiceLine: String = "Bloody Mary",
    /** The scare waits for the voice line at most this long. */
    val voiceMaxHoldMs: Long = 1_500L,
) {
    /** Total scare timeline before the state machine moves on to Cooldown. */
    val scareDurationMs: Long get() = (floatUpMs + approachMs + holdMs).toLong()
}
