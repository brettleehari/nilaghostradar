package com.nila.ghostradar.scare

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import com.nila.ghostradar.R
import com.nila.ghostradar.camera.FrameGeometry
import com.nila.ghostradar.camera.toPreview
import kotlinx.coroutines.launch

/**
 * The evil face over the doll. Must fill exactly the camera preview's bounds (it maps the target's
 * image-pixel box onto its own size).
 *
 * Timeline from [ScareConfig], starting the moment the state becomes Scaring:
 *  - t=0: face crossfades in over [ScareConfig.faceCrossfadeMs] while the doll rises by
 *    [ScareConfig.floatUpFraction] of its box height over [ScareConfig.floatUpMs].
 *  - then it approaches: scales 1x → [ScareConfig.approachScale] toward the centre of the preview
 *    over [ScareConfig.approachMs], with ±[ScareConfig.jitterDegrees] rotation jitter.
 *  - Cooldown: fades out over [ScareConfig.fadeOutMs].
 *
 * The face follows the live target box while Scaring, so it sticks to the doll if the phone moves.
 */
@Composable
fun ScareOverlay(
    state: GhostState,
    config: ScareConfig,
    modifier: Modifier = Modifier,
) {
    val faceAlpha = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val approach = remember { Animatable(0f) }

    val scaring = state is GhostState.Scaring
    val cooling = state is GhostState.Cooldown
    LaunchedEffect(scaring, cooling) {
        when {
            scaring -> {
                rise.snapTo(0f); approach.snapTo(0f); faceAlpha.snapTo(0f)
                launch { faceAlpha.animateTo(1f, tween(config.faceCrossfadeMs, easing = LinearEasing)) }
                rise.animateTo(1f, tween(config.floatUpMs, easing = FastOutSlowInEasing))
                approach.animateTo(1f, tween(config.approachMs, easing = FastOutSlowInEasing))
            }
            cooling -> faceAlpha.animateTo(0f, tween(config.fadeOutMs))
            else -> { faceAlpha.snapTo(0f); rise.snapTo(0f); approach.snapTo(0f) }
        }
    }

    val jitter by rememberInfiniteTransition(label = "jitter").animateFloat(
        initialValue = -config.jitterDegrees,
        targetValue = config.jitterDegrees,
        animationSpec = infiniteRepeatable(tween(90, easing = LinearEasing), RepeatMode.Reverse),
        label = "jitterDeg",
    )

    val target = state.targetOrNull
    if (target == null || faceAlpha.value <= 0f) return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val pw = constraints.maxWidth.toFloat()
        val ph = constraints.maxHeight.toFloat()
        val g = FrameGeometry(target.imageWidth, target.imageHeight, pw, ph)
        val b = target.box.toPreview(g)

        // Where the face centre is right now: doll box → preview centre as the approach progresses.
        val a = approach.value
        val cx = b.centerX + (pw / 2f - b.centerX) * a
        val cy = (b.centerY - rise.value * config.floatUpFraction * b.height) + (ph / 2f - b.centerY) * a
        val scale = 1f + (config.approachScale - 1f) * a
        // The face is a square sized to the box's larger side so it covers the doll's head and body.
        val side = maxOf(b.width, b.height) * 1.1f
        val sideDp = with(density) { side.toDp() }

        // PLACEHOLDER darkening so the face reads on a bright room. Nila decides the real treatment.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f * faceAlpha.value * (0.4f + 0.6f * a))))

        Image(
            painter = painterResource(R.drawable.evil_face),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(sideDp)
                .graphicsLayer {
                    translationX = cx - side / 2f
                    translationY = cy - side / 2f
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (a > 0f) jitter else 0f
                    alpha = faceAlpha.value
                    transformOrigin = TransformOrigin.Center
                },
        )
    }
}
