package com.nila.ghostradar.radar

import kotlin.random.Random

/** A fake echo on the radar: lights up when the sweep passes it, then fades until the next pass. */
data class Blip(
    /** Degrees clockwise from the top. */
    val angle: Float,
    /** 0 (centre) .. 1 (outer ring). */
    val radius: Float,
    val size: Float,
)

object Blips {
    fun random(count: Int, rng: Random = Random.Default): List<Blip> = List(count) { random(rng) }

    fun random(rng: Random = Random.Default): Blip = Blip(
        angle = rng.nextFloat() * 360f,
        radius = 0.15f + rng.nextFloat() * 0.8f,
        size = 0.6f + rng.nextFloat() * 0.8f,
    )

    /** Replaces [howMany] random blips so the picture slowly changes between sweeps. */
    fun shuffle(blips: List<Blip>, howMany: Int, rng: Random = Random.Default): List<Blip> {
        if (blips.isEmpty()) return blips
        val out = blips.toMutableList()
        repeat(howMany) { out[rng.nextInt(out.size)] = random(rng) }
        return out
    }

    /**
     * Brightness of a blip given the current sweep angle: 1 just behind the sweep line, fading to
     * 0 a full revolution later. Pure so it can be unit-tested.
     */
    fun brightness(blipAngle: Float, sweepAngle: Float, fadeDegrees: Float = 300f): Float {
        val behind = ((sweepAngle - blipAngle) % 360f + 360f) % 360f
        return (1f - behind / fadeDegrees).coerceIn(0f, 1f)
    }
}
