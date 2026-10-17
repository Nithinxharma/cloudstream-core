# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# CloudStream Engine Models & Serializers
-keep class com.lagradost.cloudstream3.** { *; }
-keep interface com.lagradost.cloudstream3.** { *; }
-keep class com.lagradost.api.** { *; }

# Serialization annotations
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
    @kotlinx.serialization.Serializable <fields>;
    @com.fasterxml.jackson.annotation.JsonProperty <fields>;
}

# JS Interpreters & DOM Parsers
-keep class org.mozilla.javascript.** { *; }
-keep class org.jsoup.** { *; }
-keep class com.fleeksoft.ksoup.** { *; }

# Dalvik Dynamic Class Loading
-keep class dalvik.system.** { *; }