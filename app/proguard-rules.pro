# Keep VpnService and app entry
-keep class com.vpn.client.** { *; }
-keep class * extends android.app.Service

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Moshi
-keep class com.vpn.client.data.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# libv2ray (AndroidLibV2rayLite) – when AAR is in app/libs
-keep class go.libv2ray.** { *; }
-keep class libv2ray.** { *; }
