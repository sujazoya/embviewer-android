# Keep JNI bridge names
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep our data classes used via JNI
-keep class com.example.embviewer.nativebridge.** { *; }
