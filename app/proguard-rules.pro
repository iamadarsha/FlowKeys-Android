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
