package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.LearningEventRequest
import com.eduk.app.service.AppMonitoringService
import kotlinx.coroutines.launch

@Composable
fun QuestionScreen(onCorrect: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }

    val questionText = "What is the primary function of the mitochondria in a cell?"
    val options = listOf("Waste Removal", "Energy Production", "Storage", "Protein Synthesis")

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Eduk Challenge", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF0B1F3A), fontWeight = FontWeight.ExtraBold)
        Text("Answer correctly to earn 10 minutes", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF62738A))
        Spacer(Modifier.height(32.dp))
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color(0xFFF4F6FA)) {
            Column(Modifier.padding(20.dp)) {
                Text("Learning challenge", style = MaterialTheme.typography.labelLarge, color = Color(0xFFFF7A1A), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(questionText, style = MaterialTheme.typography.titleLarge, color = Color(0xFF182C45), fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(24.dp))
        options.forEachIndexed { index, option ->
            OutlinedButton(
                onClick = { if (!showResult) selectedOption = index },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = if (selectedOption == index) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE6D4)) else ButtonDefaults.outlinedButtonColors(),
                shape = RoundedCornerShape(16.dp)
            ) { Text(option, color = Color(0xFF182C45), fontWeight = FontWeight.SemiBold) }
        }
        Spacer(Modifier.height(30.dp))
        if (!showResult) {
            Button(
                onClick = {
                    isCorrect = selectedOption == 1
                    showResult = true
                    val token = sessionStore.studentToken()
                    if (token == null) {
                        syncError = "This student phone is not linked to Eduk Family Cloud."
                        return@Button
                    }
                    isSyncing = true
                    scope.launch {
                        runCatching {
                            EdukCloudRepository.recordLearningEvent(token, LearningEventRequest(questionText, "Biology", isCorrect))
                        }.onSuccess { result ->
                            if (isCorrect) {
                                AppMonitoringService.grantAccess(result.minutesAwarded)
                                onCorrect()
                            }
                        }.onFailure {
                            syncError = "Your answer could not sync. Please reconnect before trying again."
                        }
                        isSyncing = false
                    }
                },
                enabled = selectedOption != null && !isSyncing,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A1A)),
                shape = RoundedCornerShape(18.dp)
            ) {
                if (isSyncing) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Confirm answer", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            ResultView(isCorrect = isCorrect)
        }
        syncError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp)) }
    }
}

@Composable
fun ResultView(isCorrect: Boolean) {
    val color = if (isCorrect) Color(0xFF147A50) else MaterialTheme.colorScheme.error
    val message = if (isCorrect) "Correct — synchronizing your 10 minutes." else "Not quite. Review the material and try another question."
    Text(text = message, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}
