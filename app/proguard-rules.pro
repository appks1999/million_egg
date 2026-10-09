# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlinx Serialization & DataStore Models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
}

-keepclassmembers class com.akito.million_egg.GameProgress { *; }
-keepclassmembers class com.akito.million_egg.PlayerState { *; }
-keepclassmembers class com.akito.million_egg.Title { *; }

# Keep Google Play Services Ads (AdMob)
-keep class com.google.android.gms.ads.** {
    public *;
}
-keep interface com.google.android.gms.ads.** {
    public *;
}
