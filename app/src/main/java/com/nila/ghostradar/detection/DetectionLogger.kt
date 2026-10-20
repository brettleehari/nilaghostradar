package com.nila.ghostradar.detection

import android.util.Log

/**
 * Day-one test helper: logs the top-3 labels and scores of each frame to Logcat.
 * Filter with: adb logcat -s GhostRadar
 * Throttled so the log stays readable.
 */
class DetectionLogger(private val minIntervalMs: Long = 250L) {
    private var lastLogAtMs = 0L

    fun log(frame: DetectionFrame) {
        if (frame.timestampMs - lastLogAtMs < minIntervalMs) return
        lastLogAtMs = frame.timestampMs
        val top3 = frame.objects.take(3).joinToString(" | ") { o ->
            "%s %.2f [%.0f,%.0f %.0fx%.0f]".format(
                o.label, o.score, o.box.left, o.box.top, o.box.width, o.box.height,
            )
        }
        Log.i(TAG, "labels: ${top3.ifEmpty { "(none)" }}  (${frame.inferenceTimeMs} ms, ${frame.imageWidth}x${frame.imageHeight})")
    }

    companion object {
        const val TAG = "GhostRadar"
    }
}
