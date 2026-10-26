import Foundation

/// Find My-style "warmer / colder" signal, 0 (nothing) .. 1 (a toy fills the frame).
/// Raw heat from the strongest trigger detection's score and box height, smoothed so it never
/// flickers with one noisy frame and cools visibly when the toy leaves.
public final class SoulHeat {
    public struct Config {
        public var scoreFloor: Float = 0.3
        public var scoreCeil: Float = 0.85
        public var sizeFull: Float = 0.45
        public var scoreWeight: Float = 0.6
        public var riseFraction: Float = 0.45
        public var fallFraction: Float = 0.18
        public var triggerLabels: Set<String> = GhostCore.triggerLabels
        public init() {}
    }

    public enum Band { case cold, warm, hot }

    private let config: Config
    public private(set) var value: Float = 0

    public init(config: Config = Config()) { self.config = config }

    @discardableResult
    public func onFrame(_ objects: [DetectedObject], imageHeight: Int) -> Float {
        let best = objects.filter { config.triggerLabels.contains($0.label) }.max { $0.score < $1.score }
        let rawHeat = best.map { raw(score: $0.score, boxHeightFraction: $0.box.height / Float(max(imageHeight, 1))) } ?? 0
        let k = rawHeat > value ? config.riseFraction : config.fallFraction
        value = min(max(value + (rawHeat - value) * k, 0), 1)
        if value < 0.005 { value = 0 }
        return value
    }

    public func raw(score: Float, boxHeightFraction: Float) -> Float {
        let s = min(max((score - config.scoreFloor) / (config.scoreCeil - config.scoreFloor), 0), 1)
        let z = min(max(boxHeightFraction / config.sizeFull, 0), 1)
        return min(max(config.scoreWeight * s + (1 - config.scoreWeight) * z, 0), 1)
    }

    public func reset() { value = 0 }

    /// The word for a heat value. HERE (a haunted lock) and GONE are decided by state.
    public static func band(_ heat: Float) -> Band {
        if heat < 0.2 { return .cold }
        if heat < 0.55 { return .warm }
        return .hot
    }
}
