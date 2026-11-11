# Ghost Radar for iOS

The same prank, feature for feature, on iPhone. Same MediaPipe EfficientDet-Lite0 int8 model and
the same numbers (`ScareConfig`, `ProximityConfig`, `HauntPolicy.Config`) as the Android build, so
the two phones behave the same in the same room.

```
ios/
  GhostCore/        Swift package: gate, haunt policy, state machine, heat, scanner, geometry. `swift test` runs on any Mac.
  GhostRadar/       The app: SwiftUI, AVFoundation camera, MediaPipe detector, Core Haptics, AVAudioPlayer, AVSpeechSynthesizer.
  project.yml       XcodeGen spec. GhostRadar.xcodeproj is generated, never committed.
  fetch_mediapipe.sh  Fetches the MediaPipe Tasks Vision 1.0.0 iOS frameworks into Vendor/ (gitignored, ~370 MB).
```

## Build

```
cd ios
swift test --package-path GhostCore          # 29 logic tests, no simulator needed
./generate_project.sh                        # fetches MediaPipe if missing, runs xcodegen
xcodebuild -project GhostRadar.xcodeproj -scheme GhostRadar -configuration Debug \
  -destination 'generic/platform=iOS' -derivedDataPath build/DerivedData -allowProvisioningUpdates build
xcrun devicectl device install app --device <udid> build/DerivedData/Build/Products/Debug-iphoneos/GhostRadar.app
```

Signing is automatic with the team in `project.yml` (same as the arivu app on this Mac). The phone
must be unlocked, paired, and have Developer Mode on (Settings › Privacy & Security).

## Simulator demo

The simulator has no camera. `GHOSTRADAR_DEMO=1` (debug builds) feeds empty frames and forces a
haunted bear 4 s after launch and every 25 s after, so the whole sequence can be watched:

```
SIMCTL_CHILD_GHOSTRADAR_DEMO=1 xcrun simctl launch booted io.github.brettleehari.ghostradar
```

On a phone, long-press anywhere on the hunt screen (debug builds) for the same thing.

`GHOSTRADAR_CLEAN=1` alongside it hides the debug panel. That is how the App Store screenshots in
`store/ios/` were taken: an iPhone 17 Pro Max simulator (1320×2868), status bar overridden to 9:41,
`xcrun simctl io <udid> screenshot` every quarter second through one demo hunt.

## Differences from Android, on purpose

- Audio plays through the `.playback` session, so the prank is heard with the mute switch on.
- Haptics are Core Haptics patterns shaped like the Android waveforms; phones without a Taptic
  engine get a single heavy impact instead.
- Minimum iOS 17, iPhone only.
