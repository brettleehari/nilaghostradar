# Nila Deha's Ghost Radar

Android Halloween prank: the camera spots a doll or toy, the phone shakes, a creepy song plays and the doll's face turns evil on screen. See `PROJECT.md` for the product brief and `NOTES.md` for the running log.

## Build

```
export JAVA_HOME=~/Library/Java/JavaVirtualMachines/jdk-21.0.12.1+1/Contents/Home   # arm64 JDK 17+
./gradlew :app:assembleDebug
./gradlew :app:lint :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -s GhostRadar
```

Detection needs a real device; the emulator camera is useless for this.

## How a hunt works

1. Open the app. The radar sweeps and says SEARCHING…. That is the start of a **session**.
2. Point the phone around the room. Things the camera recognises get a small readout under the
   distance: `CHAIR · NO SOUL`, `BED · NO SOUL`. Nothing else happens.
3. Point at a doll or a teddy. The radar locks on and the arrow swings to it. Then one of two things:
   - most of the time a small `FAINT TRACE · NO ANSWER` note appears and that is all;
   - now and then the music stops, the phone whispers "Bloody Mary", bangs, and the doll lunges.
4. Point away and everything is normal again.

What decides which toys are haunted (`HauntPolicy.Config`): nothing in the first 15 s of a session,
at least 45 s between scares, then about 3 in 10 locks, and never more than three faint traces in a
row. A session is one hunt: leaving the app for a minute or more starts a fresh one; a short
interruption (a call, the screen going off briefly) does not.
