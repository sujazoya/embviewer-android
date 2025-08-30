
@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalPermissionsApi::class
)
package com.example.embviewer.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.embviewer.R
import com.example.embviewer.ui.components.FilePicker
import com.example.embviewer.ui.viewmodel.ConversionState
import com.example.embviewer.ui.viewmodel.ConverterViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch
import java.io.File





@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: ConverterViewModel) {
    val context = LocalContext.current
    val conversionState = viewModel.conversionState   // ✅ no "by"
    val selectedFileUri = viewModel.selectedFileUri   // ✅ no "by"
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // Storage permissions
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    )

    // File share launcher
    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission handling
            if (!permissionsState.allPermissionsGranted) {
                PermissionRequest(permissionsState)
            } else {
                // File selection
                FilePicker(
                    onFileSelected = { uri ->
                        viewModel.selectFile(uri)
                    },
                    modifier = Modifier.padding(16.dp)
                )

                // Selected file info
                selectedFileUri?.let { uri ->
                    val fileName = getFileName(context, uri)
                    Text(
                        text = stringResource(R.string.selected_file, fileName ?: "Unknown"),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Convert button
                    Button(
                        onClick = {
                            viewModel.convertFile(context, uri)
                        },
                        enabled = conversionState !is ConversionState.Converting,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (conversionState is ConversionState.Converting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(text = stringResource(R.string.convert_to_dst))
                    }
                }

                // Conversion result
                when (val state = conversionState) {
                    is ConversionState.Success -> {
                        ConversionSuccess(
                            outputPath = state.outputPath,
                            metadata = state.metadata,
                            onShare = { filePath ->
                                shareFile(context, filePath, shareLauncher)
                            }
                        )
                    }
                    is ConversionState.Error -> {
                        ConversionError(
                            message = state.message,
                            onRetry = {
                                selectedFileUri?.let { uri ->
                                    viewModel.convertFile(context, uri)
                                }
                            }
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun PermissionRequest(permissionsState: com.google.accompanist.permissions.MultiplePermissionsState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.storage_permission_explanation),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { permissionsState.launchMultiplePermissionRequest() }
        ) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}

@Composable
fun ConversionSuccess(
    outputPath: String,
    metadata: String,
    onShare: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.conversion_successful),
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Metadata display
            Text(
                text = stringResource(R.string.metadata),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = metadata,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Share button
            Button(
                onClick = { onShare(outputPath) }
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.share_file))
            }
        }
    }
}

@Composable
fun ConversionError(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.conversion_failed),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry
            ) {
                Text(text = stringResource(R.string.retry))
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(nameIndex)
    }
}

private fun shareFile(context: Context, filePath: String, launcher: ManagedActivityResultLauncher<Intent, *>) {
    val file = File(filePath)
    val contentUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    launcher.launch(Intent.createChooser(shareIntent, "Share DST file"))
}