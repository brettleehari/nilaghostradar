package com.nila.ghostradar.detection

/**
 * Find My-style "warmer / colder" signal, 0 (nothing) .. 1 (a toy fills the frame).
 *
 * Each frame the strongest trigger-label detection is turned into a raw heat from its score
 * (how sure the model is) and its box height (how close it is), then smoothed: it climbs at
 * [Config.riseFraction] of the gap per frame and drops at [Config.fallFraction], so it never
 * flickers with one noisy frame and cools visibly when the toy leaves.
 *
 * Pure Kotlin; unit-tested.
 */
class SoulHeat(private val config: Config = Config()) {

    data class Config(
        /** Score at or below this adds nothing; at or above [scoreCeil] it adds the full score share. */
        val scoreFloor: Float = 0.3f,
        val scoreCeil: Float = 0.85f,
        /** Box height (fraction of the frame) at which the size share is maxed. */
        val sizeFull: Float = 0.45f,
        /** How much of the raw heat comes from the score; the rest from the size. */
        val scoreWeight: Float = 0.6f,
        val riseFraction: Float = 0.45f,
        val fallFraction: Float = 0.18f,
        val triggerLabels: Set<String> = TRIGGER_LABELS,
    )

    var value: Float = 0f
        private set

    /** Feed one frame; returns the smoothed heat. */
    fun onFrame(objects: List<DetectedObject>, imageHeight: Int): Float {
        val best = objects.filter { it.label in config.triggerLabels }.maxByOrNull { it.score }
        val raw = best?.let { raw(it.score, it.box.height / imageHeight.coerceAtLeast(1)) } ?: 0f
        val k = if (raw > value) config.riseFraction else config.fallFraction
        value = (value + (raw - value) * k).coerceIn(0f, 1f)
        if (value < 0.005f) value = 0f
        return value
    }

    /** Unsmoothed heat for one detection. */
    fun raw(score: Float, boxHeightFraction: Float): Float {
        val s = ((score - config.scoreFloor) / (config.scoreCeil - config.scoreFloor)).coerceIn(0f, 1f)
        val z = (boxHeightFraction / config.sizeFull).coerceIn(0f, 1f)
        return (config.scoreWeight * s + (1f - config.scoreWeight) * z).coerceIn(0f, 1f)
    }

    fun reset() {
        value = 0f
    }

    enum class Band { COLD, WARM, HOT }

    companion object {
        /** The word for a heat value. Thresholds are the UI's; HERE (a haunted lock) is decided by state. */
        fun band(heat: Float): Band = when {
            heat < 0.2f -> Band.COLD
            heat < 0.55f -> Band.WARM
            else -> Band.HOT
        }
    }
}
