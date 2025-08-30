#include <jni.h>
#include <string>
#include <android/log.h>

extern "C" {
#include "embroidery.h"
}

#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "EMB_JNI", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  "EMB_JNI", __VA_ARGS__)

/**
 * Convert .emb → .dst
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_embviewer_nativebridge_EmbBridge_convertEmbToDst(
        JNIEnv* env, jobject /*thiz*/,
        jstring inPath_, jstring outPath_) {

    const char* inPath = env->GetStringUTFChars(inPath_, nullptr);
    const char* outPath = env->GetStringUTFChars(outPath_, nullptr);

    EmbPattern* p = emb_pattern_create();
    if (!p) { LOGE("Failed to create pattern"); goto error; }

    // some builds require 3 args, some 2. Use 3 with NULL.
    if (!emb_pattern_read(p, inPath, NULL)) {
        LOGE("Failed to read: %s", inPath);
        emb_pattern_free(p);
        goto error;
    }

    if (!emb_pattern_write(p, outPath, NULL)) {
        LOGE("Failed to write: %s", outPath);
        emb_pattern_free(p);
        goto error;
    }

    emb_pattern_free(p);
    env->ReleaseStringUTFChars(inPath_, inPath);
    env->ReleaseStringUTFChars(outPath_, outPath);
    return 0;

    error:
    if (inPath) env->ReleaseStringUTFChars(inPath_, inPath);
    if (outPath) env->ReleaseStringUTFChars(outPath_, outPath);
    return -1;
}

/**
 * Read embroidery metadata
 */
extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_embviewer_nativebridge_EmbBridge_readMetadata(
        JNIEnv* env, jobject /*thiz*/, jstring inPath_) {

    const char* inPath = env->GetStringUTFChars(inPath_, nullptr);
    EmbPattern* p = emb_pattern_create();
    if (!p) {
        env->ReleaseStringUTFChars(inPath_, inPath);
        return nullptr;
    }

    if (!emb_pattern_read(p, inPath, NULL)) {
        emb_pattern_free(p);
        env->ReleaseStringUTFChars(inPath_, inPath);
        return nullptr;
    }

    // stitch & color counts
    int stitchCount = 0;
    int colorCount = 0;
#ifdef HAVE_EMB_STITCH_COUNT
    stitchCount = emb_stitch_count(p);
#endif
#ifdef HAVE_EMB_THREAD_COUNT
    colorCount = emb_thread_count(p);
#endif

    // bounding box (x, y, w, h in your version)
    EmbRect bounds = emb_pattern_bounds(p);
    double minX = bounds.x;
    double minY = bounds.y;
    double maxX = bounds.w;
    double maxY = bounds.h;
    double width  = maxX - minX;
    double height = maxY - minY;

    emb_pattern_free(p);
    env->ReleaseStringUTFChars(inPath_, inPath);

    // Build EmbMetadata object
    jclass metaCls = env->FindClass("com/example/embviewer/nativebridge/EmbMetadata");
    if (!metaCls) return nullptr;

    jmethodID ctor = env->GetMethodID(metaCls, "<init>", "(IIDD[D)V");
    if (!ctor) return nullptr;

    jdoubleArray boundsArr = env->NewDoubleArray(4);
    jdouble vals[4] = {minX, minY, maxX, maxY};
    env->SetDoubleArrayRegion(boundsArr, 0, 4, vals);

    jobject meta = env->NewObject(metaCls, ctor,
                                  stitchCount, colorCount,
                                  width, height, boundsArr);
    return meta;
}

/**
 * Render embroidery to PNG (not supported in libembroidery)
 */
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_embviewer_nativebridge_EmbBridge_renderEmbToPng(
        JNIEnv* env, jobject /*thiz*/,
        jstring inPath_, jstring outPngPath_,
        jint width, jint height) {

    const char* inPath = env->GetStringUTFChars(inPath_, nullptr);
    const char* outPng = env->GetStringUTFChars(outPngPath_, nullptr);

    EmbPattern* p = emb_pattern_create();
    if (!p) goto error;

    if (!emb_pattern_read(p, inPath, NULL)) {
        emb_pattern_free(p);
        goto error;
    }

    LOGE("PNG export not supported in this version of libembroidery.");
    emb_pattern_free(p);

    env->ReleaseStringUTFChars(inPath_, inPath);
    env->ReleaseStringUTFChars(outPngPath_, outPng);
    return -1;

    error:
    if (inPath) env->ReleaseStringUTFChars(inPath_, inPath);
    if (outPng) env->ReleaseStringUTFChars(outPngPath_, outPng);
    return -1;
}
