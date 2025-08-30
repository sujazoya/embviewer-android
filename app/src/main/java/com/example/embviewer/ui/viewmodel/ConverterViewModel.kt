package com.example.embviewer.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.embviewer.utils.EmbroideryConverter
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

    private val converter = EmbroideryConverter()

    fun selectFile(uri: Uri) {
        selectedFileUri = uri
        conversionState = ConversionState.Idle
    }

    fun convertFile(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            conversionState = ConversionState.Converting

            try {
                val result = withContext(Dispatchers.IO) {
                    // Create input file
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val inputFile = File.createTempFile("temp_emb_$timeStamp", ".emb", context.cacheDir)
                    inputStream?.use { input ->
                        FileOutputStream(inputFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Create output file
                    val outputFile = File.createTempFile("converted_$timeStamp", ".dst", context.cacheDir)

                    // Convert
                    val success = converter.convertEmbToDst(inputFile.absolutePath, outputFile.absolutePath)

                    if (success) {
                        val metadata = converter.getPatternMetadata(inputFile.absolutePath)
                        Pair(outputFile.absolutePath, metadata)
                    } else {
                        null
                    }
                }

                if (result != null) {
                    conversionState = ConversionState.Success(result.first, result.second)
                } else {
                    conversionState = ConversionState.Error("Conversion failed")
                }
            } catch (e: Exception) {
                conversionState = ConversionState.Error("Error: ${e.message}")
            }
        }
    }

    fun resetState() {
        conversionState = ConversionState.Idle
    }
}