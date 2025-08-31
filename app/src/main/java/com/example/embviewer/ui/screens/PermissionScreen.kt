package com.example.embviewer.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    permissions: Array<String>,
    onPermissionsGranted: () -> Unit
) {
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            onPermissionsGranted() // Continue to app
        } else {
            showRationale = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { launcher.launch(permissions) }) {
                    Text("Grant Permissions")
                }

                if (showRationale) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Please allow permissions in Settings to continue.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
