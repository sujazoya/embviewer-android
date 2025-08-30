#include <jni.h>
#include <android/log.h>
#include <cstdlib>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "embbridge", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "embbridge", __VA_ARGS__)

extern "C" {
#include "libembroidery/src/embroidery.h"
}


extern "C" JNIEXPORT jint JNICALL
Java_com_example_embviewer_NativeBridge_convertEmbToDst(
        JNIEnv* env,
        jobject /* this */,
        jstring jInputPath,
        jstring jOutputPath) {

    if (!jInputPath || !jOutputPath) {
        LOGE("Input or output path is null");
        return -10;
    }

    const char* inputPath  = env->GetStringUTFChars(jInputPath,  nullptr);
    const char* outputPath = env->GetStringUTFChars(jOutputPath, nullptr);

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        LOGE("Failed to create pattern");
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        env->ReleaseStringUTFChars(jOutputPath, outputPath);
        return -1;
    }

    if (!emb_pattern_read(pattern, inputPath, -1)) {
        LOGE("emb_pattern_read failed");
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        env->ReleaseStringUTFChars(jOutputPath, outputPath);
        return -2;
    }

    if (!emb_pattern_write(pattern, outputPath, -1)) {
        LOGE("emb_pattern_write failed");
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        env->ReleaseStringUTFChars(jOutputPath, outputPath);
        return -3;
    }

    // Stitch count from EmbArray
    int stitchCount = pattern->stitch_list ? pattern->stitch_list->count : 0;

    LOGI("Conversion OK, stitches=%d", stitchCount);

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(jInputPath, inputPath);
    env->ReleaseStringUTFChars(jOutputPath, outputPath);
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_example_embviewer_NativeBridge_getDesignMetadata(
        JNIEnv* env,
        jobject /* this */,
        jstring jInputPath) {

    if (!jInputPath) return nullptr;

    const char* inputPath = env->GetStringUTFChars(jInputPath, nullptr);

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    if (!emb_pattern_read(pattern, inputPath, -1)) {
        LOGE("Failed to read pattern for metadata");
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    // Stitch count from EmbArray
    int stitchCount = pattern->stitch_list ? pattern->stitch_list->count : 0;

    // Bounds
    EmbRect bounds = emb_pattern_bounds(pattern);
    double width_mm  = bounds.w;
    double height_mm = bounds.h;

    LOGI("Metadata: stitches=%d width=%.2f height=%.2f",
         stitchCount, width_mm, height_mm);

    jclass metaClass = env->FindClass("com/example/embviewer/DesignMetadata");
    if (!metaClass) {
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    jmethodID ctor = env->GetMethodID(metaClass, "<init>", "(IDD)V");
    if (!ctor) {
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(jInputPath, inputPath);
        return nullptr;
    }

    jobject metaObj = env->NewObject(metaClass, ctor,
                                     stitchCount, width_mm, height_mm);

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(jInputPath, inputPath);

    return metaObj;
}
