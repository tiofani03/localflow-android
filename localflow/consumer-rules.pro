# Kotlinx serialization runtime rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep serializable classes and their companion properties
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class *$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}

# Keep OkHttp rules if needed
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
