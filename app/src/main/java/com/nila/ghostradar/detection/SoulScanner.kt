package com.nila.ghostradar.detection

/**
 * Turns the detector's everyday sightings (chair, cup, bed…) into "no soul" readouts for the radar,
 * so the hunt feels like it is really inspecting the room. Trigger labels are never reported here;
 * they go through [DetectionGate] instead.
 *
 * Rules, all in [Config]:
 *  - only labels scoring at least [Config.minScore];
 *  - never a trigger label, never a label in [Config.skipLabels];
 *  - at most one readout every [Config.minGapMs];
 *  - the same label is not repeated within [Config.repeatLabelMs].
 *
 * Pure Kotlin; unit-tested.
 */
class SoulScanner(private val config: Config = Config()) {

    data class Config(
        val minScore: Float = 0.55f,
        val minGapMs: Long = 4_000L,
        val repeatLabelMs: Long = 20_000L,
        val triggerLabels: Set<String> = TRIGGER_LABELS,
        /** "person · no soul" is a joke Nila has not approved; off until she says so. */
        val skipLabels: Set<String> = setOf("person"),
    )

    private var lastReadoutMs = Long.MIN_VALUE
    private val lastSeenMs = HashMap<String, Long>()

    /** The label to announce for this frame, or null. Records what it returned. */
    fun scan(objects: List<DetectedObject>, nowMs: Long): String? {
        if (lastReadoutMs != Long.MIN_VALUE && nowMs - lastReadoutMs < config.minGapMs) return null
        val pick = objects.asSequence()
            .filter { it.score >= config.minScore }
            .filter { it.label !in config.triggerLabels && it.label !in config.skipLabels }
            .filter { lastSeenMs[it.label]?.let { t -> nowMs - t >= config.repeatLabelMs } ?: true }
            .maxByOrNull { it.score } ?: return null
        lastReadoutMs = nowMs
        lastSeenMs[pick.label] = nowMs
        return pick.label
    }

    fun reset() {
        lastReadoutMs = Long.MIN_VALUE
        lastSeenMs.clear()
    }
}
