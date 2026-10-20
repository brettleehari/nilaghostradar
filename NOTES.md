# NOTES.md — running log

Read this before starting any task. Append after finishing: what changed, what was tested on a device, new questions for Nila.

## 2026-10-20 — P0 scaffold

**What changed**
- Forked `google-ai-edge/mediapipe-samples` → `examples/object_detection/android-jetpack-compose` at commit `1675dd30` (Apache-2.0, licence kept in `LICENSE-upstream-mediapipe-samples.txt`). Only the camera pipeline, detector wrapper and the image→preview box mapping were ported; all sample UI, gallery/video modes, Lite2 and `download_models.gradle` were dropped.
- Package renamed to the placeholder `com.nila.ghostradar`. **Hari: pick the final applicationId before the first Play upload; it is permanent.**
- Build rewritten: Gradle 9.8.2, AGP 9.4.1 (built-in Kotlin, so no `kotlin.android` plugin), Kotlin 2.4.21, Compose BOM 2026.09.00, CameraX 1.6.2, MediaPipe `tasks-vision` 1.0.0. Kotlin DSL + version catalog. compileSdk 37 (current AndroidX requires ≥ 37), targetSdk 36, minSdk 26.
- `efficientdet_lite0_int8.tflite` (4.6 MB, from `storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/`) committed under `app/src/main/assets/`, stored uncompressed (`noCompress += "tflite"`, verified in the APK).
- Code: `camera/CameraView` (CameraX, 4:3, 640×480 analysis, keep-only-latest, RGBA), `camera/CoordinateMapper` (fit, image→preview, mirroring, 90°-step rotation, horizontal offset for the radar arrow), `camera/ResultsOverlay` (debug boxes), `detection/ObjectDetectorHelper` (live stream, CPU delegate, GPU behind a debug constant), `detection/DetectionGate` (3 consecutive frames ≥ 0.5 to lock, 500 ms lost → unlock), `detection/DetectionLogger` (top-3 labels to Logcat), `GhostRadarApp` (rationale screen → permission → P0 debug screen).
- 19 unit tests: `DetectionGateTest` (8), `CoordinateMapperTest` (11). All green.

**Decisions worth knowing**
- **tasks-vision pinned to 1.0.0, not 1.1.1.** 1.1.x adds a hard dependency on `litertlm-android` (an on-device LLM runtime, ~22 MB arm64 .so, plus kotlin-reflect) that object detection does not use. 1.0.0 has no such dependency and its native lib is 16 KB page-aligned (checked ELF `p_align = 0x4000` for all four ABIs, and `zipalign -c -P 16` passes on the built APK). Revisit only if a newer line drops the LLM dependency.
- `tasks-core` transitively pulls `com.google.android.datatransport:*` (Firebase transport client). With no `INTERNET` permission it cannot send anything; mention it truthfully on the Play data-safety form if asked about SDKs.
- Model-side score threshold is 0.3 so the day-one log shows weak guesses; the gate applies the real 0.5 cut-off.
- Activity locked to portrait for v1 (lint warns it is ignored on large screens from Android 16; fine for a phone prank).
- Debug APK is 70 MB because it carries four ABIs. The Play AAB will split per ABI; arm64 is ~11 MB MediaPipe + 4.6 MB model + Compose, so expect low-20s MB installed.
- Local toolchain note: the machine's default JDK (`/Library/Java/.../jdk-22`) is x86_64 and does not run on this arm64 Mac. Build with `JAVA_HOME=~/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home`. Android Studio uses its own JBR, so this only matters for the CLI.

**Tested on device**
- Nothing yet. No device was attached. `assembleDebug`, `testDebugUnitTest` and `lint` are green on the Mac.

**Day-one doll test — still to do (blocks P0 sign-off and the P3 decision)**
1. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. `adb logcat -s GhostRadar` in a terminal.
3. Point the phone at Nila's actual doll, then at a teddy bear, then at a sofa/wall. The on-screen panel and the log show the top-3 labels and scores; the panel says LOCKED when the gate fires.
4. Record here: the labels the doll produces, their scores, whether the gate locks, and the inference time. If the doll never reads as `teddy bear` ≥ 0.5 for three frames in a row, Phase 3 (Teachable Machine classifier) is needed.

## 2026-10-20 — P1 part 1: state machine + radar screen

