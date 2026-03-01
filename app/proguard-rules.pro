# Keep Compose runtime entrypoints used via annotations/reflection
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Preview methods (if you use preview)
-keepclassmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}

# Keep Android framework classes and Activities (default file already does much, this is additive)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# Keep Kotlin metadata (helps reflection-based libs)
-keepclassmembers class kotlin.Metadata { *; }

-keep class com.microsoft.onnxruntime.** { *; }
-keepclassmembers class * { native <methods>; }
-keepclasseswithmembers class * {
    native <methods>;
}
-keep class com.microsoft.onnxruntime.** { *; }
-keepclassmembers class * { native <methods>; }
-keepclasseswithmembers class * {
    native <methods>;
}