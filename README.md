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
