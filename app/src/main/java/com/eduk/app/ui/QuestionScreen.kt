package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.service.AppMonitoringService

@Composable
fun QuestionScreen(onCorrect: () -> Unit) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Eduk Challenge",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Answer to unlock 10 minutes",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Source: Biology Textbook (Scanned by AI)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What is the primary function of the mitochondria in a cell?",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val options = listOf("Waste Removal", "Energy Production", "Storage", "Protein Synthesis")
        options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { if (!showResult) selectedOption = index },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = if (selectedOption == index) 
                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(text = option)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!showResult) {
            Button(
                onClick = {
                    showResult = true
                    isCorrect = selectedOption == 1
                    if (isCorrect) {
                        AppMonitoringService.grantAccess(10)
                        onCorrect()
                    }
                },
                enabled = selectedOption != null,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Confirm Answer", fontSize = 18.sp)
            }
        } else {
            ResultView(isCorrect = isCorrect)
        }
    }
}

@Composable
fun ResultView(isCorrect: Boolean) {
    val color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val message = if (isCorrect) "Correct! You've earned 10 minutes." else "Not quite. Try another question."
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (isCorrect) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enjoy your screen time!", style = MaterialTheme.typography.bodyMedium)
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* In a real app, load new question */ }) {
                Text("Try Another Question")
            }
        }
    }
}
