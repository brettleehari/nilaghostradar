package com.nila.ghostradar.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Strong, irregular rumble for the scare. Uses VibratorManager on API 31+, Vibrator below.
 * Not LocalHapticFeedback: that is a tap tick, far too weak for a jump scare.
 */
class ScareHaptics(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /** Fires the scare pattern once (~2.1 s, matching the default scare timeline). */
    fun scare() {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createWaveform(TIMINGS, AMPLITUDES, -1)
        } else {
            VibrationEffect.createWaveform(TIMINGS, -1)
        }
        v.vibrate(effect)
    }

    /**
     * One short Find My-style tap. [strength] 0..1 sets the amplitude where the motor allows it.
     * Cheap enough to call several times a second.
     */
    fun tick(strength: Float, durationMs: Long = 18L) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val amp = (strength.coerceIn(0.05f, 1f) * 255).toInt().coerceIn(1, 255)
        val effect = if (v.hasAmplitudeControl()) {
            VibrationEffect.createOneShot(durationMs, amp)
        } else {
            VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        v.vibrate(effect)
    }

    fun stop() {
        vibrator?.cancel()
    }

    companion object {
        // Off/on pairs: a hard jolt, a stutter, then a long growing rumble while the face approaches.
        private val TIMINGS = longArrayOf(0, 180, 60, 90, 50, 90, 50, 400, 80, 250, 60, 700)
        private val AMPLITUDES = intArrayOf(0, 255, 0, 200, 0, 200, 0, 160, 0, 220, 0, 255)
    }
}
