package com.nila.ghostradar.scare

import com.nila.ghostradar.detection.DetectionGate

/**
 * Pure reducer for `Searching → Locked → Scaring → Cooldown → Searching`.
 *
 * - Searching: the gate locking moves to Locked.
 * - Locked: the very next frame promotes to Scaring (the scare starts "instantly", no build-up).
 *   Losing the lock before that drops back to Searching without a scare.
 * - Scaring: the target keeps updating so the overlay follows the doll. Ends when the scare
 *   timeline finishes or the target is lost, either way into Cooldown.
 * - Cooldown: one scare per lock. Back to Searching only once the cooldown timer has run out
 *   AND the gate is no longer locked, so a doll that stays in frame does not scare twice.
 *
 * Timers live in [GhostViewModel]; this class only receives their expiry.
 */
class GhostStateMachine {

    var state: GhostState = GhostState.Searching
        private set

    private var cooldownTimerDone = false
    private var gateLocked = false

    fun onGate(gate: DetectionGate.Output, target: Target?): GhostState {
        gateLocked = gate.locked
        state = when (val s = state) {
            GhostState.Searching ->
                if (gate.locked && target != null) GhostState.Locked(target) else s

            is GhostState.Locked ->
                if (gate.locked) GhostState.Scaring(target ?: s.target) else GhostState.Searching

            is GhostState.Scaring ->
                if (gate.locked) GhostState.Scaring(target ?: s.target)
                else enterCooldown(s.target)

            is GhostState.Cooldown ->
                if (cooldownTimerDone && !gate.locked) GhostState.Searching
                else s.copy(target = target ?: s.target)
        }
        return state
    }

    /** The scare timeline has played out. */
    fun onScareFinished(): GhostState {
        val s = state
        if (s is GhostState.Scaring) state = enterCooldown(s.target)
        return state
    }

    /** The cooldown timer (song end or max) has expired. */
    fun onCooldownFinished(): GhostState {
        cooldownTimerDone = true
        if (state is GhostState.Cooldown && !gateLocked) state = GhostState.Searching
        return state
    }

    private fun enterCooldown(target: Target?): GhostState {
        cooldownTimerDone = false
        return GhostState.Cooldown(target)
    }

    fun reset() {
        state = GhostState.Searching
        cooldownTimerDone = false
        gateLocked = false
    }
}
