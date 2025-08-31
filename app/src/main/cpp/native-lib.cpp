#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

extern "C" {
#include "embroidery.h"
}

#define LOG_TAG "NativeLib"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* --- Helpers --- */
static inline int stitch_count(const EmbPattern* p) {
    return (p && p->stitch_list) ? p->stitch_list->count : 0;
}
static inline int thread_count(const EmbPattern* p) {
    return (p && p->thread_list) ? p->thread_list->count : 0;
}

/* --- JNI: convertEmbToDst --- */
extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_embviewer_jni_NativeLib_convertEmbToDst(
        JNIEnv* env, jobject,
        jstring inputPath, jstring outputPath) {

    if (!inputPath || !outputPath) return JNI_FALSE;

    const char* inPath  = env->GetStringUTFChars(inputPath, nullptr);
    const char* outPath = env->GetStringUTFChars(outputPath, nullptr);

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        env->ReleaseStringUTFChars(inputPath, inPath);
        env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    if (!emb_pattern_read(pattern, inPath, 0)) {
        LOGE("Failed to read: %s", inPath);
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(inputPath, inPath);
        env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    if (!emb_pattern_write(pattern, outPath, 0)) {
        LOGE("Failed to write: %s", outPath);
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(inputPath, inPath);
        env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(inputPath, inPath);
    env->ReleaseStringUTFChars(outputPath, outPath);

    LOGI("Conversion successful: %s -> %s", inPath, outPath);
    return JNI_TRUE;
}

/* --- JNI: metadata --- */
extern "C" JNIEXPORT jstring JNICALL
Java_com_example_embviewer_jni_NativeLib_metadata(
        JNIEnv* env, jobject,
        jstring filePath) {

    if (!filePath) return env->NewStringUTF("Error: path null");
    const char* path = env->GetStringUTFChars(filePath, nullptr);

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        env->ReleaseStringUTFChars(filePath, path);
        return env->NewStringUTF("Error: create failed");
    }
    if (!emb_pattern_read(pattern, path, 0)) {
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(filePath, path);
        return env->NewStringUTF("Error: read failed");
    }

    int stitches = stitch_count(pattern);
    int threads  = thread_count(pattern);

    char buf[256];
    snprintf(buf, sizeof(buf),
             "File: %s\nStitches: %d\nThreads: %d",
             path, stitches, threads);

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(filePath, path);

    return env->NewStringUTF(buf);
}

/* --- JNI: stitches (x,y pairs as float array) --- */
extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_example_embviewer_jni_NativeLib_stitches(
        JNIEnv* env, jobject,
        jstring filePath) {

    if (!filePath) return nullptr;
    const char* path = env->GetStringUTFChars(filePath, nullptr);

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        env->ReleaseStringUTFChars(filePath, path);
        return nullptr;
    }
    if (!emb_pattern_read(pattern, path, 0)) {
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(filePath, path);
        return nullptr;
    }

    int count = stitch_count(pattern);
    jfloatArray arr = env->NewFloatArray(count * 2);
    if (!arr) {
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(filePath, path);
        return nullptr;
    }

    std::vector<float> coords;
    coords.reserve(count * 2);

    EmbArray* stitches = pattern->stitch_list;
    for (int i = 0; i < count; i++) {
        EmbStitch s = stitches->stitch[i];
        coords.push_back(static_cast<float>(s.x));
        coords.push_back(static_cast<float>(s.y));
    }

    env->SetFloatArrayRegion(arr, 0, count * 2, coords.data());

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(filePath, path);

    return arr;
}
