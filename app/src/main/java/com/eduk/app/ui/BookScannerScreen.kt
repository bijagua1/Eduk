package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun BookScannerScreen(onScanComplete: () -> Unit) {
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            for (i in 1..100) {
                delay(30)
                scanProgress = i / 100f
            }
            isScanning = false
            showSuccess = true
            delay(1500)
            onScanComplete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Placeholder
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (!isScanning && !showSuccess) {
                    Text(
                        "Point camera at the textbook page",
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        if (isScanning) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    progress = scanProgress,
                    modifier = Modifier.size(80.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "AI is analyzing the text...",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showSuccess) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(100.dp),
                    tint = androidx.compose.ui.graphics.Color.Yellow
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "5 Questions Generated!",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!isScanning && !showSuccess) {
            Button(
                onClick = { isScanning = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .height(72.dp)
                    .width(200.dp),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Icon(Icons.Default.Camera, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Page", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
