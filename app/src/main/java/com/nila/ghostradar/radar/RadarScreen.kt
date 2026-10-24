package com.nila.ghostradar.radar

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import com.nila.ghostradar.detection.SoulHeat
import com.nila.ghostradar.scare.GhostState
import com.nila.ghostradar.scare.Notice
import com.nila.ghostradar.scare.ProximityConfig
import com.nila.ghostradar.scare.Target
import com.nila.ghostradar.ui.theme.RadarGreen
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * The hunting screen. Black, three rings, sweeping wedge, fading blips, a big arrow and a one-word
 * readout. While Searching the arrow wanders; once something is locked it points at the target's
 * horizontal offset. Everything takes its colour and its breathing rate from [heat] (0 cold .. 1
 * hot), Find My style: green and slow when the room is clean, red and fast when a toy is close.
 *
 * Opaque by default (v1 "full-screen first"); set [backgroundAlpha] below 1 to see the camera.
 */
@Composable
fun RadarScreen(
    state: GhostState,
    modifier: Modifier = Modifier,
    notice: Notice? = null,
    heat: Float = 0f,
    proximity: ProximityConfig = ProximityConfig(),
    backgroundAlpha: Float = 1f,
) {
    val target = state.targetOrNull
    val tracking = state is GhostState.Locked || state is GhostState.Scaring
    val haunted = (state is GhostState.Locked && state.haunted) || state is GhostState.Scaring
    val colour by animateColorAsState(heatColour(heat), tween(350), label = "heatColour")
    val pulse = breathing(heat, proximity)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StatusLine(state, colour)
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = pulse; scaleY = pulse },
            contentAlignment = Alignment.Center,
        ) {
            SweepCanvas(
                modifier = Modifier.fillMaxWidth(),
                lockedBlip = target?.let { blipFor(it) },
                colour = colour,
            )
            DirectionArrow(
                angleDegrees = arrowAngle(target),
                lit = tracking,
                colour = colour,
                modifier = Modifier.size(120.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        HeatReadout(heat = heat, here = haunted, colour = colour)
        Spacer(Modifier.height(20.dp))
        RadarNotice(notice, colour = colour)
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

/**
 * Rings breathe: a sine on a phase that advances at a rate set by [heat], so a tempo change never
 * jumps. Period runs from [ProximityConfig.pulsePeriodColdMs] down to [ProximityConfig.pulsePeriodHotMs].
 */
@Composable
private fun breathing(heat: Float, proximity: ProximityConfig): Float {
    var phase by remember { mutableFloatStateOf(0f) }
    var lastNanos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dtMs = (now - lastNanos) / 1_000_000f
                    phase = (phase + dtMs / proximity.pulsePeriodMs(heat) * (2f * PI.toFloat())) % (2f * PI.toFloat())
                }
                lastNanos = now
            }
        }
    }
    return 1f + proximity.pulseAmplitude * sin(phase)
}

@Composable
private fun StatusLine(state: GhostState, colour: Color) {
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
        color = colour,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 4.sp,
        modifier = Modifier.graphicsLayer { this.alpha = alpha },
    )
}

/** One word instead of fake metres: COLD / WARM / HOT, or HERE on a haunted lock. PLACEHOLDER words for Nila. */
@Composable
private fun HeatReadout(heat: Float, here: Boolean, colour: Color) {
    val word = when {
        here -> stringResource(R.string.heat_here)
        else -> when (SoulHeat.band(heat)) {
            SoulHeat.Band.COLD -> stringResource(R.string.heat_cold)
            SoulHeat.Band.WARM -> stringResource(R.string.heat_warm)
            SoulHeat.Band.HOT -> stringResource(R.string.heat_hot)
        }
    }
    Text(
        text = word,
        color = colour,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        letterSpacing = 6.sp,
    )
    Text(
        text = stringResource(R.string.radar_heat_label),
        color = colour.copy(alpha = 0.6f),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = 3.sp,
    )
}

private const val MAX_ARROW_DEGREES = 55f
