# PROJECT.md — Nila Diha's Ghost Radar

Android Halloween prank app. The camera spots a doll or toy; the phone shakes, a creepy song plays, and on screen the doll floats up, its face turns evil, and it drifts toward the camera, growing bigger. Point the phone away and the doll is normal again. Ships on Google Play.

## Who's who

- **Nila** — designer and product owner. Her decisions on look, feel, sound, and the user journey are final.
- **Hari** — developer. Owns the build, the Play listing, and licensing.
- **Implementer** — whoever writes the code. When a decision is about how something looks, feels, or sounds, do not guess: add it under "Open questions for Nila" in `NOTES.md` and use a clearly-labelled placeholder until she answers.

## Approved user journey (signed off by Nila — change only with her OK)

1. App opens on a dark screen: glowing green radar sweeping, Find My-style arrow, "Searching…".
2. User points the camera around the room. Sofa, wall: nothing happens.
3. Camera lands on a doll or toy → **instantly**, no build-up: phone vibrates and the creepy song starts.
4. On the camera preview the doll rises, its face turns evil (black eyes, horrible grin), then it comes toward the camera, growing larger as if coming out of the screen.
5. Point the camera away → overlay disappears, doll is normal.

Explicit non-goals: no slow build-up or accelerating beeps (Nila rejected this), no real UWB/BLE tracking (the arrow is a visual), no network, no accounts, no ads, no analytics.

## Tech stack (decided — don't relitigate)

- Kotlin, Jetpack Compose, Material 3, CameraX, single activity.
- **Detection:** MediaPipe Tasks Vision Object Detector (`com.google.mediapipe:tasks-vision`), EfficientDet-Lite0 **int8**, bundled in `src/main/assets/`. v1 trigger label: COCO `"teddy bear"` (there is no "doll" class in COCO).
- **Phase 2 only if needed:** ML Kit `com.google.mlkit:object-detection-custom` running a Teachable Machine TFLite classifier trained on Nila's actual doll.
- **Overlay + animation:** plain Compose — `Canvas`, `graphicsLayer`, `Animatable`, `rememberInfiniteTransition`. No ARCore or SceneView in v1.
- **Haptics:** `VibrationEffect.createWaveform(...)` via `VibratorManager` (API 31+) with `Vibrator` fallback. Not `LocalHapticFeedback` — too weak for a scare.
- **Audio:** `MediaPlayer`, preloaded during the radar phase so playback starts with zero latency. Media3 only if `MediaPlayer` misbehaves.
- **SDK:** minSdk 26, targetSdk = current Play requirement (36 at the time of writing — verify), compileSdk latest stable. **16 KB page-size support is mandatory** — check the pinned `tasks-vision` version supports it.
- Gradle Kotlin DSL, version catalog (`gradle/libs.versions.toml`), current stable AGP, Kotlin, and Compose BOM.

## Base repo and references

**Fork this:** `google-ai-edge/mediapipe-samples` → `examples/object_detection/android-jetpack-compose` (Apache-2.0). Sparse-checkout only that folder.

- Keep: `CameraView`, `ObjectDetectorHelper`, `ResultsOverlay` (its image→preview coordinate mapping is the useful part), the CameraX analysis pipeline.
- Replace: `HomeScreen` and all visible UI. Remove Lite2 and the build-time model download (`download_models.gradle`); commit the Lite0 int8 model in assets instead.
- Rename the package (placeholder `com.nila.ghostradar` — Hari picks the final name; it is permanent once uploaded to Play).

**Read for reference, copy only after checking the LICENSE file:**
- `googlesamples/mlkit` → `android/vision-quickstart` (Apache-2.0): the custom-classifier path for Phase 2. Do **not** copy its build config — it pins CameraX to a `1.0.0-SNAPSHOT`.
- `skydoves/compose-animations`: Compose animation patterns (license not yet verified).
- `vahitkeskin/LoopSweep` → `RealisticRadarCard.kt`: Canvas radar sweep with fading blips (README says MIT; confirm the file).
- `aubreybailey/ghost-radar`: UX ideas only (warmer/colder arrow, Geiger clicks). Zero code reuse.

**Do not use:** `Kashif-E/*` (no license), anything Ultralytics YOLO (AGPL-3.0), `mhd6271/beacon-radar-android` (no license, unfinished), any Java/XML-era radar view.

## Architecture

```
app/src/main/java/.../ghostradar/
  camera/      CameraX setup, ImageAnalysis, frame → bitmap
  detection/   ObjectDetectorHelper, DetectionGate, label mapping
  radar/       RadarScreen, SweepCanvas, DirectionArrow, Blips
  scare/       GhostState, GhostViewModel, ScareOverlay, ScareTimeline
  haptics/     ScareHaptics
  audio/       ScareAudio (preload / play / reset)
  ui/theme/    colors, type
```

