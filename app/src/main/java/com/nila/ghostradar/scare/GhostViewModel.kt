package com.nila.ghostradar.scare

import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nila.ghostradar.detection.Box
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

    /** Latest upright camera frame; replaced every frame, only read on the main thread. */
    private var latestImage: Bitmap? = null

    private val _snapshot = MutableStateFlow<Bitmap?>(null)
    /** The doll cut out of the frame at the moment the scare started. Null outside a scare. */
    val snapshot: StateFlow<Bitmap?> = _snapshot.asStateFlow()

    private var fakeTargetUntilMs = 0L
    private var voiceHoldUntilMs = 0L

    /** The UI calls this when the voice line has finished (or could not play). */
    fun onVoiceLineDone() {
        voiceHoldUntilMs = 0L
    }

    /**
     * Debug/tuning aid: pretend a teddy bear sits in the middle of the frame for [durationMs].
     * Lets Hari and Nila see the whole scare without the doll.
     */
    fun injectFakeTarget(durationMs: Long = 3_000L) {
        // Frame timestamps are uptimeMillis, so the deadline can be too.
        fakeTargetUntilMs = SystemClock.uptimeMillis() + durationMs
        Log.i("GhostRadar", "debug: fake target injected for $durationMs ms")
    }

    fun onFrame(rawFrame: DetectionFrame, image: Bitmap? = null) {
        latestImage = image
        val frame = if (rawFrame.timestampMs < fakeTargetUntilMs) rawFrame.withFakeBear() else rawFrame
        _lastFrame.value = frame
        val out = gate.onFrame(frame.objects, frame.timestampMs)
        val target = out.target?.toTarget(frame.imageWidth, frame.imageHeight)
        val before = _state.value
        val hold = SystemClock.uptimeMillis() < voiceHoldUntilMs
        val after = machine.onGate(out, target, holdScare = hold)
        _state.value = after
        onTransition(before, after)
    }

    /** Call when the song ends so the cooldown is "song end or 8 s, whichever is shorter". */
    fun onAudioFinished() {
        if (_state.value is GhostState.Cooldown) finishCooldown()
    }

    private fun onTransition(before: GhostState, after: GhostState) {
        if (before !is GhostState.Locked && after is GhostState.Locked) {
            voiceHoldUntilMs = if (config.voiceLine.isBlank()) 0L else SystemClock.uptimeMillis() + config.voiceMaxHoldMs
        }
        if (before !is GhostState.Scaring && after is GhostState.Scaring) {
            _snapshot.value = latestImage?.let { cutOut(it, after.target.box, config.cutoutMarginFraction) }
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
        if (_state.value is GhostState.Searching) _snapshot.value = null
    }
}

/** Crops [box] (plus margin) out of [image], clamped to the image. */
internal fun cutOut(image: Bitmap, box: Box, margin: Float): Bitmap? {
    val mx = box.width * margin
    val my = box.height * margin
    val left = (box.left - mx).toInt().coerceIn(0, image.width - 1)
    val top = (box.top - my).toInt().coerceIn(0, image.height - 1)
    val right = (box.right + mx).toInt().coerceIn(left + 1, image.width)
    val bottom = (box.bottom + my).toInt().coerceIn(top + 1, image.height)
    return runCatching { Bitmap.createBitmap(image, left, top, right - left, bottom - top) }.getOrNull()
}

private fun DetectionFrame.withFakeBear(): DetectionFrame {
    val w = imageWidth * 0.28f
    val h = imageHeight * 0.26f
    val fake = DetectedObject(
        label = "teddy bear",
        score = 0.99f,
        box = Box(imageWidth / 2f - w / 2f, imageHeight * 0.58f - h / 2f, imageWidth / 2f + w / 2f, imageHeight * 0.58f + h / 2f),
    )
    return copy(objects = listOf(fake) + objects)
}

internal fun DetectedObject.toTarget(imageWidth: Int, imageHeight: Int): Target = Target(
    label = label,
    score = score,
    box = box,
    horizontalOffset = ((box.centerX / imageWidth) * 2f - 1f).coerceIn(-1f, 1f),
    proximity = (box.height / imageHeight).coerceIn(0f, 1f),
    imageWidth = imageWidth,
    imageHeight = imageHeight,
)
