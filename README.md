# Nila Diha's Ghost Radar

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

## iOS

The same app for iPhone lives in `ios/` (Swift, SwiftUI, same model and numbers). See `ios/README.md`.

## How a hunt works

The sequence is Hari's (2026-10-28). Every number lives in one table, `HuntSequence`
(`app/src/main/java/com/nila/ghostradar/scare/HuntSequence.kt` and `ios/GhostCore/Sources/GhostCore/Configs.swift`);
change both together.

| Step | What happens | Setting | Default |
|---|---|---|---|
| 1 | The radar sweeps, SEARCHING…. Things it recognises get a `CHAIR · NO SOUL` readout. Pointing at a toy warms the radar (green → amber → red, COLD → WARM → HOT, faster ticks, faster music) but **nothing can lock** yet. | `noLockAfterOpenMs` | 10 s |
| 2 | A toy is found: lock, `SIGNAL FOUND · LISTENING`, and a voice says "I can see you" once in full. | `nearLine` | on |
| 3 | At the end of the line, if the toy is still in view: the radar goes red and says HERE, the phone jolts, the voice says "Bloody Mary", and the lunge and one of three stingers fire this long after the line starts. If the toy left during the line, nothing happens and the hunt goes on. | `bangAfterLineMs` | 1.5 s |
| 4 | After the lunge the screen cuts to black static, SIGNAL LOST; the radar comes back red and shaking, says GONE, cools to green; two seconds in, the face flashes once with a thump. The cooldown ends with the stinger (max 8 s). | `ScareConfig` | — |
| 5 | Cool off: **nothing can lock** again for this long. | `noLockAfterScareMs` | 10 s |
| — | Leaving the app this long restarts the hunt at step 1; a short interruption (a call, the screen going off briefly) does not. | `sessionResetAfterMs` | 60 s |

"Still in view" is the detection gate's rule: the toy has been seen within the last 500 ms. Point
away and everything is normal again; the phone never shows the doll "normal" itself, the kid has to
look up at the real one.

The earlier rarity rules (`HauntPolicy`: warm-up, 45 s gap, 3 in 10 locks, re-roll) are still in the
code and tested but no longer wired in.
