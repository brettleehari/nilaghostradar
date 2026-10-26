import Foundation

/// Turns the detector's everyday sightings (chair, cup, bed…) into "no soul" readouts.
/// Trigger labels are never reported here; they go through DetectionGate.
public final class SoulScanner {
    public struct Config {
        public var minScore: Float = 0.55
        public var minGapMs: Int64 = 4_000
        public var repeatLabelMs: Int64 = 20_000
        public var triggerLabels: Set<String> = GhostCore.triggerLabels
        /// "person · no soul" is a joke Nila has not approved; off until she says so.
        public var skipLabels: Set<String> = ["person"]
        public init() {}
    }

    private let config: Config
    private var lastReadoutMs = Int64.min
    private var lastSeenMs: [String: Int64] = [:]

    public init(config: Config = Config()) { self.config = config }

    /// The label to announce for this frame, or nil. Records what it returned.
    public func scan(_ objects: [DetectedObject], nowMs: Int64) -> String? {
        if lastReadoutMs != Int64.min && nowMs - lastReadoutMs < config.minGapMs { return nil }
        let pick = objects
            .filter { $0.score >= config.minScore }
            .filter { !config.triggerLabels.contains($0.label) && !config.skipLabels.contains($0.label) }
            .filter { lastSeenMs[$0.label].map { nowMs - $0 >= config.repeatLabelMs } ?? true }
            .max { $0.score < $1.score }
        guard let pick else { return nil }
        lastReadoutMs = nowMs
        lastSeenMs[pick.label] = nowMs
        return pick.label
    }

    public func reset() { lastReadoutMs = Int64.min; lastSeenMs.removeAll() }
}
