# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to the flags specified
# in defaultProguardFile("proguard-android-optimize.txt").

# Keep data models used with Moshi / JSON serialization
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep OpenCSV beans and models
-keep class com.climateinsights.app.data.models.** { *; }
-keepclassmembers class com.climateinsights.app.data.models.** { *; }

# Retain Compose runtime rules
-keep class androidx.compose.** { *; }
