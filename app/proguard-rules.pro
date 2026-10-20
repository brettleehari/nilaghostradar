# MediaPipe Tasks loads classes and native symbols reflectively; keep its public surface.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
# Keep line numbers for readable crash reports from testers.
-keepattributes SourceFile,LineNumberTable
