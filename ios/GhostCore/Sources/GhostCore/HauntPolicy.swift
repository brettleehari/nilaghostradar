import Foundation

/// Decides, each time the radar locks on something, whether this one is haunted.
/// Rarity is the prank: most toys are "faint traces" and nothing happens; now and then one is not.
public final class HauntPolicy {
    public struct Config {
        public var warmUpMs: Int64 = 15_000
        public var minGapMs: Int64 = 45_000
        public var chance: Float = 0.3
        public var guaranteeAfterSkips: Int = 3
        public var maxPerSession: Int = 0
        public var sessionResetAfterMs: Int64 = 60_000
        /// A faint trace that is held gets a fresh roll this often (the view model asks).
        public var rerollMs: Int64 = 10_000
        public init() {}
    }

    private let config: Config
    private var random: () -> Float
    private var sessionStartMs = Int64.min
    private var lastScareMs = Int64.min
    private var skippedInARow = 0
    private var scares = 0
    private var forceNext = false
    private var pausedAtMs = Int64.min

    /// `random` returns a value in 0..<1; injected so the dice are unit-tested.
    public init(config: Config = Config(), random: @escaping () -> Float = { Float.random(in: 0..<1) }) {
        self.config = config; self.random = random
    }

    public func startSession(nowMs: Int64) {
        sessionStartMs = nowMs; lastScareMs = Int64.min; skippedInARow = 0; scares = 0
        forceNext = false; pausedAtMs = Int64.min
    }

    public func onPaused(nowMs: Int64) { pausedAtMs = nowMs }

    /// Away for sessionResetAfterMs or longer starts a new session. Returns true when it restarted.
    @discardableResult
    public func onResumed(nowMs: Int64) -> Bool {
        let away = pausedAtMs == Int64.min ? 0 : nowMs - pausedAtMs
        pausedAtMs = Int64.min
        if away < config.sessionResetAfterMs { return false }
        startSession(nowMs: nowMs)
        return true
    }

    public func forceNextHaunt() { forceNext = true }

    public func decide(nowMs: Int64) -> Bool {
        if sessionStartMs == Int64.min { sessionStartMs = nowMs }
        if forceNext { forceNext = false; return haunt(nowMs) }
        if config.maxPerSession > 0 && scares >= config.maxPerSession { return skip() }
        if nowMs - sessionStartMs < config.warmUpMs { return skip() }
        if lastScareMs != Int64.min && nowMs - lastScareMs < config.minGapMs { return skip() }
        if skippedInARow >= config.guaranteeAfterSkips { return haunt(nowMs) }
        return random() < config.chance ? haunt(nowMs) : skip()
    }

    private func haunt(_ nowMs: Int64) -> Bool { lastScareMs = nowMs; skippedInARow = 0; scares += 1; return true }
    private func skip() -> Bool { skippedInARow += 1; return false }
}
