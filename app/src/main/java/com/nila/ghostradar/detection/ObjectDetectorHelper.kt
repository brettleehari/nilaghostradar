/*
 * Derived from google-ai-edge/mediapipe-samples
 * (examples/object_detection/android-jetpack-compose), Copyright 2023 The TensorFlow Authors.
 * Licensed under the Apache License, Version 2.0; see LICENSE-upstream-mediapipe-samples.txt.
 * Modifications Copyright 2026 Ghost Radar authors.
 */
package com.nila.ghostradar.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult

/**
 * Wraps the MediaPipe Object Detector in LIVE_STREAM mode with the bundled EfficientDet-Lite0 int8.
 *
 * Threading: create and use it from one background thread (the ImageAnalysis executor). The GPU
 * delegate in particular must be initialised on the thread that later runs inference. Results
 * arrive on a MediaPipe thread via [Listener.onFrame].
 */
class ObjectDetectorHelper(
    private val context: Context,
    private val listener: Listener,
    private val useGpu: Boolean = false,
    /** Model-side cut-off. Kept low so the day-one log shows weak guesses; the gate applies 0.5. */
    private val scoreThreshold: Float = 0.3f,
    private val maxResults: Int = 3,
) {
    interface Listener {
        /** [image] is the upright frame the boxes refer to; valid until the next frame. */
        fun onFrame(frame: DetectionFrame, image: Bitmap)
        fun onError(message: String)
    }

    private var detector: ObjectDetector? = null
    private var initFailed = false
    private var lastTimestampMs = 0L

    /** Idempotent. Called lazily from the analysis thread so model loading never blocks the UI. */
    fun ensureInitialized(): Boolean {
        if (detector != null) return true
        if (initFailed) return false
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(if (useGpu) Delegate.GPU else Delegate.CPU)
            .build()
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setScoreThreshold(scoreThreshold)
            .setMaxResults(maxResults)
            .setResultListener(::onResult)
            .setErrorListener { e -> listener.onError(e.message ?: "Unknown detector error") }
            .build()
        return try {
            detector = ObjectDetector.createFromOptions(context, options)
            Log.i(TAG, "Object detector ready (model=$MODEL_ASSET, gpu=$useGpu)")
            true
        } catch (e: RuntimeException) {
            initFailed = true
            Log.e(TAG, "Object detector failed to initialise", e)
            listener.onError("Object detector failed to initialise: ${e.message}")
            false
        }
    }

    /** ImageAnalysis.Analyzer entry point. Always closes the proxy. */
    fun detectLivestreamFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxy.use { it.toUprightBitmap() }
        if (!ensureInitialized()) return
        // MediaPipe requires strictly increasing timestamps.
        val ts = maxOf(SystemClock.uptimeMillis(), lastTimestampMs + 1)
        lastTimestampMs = ts
        detector?.detectAsync(BitmapImageBuilder(bitmap).build(), ts)
    }

    private fun onResult(result: ObjectDetectorResult, input: MPImage) {
        val inferenceTime = SystemClock.uptimeMillis() - result.timestampMs()
        val objects = result.detections().mapNotNull { d ->
            val cat = d.categories().firstOrNull() ?: return@mapNotNull null
            val b = d.boundingBox()
            DetectedObject(
                label = cat.categoryName(),
                score = cat.score(),
                box = Box(b.left, b.top, b.right, b.bottom),
            )
        }.sortedByDescending { it.score }
        listener.onFrame(
            DetectionFrame(
                objects = objects,
                inferenceTimeMs = inferenceTime,
                imageWidth = input.width,
                imageHeight = input.height,
                timestampMs = result.timestampMs(),
            ),
            BitmapExtractor.extract(input),
        )
    }

    fun close() {
        detector?.close()
        detector = null
    }

    companion object {
        const val TAG = "ObjectDetectorHelper"
        const val MODEL_ASSET = "efficientdet_lite0_int8.tflite"
    }
}

/**
 * Copies an RGBA_8888 [ImageProxy] into an upright ARGB bitmap: honours the row stride (some
 * devices pad rows) and applies the sensor-to-display rotation so boxes come back in display
 * orientation. Does not close the proxy.
 */
internal fun ImageProxy.toUprightBitmap(): Bitmap {
    val plane = planes[0]
    val pixelStride = plane.pixelStride.coerceAtLeast(1)
    val strideWidth = plane.rowStride / pixelStride
    val buffer = plane.buffer.also { it.rewind() }
    val padded = Bitmap.createBitmap(strideWidth, height, Bitmap.Config.ARGB_8888)
    padded.copyPixelsFromBuffer(buffer)
    val rotation = imageInfo.rotationDegrees
    if (rotation == 0 && strideWidth == width) return padded
    val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(padded, 0, 0, width, height, matrix, true)
}
