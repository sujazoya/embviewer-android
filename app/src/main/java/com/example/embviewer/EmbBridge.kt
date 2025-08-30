package com.example.embviewer.nativebridge

data class EmbMetadata(
    val stitchCount: Int,
    val colorCount: Int,
    val width: Double,
    val height: Double,
    val bounds: DoubleArray // [minX, minY, maxX, maxY]
)

object EmbBridge {
    init {
        System.loadLibrary("emb_jni")
    }
    external fun convertEmbToDst(inputPath: String, outputPath: String): Int
    external fun readMetadata(inputPath: String): EmbMetadata?
    external fun renderEmbToPng(inputPath: String, outputPngPath: String, width: Int, height: Int): Int
}
