package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (step) {
            1 -> {
                Text("Welcome to Eduk", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("The AI-powered study gatekeeper.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(48.dp))
                Button(onClick = { step = 2 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Get Started")
                }
            }
            2 -> {
                Text("Setup Student Profile", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Student Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Grade Level") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { step = 3 }, modifier = Modifier.fillMaxWidth()) {
                    Text("Next")
                }
            }
            3 -> {
                Text("Manager Account", style = MaterialTheme.typography.headlineMedium)
                Text("Enter your credentials to receive reports.", style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Parent Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = "", onValueChange = {}, label = { Text("Set Parent PIN") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onComplete, modifier = Modifier.fillMaxWidth()) {
                    Text("Finish Setup")
                }
            }
        }
    }
}
