#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "embroidery.h"   // make sure your include path points to this header
}

#define LOG_TAG "EmbroideryConverter"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/* ----------------- Small helpers for your EmbArray layout ----------------- */

static inline int stitch_count(const EmbPattern* p) {
    return (p && p->stitch_list) ? p->stitch_list->count : 0;
}

static inline int thread_count(const EmbPattern* p) {
    return (p && p->thread_list) ? p->thread_list->count : 0;
}

typedef struct {
    double left;
    double top;
    double right;
    double bottom;
    bool   valid;
} Bounds;

static Bounds compute_bounds(const EmbPattern* p) {
    Bounds b;
    b.left = b.top =  1e9;
    b.right = b.bottom = -1e9;
    b.valid = false;

    if (!p || !p->stitch_list || p->stitch_list->count <= 0 || !p->stitch_list->stitch) {
        return b;
    }

    const EmbArray* arr = p->stitch_list;
    for (int i = 0; i < arr->count; ++i) {
        const EmbStitch& s = arr->stitch[i];  // EmbStitch has x, y
        if (s.x < b.left)   b.left   = s.x;
        if (s.x > b.right)  b.right  = s.x;
        if (s.y < b.top)    b.top    = s.y;
        if (s.y > b.bottom) b.bottom = s.y;
        b.valid = true;
    }
    return b;
}

/* ----------------- JNI: convert EMB → (auto) DST by extension ----------------- */

extern "C" JNIEXPORT jboolean JNICALL
Java_com_example_embviewer_utils_EmbroideryConverter_convertEmbToDst(
        JNIEnv* env,
        jobject /* this */,
        jstring inputPath,
        jstring outputPath) {

    if (!inputPath || !outputPath) {
        LOGE("Input or output path is null");
        return JNI_FALSE;
    }

    const char* inPath  = env->GetStringUTFChars(inputPath,  nullptr);
    const char* outPath = env->GetStringUTFChars(outputPath, nullptr);

    if (!inPath || !outPath) {
        LOGE("Failed to get string paths");
        if (inPath)  env->ReleaseStringUTFChars(inputPath,  inPath);
        if (outPath) env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        LOGE("Failed to create pattern");
        env->ReleaseStringUTFChars(inputPath,  inPath);
        env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    // format=0 → let libembroidery auto-detect from file extension
    if (!emb_pattern_read(pattern, inPath, 0)) {
        LOGE("Failed to read input file: %s", inPath);
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(inputPath,  inPath);
        env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    // For writing, format=0 → auto-detect from output file extension (.dst)
    if (!emb_pattern_write(pattern, outPath, 0)) {
        LOGE("Failed to write output file: %s", outPath);
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(inputPath,  inPath);
        env->ReleaseStringUTFChars(outputPath, outPath);
        return JNI_FALSE;
    }

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(inputPath,  inPath);
    env->ReleaseStringUTFChars(outputPath, outPath);

    LOGI("Conversion successful: %s -> %s", inPath, outPath);
    return JNI_TRUE;
}

/* ----------------- JNI: metadata as human-readable string ----------------- */

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_embviewer_utils_EmbroideryConverter_getPatternMetadata(
        JNIEnv* env,
        jobject /* this */,
        jstring filePath) {

    if (!filePath) return env->NewStringUTF("Error: File path is null");

    const char* path = env->GetStringUTFChars(filePath, nullptr);
    if (!path) return env->NewStringUTF("Error: Could not get file path");

    EmbPattern* pattern = emb_pattern_create();
    if (!pattern) {
        env->ReleaseStringUTFChars(filePath, path);
        return env->NewStringUTF("Error: Could not create pattern");
    }

    if (!emb_pattern_read(pattern, path, 0)) {
        LOGE("Failed to read file: %s", path);
        emb_pattern_free(pattern);
        env->ReleaseStringUTFChars(filePath, path);
        return env->NewStringUTF("Error: Could not read file");
    }

    const int stitches = stitch_count(pattern);
    const int threads  = thread_count(pattern);
    const Bounds b     = compute_bounds(pattern);

    double width  = 0.0;
    double height = 0.0;
    if (b.valid) {
        width  = b.right  - b.left;
        height = b.bottom - b.top;
    }

    char metadata[1024];
    snprintf(metadata, sizeof(metadata),
             "File: %s\n"
             "Width: %.2f mm\n"
             "Height: %.2f mm\n"
             "Stitch Count: %d\n"
             "Color Count: %d%s%s",
             path,
             width, height,
             stitches, threads,
             b.valid ? "\nBounds: [" : "",
             b.valid ? "" : "");  // we’ll finish bounds line only if valid

    if (b.valid) {
        // append bounds to the same buffer (safe because `metadata` is large)
        char tail[128];
        snprintf(tail, sizeof(tail), "%.2f, %.2f, %.2f, %.2f]",
                 b.left, b.top, b.right, b.bottom);
        strncat(metadata, tail, sizeof(metadata) - strlen(metadata) - 1);
    }

    emb_pattern_free(pattern);
    env->ReleaseStringUTFChars(filePath, path);

    return env->NewStringUTF(metadata);
}
