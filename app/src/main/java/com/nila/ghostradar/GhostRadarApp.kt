package com.nila.ghostradar

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nila.ghostradar.audio.AmbientAudio
import com.nila.ghostradar.audio.ScareAudio
import com.nila.ghostradar.audio.VoiceLine
import com.nila.ghostradar.camera.CameraView
import com.nila.ghostradar.camera.ResultsOverlay
import com.nila.ghostradar.detection.DetectionFrame
import com.nila.ghostradar.detection.DetectionLogger
import com.nila.ghostradar.haptics.ScareHaptics
import com.nila.ghostradar.radar.RadarScreen
import com.nila.ghostradar.scare.GhostState
import com.nila.ghostradar.scare.GhostViewModel
import com.nila.ghostradar.scare.ScareOverlay

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

    var showLicenses by rememberSaveable { mutableStateOf(false) }
    val licensesLabel = stringResource(R.string.licenses_open)
    if (showLicenses) {
        LicensesScreen(onBack = { showLicenses = false })
        return
    }

    Box(Modifier.fillMaxSize()) {
        if (granted) {
            HuntScreen()
        } else {
            CameraRationaleScreen(
                deniedOnce = deniedOnce,
                onContinue = { launcher.launch(Manifest.permission.CAMERA) },
            )
        }
        // PLACEHOLDER placement: small ⓘ in the top-right opens the open-source notices.
        TextButton(
            onClick = { showLicenses = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeDrawingPadding()
                .padding(4.dp)
                .semantics { contentDescription = licensesLabel },
        ) {
            Text(
                text = "ⓘ",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * Camera analysing underneath, the scare overlay on the preview, the radar on top.
 * The radar is opaque while Searching and Locked ("full-screen first") and fades away the moment
 * the scare starts so the doll and the face are visible; it returns when the state is Searching again.
 * Scaring starts haptics and audio; the view model's timers handle the rest.
 *
 * Debug builds: long-press anywhere to inject a fake target and see the whole scare without a doll.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HuntScreen(viewModel: GhostViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lastFrame by viewModel.lastFrame.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val heat by viewModel.heat.collectAsStateWithLifecycle()
    val logger = remember { DetectionLogger() }
    val haptics = remember { ScareHaptics(context) }
    val audio = remember {
        ScareAudio(context, R.raw.scare_track, onFinished = viewModel::onAudioFinished)
    }
    val ambience = remember { AmbientAudio(context, R.raw.radar_ambience) }
    val voice = remember { VoiceLine(context) }

    // Preload during the radar phase; release with the screen.
    DisposableEffect(audio) {
        audio.preload()
        ambience.preload()
        onDispose {
            audio.release()
            ambience.release()
            voice.release()
            haptics.stop()
        }
    }

    // A session is one hunt; a long time away (home, screen off) starts a fresh one.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.onHuntPaused() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onHuntResumed() }

    // Side effects on state entry. The ambience stops the moment a lock happens so the stinger
    // lands on silence.
    LaunchedEffect(state::class) {
        when (state) {
            GhostState.Searching -> { haptics.stop(); audio.stop(); voice.stop(); ambience.play() }
            is GhostState.Locked -> {
                val s = state as GhostState.Locked
                val line = viewModel.config.voiceLine
                if (s.haunted && line.isNotBlank()) {
                    ambience.pause()
                    voice.speak(line, onDone = viewModel::onVoiceLineDone)
                } else {
                    viewModel.onVoiceLineDone()
                }
            }
            is GhostState.Scaring -> { ambience.pause(); haptics.scare(); audio.play() }
            is GhostState.Cooldown -> haptics.stop()
        }
    }

    val prox = viewModel.proximity

    // The radar stays up through Locked so a haunted lock shows red HERE while the phone whispers;
    // it drops the instant Scaring starts and the doll takes the screen.
    val showRadar = state is GhostState.Searching || state is GhostState.Locked
    val radarAlpha by animateFloatAsState(
        targetValue = if (showRadar) RADAR_BACKGROUND_ALPHA else 0f,
        animationSpec = tween(if (showRadar) 600 else 150),
        label = "radarAlpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CameraView(
            useGpu = BuildConfig.DEBUG && USE_GPU_DELEGATE,
            onFrame = { frame, image ->
                logger.log(frame)
                viewModel.onFrame(frame, image)
            },
            overlay = { frame ->
                if (BuildConfig.DEBUG && SHOW_DEBUG_BOXES) frame?.let { ResultsOverlay(it) }
                ScareOverlay(state = state, snapshot = snapshot, config = viewModel.config)
            },
        )
        if (radarAlpha > 0.01f) {
            RadarScreen(
                state = state,
                notice = notice,
                heat = heat,
                proximity = prox,
                backgroundAlpha = 1f,
                modifier = Modifier.graphicsLayer { alpha = radarAlpha },
            )
        }
        if (BuildConfig.DEBUG) {
            // Topmost so the gesture is not swallowed by PreviewView's own touch handling.
            Box(
                Modifier
                    .fillMaxSize()
                    .combinedClickable(onClick = {}, onLongClick = { viewModel.injectFakeTarget() }),
            )
            DebugPanel(lastFrame, state)
        }
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
                text = stringResource(R.string.debug_waiting),
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

/** Debug flags. CPU delegate is the v1 default. */
private const val USE_GPU_DELEGATE = false
private const val SHOW_DEBUG_BOXES = true
/** Radar opacity while Searching. 1 = full-screen radar, lower = see the camera through it. */
private const val RADAR_BACKGROUND_ALPHA = 1f
