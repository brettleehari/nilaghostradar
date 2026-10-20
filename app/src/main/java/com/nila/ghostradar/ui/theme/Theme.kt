package com.nila.ghostradar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GhostColorScheme = darkColorScheme(
    primary = RadarGreen,
    onPrimary = Void,
    secondary = RadarGreenDim,
    background = Void,
    onBackground = Bone,
    surface = Void,
    onSurface = Bone,
)

/** Always dark. No dynamic colour: the look is Nila's, not the wallpaper's. */
@Composable
fun GhostRadarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = GhostColorScheme,
        typography = Typography,
        content = content,
    )
}
