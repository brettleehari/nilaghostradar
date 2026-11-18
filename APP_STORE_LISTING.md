# App Store Connect prep

Working notes for the App Store record. Hari owns the account and the upload; Nila owns the words and pictures. Character limits are Apple's.

## App information

| Field | Value (draft) | Limit |
|---|---|---|
| Name | Nila Diha's Ghost Radar | 30 |
| Subtitle | Something hides in that toy (the first draft, "Something is hiding in that toy", is 31 characters and was rejected) | 30 |
| Bundle ID | `io.github.brettleehari.ghostradar` (permanent after the first upload) | — |
| SKU | `ghostradar` | — |
| Apple ID (App Store Connect) | 6834949463 | — |
| Primary language | English (U.S.) | — |
| Primary category | Entertainment | — |
| Secondary category | none | — |
| Content rights | Does not contain, show, or access third-party content | — |
| Age rating | Entered 2026-11-14: Horror/Fear Themes **Frequent** (the scare is the whole app), everything else None, no in-app controls → Apple calculated **13+** (12+ in Brazil, Korea, Vietnam). Change to Infrequent for a lower rating if Hari prefers. Not made for kids. | — |
| Pricing | Free, all 175 countries or regions (entered). Mac and Apple Vision Pro availability switched off. | — |
| Support URL | https://github.com/brettleehari/nilaghostradar | — |
| Privacy policy URL | Entered: https://github.com/brettleehari/nilaghostradar/blob/main/PRIVACY.md (public already). Swap to https://brettleehari.github.io/nilaghostradar/PRIVACY once GitHub Pages is on. | — |

## App Privacy

**Data Not Collected.** Matches `PrivacyInfo.xcprivacy` (no tracking, no collected data, no required-reason APIs). The camera is used only to run the on-device detector; frames are never stored or sent.

## Version page (1.0)

- **Promotional text** (170): Point the camera at a doll or a teddy bear. The radar finds it. Then it finds you.
- **Description** (4 000, draft — Nila rewrites):

  A Halloween prank for the living room. The radar sweeps, the arrow swings, and when the camera finds a doll or a teddy bear the phone shakes, the music starts and the toy's face is not the toy's face any more.

  Everything happens on your iPhone: no account, no internet, no ads, nothing recorded, nothing sent anywhere.

  Works best with a teddy bear in a well-lit room. Hold still once the radar says it is listening.

- **Keywords** (100, comma-separated, no spaces needed): ghost,radar,halloween,prank,scare,haunted,teddy,doll,spooky,jump scare,camera
- **What's New**: First release.
- **Screenshots**: 6.9" iPhone set (1320×2868) is required; 6.5" optional. Three shots: the green radar searching, the red lock ("HERE"), the scare. Captured from the demo build in the simulator or from a real phone.
- **App Preview video**: optional. Skip for 1.0.
- **Copyright**: 2026 Hariprasad Sudharshan

## App Review information

- Sign-in required: No (entered).
- Release: Manual (Hari presses Release after approval). Entered.
- Contact: Hariprasad Sudharshan, hariprasadsudharsan@gmail.com, phone number Hari adds in the console.
- **Notes for the reviewer** (entered 2026-11-18, after the Guideline 2.1 "Information Needed" rejection; answers Apple's six points):

  HOW TO TEST: Open the app, tap Start hunting, allow the camera, then point the REAR camera at a teddy bear or any plush toy, about 1 m away, in a well-lit room. The radar readout warms up (COLD → WARM → HOT → HERE). When the toy is recognised the radar shows "SIGNAL FOUND · LISTENING" and a child's voice says "I can see you". Keep the camera on the toy: about six seconds later the phone vibrates, a voice says "Bloody Mary", a stinger plays and the toy's face changes on screen and lunges toward the viewer. Point the camera away and everything stops; the toy looks normal again. A screen recording of this flow on a physical iPhone is attached.

  PURPOSE AND AUDIENCE: A Halloween prank / novelty entertainment app for teens and adults who want to startle a friend. The phone pretends to be a ghost detector; the camera recognises a toy and a jump scare follows. There is nothing to win, buy or save. It is not marketed to children (age rating 13+).

  SETUP: No account, no sign-in, no sample files, no in-app purchases. The only permission is the camera. The app also uses the vibration motor and the speaker.

  EXTERNAL SERVICES: None. The app has no network code at all. Object recognition runs on the device with a bundled open-source MediaPipe EfficientDet-Lite0 model (Apache-2.0). No data provider, authentication service, payment processor, analytics or AI service is used. Nothing is recorded, saved or transmitted; App Privacy is "Data Not Collected".

  REGIONAL DIFFERENCES: None. The app works identically in every region; the only language is English.

  REGULATED INDUSTRY / THIRD-PARTY MATERIAL: Not applicable. All sounds, voice lines (recorded by the developer's family), images and the icon were made for this app and are owned by the developer; the only third-party component is the Apache-2.0 model, credited on the in-app Open source licences screen.

- Attachment: a screen recording made ON the iPhone (Control Center → Screen Recording, with microphone on), starting at the Home Screen and showing: launch, Start hunting, the camera permission, the radar warming up, the lock, the scare, pointing away. 30–60 s, MP4/MOV, under 500 MB. Hari records it; the implementer attaches it and resubmits.

## Build

See `PUBLISHING.md` section 2 for the archive and upload commands. Version numbers are `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in `ios/project.yml`; bump the build number for every upload.

## Pre-submit checklist

- [x] Apple Developer Program enrolment approved (2026-11-14; the updated Program License Agreement had to be accepted too). The *Apple Distribution* certificate appears with the first successful export.
- [x] App record created with the bundle id above (2026-11-14, Apple ID 6834949463).
- [ ] Nila signs off the face, stingers, loops, her two voice lines, icon and all words on this page.
- [x] Screenshots uploaded (6.9", the six in `store/ios/`, 2026-11-14).
- [x] Privacy policy URL live (GitHub page of `PRIVACY.md`).
- [x] Build uploaded (1.0 build 1, 2026-11-14). Still to do: install and run it once on the iPhone from TestFlight.
- [x] Age rating, App Privacy (published), review contact and notes filled in. Review video still to attach (Hari records it).
- [x] Submit for Review. Submitted 2026-11-15 (review submission 05e79153, version 1.0 with build 1). Release stays Manual: after approval press Release This Version.
- [ ] **Rejected 2026-11-18, Guideline 2.1 Information Needed** (new-app submission: Apple wants a device screen recording plus purpose, setup, external services, regional differences, regulated-material statements in the notes). Notes rewritten and saved the same day. Still to do: attach the recording, then Resubmit to App Review.
