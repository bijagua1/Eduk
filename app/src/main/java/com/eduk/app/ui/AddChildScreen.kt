package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.School
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
import com.eduk.app.cloud.CreateChildRequest
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.PairingCodeResponse
import kotlinx.coroutines.launch

private val EdukNavy = Color(0xFF0B1F3A)
private val EdukOrange = Color(0xFFFF7A1A)

@Composable
fun AddChildScreen(onChildCreated: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var gradeLevel by remember { mutableStateOf("") }
    var timeLimit by remember { mutableStateOf("60") }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pairingCode by remember { mutableStateOf<PairingCodeResponse?>(null) }

    fun createChild() {
        val parentToken = sessionStore.parentToken()
        if (parentToken == null) {
            errorMessage = "Your parent session has expired. Please sign in again."
            return
        }
        isSaving = true
        errorMessage = null
        scope.launch {
            runCatching {
                val child = EdukCloudRepository.createChild(
                    parentToken,
                    CreateChildRequest(
                        displayName = displayName.trim(),
                        username = username.trim().lowercase(),
                        pin = pin,
                        gradeLevel = gradeLevel.toInt(),
                        dailyTimeLimitMinutes = timeLimit.toInt()
                    )
                ).child
                EdukCloudRepository.createPairingCode(parentToken, child.id)
            }.onSuccess { code ->
                pairingCode = code
            }.onFailure {
                errorMessage = "We could not create this child account. Check the username, PIN, and connection, then try again."
            }
            isSaving = false
        }
    }

    Scaffold(
        containerColor = Color(0xFFF6F7FB),
        topBar = {
            TopAppBar(
                title = { Text("Create child account", fontWeight = FontWeight.Bold, color = EdukNavy) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF6F7FB))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = EdukNavy
            ) {
                Column(Modifier.padding(24.dp)) {
                    Icon(Icons.Default.PersonAdd, null, tint = EdukOrange, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(18.dp))
                    Text("A real account for their own phone.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Create the username and PIN your child will use, then give them the one-time pairing code.", color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Child identity", color = EdukNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Child's name") },
                leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().filter { character -> character.isLetterOrDigit() || character in "._-" } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Student username") },
                supportingText = { Text("Used to sign in on the child’s phone.") },
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))
            Text("Sign-in and daily limit", color = EdukNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("4–8 digit PIN") },
                    leadingIcon = { Icon(Icons.Default.Key, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = gradeLevel,
                    onValueChange = { gradeLevel = it.filter(Char::isDigit) },
                    modifier = Modifier.weight(0.72f),
                    label = { Text("Grade") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = timeLimit,
                onValueChange = { timeLimit = it.filter(Char::isDigit) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Daily screen-time limit (minutes)") },
                leadingIcon = { Icon(Icons.Default.School, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )

            errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = ::createChild,
                enabled = !isSaving && displayName.isNotBlank() && username.length >= 3 && pin.length >= 4 && gradeLevel.toIntOrNull() in 1..12 && timeLimit.toIntOrNull() in 0..1440,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EdukOrange)
            ) {
                if (isSaving) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Create account & pairing code", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pairingCode?.let { pairing ->
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            icon = { Icon(Icons.Default.Key, null, tint = EdukOrange, modifier = Modifier.size(36.dp)) },
            title = { Text("Pair the child’s phone", color = EdukNavy, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("On the child’s phone, choose Student Mode, enter their username and PIN, then type this one-time code.")
                    Spacer(Modifier.height(20.dp))
                    Text(pairing.code, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, color = EdukOrange, letterSpacing = 6.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("This code expires in 10 minutes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            },
            confirmButton = {
                Button(onClick = { pairingCode = null; onChildCreated() }, colors = ButtonDefaults.buttonColors(containerColor = EdukNavy)) {
                    Text("I saved the code")
                }
            }
        )
    }
}
