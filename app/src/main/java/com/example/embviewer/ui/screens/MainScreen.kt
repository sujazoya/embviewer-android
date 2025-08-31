package com.example.embviewer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.embviewer.MainActivity
import com.example.embviewer.ui.viewmodel.ConversionState
import com.example.embviewer.ui.viewmodel.ConverterViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ConverterViewModel = viewModel()
) {
    val state = viewModel.conversionState
    val selectedFileUri: Uri? = viewModel.selectedFileUri
    val context = LocalContext.current

    // System file picker restricted to .emb files
    val pickEmbFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flags)
            } catch (_: Exception) { }
            viewModel.selectFile(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("EmbViewer") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { pickEmbFile.launch(arrayOf("application/octet-stream", "*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📂 Pick .EMB File")
            }

            if (selectedFileUri != null) {
                Text("Selected file: ${selectedFileUri.lastPathSegment}")
                Button(
                    onClick = { viewModel.convertFile(context, selectedFileUri) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚙️ Convert to .DST")
                }
            }

            when (state) {
                is ConversionState.Idle -> {
                    Text("Idle — pick a .emb file to start.")
                }
                is ConversionState.Converting -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text("Converting…")
                    }
                }
                is ConversionState.Success -> {
                    Text("✅ Conversion successful", color = MaterialTheme.colorScheme.primary)
                    Text("Output: ${state.outputPath}")
                    Text("Metadata:\n${state.metadata}")

                    Button(onClick = { viewModel.resetState() }) {
                        Text("Convert another")
                    }

                    // Share button
                    Button(onClick = {
                        val file = File(state.outputPath)
                        if (file.exists()) {
                            (context as MainActivity).shareFile(context, file)
                        }
                    }) {
                        Text("Share DST File")
                    }

                    Spacer(Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Output File:", style = MaterialTheme.typography.labelMedium)
                            Text(state.outputPath, style = MaterialTheme.typography.bodyMedium)

                            Spacer(Modifier.height(8.dp))

                            Text("Metadata:", style = MaterialTheme.typography.labelMedium)
                            Text(state.metadata, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { shareFile(context, state.outputPath) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📤 Share")
                        }
                        Button(
                            onClick = { viewModel.resetState() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🔄 Convert another")
                        }
                    }
                }
                is ConversionState.Error -> {
                    Text("❌ ${state.message}", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.resetState() }) { Text("Try again") }
                }
            }
        }
    }
}

// File sharing via Android Intent
fun shareFile(context: android.content.Context, path: String) {
    val fileUri = Uri.parse(path)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "*/*"
        putExtra(Intent.EXTRA_STREAM, fileUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share DST File"))
}
