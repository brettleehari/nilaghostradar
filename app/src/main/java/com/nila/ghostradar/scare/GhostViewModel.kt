package com.nila.ghostradar.scare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nila.ghostradar.detection.DetectedObject
import com.nila.ghostradar.detection.DetectionFrame
import com.nila.ghostradar.detection.DetectionGate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the gate, the state machine and the two timers (scare length, cooldown max).
 * Feed it every [DetectionFrame] from the camera; render from [state].
 */
class GhostViewModel(
    private val gate: DetectionGate = DetectionGate(),
    val config: ScareConfig = ScareConfig(),
) : ViewModel() {

    private val machine = GhostStateMachine()
    private val _state = MutableStateFlow<GhostState>(GhostState.Searching)
    val state: StateFlow<GhostState> = _state.asStateFlow()

    private val _lastFrame = MutableStateFlow<DetectionFrame?>(null)
    /** Latest raw frame, for the debug panel. */
    val lastFrame: StateFlow<DetectionFrame?> = _lastFrame.asStateFlow()

    private var scareTimer: Job? = null
    private var cooldownTimer: Job? = null

    fun onFrame(frame: DetectionFrame) {
        _lastFrame.value = frame
        val out = gate.onFrame(frame.objects, frame.timestampMs)
        val target = out.target?.toTarget(frame.imageWidth, frame.imageHeight)
        val before = _state.value
        val after = machine.onGate(out, target)
        _state.value = after
        onTransition(before, after)
    }

    /** Call when the song ends so the cooldown is "song end or 8 s, whichever is shorter". */
    fun onAudioFinished() {
        if (_state.value is GhostState.Cooldown) finishCooldown()
    }

    private fun onTransition(before: GhostState, after: GhostState) {
        if (before !is GhostState.Scaring && after is GhostState.Scaring) {
            scareTimer?.cancel()
            scareTimer = viewModelScope.launch {
                delay(config.scareDurationMs)
                val b = _state.value
                val a = machine.onScareFinished()
                _state.value = a
                onTransition(b, a)
            }
        }
        if (before !is GhostState.Cooldown && after is GhostState.Cooldown) {
            scareTimer?.cancel()
            cooldownTimer?.cancel()
            cooldownTimer = viewModelScope.launch {
                delay(config.cooldownMaxMs)
                finishCooldown()
            }
        }
    }

    private fun finishCooldown() {
        cooldownTimer?.cancel()
        _state.value = machine.onCooldownFinished()
    }
}

internal fun DetectedObject.toTarget(imageWidth: Int, imageHeight: Int): Target = Target(
    label = label,
    score = score,
    box = box,
    horizontalOffset = ((box.centerX / imageWidth) * 2f - 1f).coerceIn(-1f, 1f),
    proximity = (box.height / imageHeight).coerceIn(0f, 1f),
)
