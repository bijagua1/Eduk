package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.StudentLoginRequest
import com.eduk.app.cloud.StudentPairRequest
import kotlinx.coroutines.launch

private val StudentNavy = Color(0xFF0B1F3A)
private val StudentOrange = Color(0xFFFF7A1A)

@Composable
fun StudentDeviceScreen(onReady: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var isPairing by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pairingCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun continueToStudentMode() {
        isSubmitting = true
        errorMessage = null
        scope.launch {
            runCatching {
                if (isPairing) {
                    EdukCloudRepository.pairStudentDevice(
                        StudentPairRequest(
                            username = username.trim().lowercase(),
                            pin = pin,
                            pairingCode = pairingCode,
                            deviceId = sessionStore.deviceId(),
                            deviceLabel = sessionStore.deviceLabel()
                        )
                    )
                } else {
                    EdukCloudRepository.loginStudent(
                        StudentLoginRequest(
                            username = username.trim().lowercase(),
                            pin = pin,
                            deviceId = sessionStore.deviceId()
                        )
                    )
                }
            }.onSuccess { session ->
                sessionStore.saveStudentSession(session.token, session.child.id, session.expiresAt)
                onReady()
            }.onFailure {
                errorMessage = if (isPairing) {
                    "We could not pair this phone. Recheck the username, PIN, and six-digit code from the parent’s dashboard."
                } else {
                    "This phone is not paired yet, or the student username and PIN are incorrect."
                }
            }
            isSubmitting = false
        }
    }

    Scaffold(containerColor = Color(0xFFF6F7FB)) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))
            Surface(color = StudentNavy, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(26.dp)) {
                    Icon(if (isPairing) Icons.Default.Link else Icons.Default.LockOpen, null, tint = StudentOrange, modifier = Modifier.size(38.dp))
                    Spacer(Modifier.height(18.dp))
                    Text(if (isPairing) "Pair this phone" else "Welcome back", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (isPairing) "Ask your parent for your username, PIN, and one-time pairing code." else "Sign in with the student account your parent created.",
                        color = Color.White.copy(alpha = 0.74f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = isPairing,
                    onClick = { isPairing = true; errorMessage = null },
                    label = { Text("First time on this phone") },
                    leadingIcon = if (isPairing) ({ Icon(Icons.Default.Link, null, Modifier.size(18.dp)) }) else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isPairing,
                    onClick = { isPairing = false; errorMessage = null },
                    label = { Text("Sign in") },
                    leadingIcon = if (!isPairing) ({ Icon(Icons.Default.LockOpen, null, Modifier.size(18.dp)) }) else null,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().filter { char -> char.isLetterOrDigit() || char in "._-" } },
                label = { Text("Student username") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                label = { Text("Student PIN") },
                leadingIcon = { Icon(Icons.Default.Key, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
            if (isPairing) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pairingCode,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pairingCode = it },
                    label = { Text("6-digit pairing code") },
                    leadingIcon = { Icon(Icons.Default.Link, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
            }
            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = ::continueToStudentMode,
                enabled = !isSubmitting && username.length >= 3 && pin.length >= 4 && (!isPairing || pairingCode.length == 6),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StudentOrange),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (isPairing) "Pair & enter Student Mode" else "Sign in to Student Mode", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
