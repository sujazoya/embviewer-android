package com.example.embviewer.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.embviewer.jni.NativeLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ConversionState {
    object Idle : ConversionState()
    object Converting : ConversionState()
    data class Success(val outputPath: String, val metadata: String) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

class ConverterViewModel : ViewModel() {

    var conversionState by mutableStateOf<ConversionState>(ConversionState.Idle)
        private set

    var selectedFileUri by mutableStateOf<Uri?>(null)
        private set

    fun selectFile(uri: Uri) {
        selectedFileUri = uri
        conversionState = ConversionState.Idle
    }

    fun convertFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            conversionState = ConversionState.Converting

            try {
                val result = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val inputFile = File.createTempFile("temp_emb_$timeStamp", ".emb", context.cacheDir)

                    inputStream?.use { input ->
                        FileOutputStream(inputFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    val outputFile = File.createTempFile("converted_$timeStamp", ".dst", context.cacheDir)

                    val success = NativeLib.convertEmbToDst(
                        inputFile.absolutePath,
                        outputFile.absolutePath
                    )

                    if (success) {
                        val metadata = NativeLib.metadata(inputFile.absolutePath) ?: "No metadata"
                        Pair(outputFile.absolutePath, metadata)
                    } else null
                }

                conversionState = if (result != null) {
                    ConversionState.Success(result.first, result.second)
                } else {
                    ConversionState.Error("Conversion failed")
                }
            } catch (e: Exception) {
                conversionState = ConversionState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetState() {
        conversionState = ConversionState.Idle
        selectedFileUri = null
    }
}
