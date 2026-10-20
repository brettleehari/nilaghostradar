package com.nila.ghostradar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nila.ghostradar.camera.CameraView
import com.nila.ghostradar.camera.ResultsOverlay
import com.nila.ghostradar.detection.DetectionFrame
import com.nila.ghostradar.detection.DetectionLogger
import com.nila.ghostradar.radar.RadarScreen
import com.nila.ghostradar.scare.GhostState
import com.nila.ghostradar.scare.GhostViewModel

/** Root of the app: rationale → permission → hunt. */
@Composable
fun GhostRadarApp() {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var deniedOnce by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok
        if (!ok) deniedOnce = true
    }

    if (granted) {
        HuntScreen()
    } else {
        CameraRationaleScreen(
            deniedOnce = deniedOnce,
            onContinue = { launcher.launch(Manifest.permission.CAMERA) },
        )
    }
}

/**
 * Camera analysing underneath, radar on top. The radar is opaque in v1 ("full-screen first");
 * drop [RADAR_BACKGROUND_ALPHA] to see the preview through it. Debug builds keep the label panel
 * for the day-one doll test.
 */
@Composable
fun HuntScreen(viewModel: GhostViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastFrame by viewModel.lastFrame.collectAsStateWithLifecycle()
    val logger = remember { DetectionLogger() }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CameraView(
            useGpu = BuildConfig.DEBUG && USE_GPU_DELEGATE,
            onFrame = { frame ->
                logger.log(frame)
                viewModel.onFrame(frame)
            },
            overlay = { frame ->
                if (BuildConfig.DEBUG && SHOW_DEBUG_BOXES) frame?.let { ResultsOverlay(it) }
            },
        )
        RadarScreen(state = state, backgroundAlpha = RADAR_BACKGROUND_ALPHA)
        if (BuildConfig.DEBUG) DebugPanel(lastFrame, state)
    }
}

@Composable
private fun DebugPanel(frame: DetectionFrame?, state: GhostState) {
    Column(
        Modifier
            .safeDrawingPadding()
            .padding(12.dp)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(8.dp),
    ) {
        Text(
            text = state::class.simpleName ?: "?",
            style = MaterialTheme.typography.labelSmall,
            color = if (state is GhostState.Searching) Color.White else MaterialTheme.colorScheme.primary,
        )
        if (frame == null) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.debug_waiting),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        } else {
            Text(
                text = "${frame.inferenceTimeMs} ms  ${frame.imageWidth}x${frame.imageHeight}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
            for (o in frame.objects.take(3)) {
                Text(
                    text = "%-14s %.2f".format(o.label, o.score),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                )
            }
        }
    }
}

/** Debug flags. CPU delegate and an opaque radar are the v1 defaults. */
private const val USE_GPU_DELEGATE = false
private const val SHOW_DEBUG_BOXES = true
private const val RADAR_BACKGROUND_ALPHA = 1f
