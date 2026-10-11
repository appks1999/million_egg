# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Kotlinx Serialization & DataStore Models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep kotlinx.serialization runtime
-keep class kotlinx.serialization.** { *; }

# Keep all KSerializer implementations and Companion objects
-keep class * implements kotlinx.serialization.KSerializer {
    <init>(...);
}
-keepclassmembers class * implements kotlinx.serialization.KSerializer {
    *** INSTANCE;
}

# Keep serializable model classes and generated $serializer / Companion classes
-keep class com.akito.million_egg.GameProgress { *; }
-keep class com.akito.million_egg.GameProgress$* { *; }
-keep class com.akito.million_egg.PlayerState { *; }
-keep class com.akito.million_egg.PlayerState$* { *; }
-keep class com.akito.million_egg.Title { *; }
-keep class com.akito.million_egg.Title$* { *; }
-keep class com.akito.million_egg.PlayerStateSerializer { *; }
-keep class com.akito.million_egg.GameProgressSerializer { *; }

# Keep AndroidX Startup, WorkManager, and Room Database (required by AdMob SDK)
-keep class androidx.startup.** { *; }
-keep class androidx.work.** { *; }
-keep class androidx.room.** { *; }

# Keep Google Play Services Ads (AdMob)
-keep class com.google.android.gms.ads.** {
    public *;
}
-keep interface com.google.android.gms.ads.** {
    public *;
}
