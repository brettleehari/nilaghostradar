/*
 * Derived from google-ai-edge/mediapipe-samples
 * (examples/object_detection/android-jetpack-compose), Copyright 2023 The TensorFlow Authors.
 * Licensed under the Apache License, Version 2.0; see LICENSE-upstream-mediapipe-samples.txt.
 * Modifications Copyright 2026 Ghost Radar authors.
 */
package com.nila.ghostradar.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nila.ghostradar.detection.DetectionFrame
import com.nila.ghostradar.detection.TRIGGER_LABELS
import com.nila.ghostradar.ui.theme.RadarGreen

/**
 * Debug overlay: draws every detection box with its label and score. Must fill exactly the same
 * area as the camera preview it sits on, because it scales image pixels to its own size.
 * Trigger labels are drawn in radar green, everything else in grey.
 */
@Composable
fun ResultsOverlay(
    frame: DetectionFrame,
    modifier: Modifier = Modifier,
    mirrored: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxSize()) {
        val g = FrameGeometry(
            imageWidth = frame.imageWidth,
            imageHeight = frame.imageHeight,
            previewWidth = size.width,
            previewHeight = size.height,
            mirrored = mirrored,
        )
        for (obj in frame.objects) {
            val b = obj.box.toPreview(g)
            val colour = if (obj.label in TRIGGER_LABELS) RadarGreen else Color.LightGray
            drawRect(
                color = colour,
                topLeft = Offset(b.left, b.top),
                size = Size(b.width, b.height),
                style = Stroke(width = 3.dp.toPx()),
            )
            val label = "%s %.2f".format(obj.label, obj.score)
            val layout = textMeasurer.measure(label, TextStyle(color = Color.White, fontSize = 12.sp))
            drawRect(
                color = Color.Black.copy(alpha = 0.7f),
                topLeft = Offset(b.left, b.top),
                size = Size(layout.size.width + 8.dp.toPx(), layout.size.height.toFloat()),
            )
            drawText(layout, topLeft = Offset(b.left + 4.dp.toPx(), b.top))
        }
    }
}
