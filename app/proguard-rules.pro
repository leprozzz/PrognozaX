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

# Data models & API
-keep class com.prognozax.app.data.models.** { *; }
-keepclassmembers class com.prognozax.app.data.models.** { *; }
-keep class com.prognozax.app.data.api.** { *; }
-keepclassmembers class com.prognozax.app.data.api.** { *; }

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class *JsonAdapter {
    public <init>(...);
}
-keepclassmembers class * {
    @com.squareup.moshi.JsonClass <fields>;
}

# Retrofit
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# OpenCSV
-keep class com.opencsv.** { *; }
-dontwarn com.opencsv.**
-dontwarn org.apache.commons.beanutils.**
-dontwarn org.apache.commons.collections.**

# Compose
-keep class androidx.compose.** { *; }

# Vico Charts
-keep class com.patrykandpatrick.vico.** { *; }
-dontwarn com.patrykandpatrick.vico.**
