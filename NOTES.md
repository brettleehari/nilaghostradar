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

## 2026-10-20 — P4 part 1: release pipeline and AAB

**What changed**
- Upload keystore generated at `~/keystores/ghostradar-upload.jks` (RSA 2048, valid to 2054, alias `upload`); credentials in `keystore.properties` (gitignored). `app/build.gradle.kts` reads it and signs release builds; without the file, release builds are unsigned.
- Bundle splits: ABI, density, language.
- **Network permissions stripped.** The merged manifest had picked up `INTERNET` and `ACCESS_NETWORK_STATE` from `com.google.android.datatransport` (a `tasks-core` transitive). Removed with `tools:node="remove"`; the release APK now declares only CAMERA and VIBRATE.
- **R8 crash fixed.** The first release build died in `Graph.<clinit>` with Flogger's "no caller found on the stack": R8 had inlined/renamed Flogger's caller-finder. `-keep class com.google.common.flogger.** { *; }` plus attribute keeps fixed it. Lesson: always smoke-test the release APK, never just the debug one.
- Open-source notices screen (`LicensesScreen`, `res/raw/third_party_notices.txt`, ⓘ button top-right on every screen). Placement is placeholder; Nila decides.
- `PLAY_LISTING.md`: build steps, permanent decisions, listing draft, data-safety and IARC answers, pre-launch checklist.

**Artifacts (not committed, in `app/build/outputs/`)**
- `bundle/release/app-release.aab` — 30.0 MB, signed by the upload key (jarsigner verified).
- `apk/release/app-release.apk` — 55.7 MB universal, signed (apksigner v2 verified), `zipalign -c -P 16` passes.
- Estimated arm64 install ≈ 20 MB (11 MB MediaPipe native, 4.6 MB model, 4.5 MB dex).

