import Foundation

/// Pure reducer for `searching → locked → scaring → cooldown → searching`.
///  - searching: the gate locking moves to locked.
///  - locked: the very next frame promotes to scaring unless the caller asks to hold (voice line
///    still playing). Losing the lock drops back to searching. An unhaunted lock (a faint trace)
///    sits here until lost, or until the caller re-rolls and `wakeUp` turns it haunted.
///  - scaring: the target keeps updating so the overlay follows the doll. Ends into cooldown.
///  - cooldown: back to searching only once the timer has run out AND the gate is released.
public final class GhostStateMachine {
    public private(set) var state: GhostState = .searching
    private let decideHaunted: () -> Bool
    private var cooldownTimerDone = false
    private var gateLocked = false

    /// `decideHaunted` is asked once per new lock. False makes that lock a faint trace.
    public init(decideHaunted: @escaping () -> Bool = { true }) { self.decideHaunted = decideHaunted }

    @discardableResult
    public func onGate(_ gate: DetectionGate.Output, target: Target?, holdScare: Bool = false) -> GhostState {
        gateLocked = gate.locked
        switch state {
        case .searching:
            if gate.locked, let t = target { state = .locked(t, haunted: decideHaunted()) }
        case .locked(let t, let haunted):
            if !gate.locked { state = .searching }
            else if !haunted || holdScare { state = .locked(target ?? t, haunted: haunted) }
            else { state = .scaring(target ?? t) }
        case .scaring(let t):
            state = gate.locked ? .scaring(target ?? t) : enterCooldown(t)
        case .cooldown(let t):
            state = (cooldownTimerDone && !gate.locked) ? .searching : .cooldown(target ?? t)
        }
        return state
    }

    /// A faint trace re-rolled as haunted: the lock is kept, the next frame scares. No-op otherwise.
    @discardableResult
    public func wakeUp() -> GhostState {
        if case .locked(let t, false) = state { state = .locked(t, haunted: true) }
        return state
    }

    @discardableResult
    public func onScareFinished() -> GhostState {
        if case .scaring(let t) = state { state = enterCooldown(t) }
        return state
    }

    @discardableResult
    public func onCooldownFinished() -> GhostState {
        cooldownTimerDone = true
        if state.isCooldown && !gateLocked { state = .searching }
        return state
    }

    private func enterCooldown(_ t: Target?) -> GhostState {
        cooldownTimerDone = false
        return .cooldown(t)
    }
}
