package com.example.embviewer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : ComponentActivity() {

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedFileUri = uri
                loadMetadata(uri)
            }
        }

    private var selectedFileUri by mutableStateOf<Uri?>(null)
    private var metadata by mutableStateOf<DesignMetadata?>(null)
    private var dstPath by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request storage permissions
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                        Button(onClick = { pickFileLauncher.launch("*/*") }) {
                            Text("Pick .EMB File")
                        }

                        selectedFileUri?.let { uri ->
                            Text("Selected file: ${getFileName(uri)}")
                        }

                        metadata?.let { meta ->
                            Text("Stitches: ${meta.stitchCount}")
                            Text("Width (mm): %.2f".format(meta.widthMm))
                            Text("Height (mm): %.2f".format(meta.heightMm))

                            Button(onClick = { convertToDst(selectedFileUri!!) }) {
                                Text("Convert to DST")
                            }
                        }

                        dstPath?.let { path ->
                            ClickableText(text = androidx.compose.ui.text.AnnotatedString("DST Saved! Tap to share"),
                                onClick = { shareDst(path) })
                        }
                    }
                }
            }
        }
    }

    private fun loadMetadata(uri: Uri) {
        try {
            val file = copyUriToFile(uri)
            val meta = NativeBridge.getDesignMetadata(file.absolutePath)
            metadata = meta
            dstPath = null
        } catch (e: Exception) {
            Log.e("EmbViewer", "Metadata load error", e)
            Toast.makeText(this, "Failed to load metadata", Toast.LENGTH_SHORT).show()
        }
    }

    private fun convertToDst(uri: Uri) {
        try {
            val inputFile = copyUriToFile(uri)
            val outputFile = File(cacheDir, inputFile.nameWithoutExtension + ".dst")
            val result = NativeBridge.convertEmbToDst(inputFile.absolutePath, outputFile.absolutePath)

            if (result == 0) {
                dstPath = outputFile.absolutePath
                Toast.makeText(this, "Conversion successful!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Conversion failed: $result", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("EmbViewer", "Conversion error", e)
            Toast.makeText(this, "Conversion error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareDst(path: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share DST file"))
    }

    private fun copyUriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val file = File(cacheDir, getFileName(uri))
        FileOutputStream(file).use { out ->
            inputStream?.copyTo(out)
        }
        inputStream?.close()
        return file
    }

    private fun getFileName(uri: Uri): String {
        var name = "file.emb"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
        return name
    }
}
