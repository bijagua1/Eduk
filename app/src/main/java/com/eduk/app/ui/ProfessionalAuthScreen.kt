package com.eduk.app.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
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
import com.eduk.app.cloud.EdukCloudException
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.ParentLoginRequest
import com.eduk.app.cloud.ParentRegisterRequest
import kotlinx.coroutines.launch

private val AuthNavy = Color(0xFF0B1F3A)
private val AuthOrange = Color(0xFFFF7A1A)
private val AuthBody = Color(0xFF52667D)

@Composable
fun ProfessionalAuthScreen(
    country: String,
    language: String,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var isLogin by remember { mutableStateOf(true) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = AuthNavy,
        unfocusedTextColor = AuthNavy,
        disabledTextColor = AuthNavy,
        cursorColor = AuthOrange,
        focusedBorderColor = AuthOrange,
        unfocusedBorderColor = Color(0xFF8190A0),
        focusedLabelColor = AuthOrange,
        unfocusedLabelColor = AuthBody,
        focusedLeadingIconColor = AuthOrange,
        unfocusedLeadingIconColor = AuthBody,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White
    )

    fun authenticate() {
        isSubmitting = true
        errorMessage = null
        scope.launch {
            runCatching {
                if (isLogin) {
                    EdukCloudRepository.loginParent(ParentLoginRequest(email.trim(), password))
                } else {
                    EdukCloudRepository.registerParent(
                        ParentRegisterRequest(
                            email = email.trim(),
                            password = password,
                            displayName = displayName.trim(),
                            country = country,
                            language = language
                        )
                    )
                }
            }.onSuccess { session ->
                sessionStore.saveParentSession(session.token, session.family.id, session.expiresAt)
                onAuthSuccess()
            }.onFailure { error ->
                val cloudError = error as? EdukCloudException
                errorMessage = when {
                    cloudError?.errorCode == "PARENT_EXISTS" -> "An Eduk parent account already exists for this email. Choose Sign in instead."
                    cloudError?.errorCode == "INVALID_REQUEST" -> "Please enter a valid email, a name, and a password with at least 8 characters."
                    cloudError?.errorCode == "DATABASE_UNAVAILABLE" -> "Eduk Family Cloud is temporarily unavailable. Please try again in a moment."
                    cloudError != null -> cloudError.message
                    else -> "We could not reach Eduk Family Cloud. Check your connection and try again."
                }
            }
            isSubmitting = false
        }
    }

    Scaffold(containerColor = Color(0xFFF6F7FB)) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(color = AuthNavy, shape = RoundedCornerShape(26.dp), modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (isLogin) Icons.Default.Lock else Icons.Default.Person, null, tint = AuthOrange, modifier = Modifier.size(40.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(if (isLogin) "Parent sign in" else "Create parent account", color = AuthNavy, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (isLogin) "Manage your family through Eduk Family Cloud." else "Your account creates the secure family space for child devices.",
                color = AuthBody,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(30.dp))

            if (!isLogin) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Parent name") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = AuthNavy),
                    colors = inputColors,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(14.dp))
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email address") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AuthNavy),
                colors = inputColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(if (isLogin) "Password" else "Create password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AuthNavy),
                colors = inputColors,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true
            )
            if (!isLogin) {
                Text("Use at least 8 characters. Your child will receive separate credentials and a PIN.", color = AuthBody, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 9.dp))
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
            }
            Spacer(Modifier.height(30.dp))
            Button(
                onClick = ::authenticate,
                enabled = !isSubmitting && email.isNotBlank() && password.length >= 8 && (isLogin || displayName.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuthOrange, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isSubmitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text(if (isLogin) "Sign in to Family Cloud" else "Create secure family account", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = { isLogin = !isLogin; errorMessage = null }) {
                Text(if (isLogin) "New to Eduk? Create an account" else "Already have an account? Sign in", color = AuthNavy, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
