package com.example.embviewer

object NativeBridge {
    init { System.loadLibrary("embbridge") }
    external fun convertEmbToDst(inputPath: String, outputPath: String): Int
    external fun getDesignMetadata(inputPath: String): DesignMetadata?
}
