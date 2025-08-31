package com.example.embviewer

object ativeBridge {
    init { System.loadLibrary("embbridge") }
    external fun convertEmbToDst(inputPath: String, outputPath: String): Int
    external fun getDesignMetadata(inputPath: String): DesignMetadata?
}
