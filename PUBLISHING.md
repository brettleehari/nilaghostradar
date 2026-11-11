# Publishing — Google Play and the App Store

What is done, what Hari must do himself (accounts, money, permanent names), and the exact commands. Nila signs off the assets and the words before anything goes public.

## Begin here

The two accounts take days to clear, so start them first; everything else can happen while you wait.

1. **Today:** register at play.google.com/console (fee + ID verification) and enrol at developer.apple.com/programs (fee, up to 48 h). → Play step 1, App Store step 1.
2. **Today:** GitHub → repo Settings → Pages → deploy from `main`. The privacy URL both stores ask for becomes `https://brettleehari.github.io/nilaghostradar/PRIVACY`.
3. **Today:** back up `~/keystores/ghostradar-upload.jks` and `keystore.properties` (password manager + a second disk).
4. **Before the first upload:** choose the final Android `applicationId` (permanent) and confirm the iOS bundle id. → Play step 2, App Store step 3.
5. **With Nila:** store name, screenshots, and her sign-off on everything the player sees and hears. → Checklist below.
6. **When the accounts clear:** Play steps 3–9, App Store steps 2–9.

## 1. Google Play

1. **Account.** play.google.com/console → pay the one-time registration fee → identity verification (ID document; a few days). A personal account must run a **closed test with at least 12 testers for 14 days** before it can apply for production.
2. **Final applicationId**, then build and sign:
   ```
   export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home ANDROID_HOME=$HOME/Library/Android/sdk
   ./gradlew :app:bundleRelease      # app/build/outputs/bundle/release/app-release.aab (≈34 MB)
   ./gradlew :app:assembleRelease    # signed APK for a last smoke test on a phone
   $ANDROID_HOME/build-tools/<latest>/zipalign -c -P 16 -v 4 app/build/outputs/apk/release/app-release.apk   # 16 KB check, must say "Verification successful"
   ```
   Bump `versionCode` (and `versionName`) in `app/build.gradle.kts` for every upload.
3. **Create the app** in the console: name, default language, App/Game → App, Free. Declarations: no ads, no login needed ("App access: all functionality available without special access"), not a government app, no financial features, no health features, **target audience 13+ or 18+ and not designed for children** (the brief: never a kids listing).
4. **Play App Signing.** Accept it on the first upload; our `.jks` becomes the upload key and Google holds the app signing key.
5. **Internal testing** → create release → upload the `.aab` → add testers by email (up to 100) → they install from the opt-in link within minutes. This is the fast path for Nila's friends.
6. **Store listing**: title, short and full description (draft in `PLAY_LISTING.md`, Nila rewrites), icon `store/icon_512.png`, feature graphic `store/feature_graphic_1024x500.png`, screenshots, category Entertainment, contact email, privacy policy URL.
7. **Data safety**: no data collected, no data shared; camera processed on device. **Content rating** (IARC questionnaire): fear/horror themes yes, everything else no.
8. **Closed testing**: promote the internal release to a closed track, 12+ testers opted in, keep it running 14 days.
9. **Apply for production access** (console shows the button after the 14 days) → answer the questions about the test → create the production release → review (hours to a few days) → live.

## 2. App Store

1. **Account.** developer.apple.com/programs → enrol as an individual with the Apple ID `brettleehari@icloud.com` (yearly fee; approval can take up to 48 h). Until then the Mac only has an *Apple Development* certificate, which cannot upload.
2. **After enrolment**, in Xcode → Settings → Accounts, sign in so automatic signing can make the *Apple Distribution* certificate and the App Store profile.
3. **App Store Connect** → My Apps → + → New App: iOS, name, primary language, bundle id `io.github.brettleehari.ghostradar`, SKU `ghostradar`. The bundle id appears in the list once Xcode has registered it (or add it under Certificates, Identifiers & Profiles → Identifiers, capabilities: none).
4. **Archive and upload** (version in `ios/project.yml`: `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`; bump the build number for every upload):
   ```
   cd ios && ./generate_project.sh
   xcodebuild -project GhostRadar.xcodeproj -scheme GhostRadar -configuration Release \
     -destination 'generic/platform=iOS' -archivePath build/GhostRadar.xcarchive -allowProvisioningUpdates archive
   xcodebuild -exportArchive -archivePath build/GhostRadar.xcarchive -exportOptionsPlist ExportOptions.plist \
     -exportPath build/export -allowProvisioningUpdates      # uploads straight to App Store Connect
   ```
   Already in place: `PrivacyInfo.xcprivacy` (no tracking, no data, no required-reason APIs), `ITSAppUsesNonExemptEncryption = false` (no export-compliance question per build), `NSCameraUsageDescription`.
5. **TestFlight**: the build appears after processing (10–30 min). Internal testers (your team, up to 100) need no review; external testers (link, up to 10 000) need a short Beta App Review the first time.
6. **App information**: name, subtitle, category Entertainment, **age rating questionnaire** (Horror/Fear themes: infrequent or mild → usually 9+ or 12+; everything else none), privacy policy URL, support URL (the GitHub repo is fine).
7. **App Privacy**: "Data Not Collected". Matches the manifest.
8. **Version page**: screenshots, description, keywords, what's new, review notes with the demo video, contact phone and email for the reviewer. Pricing: free, all territories or India + the ones you want.
9. **Submit for Review** → typically 1–3 days → Release manually or automatically.

## 3. Checklist before going public

| Item | Status |
|---|---|
| Final Android `applicationId` (now the placeholder `com.nila.ghostradar`; **permanent after the first upload**) | **Hari to choose**, then change `app/build.gradle.kts` and the package directory |
| iOS bundle id `io.github.brettleehari.ghostradar` (permanent after the first upload) | Set in `ios/project.yml`; Hari to confirm |
| Store name. "Nila" alone is likely taken on both stores; "Nila Diha's Ghost Radar" is the draft | **Nila and Hari** |
| Nila's sign-off: evil face, three stingers, three loops, her two voice lines, icon, radar colour and words | **Open** (NOTES.md) |
| Privacy policy URL (both stores need one because of the camera) | `PRIVACY.md` is written. Publish it as a web page: GitHub → repo Settings → Pages → deploy from `main`, then the URL is `https://brettleehari.github.io/nilaghostradar/PRIVACY` |
| Screenshots from real phones: the radar (green), the lock (red HERE), the scare | **To take**. Play: at least 2, 16:9 or 9:16, 320–3840 px. App Store: 6.9" iPhone set (1320×2868) is required; 6.5" optional |
| Reviewer help: reviewers will not have a teddy bear in front of them | Put a 20 s phone video of the hunt in the review notes (both stores), plus "point the camera at a teddy bear; the scare follows about 6 s after the lock" |
| Backups of `~/keystores/ghostradar-upload.jks` and `keystore.properties` | **Hari** (password manager + a second disk) |

## 4. After the first release

- Keep `versionCode` / build number monotonic on both stores.
- The private media override slots (`app/src/PRIVATE_MEDIA.md`) do not apply to iOS; the iOS bundle ships what is in `ios/GhostRadar/Resources`.
- Re-check `targetSdk` against the current Play requirement every August.
