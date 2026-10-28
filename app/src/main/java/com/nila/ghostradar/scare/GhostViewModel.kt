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
import com.nila.ghostradar.detection.SoulHeat
import com.nila.ghostradar.detection.SoulScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the gate, the state machine, the [HuntSequence] clock and the two timers (scare length,
 * cooldown max). Feed it every [DetectionFrame] from the camera; render from [state].
 *
 * A lock starts unhaunted: the hunt screen plays "I can see you" and calls [onNearLineDone]; if the
 * toy is still in view then, the lock wakes up (haunted), the screen says "Bloody Mary", and the
 * scare fires [HuntSequence.bangAfterLineMs] later. Nothing can lock in the first
 * [HuntSequence.noLockAfterOpenMs] of a hunt or for [HuntSequence.noLockAfterScareMs] after a scare.
 */
class GhostViewModel(
    private val gate: DetectionGate = DetectionGate(),
    val config: ScareConfig = ScareConfig(),
    val sequence: HuntSequence = HuntSequence(),
    private val scanner: SoulScanner = SoulScanner(),
    private val heatMeter: SoulHeat = SoulHeat(),
    val proximity: ProximityConfig = ProximityConfig(),
) : ViewModel() {

    // Every lock starts unhaunted; onNearLineDone wakes it up.
    private val machine = GhostStateMachine(decideHaunted = { false })

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

    private val _notice = MutableStateFlow<Notice?>(null)
    /** The small line on the radar ("SIGNAL FOUND", "CHAIR · NO SOUL"); null when there is none. */
    val notice: StateFlow<Notice?> = _notice.asStateFlow()
    private var noticeTimer: Job? = null

    private val _heat = MutableStateFlow(0f)
    /** Warmer/colder, 0..1: how close a toy looks right now. 1 while a haunted lock or scare is on. */
    val heat: StateFlow<Float> = _heat.asStateFlow()
    private var noticeCount = 0L

    private var fakeTargetUntilMs = 0L
    private var cooldownStartMs = 0L
    private var voiceHoldUntilMs = 0L
    /** The gate is fed nothing until this uptime: step 1 after opening, step 4 after a scare. */
    private var noLockUntilMs = 0L
    private var pausedAtMs = Long.MIN_VALUE

    init {
        startHunt(SystemClock.uptimeMillis())
    }

    private fun startHunt(nowMs: Long) {
        noLockUntilMs = nowMs + sequence.noLockAfterOpenMs
        Log.i("GhostRadar", "hunt started: no lock for ${sequence.noLockAfterOpenMs} ms")
    }

    /** Hunt screen left the foreground (home, screen off, another app). */
    fun onHuntPaused() {
        pausedAtMs = SystemClock.uptimeMillis()
    }

    /** Hunt screen is back. Away for [HuntSequence.sessionResetAfterMs] or longer restarts the hunt. */
    fun onHuntResumed() {
        val now = SystemClock.uptimeMillis()
        val away = if (pausedAtMs == Long.MIN_VALUE) 0L else now - pausedAtMs
        pausedAtMs = Long.MIN_VALUE
        if (away < sequence.sessionResetAfterMs) return
        scanner.reset()
        startHunt(now)
        Log.i("GhostRadar", "hunt restarted after a long absence")
    }

    /** The UI calls this when "Bloody Mary" has finished (or could not play). */
    fun onVoiceLineDone() {
        voiceHoldUntilMs = 0L
    }

    /**
     * The UI calls this when "I can see you" has finished (or could not play). If the toy is still
     * locked, it wakes up: the lock turns haunted and the scare follows.
     */
    fun onNearLineDone() {
        val before = _state.value
        if (before !is GhostState.Locked || before.haunted) return
        val after = machine.wakeUp()
        _state.value = after
        onTransition(before, after)
        Log.i("GhostRadar", "still in view at the end of the line: haunted")
    }

    /**
     * Debug/tuning aid: pretend a teddy bear sits in the middle of the frame for [durationMs],
     * ignoring the no-lock windows. Lets Hari and Nila see the whole scare without the doll.
     */
    fun injectFakeTarget(durationMs: Long = 10_000L) {
        noLockUntilMs = 0L
        // Frame timestamps are uptimeMillis, so the deadline can be too.
        fakeTargetUntilMs = SystemClock.uptimeMillis() + durationMs
        Log.i("GhostRadar", "debug: fake target injected for $durationMs ms")
    }

    fun onFrame(rawFrame: DetectionFrame, image: Bitmap? = null) {
        latestImage = image
        val frame = if (rawFrame.timestampMs < fakeTargetUntilMs) rawFrame.withFakeBear() else rawFrame
        _lastFrame.value = frame
        val now = SystemClock.uptimeMillis()
        // Steps 1 and 4: the radar still warms, but nothing can lock.
        val canLock = now >= noLockUntilMs
        val out = gate.onFrame(if (canLock) frame.objects else emptyList(), frame.timestampMs)
        val target = out.target?.toTarget(frame.imageWidth, frame.imageHeight)
        val before = _state.value
        val hold = now < voiceHoldUntilMs
        val after = machine.onGate(out, target, holdScare = hold)
        _state.value = after
        onTransition(before, after)
        val measured = heatMeter.onFrame(frame.objects, frame.imageHeight)
        _heat.value = when {
            after is GhostState.Locked && after.haunted -> 1f
            after is GhostState.Scaring -> 1f
            // Aftermath: the radar comes back red and cools to green over the cooldown.
            after is GhostState.Cooldown ->
                (1f - (now - cooldownStartMs).toFloat() / config.cooldownMaxMs).coerceIn(0f, 1f)
            else -> measured
        }
        if (after is GhostState.Searching) {
            scanner.scan(frame.objects, frame.timestampMs)?.let { show(Notice.NoSoul(it, ++noticeCount)) }
        }
    }

    /** The found notice stays up for the whole lock; everything else clears after [ScareConfig.noticeMs]. */
    private fun show(n: Notice) {
        Log.i("GhostRadar", "notice: $n")
        _notice.value = n
        noticeTimer?.cancel()
        if (n is Notice.FaintTrace) return
        noticeTimer = viewModelScope.launch {
            delay(config.noticeMs)
            if (_notice.value?.id == n.id) _notice.value = null
        }
    }

    /** Call when the song ends so the cooldown is "song end or 8 s, whichever is shorter". */
    fun onAudioFinished() {
        Log.i("GhostRadar", "scare track finished")
        if (_state.value is GhostState.Cooldown) finishCooldown()
    }

    private fun onTransition(before: GhostState, after: GhostState) {
        if (before::class != after::class) Log.i("GhostRadar", "state: ${before::class.simpleName} -> ${after::class.simpleName}")
        if (before is GhostState.Locked && after !is GhostState.Locked && _notice.value is Notice.FaintTrace) {
            _notice.value = null
        }
        val newLock = before !is GhostState.Locked && after is GhostState.Locked
        val wokeUp = before is GhostState.Locked && !before.haunted && after is GhostState.Locked && after.haunted
        if (newLock) show(Notice.FaintTrace(++noticeCount))
        if (wokeUp) {
            _notice.value = null
            voiceHoldUntilMs = if (config.voiceLine.isBlank()) 0L else SystemClock.uptimeMillis() + sequence.bangAfterLineMs
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
            cooldownStartMs = SystemClock.uptimeMillis()
            _heat.value = 1f
            scareTimer?.cancel()
            cooldownTimer?.cancel()
            cooldownTimer = viewModelScope.launch {
                delay(config.cooldownMaxMs)
                finishCooldown()
            }
        }
        if (before is GhostState.Cooldown && after is GhostState.Searching) {
            // Step 4: cool off.
            noLockUntilMs = SystemClock.uptimeMillis() + sequence.noLockAfterScareMs
            Log.i("GhostRadar", "cooling off: no lock for ${sequence.noLockAfterScareMs} ms")
        }
    }

    private fun finishCooldown() {
        cooldownTimer?.cancel()
        val before = _state.value
        _state.value = machine.onCooldownFinished()
        onTransition(before, _state.value)
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
