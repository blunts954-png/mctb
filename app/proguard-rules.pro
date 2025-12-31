# MCTB Auto-Reply ProGuard Rules
# Production-ready configuration for release builds

# ================================
# Debugging & Stack Traces
# ================================
# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for better debugging
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ================================
# Kotlin & Coroutines
# ================================
# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ================================
# AndroidX DataStore
# ================================
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.*.** { *; }

# ================================
# Jetpack Compose
# ================================
# Keep Compose runtime classes
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }

# Keep Composable functions (for reflection)
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Don't warn about Compose internals
-dontwarn androidx.compose.**

# ================================
# Navigation Component
# ================================
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.navigation.Navigator

# ================================
# App-Specific Rules
# ================================
# Keep all broadcast receivers (used by CallReceiver, BootReceiver)
-keep public class * extends android.content.BroadcastReceiver {
    public <init>();
}

# Keep all services (CallMonitorService)
-keep public class * extends android.app.Service {
    public <init>();
}

# Keep our app's data models and preferences
-keep class com.mctb.autoreply.data.** { *; }

# Keep SMS handler utility methods
-keep class com.mctb.autoreply.util.SmsHandler {
    public <methods>;
}

# ================================
# General Android
# ================================
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep view constructors (for inflation)
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ================================
# Optimization
# ================================
# Allow aggressive optimization
-optimizationpasses 5
-dontpreverify
-repackageclasses ''
-allowaccessmodification

# Don't obfuscate (easier debugging, minimal security benefit for this app)
-dontobfuscate
