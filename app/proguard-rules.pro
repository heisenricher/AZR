# Baseline ProGuard rules for OfflineToolbox
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
