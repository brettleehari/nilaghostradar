/*
 * Derived from google-ai-edge/mediapipe-samples
 * (examples/object_detection/android-jetpack-compose), Copyright 2023 The TensorFlow Authors.
 * Licensed under the Apache License, Version 2.0; see LICENSE-upstream-mediapipe-samples.txt.
 * Modifications Copyright 2026 Ghost Radar authors.
 */
package com.nila.ghostradar.camera

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nila.ghostradar.detection.DetectionFrame
import com.nila.ghostradar.detection.ObjectDetectorHelper
import java.util.concurrent.Executors

/**
 * Back-camera preview with MediaPipe object detection running on every analysed frame.
 *
 * - Preview and analysis are both 4:3; analysis targets 640x480 (PROJECT.md performance rule).
 * - The preview box is sized to the analysed frame's aspect ratio so [overlay] can map image
 *   pixels to its own bounds 1:1 (see [FrameGeometry]).
 * - [onFrame] is delivered on the main thread for every detection result.
 */
@Composable
fun CameraView(
    onFrame: (DetectionFrame, Bitmap) -> Unit,
    modifier: Modifier = Modifier,
    useGpu: Boolean = false,
    overlay: @Composable BoxScope.(DetectionFrame?) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val currentOnFrame by rememberUpdatedState(onFrame)

    var latestFrame by remember { mutableStateOf<DetectionFrame?>(null) }
    // Upright frame size; portrait 3:4 until the first result arrives.
    var frameSize by remember { mutableStateOf(FitSize(3f, 4f)) }
    var active by remember { mutableStateOf(true) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val detector = remember {
        ObjectDetectorHelper(
            context = context.applicationContext,
            useGpu = useGpu,
            listener = object : ObjectDetectorHelper.Listener {
                override fun onFrame(frame: DetectionFrame, image: Bitmap) {
                    mainExecutor.execute {
                        if (!active) return@execute
                        frameSize = FitSize(frame.imageWidth.toFloat(), frame.imageHeight.toFloat())
                        latestFrame = frame
                        currentOnFrame(frame, image)
                    }
                }

                override fun onError(message: String) {
                    Log.e(TAG, message)
                }
            },
        )
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    LaunchedEffect(lifecycleOwner) {
        val provider = ProcessCameraProvider.awaitInstance(context)
        cameraProvider = provider

        val fourByThree = AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        val preview = Preview.Builder()
            .setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(fourByThree).build())
            .build()
            .also { it.surfaceProvider = previewView.surfaceProvider }

        val analysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(fourByThree)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            .also { it.setAnalyzer(analysisExecutor, detector::detectLivestreamFrame) }

        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        Log.i(TAG, "Camera bound; analysis target ${ANALYSIS_WIDTH}x${ANALYSIS_HEIGHT}")
    }

    DisposableEffect(Unit) {
        onDispose {
            active = false
            cameraProvider?.unbindAll()
            analysisExecutor.execute { detector.close() }
            analysisExecutor.shutdown()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val fitted = fittedBoxSize(
            containerSize = FitSize(maxWidth.value, maxHeight.value),
            boxSize = frameSize,
        )
        Box(Modifier.size(fitted.width.dp, fitted.height.dp)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            overlay(latestFrame)
        }
    }
}

private const val TAG = "CameraView"
private const val ANALYSIS_WIDTH = 640
private const val ANALYSIS_HEIGHT = 480
