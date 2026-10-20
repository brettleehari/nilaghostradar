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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nila.ghostradar.camera.CameraView
import com.nila.ghostradar.camera.ResultsOverlay
import com.nila.ghostradar.detection.DetectionFrame
import com.nila.ghostradar.detection.DetectionGate
import com.nila.ghostradar.detection.DetectionLogger

/** Root of the app: rationale → permission → camera. The radar screen replaces this in P1. */
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
        DetectionDebugScreen()
    } else {
        CameraRationaleScreen(
            deniedOnce = deniedOnce,
            onContinue = { launcher.launch(Manifest.permission.CAMERA) },
        )
    }
}

/**
 * P0 screen: live camera, every detection boxed, top-3 labels on screen and in Logcat, and the
 * gate's lock state. This is the day-one doll test harness; it is replaced by the radar in P1.
 */
@Composable
fun DetectionDebugScreen() {
    val logger = remember { DetectionLogger() }
    val gate = remember { DetectionGate() }
    var latest by remember { mutableStateOf<DetectionFrame?>(null) }
    var locked by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CameraView(
            useGpu = BuildConfig.DEBUG && USE_GPU_DELEGATE,
            onFrame = { frame ->
                logger.log(frame)
                locked = gate.onFrame(frame.objects, frame.timestampMs).locked
                latest = frame
            },
            overlay = { frame -> frame?.let { ResultsOverlay(it) } },
        )

        Column(
            Modifier
                .align(Alignment.TopStart)
                .safeDrawingPadding()
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
        ) {
            val f = latest
            if (f == null) {
                Text(stringResource(R.string.debug_waiting), style = MaterialTheme.typography.labelSmall)
            } else {
                Text(
                    text = "${f.inferenceTimeMs} ms  ${f.imageWidth}x${f.imageHeight}  " +
                        if (locked) "LOCKED" else "searching",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (locked) MaterialTheme.colorScheme.primary else Color.White,
                )
                for (o in f.objects.take(3)) {
                    Text(
                        text = "%-14s %.2f".format(o.label, o.score),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** Debug flag: flip to true to try the GPU delegate. CPU is the default per PROJECT.md. */
private const val USE_GPU_DELEGATE = false
