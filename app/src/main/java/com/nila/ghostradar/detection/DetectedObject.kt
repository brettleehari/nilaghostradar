package com.nila.ghostradar.detection

/** Axis-aligned box in image pixels (upright image, origin top-left). Pure Kotlin so it is unit-testable. */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/** One detection from the model, decoupled from MediaPipe types. */
data class DetectedObject(
    val label: String,
    val score: Float,
    val box: Box,
)

/** Everything the detector reports for one analysed camera frame. */
data class DetectionFrame(
    /** Sorted by score, highest first. */
    val objects: List<DetectedObject>,
    val inferenceTimeMs: Long,
    /** Size of the upright image the boxes refer to. */
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
)