**Tested on the emulator (release APK, R8-minified)**
- Camera binds, detector loads, frames flow, radar renders, licences screen opens. No crash. (The "System UI isn't responding" dialog seen once is the emulator's own SystemUI under the software GPU, not the app.)

**Blocked on Hari / Nila before upload**
- Final applicationId (placeholder `com.nila.ghostradar` is permanent once uploaded).
- Placeholder face, track and icon must be replaced before anything beyond the internal track.
- Day-one doll test on a real phone.

## 2026-10-20 — P2: v1 assets replace the placeholders

**What changed**
- No assets had arrived from Nila, so project-owned v1 assets were produced and the placeholders deleted: `evil_face.png` (1024 px cracked porcelain doll with hollow eyes, black tear streaks, needle grin, blush, cracks), `scare_track.wav` (10 s: sub drone, dissonant string cluster, whispered noise bursts, accelerating heartbeat, broken music-box notes, reverse swell into a hit at 8.2 s), a launcher icon (radar + ghost) and Play store graphics in `store/`.
- All of it is generated from scratch, nothing sampled or copied, so it is shippable. It is still **Nila's call**: every item is listed in ASSETS.md as "awaiting sign-off or replacement".
- The scare visuals end at ~2.1 s; the track runs 10 s so the cooldown is capped by the 8 s rule, not the song.

**Still P2**
- The on-device tuning session with Nila (`ScareConfig` numbers) needs a phone and her presence.

**New questions for Nila**
- Face: keep the porcelain-doll direction, or draw her own? If her own, deliver a transparent PNG at least 1024 px square with the face centred.
- Track: does 10 s feel right, and should the hit at 8.2 s move earlier to match the 2 s lunge?
- Icon and feature graphic: approve, or brief a redesign.

## 2026-10-20 — First real phone: moto g stylus (2022), Android 12. Scare redesigned.

**Day-one test (partial)**
- Teddy bear figurine (ceramic, ~15 cm, held in hand): `teddy bear` 0.77–0.99, tight box, lock and scare fire on their own. **Teddy bear path works on a real phone.**
- Nila's doll: not yet tested.
- Inference on this phone (Helio G88 class): 150–400 ms per frame on CPU. Lock takes ~1 s. Try the GPU delegate in the tuning session.

**Scare redesign (Hari's call, 2026-10-20; supersedes the "face grows" journey step 4 — Nila to confirm)**
- At lock the doll is cut out of the live camera frame (box + 12 % margin) and drawn exactly where it is; its face region (centre 50 %/24 %, width 52 % of the cutout) crossfades to the evil face over 250 ms; the whole doll lifts 6 % and lunges 3.2× toward screen centre over 800 ms with jitter, holds 700 ms, fades on Cooldown. Face placement is a heuristic; numbers live in `ScareConfig`.
- Track replaced: the loud part is now at t=0 (bang + shriek + crash, peak at 16 ms), with a 7 s dread tail. Purpose: a stun, kids drop the phone. MediaPlayer volume pinned to max; device media volume still applies.
- `DetectionFrame` listener now also delivers the frame bitmap; the view model keeps the latest one and crops on entering Scaring (`snapshot` flow).

**Hardware notes**
- USB on this phone drops under screencap bursts; keep adb captures sparse.

## 2026-10-20 — Radar background music

- `audio/AmbientAudio`: looping, 60 % volume, plays while Searching, pauses on Locked/Scaring so the stinger lands on silence, resumes on Searching. Preloaded with the scare track.
- `res/raw/radar_ambience.wav`: original worn-music-box loop (21 s, seamless). Hari asked for Annabelle's "I Can See You": copyrighted, so it cannot be bundled. For a private sideload he can overwrite the file locally; never commit it.
- Verified on the moto g: no MediaPlayer errors, audio tracks active while on the radar.

## 2026-10-20 — Voice line and private media slots

- `audio/VoiceLine`: Android TextToSpeech, pitch 0.5, rate 0.7. On Locked the phone says `ScareConfig.voiceLine` ("Bloody Mary"); the state machine holds Locked (new `holdScare` flag, 2 tests) until the line finishes or `voiceMaxHoldMs` (1.5 s), then Scaring fires with the stinger. If TTS is missing the hold is skipped. Verified on the moto g: binds to Google TTS, no errors.
- Private overrides: `app/src/release/res/raw/` and `app/src/debug/res/raw/` are gitignored; a file with the same resource name there replaces the committed one for that build type (verified: the release APK picked up the override). See `app/src/PRIVATE_MEDIA.md`. Hari wants Annabelle's cue on Nila's phone; it is his file to place, never ours to fetch or commit.
- Decision for later: the Play build keeps the music-box loop; Hari will pick generic haunting music for the public version.

## 2026-10-21 — Doll test result, and rarity

**Day-one test: passed.** Over 2 466 logged frames with Hari showing the doll and the bear figurine, `teddy bear` reached 0.88 (260 frames) and the scare fired on both. The stock COCO model covers Nila's doll. **Phase 3 (custom classifier) is not needed.** Other labels seen around the room: person 0.86, bed, suitcase, dining table, cell phone, chair, cat 0.75 (a plush?), cup. Trigger list stays `teddy bear` only.

**Rarity (Hari's call, 2026-10-21): "it's rarity that creates the prank".**
- `scare/HauntPolicy` (pure, 7 tests): per lock, haunted or a faint trace. Defaults: no scare in the first 15 s of a session, at least 45 s between scares, then 30 % chance per lock, guaranteed on the 4th consecutive skipped lock, no per-session cap. All in `HauntPolicy.Config`; tune with Hari and Nila.
- `GhostState.Locked` gained `haunted`. An unhaunted lock stays Locked (radar stays up, status says "FAINT TRACE…", echo and arrow still react) until the toy leaves the frame; it never scares, never speaks, never pauses the music.
- The debug long-press forces the next lock to be haunted so demos still work.
- Machine asks the policy once per new lock (test).

## 2026-10-21 — Session defined, notices, "no soul" readouts

**Session (Hari asked for a plain definition).** One hunt = the hunt screen is open. It starts when the screen appears and restarts when the app comes back after being away (home, screen off, another app) for 60 s or more (`HauntPolicy.Config.sessionResetAfterMs`); a shorter absence keeps the warm-up, gap and skip counters, so a phone call does not hand out a free warm-up. `HauntPolicy.onPaused/onResumed` (2 tests), driven by `ON_STOP`/`ON_START` in `HuntScreen`. Plain-words version in README "How a hunt works".

**Faint trace is now a notice, not the status line.** The status line keeps blinking SEARCHING… on an unhaunted lock; a small pill under the readout says `FAINT TRACE · NO ANSWER` for 2.5 s (`ScareConfig.noticeMs`) and slides away. Echo and arrow still track the toy. `scare/Notice`, `radar/RadarNotice`, `GhostViewModel.notice`.

**"No soul" readouts (Hari: "make use of the detection pipeline").** While Searching, everyday objects the detector recognises get the same pill: `BED · NO SOUL`, `CHAIR · NO SOUL`. `detection/SoulScanner` (pure, 5 tests): score ≥ 0.55, never a trigger label, at most one readout every 4 s, the same label not within 20 s. `person` is skipped by default (see question below). Verified on the moto g: `BED · NO SOUL` appeared within 3 s of opening the app.

**Tests:** 52 pass. Lint unchanged (8 informational warnings).

**New questions for Nila**
- Notice copy and look: `FAINT TRACE · NO ANSWER` and `<THING> · NO SOUL` in a thin green pill. Keep, reword, or move?
- Should people get a readout too (`PERSON · NO SOUL`)? Off for now; one line in `SoulScanner.Config.skipLabels` turns it on.
- Is 2.5 s on screen right for a notice?

## 2026-10-24 — Warmer/colder (Find My feel), ticks, tempo, three stingers

**Hari's ask:** no reading needed; colour says how near a soul is; music rises; two or three haunted tracks at random.

- `detection/SoulHeat` (pure, 4 tests): per frame, the strongest teddy-bear detection → raw heat from score (60 %) and box height (40 %), smoothed (rises 45 % of the gap per frame, falls 18 %). `GhostViewModel.heat` is that value, pinned to 1 during a haunted Lock and Scaring.
- **Colour:** `radar/RadarPalette.heatColour` ramps green → amber (`#FFB300`) → red (`#FF1E3C`); rings, sweep, arrow, status line, readout and notice pill all follow it (350 ms colour tween).
- **Words instead of metres:** readout now COLD / WARM / HOT (heat < 0.2 / < 0.55 / else), HERE on a haunted lock. Label "SOUL PROXIMITY". `fakeDistanceMetres` and its test removed.
- **Pulse:** the dish breathes ±2.5 % on a phase whose speed follows heat (period 2.4 s cold → 0.45 s hot), so tempo changes never jump.
- **Haptic ticks:** `ScareHaptics.tick(strength)`; from heat 0.15 the phone taps every 1.1 s, down to every 220 ms at heat 1, strength 0.25 → 0.9. Only while Searching or on a faint-trace lock; the scare owns the motor otherwise.
- **Music tempo:** `AmbientAudio.setTempo` via `PlaybackParams` (speed 1.0 → 1.6, pitch held), quantised to 0.05; volume 0.6 → 0.9. Applied only while playing: setting PlaybackParams on a paused MediaPlayer starts it.
- **Three stingers at random:** `ScareAudio` now preloads a list and picks one per scare, never the same twice running. `scare_track_2.wav` (deeper bang, falling wail, bells) and `scare_track_3.wav` (double bang, high shriek, glass) come from the same in-repo generator; listed in ASSETS.md. Private override slots take the same three names.
- The radar now stays up through a haunted Lock (red, HERE, whisper) and drops when Scaring starts.
- All numbers in `ProximityConfig` (and `SoulHeat.Config`).
- Verified on the moto g: colour ramp green → amber with HOT as a toy appears; stinger #3 picked; no errors. Lint unchanged, 55 tests pass.

**Nila must confirm (these sit next to her "no slow build-up, no accelerating beeps" rule):** the haptic ticks and the music speeding up are a navigation feel while hunting, not the scare itself, but it is her rule. Either is one line to switch off (`tickFromHeat = 2f`, `tempoMaxSpeed = 1f`).

**New questions for Nila**
- Colour ramp green → amber → red: keep, or her own colours?
- Words COLD / WARM / HOT / HERE: keep, or spookier (e.g. QUIET / STIRRING / CLOSE / HERE)?
- Three stingers: approve, or record her own?

## 2026-10-24 — Jolt at the lock, heartbeat when hot

- `ScareHaptics.jolt()`: one 130 ms full-strength hit the instant a haunted lock happens (radar goes red, HERE), half a second before the whisper ends and the bang lands. Two flinches per scare. `ProximityConfig.joltAtLock`.
- `ScareHaptics.heartbeat(strength)`: lub (60 ms) – gap – dub (45 ms). From heat 0.55 (the HOT band) the hunting ticks become this, every 950 ms quickening to every 480 ms at heat 1. Still stops the moment the scare starts.
- Both are haptics only; nothing on screen changed. Both fall under Nila's "no build-up" question already logged above.

## 2026-10-24 — The aftermath (Hari: "why do we show the normal camera after we hit the ghost")

Before: after the lunge the overlay faded and the bare camera sat on screen for the whole cooldown (up to 7 s). Flat, and it says "just a phone".

Now, the third beat of the scare:
- **Cut to black.** On entering Cooldown: black, a burst of static (re-rolled every frame), SIGNAL LOST flickering in red, for `ScareConfig.blackoutMs` (1.2 s). `radar/SignalLost`.
- **The radar comes back wounded.** It fades in red, shaking up to 14 px (`ProximityConfig.shakeMaxPx`), status line SIGNAL LOST blinking, readout GONE, and cools to green over the cooldown: `GhostViewModel.heat` ramps 1 → 0 across `cooldownMaxMs`. The heartbeat keeps going and slows with it.
- **Aftershock.** At `aftershockAtMs` (2.0 s after the lunge) the face flashes full-width over black for 90 ms with a 90 ms full-strength thump. `radar/AftershockFlash`.
- The camera is never shown plain after a scare; "point away → doll is normal" is still true of the real doll, which is the point.
- Measured on the moto g: Locked → Scaring 1.35 s (voice line), Scaring → Cooldown 1.75 s, stinger ends 7.2 s after it started, cooldown ends with it. No errors.

**Changes Nila's approved journey (step 5):** she signed off "overlay disappears, doll is normal" on the camera. She needs to see this version and say yes or no. SIGNAL LOST / GONE are placeholder words.

## 2026-10-24 — Three radar loops at random

- `AmbientAudio` now preloads a list and picks one at random each time the hunt starts or resumes after a scare, never the same twice running. Tempo and volume follow whichever is playing.
- `radar_ambience_2.wav` (slower E-minor lullaby, lower bells, more hiss) and `radar_ambience_3.wav` (sparse high celesta over a tritone drone) from the same in-repo generator. Original melodies; listed in ASSETS.md.
- Private override slots: Hari's own files need the same three names now, or the generated ones fill the gaps.

## 2026-10-24 — "From HOT it's not detecting" (Hari, on the phone)

Log: bear at 0.77–0.87 for 34 s straight, locked at 14.9 s into the session → warm-up skip → faint trace. It stayed Locked (one decision per lock) while the radar still said HOT with the heartbeat going and the pill gone after 2.5 s. Detection was fine; the rarity rule was working but the screen contradicted it.

- Fix: on a faint-trace lock the heat is capped at `ProximityConfig.traceHeatCap` (0.4), so HOT settles to WARM, amber, ticks instead of heartbeat: "there, but not awake". The FAINT TRACE pill now stays for the whole lock and clears when the toy leaves.
- Hari's answer: yes. A held faint trace is re-rolled every `HauntPolicy.Config.rerollMs` (10 s) through the same policy (warm-up, gap, chance, guarantee all apply), so holding ~30 s on a toy always ends in a scare. `GhostStateMachine.wakeUp()` (2 tests) flips the lock to haunted; the hunt screen's lock effects are keyed on the haunted flag so the jolt, whisper and music stop fire on the wake-up too.

## 2026-10-24 — App name

- Launcher name is now "Nila" (`app_name`, Hari's call). The camera screen still says "Ghost Radar needs your camera"; the Settings path in the denied text says Nila. The Play listing title can stay "Nila Diha's Ghost Radar".

## 2026-10-26 — iOS app (Hari: "create an equivalent iOS app, feature parity")

- `ios/`: SwiftUI app + `GhostCore` Swift package. Same MediaPipe model file, same detector settings (live stream, 0.3, 3 results), same gate/policy/state machine/heat/scanner logic ported line for line with the same numbers; 29 package tests pass on the Mac (`swift test`).
- Parity list: rationale → camera permission → hunt; radar with heat colour, pulse, shake, COLD/WARM/HOT/HERE/GONE, notices (faint trace sticky, no-soul readouts); sessions with pause/resume reset; 10 s re-roll; jolt, ticks, heartbeat, scare rumble, aftershock (Core Haptics); three loops and three stingers at random, tempo and volume follow heat (AVAudioPlayer rate keeps pitch); "Bloody Mary" via AVSpeechSynthesizer; doll cutout + evil face lunge; blackout + SIGNAL LOST + aftershock flash; licences screen; debug panel and long-press demo; launcher name Nila.
- MediaPipe iOS frameworks come as CocoaPods archives; CocoaPods could not be installed (Homebrew's Ruby is broken on this Mac), so `ios/fetch_mediapipe.sh` fetches the same archives and the xcodegen spec links the XCFrameworks and force-loads the graph library, as the podspec's xcconfig does.
- Project is generated with xcodegen, never committed; team, bundle-id prefix (`io.github.brettleehari.ghostradar`) and iOS 17 floor follow the arivu app on this Mac.
- Verified on the iPhone 17 simulator with the demo flag: radar, red HERE lock, scare, SIGNAL LOST static, orange GONE cooldown. Device build signs; install needs the iPhone unlocked.
- Not yet verified on a real iPhone: camera orientation (connection rotated to portrait, boxes assumed upright 480x640), detection speed, haptic feel, speech voice. First thing to test when the phone is unlocked.

## 2026-10-28 — Nila's voice in (Hari: two WhatsApp voice notes)

- Two recordings from Nila, both now in the app on Android and iOS, trimmed and faded, nothing else done to them (they were already at full scale from WhatsApp):
  - "Bloody Mary, Bloody Mary" (`voice_bloody_mary.wav`, 3.3 s) replaces the phone's text-to-speech at a haunted lock. `VoiceLine` (TTS on Android, AVSpeechSynthesizer on iOS) is gone; `VoiceClip` preloads one clip and reports when it ends, so the scare still waits for the line (max `voiceMaxHoldMs`, 1.5 s, so the bang lands about halfway through her line — change that number if she wants the whole line first).
  - "I can see you" (`voice_i_can_see_you.wav`, 4.6 s) is Hari's "whenever we near the ghost": it plays the moment the readout turns HOT while still Searching, not again within `ProximityConfig.nearWhisperGapMs` (6 s). A faint trace that follows lets it finish (there, but not awake); a haunted lock stops it and says Bloody Mary; a scare stops it.
- Source files kept in `~/Desktop/GhostRadar/nila_voice/` (the original `.opus` notes and the converted WAVs). Listed in ASSETS.md as project-owned, recorded by Nila.
- Tested: Android debug build on the Moto g (install + launch); iOS debug build on the iPhone. Nila and Hari to judge the levels and whether "I can see you" should fire on HOT or only on a faint trace.

## Open questions for Nila

- Evil face: her own drawing, or the real doll's face photo-cropped and warped?
- Does the song play once or loop? What happens on screen when it ends?
- Is there an "off" switch or a parent mode to stop the scare?
- Radar colour: green, or something else? (Placeholder is `#39FF14`.)
- "I can see you": on turning HOT (now), or only on a faint-trace lock? And should the bang wait for the whole "Bloody Mary" line (set `voiceMaxHoldMs` to ~3.5 s) or cut in at 1.5 s as now?
- Camera rationale copy: current placeholder reads "The camera looks for ghosts hiding in toys. Everything happens on this phone. Nothing is recorded, saved, or sent anywhere." OK, or does she want it spookier?
- Launcher icon: the current one is a placeholder (green rings + sweep). What does she want?
- Where should the ⓘ (open-source notices) button live? Top-right on every screen for now.
- Store listing copy and screenshots (see PLAY_LISTING.md).
