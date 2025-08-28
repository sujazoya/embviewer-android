#include <jni.h>
#include <android/log.h>
#include <cstdlib>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "embbridge", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "embbridge", __VA_ARGS__)

// Include libembroidery headers
#include "pattern.h"   // Adjust include path according to your submodule layout
#include "read.h"
#include "write.h"

extern "C" JNIEXPORT jint JNICALL
Java_com_example_embviewer_NativeBridge_convertEmbToDst(
    JNIEnv* env, jclass, jstring jInputPath, jstring jOutputPath) {

    const char* inputPath = env->GetStringUTFChars(jInputPath, nullptr);
    const char* outputPath = env->GetStringUTFChars(jOutputPath, nullptr);

    LOGI("convertEmbToDst input=%s out=%s", inputPath, outputPath);

    // Create a new pattern
    Pattern* pattern = pattern_new();
    if (!pattern) {
        LOGE("Failed to allocate pattern");
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        env->ReleaseStringUTFChars(jOutputPath, outputPath);
        return -1;
    }

    // Read EMB file
    if (pattern_read(pattern, inputPath) != 0) {
        LOGE("Failed to read EMB file");
        pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        env->ReleaseStringUTFChars(jOutputPath, outputPath);
        return -2;
    }

    // Write DST file
    if (pattern_write_dst(pattern, outputPath) != 0) {
        LOGE("Failed to write DST file");
        pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        env->ReleaseStringUTFChars(jOutputPath, outputPath);
        return -3;
    }

    LOGI("Conversion successful, stitch count: %d", pattern->stitch_count);

    // Free pattern
    pattern_free(pattern);

    env->ReleaseStringUTFChars(jInputPath, inputPath);
    env->ReleaseStringUTFChars(jOutputPath, outputPath);
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_embviewer_NativeBridge_getDesignMetadata(
    JNIEnv* env, jclass, jstring jInputPath) {

    const char* inputPath = env->GetStringUTFChars(jInputPath, nullptr);

    // Allocate pattern
    Pattern* pattern = pattern_new();
    if (!pattern) {
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    // Read EMB file
    if (pattern_read(pattern, inputPath) != 0) {
        LOGE("Failed to read EMB for metadata");
        pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    int stitchCount = pattern->stitch_count;
    double width_mm = pattern->max_x - pattern->min_x;
    double height_mm = pattern->max_y - pattern->min_y;

    LOGI("Metadata: stitches=%d width=%.2fmm height=%.2fmm", stitchCount, width_mm, height_mm);

    // Find Kotlin DesignMetadata class and constructor
    jclass metaClass = env->FindClass("com/example/embviewer/DesignMetadata");
    if (!metaClass) {
        LOGE("DesignMetadata class not found");
        pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(metaClass, "<init>", "(IDD)V");
    if (!ctor) {
        LOGE("DesignMetadata constructor not found");
        pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    jobject metaObj = env->NewObject(metaClass, ctor, stitchCount, width_mm, height_mm);

    // Free pattern
    pattern_free(pattern);
    env->ReleaseStringUTFChars(jInputPath, inputPath);

    return metaObj;
}
