# Third-party code and dependencies

No new dependency without a row here (name, version, licence).

## Forked / copied code

| What | Source | Licence |
|---|---|---|
| Camera pipeline, detector wrapper, box mapping (`camera/CameraView.kt`, `camera/CoordinateMapper.kt`, `camera/ResultsOverlay.kt`, `detection/ObjectDetectorHelper.kt`) | `google-ai-edge/mediapipe-samples`, `examples/object_detection/android-jetpack-compose`, commit `1675dd30` | Apache-2.0 (`LICENSE-upstream-mediapipe-samples.txt`) |
| `efficientdet_lite0_int8.tflite` | MediaPipe model card, `storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/1/` | Apache-2.0 |

## Gradle dependencies (see `gradle/libs.versions.toml`)

| Dependency | Version | Licence |
|---|---|---|
| com.google.mediapipe:tasks-vision (+ tasks-core, flogger, guava, protobuf-javalite, datatransport transitively) | 1.0.0 | Apache-2.0 |
| androidx.camera:camera-core / camera2 / lifecycle / view | 1.6.2 | Apache-2.0 |
| androidx.compose BOM (ui, graphics, material3, tooling) | 2026.09.00 | Apache-2.0 |
| androidx.activity:activity-compose | 1.13.0 | Apache-2.0 |
| androidx.lifecycle:runtime-ktx / runtime-compose / viewmodel-compose | 2.11.0 | Apache-2.0 |
| androidx.core:core-ktx | 1.19.1 | Apache-2.0 |
| org.jetbrains.kotlinx:kotlinx-coroutines-android / -test | 1.11.0 | Apache-2.0 |
| junit:junit (test only) | 4.13.2 | EPL-1.0 |

## Build plugins

| Plugin | Version | Licence |
|---|---|---|
| com.android.application (AGP) | 9.4.1 | Apache-2.0 |
| org.jetbrains.kotlin.plugin.compose | 2.4.21 | Apache-2.0 |

## Reference-only (no code copied)

- `googlesamples/mlkit` vision-quickstart (Apache-2.0) — Phase 3 path, not yet touched.
- `vahitkeskin/LoopSweep`, `skydoves/compose-animations`, `aubreybailey/ghost-radar` — not yet consulted; licences to be confirmed before any copy.

An OSS licences screen is still to be added (P4).

## iOS build (`ios/`)

| Dependency | Version | License | Use |
|---|---|---|---|
| MediaPipeTasksVision (iOS) | 1.0.0 | Apache-2.0 | Object detector, live-stream mode |
| MediaPipeTasksCommon (iOS) | 1.0.0 | Apache-2.0 | MediaPipe runtime the vision tasks need |

Both are fetched by `ios/fetch_mediapipe.sh` from the archives the CocoaPods specs point at; their
LICENSE and NOTICE files land in `ios/Vendor/` and the texts are already in
`third_party_notices.txt`, which the iOS licences screen shows. Everything else is Apple SDK.
