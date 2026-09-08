# FlowKeys Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Keep Sherpa-ONNX JNI bindings (for Phase 2)
-keep class com.k2fsa.sherpa.onnx.** { *; }
-keepclassmembers class com.k2fsa.sherpa.onnx.** {
    native <methods>;
}

# Keep Coroutines and Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep FlowKeys models, polishers and cloud providers
-keep class com.flowkeys.android.core.model.** { *; }
-keep class com.flowkeys.android.polish.** { *; }
-keep class com.flowkeys.android.providers.** { *; }
-keep class com.flowkeys.android.modes.** { *; }
-keep class com.flowkeys.android.data.** { *; }
-keep class com.flowkeys.android.accessibility.** { *; }
-keep class com.flowkeys.android.overlay.** { *; }
-keep class com.flowkeys.android.asr.** { *; }
-keep class com.flowkeys.android.service.** { *; }

# Keep Android core components
-keep public class * extends android.app.Service
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver

# Strip debug and verbose logs from release APK
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Keep ML Kit Translate
-keep class com.google.mlkit.nl.translate.** { *; }

