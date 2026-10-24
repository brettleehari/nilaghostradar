# Play Console prep (P4)

Working notes for the Play listing. Hari owns the console; Nila owns the words and pictures.

## Build

```
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home
./gradlew :app:bundleRelease          # → app/build/outputs/bundle/release/app-release.aab
./gradlew :app:assembleRelease        # signed release APK for local smoke tests
```

- Signed with the upload key in `~/keystores/ghostradar-upload.jks`; credentials in `keystore.properties` (gitignored). **Back both up.** With Play App Signing the upload key can be reset if lost, but it is a support ticket.
- Bump `versionCode` in `app/build.gradle.kts` for every upload; Play rejects a reused code.
- R8 and resource shrinking are on. Mapping file for crash de-obfuscation: `app/build/outputs/mapping/release/mapping.txt` (upload it with each release).
- Per-ABI, density and language splits are enabled in the bundle. Estimated arm64 install ≈ 20 MB.

## Before the first upload — decisions that are permanent

- **applicationId.** Currently the placeholder `com.nila.ghostradar`. It cannot change after upload. Hari decides.
- **App name.** "Nila" in `strings.xml` (Hari, 2026-10-24); the launcher shows it, the listing title can differ.
- **Listing category:** Entertainment. **Not** a kids/family listing (see the brief).

## Store listing (draft — Nila to rewrite)

- Title: Nila Deha's Ghost Radar
- Short description: Point your phone at a toy. Something is hiding inside it.
- Full description: A Halloween prank for the living room. The radar sweeps, the arrow swings, and when the camera finds a doll or a teddy bear the phone shakes, the music starts and the toy's face is not the toy's face any more. Everything happens on your phone: no account, no internet, no ads, nothing recorded.
- Graphics: v1 icon at `store/icon_512.png`, feature graphic at `store/feature_graphic_1024x500.png` (Nila to approve or redo). Still needed: at least 2 phone screenshots (the radar and the scare), taken on a real phone.

## Data safety form

- Data collected: **none**. Data shared: **none**.
- Camera frames are processed on-device and discarded; nothing is stored or transmitted. The app has no INTERNET permission (the one requested by a MediaPipe transitive library is stripped in the manifest).
- No analytics, ads, crash reporting or accounts.

## Content rating (IARC)

Answer truthfully: the app contains fear/horror themes (jump scare, creepy face, ominous music). No violence, no blood, no sexual content, no gambling, no user interaction, no sharing of location or personal info.

## Permissions declared

`CAMERA` (core feature), `VIBRATE` (the scare). Nothing else.

## Pre-launch checklist

- [ ] Final applicationId chosen and set.
- [ ] Nila signs off (or replaces) the v1 face, track, icon and feature graphic listed in `ASSETS.md`.
- [ ] Day-one doll test passed on a real phone (or P3 classifier shipped).
- [ ] `versionCode` bumped.
- [ ] AAB uploaded to **Internal testing**, mapping file attached.
- [ ] Testers (Hari, Nila) added to the internal track; install from the Play link, not sideload.
