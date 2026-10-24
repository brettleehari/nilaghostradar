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
2. Point the phone around the room. The radar is green, breathes slowly and says COLD. Things the
   camera recognises get a small readout: `CHAIR · NO SOUL`, `BED · NO SOUL`. Nothing else happens.
3. Point toward a doll or a teddy. Find My style, the radar warms as you get closer: green turns
   amber then red, COLD becomes WARM then HOT, the rings breathe faster, the phone ticks faster and
   the music speeds up. The arrow swings to the toy. At the lock, one of two things:
   - most of the time a small `FAINT TRACE · NO ANSWER` note appears and that is all;
   - now and then the radar goes red and says HERE, the phone jolts, whispers "Bloody Mary",
     one of three stingers bangs, and the doll lunges.
4. After the lunge the screen cuts to black static, SIGNAL LOST. The radar comes back red and
   shaking, says GONE, and cools to green; two seconds in, the face flashes once with a thump.
5. Point away and everything is normal again. The phone never shows the doll "normal" itself;
   the kid has to look up at the real one.

What decides which toys are haunted (`HauntPolicy.Config`): nothing in the first 15 s of a session,
at least 45 s between scares, then about 3 in 10 locks, and never more than three faint traces in a
row. Holding on a toy that gave a faint trace rolls the dice again every 10 s, so patience
wakes it. A session is one hunt: leaving the app for a minute or more starts a fresh one; a short
interruption (a call, the screen going off briefly) does not.
