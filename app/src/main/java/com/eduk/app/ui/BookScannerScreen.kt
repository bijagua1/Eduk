package com.eduk.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.StudyMaterialRequest
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun BookScannerScreen(onScanComplete: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val sessionStore = remember { EdukSessionStore(context) }
    var hasCameraPermission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var generatedCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
        if (!granted) errorMessage = "Camera permission is needed to scan a textbook page."
    }

    fun bindCamera(view: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            runCatching {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            }.onFailure { errorMessage = "Eduk could not start this camera." }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(hasCameraPermission, previewView) {
        if (hasCameraPermission && previewView != null) bindCamera(previewView!!)
    }

    fun captureAndGenerate() {
        val token = sessionStore.studentToken()
        val childId = sessionStore.studentChildId()
        if (token == null || childId == null) {
            errorMessage = "This phone must be paired with a student account before scanning study material."
            return
        }
        isCapturing = true
        errorMessage = null
        val photoFile = File.createTempFile("eduk-textbook-", ".jpg", context.cacheDir)
        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exception: ImageCaptureException) {
                isCapturing = false
                errorMessage = "Eduk could not capture that page. Please try again."
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val imageBase64 = Base64.encodeToString(photoFile.readBytes(), Base64.NO_WRAP)
                scope.launch {
                    runCatching {
                        EdukCloudRepository.submitStudyMaterial(
                            token,
                            childId,
                            StudyMaterialRequest(
                                sourceType = "book_photo",
                                displayName = "Textbook page",
                                imageBase64 = imageBase64,
                                imageMimeType = "image/jpeg",
                                questionCount = 5
                            )
                        )
                    }.onSuccess { response ->
                        generatedCount = response.questions.size
                    }.onFailure {
                        errorMessage = "Eduk could not generate questions from that page. Use a clear, well-lit page and try again."
                    }
                    photoFile.delete()
                    isCapturing = false
                }
            }
        })
    }

    Scaffold(containerColor = Color(0xFFF6F7FB)) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Scan your study page", style = MaterialTheme.typography.headlineSmall, color = Color(0xFF0B1F3A))
            Text("Eduk uses the page you capture to create your next learning challenge.", color = Color(0xFF52667D), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(20.dp))
            Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().weight(1f), color = Color(0xFF0B1F3A)) {
                when {
                    !hasCameraPermission -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color(0xFFFF7A1A), modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Allow camera access to scan a textbook page.", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A1A))) { Text("Allow camera") }
                    }
                    generatedCount > 0 -> Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFFF7A1A), modifier = Modifier.size(62.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("$generatedCount validated questions are ready", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(10.dp))
                        Text("Your parent can review them before they become part of a challenge.", color = Color(0xFFD5DEEA), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(22.dp))
                        Button(onClick = onScanComplete, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A1A))) { Text("Back to learning") }
                    }
                    else -> AndroidView(factory = { previewContext -> PreviewView(previewContext).also { previewView = it } }, modifier = Modifier.fillMaxSize())
                }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            if (hasCameraPermission && generatedCount == 0) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = ::captureAndGenerate,
                    enabled = !isCapturing,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A1A)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    if (isCapturing) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    else { Icon(Icons.Default.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Capture page & create questions") }
                }
            }
        }
    }
}
