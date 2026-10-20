package com.nila.ghostradar.radar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.nila.ghostradar.ui.theme.RadarGreen

/**
 * Find My-style arrow. [angleDegrees] is where it should point (0 = straight up, positive =
 * clockwise); the spring makes it swing there rather than snap.
 */
@Composable
fun DirectionArrow(
    angleDegrees: Float,
    modifier: Modifier = Modifier,
    colour: Color = RadarGreen,
    lit: Boolean = false,
) {
    val angle by animateFloatAsState(
        targetValue = angleDegrees,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "arrow",
    )
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val h = size.minDimension * 0.42f
        val w = size.minDimension * 0.30f
        val path = Path().apply {
            moveTo(c.x, c.y - h)            // tip
            lineTo(c.x + w, c.y + h * 0.7f) // right foot
            lineTo(c.x, c.y + h * 0.35f)    // notch
            lineTo(c.x - w, c.y + h * 0.7f) // left foot
            close()
        }
        rotate(angle, pivot = c) {
            if (lit) drawPath(path, colour.copy(alpha = 0.35f), style = Stroke(width = 14.dp.toPx(), join = StrokeJoin.Round))
            drawPath(path, if (lit) colour else colour.copy(alpha = 0.9f))
            drawPath(path, Color.Black.copy(alpha = 0.35f), style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
        }
    }
}
