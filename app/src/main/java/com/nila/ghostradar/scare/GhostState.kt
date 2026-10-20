package com.nila.ghostradar.scare

import com.nila.ghostradar.detection.Box

/** What the detector locked on to, reduced to what the UI needs. */
data class Target(
    val label: String,
    val score: Float,
    /** Box in upright image pixels. */
    val box: Box,
    /** Horizontal offset of the box centre from frame centre, -1 (left) .. 1 (right). Drives the arrow. */
    val horizontalOffset: Float,
    /** Box height as a fraction of frame height, 0..1. Bigger means closer. Drives the fake distance. */
    val proximity: Float,
)

/** The one state the whole app renders from. */
sealed interface GhostState {
    data object Searching : GhostState
    data class Locked(val target: Target) : GhostState
    data class Scaring(val target: Target) : GhostState
    data class Cooldown(val target: Target?) : GhostState

    val targetOrNull: Target?
        get() = when (this) {
            Searching -> null
            is Locked -> target
            is Scaring -> target
            is Cooldown -> target
        }
}
