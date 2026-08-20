package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(role: String, onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    
    // Parent State
    var parentEmail by remember { mutableStateOf("") }
    var parentPin by remember { mutableStateOf("") }
    
    // Child State
    var studentName by remember { mutableStateOf("") }
    var parentLinkEmail by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (role == "parent") {
            ParentOnboarding(
                step = step,
                email = parentEmail,
                onEmailChange = { parentEmail = it },
                pin = parentPin,
                onPinChange = { parentPin = it },
                onNext = { if (step < 2) step++ else onComplete() }
            )
        } else {
            ChildOnboarding(
                step = step,
                name = studentName,
                onNameChange = { studentName = it },
                parentEmail = parentLinkEmail,
                onParentEmailChange = { parentLinkEmail = it },
                onNext = { if (step < 2) step++ else onComplete() }
            )
        }
    }
}

@Composable
fun ParentOnboarding(
    step: Int,
    email: String,
    onEmailChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    onNext: () -> Unit
) {
    when (step) {
        1 -> {
            Text("Create Parent Account", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Parent Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = onPinChange,
                label = { Text("Set Master PIN") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = email.isNotEmpty() && pin.length >= 4) {
                Text("Create Account")
            }
        }
        2 -> {
            Text("Account Created!", style = MaterialTheme.typography.headlineMedium)
            Text("Now you can add your children and scan their books from the dashboard.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Go to Dashboard")
            }
        }
    }
}

@Composable
fun ChildOnboarding(
    step: Int,
    name: String,
    onNameChange: (String) -> Unit,
    parentEmail: String,
    onParentEmailChange: (String) -> Unit,
    onNext: () -> Unit
) {
    when (step) {
        1 -> {
            Text("Student Setup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Student Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = parentEmail,
                onValueChange = onParentEmailChange,
                label = { Text("Parent's Registered Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth(), enabled = name.isNotEmpty() && parentEmail.isNotEmpty()) {
                Text("Link to Parent")
            }
        }
        2 -> {
            Text("Device Linked!", style = MaterialTheme.typography.headlineMedium)
            Text("Eduk is now active. You will need to answer questions to earn screen time.", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text("Start Learning")
            }
        }
    }
}
