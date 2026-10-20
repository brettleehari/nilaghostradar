package com.nila.ghostradar.scare

import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import com.nila.ghostradar.R
import com.nila.ghostradar.camera.FrameGeometry
import com.nila.ghostradar.camera.toPreview
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The possessed doll. Must fill exactly the camera preview's bounds (it maps the target's
 * image-pixel box onto its own size).
 *
 * At Scaring: [snapshot] (the doll cut out of the live frame) is drawn exactly where the doll is,
 * so nothing visibly changes for a split second. Then its face crossfades to the evil face, it
 * lifts slightly, and the whole doll lunges toward the viewer: scaled [ScareConfig.approachScale]x
 * toward the preview centre over [ScareConfig.approachMs] with jitter, holds, and fades on Cooldown.
 * Without a snapshot (should not happen) it falls back to the face alone.
 */
@Composable
fun ScareOverlay(
    state: GhostState,
    snapshot: Bitmap?,
    config: ScareConfig,
    modifier: Modifier = Modifier,
) {
    val overlayAlpha = remember { Animatable(0f) }
    val faceAlpha = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val approach = remember { Animatable(0f) }

    val scaring = state is GhostState.Scaring
    val cooling = state is GhostState.Cooldown
    LaunchedEffect(scaring, cooling) {
        when {
            scaring -> {
                rise.snapTo(0f); approach.snapTo(0f); faceAlpha.snapTo(0f); overlayAlpha.snapTo(1f)
                launch { faceAlpha.animateTo(1f, tween(config.faceCrossfadeMs, easing = LinearEasing)) }
                rise.animateTo(1f, tween(config.floatUpMs, easing = FastOutSlowInEasing))
                approach.animateTo(1f, tween(config.approachMs, easing = FastOutSlowInEasing))
            }
            cooling -> overlayAlpha.animateTo(0f, tween(config.fadeOutMs))
            else -> { overlayAlpha.snapTo(0f); faceAlpha.snapTo(0f); rise.snapTo(0f); approach.snapTo(0f) }
        }
    }

    val jitter by rememberInfiniteTransition(label = "jitter").animateFloat(
        initialValue = -config.jitterDegrees,
        targetValue = config.jitterDegrees,
        animationSpec = infiniteRepeatable(tween(80, easing = LinearEasing), RepeatMode.Reverse),
        label = "jitterDeg",
    )

    val target = state.targetOrNull
    if (target == null || overlayAlpha.value <= 0f) return
    val doll = remember(snapshot) { snapshot?.asImageBitmap() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val pw = constraints.maxWidth.toFloat()
        val ph = constraints.maxHeight.toFloat()
        val g = FrameGeometry(target.imageWidth, target.imageHeight, pw, ph)
        val b = target.box.toPreview(g)

        // Cutout rectangle in preview px: the box plus the same margin used when cropping.
        val mx = b.width * config.cutoutMarginFraction
        val my = b.height * config.cutoutMarginFraction
        val cw = b.width + 2 * mx
        val ch = b.height + 2 * my
        val cLeft = b.left - mx
        val cTop = b.top - my

        val a = approach.value
        val scale = 1f + (config.approachScale - 1f) * a
        // Centre of the cutout now: where the doll is → preview centre as the lunge progresses.
        val cx0 = cLeft + cw / 2f
        val cy0 = cTop + ch / 2f - rise.value * config.floatUpFraction * b.height
        val cx = cx0 + (pw / 2f - cx0) * a
        val cy = cy0 + (ph / 2f - cy0) * a

        // Face placement inside the cutout.
        val fw = cw * config.faceWidthFraction
        val fh = fw // the PNG is square
        val fx = cw * config.faceCenterXFraction - fw / 2f
        val fy = ch * config.faceCenterYFraction - fh / 2f

        // Darkening behind the doll grows with the lunge.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * overlayAlpha.value * (0.3f + 0.7f * a))),
        )

        Box(
            Modifier
                .offset { IntOffset(cLeft.roundToInt(), cTop.roundToInt()) }
                .size(with(density) { cw.toDp() }, with(density) { ch.toDp() })
                .graphicsLayer {
                    translationX = cx - (cLeft + cw / 2f)
                    translationY = cy - (cTop + ch / 2f)
                    scaleX = scale
                    scaleY = scale
                    rotationZ = if (a > 0f && a < 1f) jitter else 0f
                    alpha = overlayAlpha.value
                    transformOrigin = TransformOrigin.Center
                },
        ) {
            if (doll != null) {
                Image(
                    bitmap = doll,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Image(
                painter = painterResource(R.drawable.evil_face),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset { IntOffset(fx.roundToInt(), fy.roundToInt()) }
                    .size(with(density) { fw.toDp() }, with(density) { fh.toDp() })
                    .graphicsLayer { alpha = faceAlpha.value },
            )
        }
    }
}
