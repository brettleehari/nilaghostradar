package com.nila.ghostradar.radar

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.nila.ghostradar.ui.theme.RadarAmber
import com.nila.ghostradar.ui.theme.RadarGreen
import com.nila.ghostradar.ui.theme.RadarRed

/** Radar colour for a heat value: green when cold, amber when warm, red when hot. PLACEHOLDER ramp for Nila. */
fun heatColour(heat: Float): Color {
    val h = heat.coerceIn(0f, 1f)
    return if (h < 0.5f) lerp(RadarGreen, RadarAmber, h / 0.5f) else lerp(RadarAmber, RadarRed, (h - 0.5f) / 0.5f)
}
