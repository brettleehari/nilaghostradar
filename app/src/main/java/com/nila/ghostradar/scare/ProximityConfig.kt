package com.nila.ghostradar.scare

/**
 * Every number for the warmer/colder feel, in one place. Heat is 0..1 from [com.nila.ghostradar.detection.SoulHeat].
 * Colour and words are Nila's call; haptic ticks and music tempo were Hari's ask (2026-10-24) and
 * sit next to Nila's "no build-up" rule, so she must confirm them (see NOTES.md).
 */
data class ProximityConfig(
    /** Rings breathe this much (scale) and this fast: period at heat 0 and heat 1. */
    val pulseAmplitude: Float = 0.025f,
    val pulsePeriodColdMs: Int = 2_400,
    val pulsePeriodHotMs: Int = 450,
    /** Haptic ticks start at this heat; period and strength at the two ends. */
    val tickFromHeat: Float = 0.15f,
    val tickPeriodWarmMs: Long = 1_100L,
    val tickPeriodHotMs: Long = 220L,
    val tickDurationMs: Long = 18L,
    val tickStrengthMin: Float = 0.25f,
    val tickStrengthMax: Float = 0.9f,
    /** From this heat the ticks become a two-beat heartbeat; its period at that heat and at heat 1. */
    val heartbeatFromHeat: Float = 0.55f,
    val heartbeatPeriodSlowMs: Long = 950L,
    val heartbeatPeriodFastMs: Long = 480L,
    /** One hard hit when a haunted lock happens, before the whisper and the bang. */
    val joltAtLock: Boolean = true,
    /** Ambience playback speed at heat 1 (1.0 = normal). Pitch stays put. */
    val tempoMaxSpeed: Float = 1.6f,
    /** Ambience volume at heat 0 and heat 1. */
    val ambienceVolumeCold: Float = 0.6f,
    val ambienceVolumeHot: Float = 0.9f,
) {
    fun pulsePeriodMs(heat: Float): Float = lerp(pulsePeriodColdMs.toFloat(), pulsePeriodHotMs.toFloat(), heat)
    fun tickPeriodMs(heat: Float): Long = lerp(tickPeriodWarmMs.toFloat(), tickPeriodHotMs.toFloat(), warmth(heat)).toLong()
    fun tickStrength(heat: Float): Float = lerp(tickStrengthMin, tickStrengthMax, warmth(heat))
    fun tempo(heat: Float): Float = lerp(1f, tempoMaxSpeed, heat)
    fun heartbeat(heat: Float): Boolean = heat >= heartbeatFromHeat
    fun heartbeatPeriodMs(heat: Float): Long {
        val t = ((heat - heartbeatFromHeat) / (1f - heartbeatFromHeat)).coerceIn(0f, 1f)
        return lerp(heartbeatPeriodSlowMs.toFloat(), heartbeatPeriodFastMs.toFloat(), t).toLong()
    }
    fun ambienceVolume(heat: Float): Float = lerp(ambienceVolumeCold, ambienceVolumeHot, heat)

    /** 0 at [tickFromHeat], 1 at heat 1. */
    private fun warmth(heat: Float) = ((heat - tickFromHeat) / (1f - tickFromHeat)).coerceIn(0f, 1f)

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
}
