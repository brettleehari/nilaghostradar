package com.nila.ghostradar.radar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nila.ghostradar.R
import com.nila.ghostradar.ui.theme.RadarRed
import kotlin.random.Random

/**
 * The cut after the lunge: hard black, a burst of static, SIGNAL LOST flickering. The phone never
 * shows the doll "normal"; the kid has to look up at the real one. PLACEHOLDER copy for Nila.
 */
@Composable
fun SignalLost(modifier: Modifier = Modifier) {
    var frame by mutableLongStateOf(0L)
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frame = it }
    }
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val rng = Random(frame)
            // Static: a few hundred short grey dashes, re-rolled every frame.
            repeat(260) {
                val y = rng.nextFloat() * size.height
                val x = rng.nextFloat() * size.width
                val len = 2f + rng.nextFloat() * 28f
                val a = 0.05f + rng.nextFloat() * 0.35f
                drawLine(Color.White.copy(alpha = a), Offset(x, y), Offset(x + len, y), strokeWidth = 1.5f)
            }
            // A couple of rolling tear lines.
            repeat(2) {
                val y = rng.nextFloat() * size.height
                drawLine(Color.White.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), strokeWidth = 3f)
            }
        }
        val flicker = Random(frame / 3).nextFloat()
        Text(
            text = stringResource(R.string.radar_cooldown),
            color = RadarRed,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            letterSpacing = 6.sp,
            modifier = Modifier.graphicsLayer { alpha = if (flicker < 0.25f) 0.15f else 1f },
        )
    }
}

/** The aftershock: the face, huge, for a couple of frames when the kid has just breathed out. */
@Composable
fun AftershockFlash(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.evil_face),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.85f),
        )
    }
}
