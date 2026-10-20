# MediaPipe Tasks loads classes and native symbols reflectively; keep its public surface.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
# Keep line numbers for readable crash reports from testers.
-keepattributes SourceFile,LineNumberTable

# Flogger (used by MediaPipe) finds its caller by walking the stack; inlining or renaming its
# classes breaks that with "no caller found on the stack". Keep it whole.
-keep class com.google.common.flogger.** { *; }
-dontwarn com.google.common.flogger.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
