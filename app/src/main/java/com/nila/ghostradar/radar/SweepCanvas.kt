package com.nila.ghostradar.radar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.nila.ghostradar.ui.theme.RadarGreen
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * The radar dish: three glowing rings, a crosshair, a rotating sweep wedge, random fading blips
 * and, when [lockedBlip] is set, a bright pulsing echo for the real target.
 * Draws square; put it in a square slot.
 */
@Composable
fun SweepCanvas(
    modifier: Modifier = Modifier,
    lockedBlip: Blip? = null,
    colour: Color = RadarGreen,
    sweepPeriodMs: Int = 2_000,
) {
    val transition = rememberInfiniteTransition(label = "radar")
    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(sweepPeriodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
        label = "pulse",
    )

    var blips by remember { mutableStateOf(Blips.random(7)) }
    LaunchedEffect(sweepPeriodMs) {
        while (true) {
            delay(sweepPeriodMs.toLong())
            blips = Blips.shuffle(blips, howMany = 2)
        }
    }

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f * 0.95f
        val ringStroke = 1.5.dp.toPx()

        // Faint dish fill so the black has some depth.
        drawCircle(colour.copy(alpha = 0.05f), radius = outer, center = centre)

        // Three rings with a soft outer glow.
        for (i in 1..3) {
            val r = outer * i / 3f
            drawCircle(colour.copy(alpha = 0.12f), radius = r, center = centre, style = Stroke(ringStroke * 4))
            drawCircle(colour.copy(alpha = 0.7f), radius = r, center = centre, style = Stroke(ringStroke))
        }
        // Crosshair.
        drawLine(colour.copy(alpha = 0.35f), Offset(centre.x - outer, centre.y), Offset(centre.x + outer, centre.y), ringStroke)
        drawLine(colour.copy(alpha = 0.35f), Offset(centre.x, centre.y - outer), Offset(centre.x, centre.y + outer), ringStroke)

        // Sweep wedge: a sweep gradient that is transparent for most of the circle and brightens
        // toward the leading edge, rotated so the edge sits at sweepAngle (0° = top).
        rotate(degrees = sweepAngle - 90f, pivot = centre) {
            drawCircle(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.75f to Color.Transparent,
                    0.97f to colour.copy(alpha = 0.35f),
                    1.0f to colour.copy(alpha = 0.85f),
                    center = centre,
                ),
                radius = outer,
                center = centre,
            )
            drawLine(colour, centre, Offset(centre.x + outer, centre.y), ringStroke * 1.5f)
        }

        // Random echoes.
        for (b in blips) {
            val a = Blips.brightness(b.angle, sweepAngle)
            if (a <= 0f) continue
            drawBlip(centre, outer, b, colour.copy(alpha = a), glow = a * 0.25f)
        }

        // The real one.
        lockedBlip?.let { b ->
            drawBlip(centre, outer, b.copy(size = b.size * pulse), colour, glow = 0.5f * pulse)
        }
        drawCircle(colour, radius = 3.dp.toPx(), center = centre)
    }
}

private fun DrawScope.drawBlip(centre: Offset, outer: Float, b: Blip, colour: Color, glow: Float) {
    val rad = Math.toRadians(b.angle.toDouble() - 90.0)
    val p = Offset(
        x = centre.x + (outer * b.radius * cos(rad)).toFloat(),
        y = centre.y + (outer * b.radius * sin(rad)).toFloat(),
    )
    val r = 4.dp.toPx() * b.size
    drawCircle(colour.copy(alpha = glow), radius = r * 3f, center = p)
    drawCircle(colour, radius = r, center = p)
}
