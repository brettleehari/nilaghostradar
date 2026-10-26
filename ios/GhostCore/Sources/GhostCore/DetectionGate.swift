import Foundation

/// Debounces raw detections into a stable "locked on a target" signal.
///  - A frame counts as a hit when it contains a trigger label with score >= minScore.
///  - framesToLock consecutive hits are needed before the gate locks (kills false scares).
///  - Once locked, the gate stays locked until no hit has been seen for lostTimeoutMs
///    (the "pointed away = normal" rule).
public final class DetectionGate {
    public struct Config {
        public var minScore: Float = 0.5
        public var framesToLock: Int = 3
        public var lostTimeoutMs: Int64 = 500
        public var triggerLabels: Set<String> = GhostCore.triggerLabels
        public init() {}
    }

    public struct Output {
        public let locked: Bool
        /// Best trigger detection in this frame, or the last one seen while still locked.
        public let target: DetectedObject?
        public let justLocked: Bool
        public let justLost: Bool
    }

    private let config: Config
    private var consecutiveHits = 0
    private var locked = false
    private var lastHitAtMs = Int64.min
    private var lastTarget: DetectedObject?

    public init(config: Config = Config()) { self.config = config }

    public var isLocked: Bool { locked }

    public func onFrame(_ objects: [DetectedObject], nowMs: Int64) -> Output {
        let hit = objects
            .filter { config.triggerLabels.contains($0.label) && $0.score >= config.minScore }
            .max { $0.score < $1.score }
        let wasLocked = locked
        if let hit {
            consecutiveHits += 1
            lastHitAtMs = nowMs
            lastTarget = hit
            if !locked && consecutiveHits >= config.framesToLock { locked = true }
        } else {
            consecutiveHits = 0
            if locked && nowMs - lastHitAtMs >= config.lostTimeoutMs {
                locked = false
                lastTarget = nil
            }
        }
        return Output(locked: locked, target: locked ? lastTarget : hit,
                      justLocked: locked && !wasLocked, justLost: !locked && wasLocked)
    }

    public func reset() {
        consecutiveHits = 0; locked = false; lastHitAtMs = Int64.min; lastTarget = nil
    }
}
