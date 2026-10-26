import Foundation

/// What the detector locked on to, reduced to what the UI needs.
public struct Target: Equatable {
    public let label: String
    public let score: Float
    /// Box in upright image pixels.
    public let box: Box
    /// Horizontal offset of the box centre from frame centre, -1 (left) .. 1 (right).
    public let horizontalOffset: Float
    /// Box height as a fraction of frame height, 0..1. Bigger means closer.
    public let proximity: Float
    public let imageWidth: Int
    public let imageHeight: Int

    public init(label: String, score: Float, box: Box, horizontalOffset: Float, proximity: Float,
                imageWidth: Int = 480, imageHeight: Int = 640) {
        self.label = label; self.score = score; self.box = box
        self.horizontalOffset = horizontalOffset; self.proximity = proximity
        self.imageWidth = imageWidth; self.imageHeight = imageHeight
    }

    public init(_ o: DetectedObject, imageWidth: Int, imageHeight: Int) {
        self.init(label: o.label, score: o.score, box: o.box,
                  horizontalOffset: min(max((o.box.centerX / Float(imageWidth)) * 2 - 1, -1), 1),
                  proximity: min(max(o.box.height / Float(imageHeight), 0), 1),
                  imageWidth: imageWidth, imageHeight: imageHeight)
    }
}

/// The one state the whole app renders from.
public enum GhostState: Equatable {
    case searching
    /// haunted == false is a faint trace: the radar reacts but this lock will never scare.
    case locked(Target, haunted: Bool)
    case scaring(Target)
    case cooldown(Target?)

    public var target: Target? {
        switch self {
        case .searching: return nil
        case .locked(let t, _): return t
        case .scaring(let t): return t
        case .cooldown(let t): return t
        }
    }

    public var isSearching: Bool { if case .searching = self { return true } else { return false } }
    public var isScaring: Bool { if case .scaring = self { return true } else { return false } }
    public var isCooldown: Bool { if case .cooldown = self { return true } else { return false } }
    public var isLocked: Bool { if case .locked = self { return true } else { return false } }
    public var isHauntedLock: Bool { if case .locked(_, let h) = self { return h } else { return false } }
    public var isFaintTrace: Bool { if case .locked(_, let h) = self { return !h } else { return false } }
}

/// A small, short-lived line on the radar.
public enum Notice: Equatable {
    case faintTrace(id: Int64)
    case noSoul(label: String, id: Int64)

    public var id: Int64 {
        switch self {
        case .faintTrace(let id): return id
        case .noSoul(_, let id): return id
        }
    }
    public var isFaintTrace: Bool { if case .faintTrace = self { return true } else { return false } }
}
