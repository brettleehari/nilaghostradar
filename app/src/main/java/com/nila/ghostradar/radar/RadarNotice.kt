package com.nila.ghostradar.radar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nila.ghostradar.R
import com.nila.ghostradar.scare.Notice
import com.nila.ghostradar.ui.theme.RadarGreen

/**
 * A small pill under the readout: "FAINT TRACE · NO ANSWER", "CHAIR · NO SOUL". Fixed-height slot
 * so the radar never jumps; each notice slides in, sits for ScareConfig.noticeMs, fades out.
 * PLACEHOLDER copy and style; Nila decides (open question in NOTES.md).
 */
@Composable
fun RadarNotice(notice: Notice?, modifier: Modifier = Modifier) {
    Box(modifier.height(36.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = notice,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut()
            },
            label = "radarNotice",
        ) { n ->
            if (n != null) {
                Text(
                    text = noticeText(n),
                    color = RadarGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .border(1.dp, RadarGreen.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun noticeText(n: Notice): String = when (n) {
    is Notice.FaintTrace -> stringResource(R.string.notice_trace)
    is Notice.NoSoul -> stringResource(R.string.notice_no_soul, n.label.uppercase())
}