**What changed**
- `scare/GhostState`, `scare/GhostStateMachine` (pure reducer, 10 tests), `scare/GhostViewModel` (gate + reducer + scare/cooldown timers, `onAudioFinished()` hook for "song end or 8 s"), `scare/ScareConfig` (all brief defaults in one object).
- `radar/SweepCanvas` (three glowing rings, crosshair, rotating sweep-gradient wedge, seven random blips that light up as the sweep passes and fade, pulsing echo for the real target), `radar/DirectionArrow` (spring-animated arrow; wanders ±45° while Searching, points at the target's horizontal offset × 55° when locked), `radar/RadarScreen` (status line, dish with the arrow in the centre, fake distance readout: random walk 3–9.5 m while Searching, derived from box height when locked).
- `HuntScreen` stacks the radar (opaque, "full-screen first") over the still-running `CameraView`. Debug builds keep the label panel and detection boxes. Constants at the bottom of `GhostRadarApp.kt` flip GPU delegate, debug boxes, and radar opacity.
- Reducer rules: Locked promotes to Scaring on the very next frame (no build-up). Scaring → Cooldown on scare timeout or target lost. Cooldown → Searching only after the timer AND the gate has released, so a doll left in frame scares once.

**Tested on the emulator (arivu_4gb, API 36 arm64, emulated back camera)**
- Rationale screen → grant → hunt screen all work. Camera binds, the detector loads, inference 15–19 ms per 480×640 frame on the emulated CPU. Radar renders as designed: rings, sweep, fading blips, wandering arrow, blinking status, drifting distance. Debug panel shows the emulator's fake scene as "tv 0.32".
- Found and fixed: rationale body text was black on black (Text outside a Surface defaults to black content colour). Colours are now explicit.
- Not exercised: Locked/Scaring/Cooldown, because the emulator scene has no teddy bear. Still needs a real phone and the doll.

**Still to do in P1**
- `haptics/ScareHaptics`, `audio/ScareAudio` (preload during radar, play on Scaring, call `onAudioFinished`), `scare/ScareOverlay` with the placeholder face and the `ScareConfig` timeline, and the semi-transparent radar over the preview.

**New questions for Nila**
- Status copy is placeholder: "SEARCHING…", "SIGNAL LOCKED", "IT SEES YOU", "PARANORMAL DISTANCE". Her words?
- Arrow sits in the middle of the dish. OK, or above it like Find My?
- Sweep speed is one turn per 2 s. Faster, slower?

## 2026-10-20 — P1 part 2: haptics, audio, scare overlay, placeholders

**What changed**
- `haptics/ScareHaptics`: VibratorManager (API 31+) / Vibrator fallback, amplitude waveform (jolt, stutter, long rumble ≈ 2.1 s) matching the scare timeline.
- `audio/ScareAudio`: MediaPlayer preloaded (prepareAsync) when the hunt screen opens, `play()` on Scaring, completion → `GhostViewModel.onAudioFinished()` so cooldown ends at song end or 8 s. Plays once; loop is still Nila's call.
- `scare/ScareOverlay`: evil face over the target box inside the camera preview's bounds. Timeline from `ScareConfig`: crossfade in 300 ms while rising 15 % of box height over 600 ms, then scale 1→5× toward preview centre over 1 500 ms with ±3° jitter, fade out 400 ms on Cooldown. Follows the live box while Scaring. Placeholder darkening behind the face.
- `HuntScreen`: radar is opaque while Searching and fades out (150 ms) the moment a lock happens so the preview and face show; fades back (600 ms) on Searching. Scaring starts haptics + audio; Searching stops both.
- Debug builds: **long-press anywhere** injects a fake "teddy bear" in the centre of the frame for 3 s, so the scare can be seen without the doll. The gesture layer sits above PreviewView, which otherwise eats touches.
- `Target` now carries the image size so the overlay can map its box without the frame.
- Placeholder assets, generated in-repo, **never to ship** (see ASSETS.md): `drawable-nodpi/evil_face_placeholder.png` (drawn: pale oval, hollow black eyes, jagged grin) and `raw/scare_track_placeholder.wav` (8 s synthesized: detuned low drone, minor-second clash, speeding heartbeat, noise swell, final hit).

**Tested on the emulator (arivu_4gb)**
- Long-press → Locked → Scaring → Cooldown → Searching, verified with screenshots: radar fades, face appears over the fake target, grows toward centre with darkening, fades on Cooldown, radar returns. No crashes, no MediaPlayer errors in logcat.
- Haptics cannot be felt and audio cannot be heard on the emulator; only "no errors" is verified. Both need a phone.
- Emulator throughput varies wildly between boots (16 ms to 800 ms per frame). Not meaningful for performance.

**P1 status:** complete with placeholders. Next is the real-device pass (below), then P2 with Nila's assets.

**New questions for Nila**
- Song once or loop? Currently once; the scare visuals end after ~2.1 s and the song keeps playing through cooldown.
- The placeholder darkening behind the face (black, up to ~45 %): keep, change, or drop?
- Should the radar stay faintly visible during the scare (HUD style) or vanish completely as now?

## Open questions for Nila

- Evil face: her own drawing, or the real doll's face photo-cropped and warped?
- Does the song play once or loop? What happens on screen when it ends?
- Is there an "off" switch or a parent mode to stop the scare?
- Radar colour: green, or something else? (Placeholder is `#39FF14`.)
- Camera rationale copy: current placeholder reads "The camera looks for ghosts hiding in toys. Everything happens on this phone. Nothing is recorded, saved, or sent anywhere." OK, or does she want it spookier?
- Launcher icon: the current one is a placeholder (green rings + sweep). What does she want?
