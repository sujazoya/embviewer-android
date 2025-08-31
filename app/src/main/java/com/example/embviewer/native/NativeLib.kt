package com.example.embviewer.jni

object NativeLib {
    init {
        System.loadLibrary("embroidery-converter") // ✅ must match add_library in CMakeLists.txt
    }

    external fun convertEmbToDst(inPath: String, outPath: String): Boolean
    external fun metadata(inPath: String): String?
    external fun stitches(inPath: String): FloatArray?
}