**State machine** (one `StateFlow<GhostState>` from `GhostViewModel`):

```
Searching → Locked(box, label, score) → Scaring(box) → Cooldown → Searching
```

- **DetectionGate:** label ∈ `TRIGGER_LABELS`, score ≥ 0.5, for **3 consecutive frames** before `Locked` (kills false scares). Target lost for ≥ 500 ms → back to `Searching` (this is the "pointed away = normal" rule). One scare per lock; `Cooldown` lasts until the song ends or 8 s, whichever is shorter.
- **Coordinate mapping:** reuse the sample's image→preview logic; handle rotation and front/back mirroring. Unit-test it.
- **ScareTimeline defaults** (tune with Nila on a real device, all values in one `ScareConfig` object): t=0 haptics + audio start; float up 15 % of box height over 600 ms; crossfade normal→evil face over 300 ms; scale 1× → 5× toward screen centre over 1 500 ms with ±3° rotation jitter; fade out on `Cooldown`.
- **Radar screen:** black background, three green rings, rotating `Brush.sweepGradient` wedge, random fading blips, large arrow animated with `animateFloatAsState` (spring), fake distance readout. While `Searching` the arrow wanders slowly; once anything is in frame it points at the detection box's horizontal offset from screen centre. Full-screen first, then semi-transparent over the camera preview.
- Performance: analysis on a background executor, `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST`, 640×480 analysis resolution, CPU delegate by default (GPU behind a debug flag).

## Assets

- Evil-face PNGs with transparent backgrounds, drawn with Nila. Placeholder until she delivers; never ship the placeholder.
- Song: only something Hari owns or that is CC0 / royalty-free. Record the source and license in `ASSETS.md`. **Never commit copyrighted music.**
- SFX (radar ping, Geiger click): CC0 sources, also listed in `ASSETS.md`.
- Model: `efficientdet_lite0_int8.tflite` in `src/main/assets/`, with `androidResources { noCompress += "tflite" }`.

## Build, run, test

```
./gradlew :app:assembleDebug
./gradlew :app:lint :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- Detection work needs a **real device**. The emulator camera is useless for this.
- **Day-one test, before anything else:** point the app at Nila's actual doll and at a teddy bear. Log the top-3 labels and scores to Logcat. Write the result in `NOTES.md`. This single result decides whether Phase 2 (custom model) is needed.
- Unit-test: `DetectionGate` debounce and lost-target logic, coordinate mapping, the `GhostState` transitions. UI tests are optional.

## Conventions

- Kotlin only. Compose only — no XML layouts beyond manifest and theme resources. Coroutines + Flow; no RxJava, no Hilt unless the app grows past one ViewModel.
- Permissions: **`CAMERA` and `VIBRATE` only.** Never add `INTERNET`.
- No new dependency without adding its name, version, and license to `THIRD_PARTY.md`. Keep Apache-2.0 notices; ship an OSS licenses screen (`com.google.android.gms:oss-licenses-plugin` or equivalent).
- Small commits, one change each, conventional commit messages (`feat:`, `fix:`, `chore:`).
- Before starting any task read `NOTES.md`; after finishing, append what changed, what was tested on device, and any new question for Nila.
- Plain-language camera rationale screen before the runtime permission request: the camera looks for ghosts hiding in toys; nothing leaves the phone.

## Play Store constraints

- List as a **general-audience entertainment/novelty app. Do not market it to children.** A kids listing triggers the Families policy, and a horror jump-scare conflicts with it anyway. Fill in the IARC content questionnaire truthfully (fear/horror themes).
- No ads, no analytics, no data collection. Data safety form: no data collected; camera frames processed on-device.
- Ship an Android App Bundle (AAB) with per-ABI splits and R8. Verify 16 KB page alignment with APK Analyzer or Android's `check_elf_alignment.sh`.
- Expected size: low tens of MB (Lite0 int8 is ~4.4 MB; MediaPipe native libs add the rest). Do not bundle Lite2.

## Phases

- **P0** — fork, package rename, builds green, camera + detection running on device, labels logged, **day-one doll test done.**
- **P1** — radar screen + arrow + Searching state; state machine; haptics + audio; basic scare overlay with placeholder face.
- **P2** — Nila's art and sounds in; a tuning session with Nila on a real device for the `ScareConfig` numbers.
- **P3 (only if P0 says so)** — Teachable Machine classifier via ML Kit custom object detection.
- **P4** — Play prep: icon, listing, licenses screen, rationale screen, AAB, internal testing track.

## Open questions for Nila (don't guess — ask)

- Evil face: her own drawing, or the real doll's face photo-cropped and warped?
- Does the song play once or loop? What happens on screen when it ends?
- Is there an "off" switch or a parent mode to stop the scare?
- Radar colour: green, or something else?
