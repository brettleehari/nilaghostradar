# App Store Connect prep

Working notes for the App Store record. Hari owns the account and the upload; Nila owns the words and pictures. Character limits are Apple's.

## App information

| Field | Value (draft) | Limit |
|---|---|---|
| Name | Nila Diha's Ghost Radar | 30 |
| Subtitle | Something is hiding in that toy | 30 |
| Bundle ID | `io.github.brettleehari.ghostradar` (permanent after the first upload) | — |
| SKU | `ghostradar` | — |
| Primary language | English (U.S.) | — |
| Primary category | Entertainment | — |
| Secondary category | none | — |
| Content rights | Does not contain, show, or access third-party content | — |
| Age rating | Horror/Fear Themes: **Infrequent/Mild** → expect 9+ or 12+. Everything else: None. Not made for kids. | — |
| Pricing | Free, all territories (or the ones Hari picks) | — |
| Support URL | https://github.com/brettleehari/nilaghostradar | — |
| Privacy policy URL | https://brettleehari.github.io/nilaghostradar/PRIVACY (needs GitHub Pages switched on) | — |

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

- Sign-in required: No.
- Contact: Hariprasad Sudharshan, hariprasadsudharsan@gmail.com, phone number Hari adds in the console.
- **Notes for the reviewer** (draft):

  This is a camera-based prank. Point the rear camera at a teddy bear (any plush toy works best) in a well-lit room. The radar shows "SIGNAL FOUND · LISTENING", a child's voice says "I can see you", and about six seconds after the lock the phone vibrates, a stinger plays and the toy's face changes on screen. Point away and it stops. The camera runs entirely on device; nothing is recorded or transmitted. A 20-second demo video is attached.

- Attachment: a phone video of one full hunt (record with another phone; Hari to make).

## Build

See `PUBLISHING.md` section 2 for the archive and upload commands. Version numbers are `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` in `ios/project.yml`; bump the build number for every upload.

## Pre-submit checklist

- [ ] Apple Developer Program enrolment approved (the Mac then gets an *Apple Distribution* certificate).
- [ ] App record created with the bundle id above.
- [ ] Nila signs off the face, stingers, loops, her two voice lines, icon and all words on this page.
- [ ] Screenshots uploaded (6.9").
- [ ] Privacy policy URL live.
- [ ] Build uploaded, visible in TestFlight, installed and run once on the iPhone from TestFlight.
- [ ] Age rating, App Privacy, review notes and video filled in.
- [ ] Submit for Review.
