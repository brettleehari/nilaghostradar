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
- **Notes for the reviewer** (draft):

  This is a camera-based prank. Point the rear camera at a teddy bear (any plush toy works best) in a well-lit room. The radar shows "SIGNAL FOUND · LISTENING", a child's voice says "I can see you", and about six seconds after the lock the phone vibrates, a stinger plays and the toy's face changes on screen. Point away and it stops. The camera runs entirely on device; nothing is recorded or transmitted. A 20-second demo video is attached.

- Attachment: a phone video of one full hunt (record with another phone; Hari to make).

## Build

See `PUBLISHING.md` section 2 for the archive and upload commands. Version numbers are `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in `ios/project.yml`; bump the build number for every upload.

## Pre-submit checklist

- [x] Apple Developer Program enrolment approved (2026-11-14; the updated Program License Agreement had to be accepted too). The *Apple Distribution* certificate appears with the first successful export.
- [x] App record created with the bundle id above (2026-11-14, Apple ID 6834949463).
- [ ] Nila signs off the face, stingers, loops, her two voice lines, icon and all words on this page.
- [x] Screenshots uploaded (6.9", the six in `store/ios/`, 2026-11-14).
- [x] Privacy policy URL live (GitHub page of `PRIVACY.md`).
- [ ] Build uploaded, visible in TestFlight, installed and run once on the iPhone from TestFlight.
- [x] Age rating, App Privacy (published), review contact and notes filled in. Review video still to attach (Hari records it).
- [ ] Submit for Review.
