package com.example.embviewer.utils

class EmbroideryConverter {

    external fun convertEmbToDst(inputPath: String, outputPath: String): Boolean

    external fun getPatternMetadata(filePath: String): String

    companion object {
        init {
            System.loadLibrary("embroidery-converter")
        }
    }
}