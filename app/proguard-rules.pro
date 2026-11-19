# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to the flags specified
# in defaultProguardFile("proguard-android-optimize.txt").

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.prognozax.app.ui.navigation.** { *; }

# Keep reflection-based Moshi models and Retrofit API interface annotations
-keepclassmembers class com.prognozax.app.data.models.** { *; }
-keep class com.prognozax.app.data.models.** { *; }
-keepclassmembers class com.prognozax.app.data.api.** { *; }
-keep class com.prognozax.app.data.api.** { *; }

# Moshi reflection & JsonClass support
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class *JsonAdapter {
    public <init>(...);
}
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}

# Retrofit (uses its own consumer rules; keep service interface annotations only)
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
