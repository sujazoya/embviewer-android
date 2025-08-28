#include <jni.h>
#include <android/log.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "embbridge", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "embbridge", __VA_ARGS__)

extern "C" JNIEXPORT jint JNICALL
Java_com_example_embviewer_NativeBridge_convertEmbToDst(
    JNIEnv* env, jclass, jstring jInputPath, jstring jOutputPath) {

    const char *inputPath = env->GetStringUTFChars(jInputPath, nullptr);
    const char *outputPath = env->GetStringUTFChars(jOutputPath, nullptr);

    LOGI("convertEmbToDst input=%s out=%s", inputPath, outputPath);
    // TODO: Replace with real libembroidery API calls
    int result = -999;

    env->ReleaseStringUTFChars(jInputPath, inputPath);
    env->ReleaseStringUTFChars(jOutputPath, outputPath);
    return result;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_embviewer_NativeBridge_getDesignMetadata(
    JNIEnv* env, jclass, jstring jInputPath) {

    const char *inputPath = env->GetStringUTFChars(jInputPath, nullptr);

    int stitchCount = 0;
    double width_mm = 0.0;
    double height_mm = 0.0;

    jclass metaClass = env->FindClass("com/example/embviewer/DesignMetadata");
    if (!metaClass) return nullptr;
    jmethodID ctor = env->GetMethodID(metaClass, "<init>", "(IDD)V");
    if (!ctor) return nullptr;
    jobject metaObj = env->NewObject(metaClass, ctor, stitchCount, width_mm, height_mm);

    env->ReleaseStringUTFChars(jInputPath, inputPath);
    return metaObj;
}
