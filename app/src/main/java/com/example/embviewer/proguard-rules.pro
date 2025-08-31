# Keep JNI bridge
-keep class com.example.embviewer.jni.NativeLib { *; }
-keepclasseswithmembernames class * { native <methods>; }