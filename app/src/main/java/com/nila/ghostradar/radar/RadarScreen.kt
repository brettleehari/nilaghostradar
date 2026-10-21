package com.nila.ghostradar.radar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nila.ghostradar.R
import com.nila.ghostradar.scare.GhostState
import com.nila.ghostradar.scare.Notice
import com.nila.ghostradar.scare.Target
import com.nila.ghostradar.ui.theme.RadarGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The hunting screen. Black, three green rings, sweeping wedge, fading blips, a big arrow and a
 * fake distance readout. While Searching the arrow wanders; once something is locked it points
 * at the target's horizontal offset and the readout shrinks as the box grows.
 *
 * Opaque by default (v1 "full-screen first"); set [backgroundAlpha] below 1 to see the camera.
 */
@Composable
fun RadarScreen(
    state: GhostState,
    modifier: Modifier = Modifier,
    notice: Notice? = null,
    backgroundAlpha: Float = 1f,
) {
    val target = state.targetOrNull
    val tracking = state is GhostState.Locked || state is GhostState.Scaring

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusLine(state)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SweepCanvas(
                modifier = Modifier.fillMaxWidth(),
                lockedBlip = target?.let { blipFor(it) },
            )
            DirectionArrow(
                angleDegrees = arrowAngle(target),
                lit = tracking,
                modifier = Modifier.size(120.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        DistanceReadout(target)
        Spacer(Modifier.height(20.dp))
        RadarNotice(notice)
    }
}

/** Where the echo sits: angle from the horizontal offset, radius from how close the doll looks. */
private fun blipFor(t: Target) = Blip(
    angle = t.horizontalOffset * MAX_ARROW_DEGREES,
    radius = (1f - t.proximity * 1.2f).coerceIn(0.1f, 0.95f),
    size = 1.6f,
)

@Composable
private fun arrowAngle(target: Target?): Float {
    // Wander slowly while searching; point at the target otherwise.
    var wander by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(target == null) {
        if (target == null) {
            while (true) {
                wander = Random.nextFloat() * 90f - 45f
                delay(1_600L + Random.nextLong(900L))
            }
        }
    }
    return target?.let { it.horizontalOffset * MAX_ARROW_DEGREES } ?: wander
}

@Composable
private fun StatusLine(state: GhostState) {
    val blink by rememberInfiniteTransition(label = "blink").animateFloat(
        initialValue = 1f, targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "blinkAlpha",
    )
    // PLACEHOLDER copy; Nila decides the words (open question in NOTES.md).
    val (text, alpha) = when (state) {
        GhostState.Searching -> stringResource(R.string.radar_searching) to blink
        // A faint trace keeps the status line searching; the notice slot below tells the story.
        is GhostState.Locked ->
            if (state.haunted) stringResource(R.string.radar_locked) to 1f
            else stringResource(R.string.radar_searching) to blink
        is GhostState.Scaring -> stringResource(R.string.radar_scaring) to 1f
        is GhostState.Cooldown -> stringResource(R.string.radar_cooldown) to 0.6f
    }
    Text(
        text = text,
        color = RadarGreen,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 4.sp,
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
    )
}

@Composable
private fun DistanceReadout(target: Target?) {
    // Fake. Searching: a slow random walk between 3 and 9 m. Locked: closer as the box grows.
    var drift by remember { mutableFloatStateOf(6.4f) }
    LaunchedEffect(target == null) {
        if (target == null) {
            while (true) {
                drift = (drift + Random.nextFloat() * 1.2f - 0.6f).coerceIn(3f, 9.5f)
                delay(500L)
            }
        }
    }
    val metres = target?.let { fakeDistanceMetres(it.proximity) } ?: drift
    Text(
        text = "%.1f m".format(metres),
        color = RadarGreen,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
    )
    Text(
        text = stringResource(R.string.radar_distance_label),
        color = RadarGreen.copy(alpha = 0.6f),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 3.sp,
    )
}

/** Box height fraction → pretend metres. Pure; unit-tested. */
fun fakeDistanceMetres(proximity: Float): Float =
    (0.45f / proximity.coerceAtLeast(0.05f)).coerceIn(0.3f, 5f)

private const val MAX_ARROW_DEGREES = 55f
