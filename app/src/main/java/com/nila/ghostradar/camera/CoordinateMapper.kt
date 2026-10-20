/*
 * Derived from google-ai-edge/mediapipe-samples
 * (examples/object_detection/android-jetpack-compose), Copyright 2023 The TensorFlow Authors.
 * Licensed under the Apache License, Version 2.0; see LICENSE-upstream-mediapipe-samples.txt.
 * Modifications Copyright 2026 Ghost Radar authors.
 */
package com.nila.ghostradar.camera

import com.nila.ghostradar.detection.Box

/** Width/height pair without Android or Compose dependencies. */
data class FitSize(val width: Float, val height: Float)

/**
 * Scales [boxSize] down (or up) to fit inside [containerSize] while preserving its aspect ratio.
 * Used to size the camera preview so the whole analysed frame is visible, never cropped.
 */
fun fittedBoxSize(containerSize: FitSize, boxSize: FitSize): FitSize {
    val boxAspect = boxSize.width / boxSize.height
    val containerAspect = containerSize.width / containerSize.height
    return if (boxAspect > containerAspect) {
        FitSize(width = containerSize.width, height = containerSize.width / boxAspect)
    } else {
        FitSize(width = containerSize.height * boxAspect, height = containerSize.height)
    }
}

/**
 * Describes how an upright analysis image of [imageWidth]x[imageHeight] is shown in a preview of
 * [previewWidth]x[previewHeight] (same aspect ratio, as guaranteed by [fittedBoxSize]).
 * [mirrored] is true for the front camera, whose preview is shown flipped horizontally.
 */
data class FrameGeometry(
    val imageWidth: Int,
    val imageHeight: Int,
    val previewWidth: Float,
    val previewHeight: Float,
    val mirrored: Boolean = false,
)

/** Maps a box in image pixels to preview coordinates (same units as [FrameGeometry.previewWidth]). */
fun Box.toPreview(g: FrameGeometry): Box {
    val sx = g.previewWidth / g.imageWidth
    val sy = g.previewHeight / g.imageHeight
    val l = left * sx
    val r = right * sx
    return if (g.mirrored) {
        Box(left = g.previewWidth - r, top = top * sy, right = g.previewWidth - l, bottom = bottom * sy)
    } else {
        Box(left = l, top = top * sy, right = r, bottom = bottom * sy)
    }
}

/**
 * Rotates a box given in a raw sensor frame of [srcWidth]x[srcHeight] by [rotationDegrees]
 * clockwise (0, 90, 180, 270) into the upright frame. Only needed when detection runs on the
 * unrotated frame; [ObjectDetectorHelper] rotates the bitmap first, so this is the fallback path.
 */
fun Box.rotate(rotationDegrees: Int, srcWidth: Int, srcHeight: Int): Box = when (((rotationDegrees % 360) + 360) % 360) {
    0 -> this
    90 -> Box(left = srcHeight - bottom, top = left, right = srcHeight - top, bottom = right)
    180 -> Box(left = srcWidth - right, top = srcHeight - bottom, right = srcWidth - left, bottom = srcHeight - top)
    270 -> Box(left = top, top = srcWidth - right, right = bottom, bottom = srcWidth - left)
    else -> throw IllegalArgumentException("rotation must be a multiple of 90, was $rotationDegrees")
}

/** Horizontal offset of the box centre from the preview centre, in -1..1. Drives the radar arrow. */
fun Box.horizontalOffset(g: FrameGeometry): Float {
    val p = toPreview(g)
    return ((p.centerX / g.previewWidth) * 2f - 1f).coerceIn(-1f, 1f)
}
