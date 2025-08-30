package com.example.embviewer

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream

fun getDisplayName(context: Context, uri: Uri): String {
    val doc = DocumentFile.fromSingleUri(context, uri)
    return doc?.name ?: uri.lastPathSegment ?: "file.emb"
}

fun copyUriToCache(resolver: ContentResolver, uri: Uri, outFile: File): File? {
    return try {
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
        outFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
