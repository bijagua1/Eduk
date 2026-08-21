package com.eduk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.cloud.ActiveStudentChallenge
import com.eduk.app.cloud.ChallengeAttemptRequest
import com.eduk.app.cloud.ChallengeAttemptResponse
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.StudentChallengeQuestion
import com.eduk.app.service.AppMonitoringService
import com.google.gson.Gson
import kotlinx.coroutines.launch

private val ChallengeNavy = Color(0xFF0B1F3A)
private val ChallengeOrange = Color(0xFFFF7A1A)
private val ChallengeInk = Color(0xFF182C45)
private val ChallengeMuted = Color(0xFF62738A)

@Composable
fun QuestionScreen(onCorrect: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var challenge by remember { mutableStateOf<ActiveStudentChallenge?>(null) }
    var questionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ChallengeAttemptResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var questionStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun loadChallenge() {
        val token = sessionStore.studentToken()
        if (token == null) {
            loading = false
            errorMessage = "This phone is not linked to Eduk Family Cloud. Ask a parent for a new pairing code."
            return
        }
        loading = true
        errorMessage = null
        scope.launch {
            runCatching { EdukCloudRepository.getStudentChallenge(token) }
                .onSuccess { response ->
                    challenge = response.challenge
                    questionIndex = 0
                    selectedAnswer = null
                    result = null
                    questionStartedAt = System.currentTimeMillis()
                    errorMessage = response.message
                }
                .onFailure { errorMessage = "Your challenge could not load. Reconnect to Eduk Family Cloud and try again." }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadChallenge() }

    val currentQuestion = challenge?.questions?.getOrNull(questionIndex)
    val options = remember(currentQuestion?.id, currentQuestion?.choicesJson) {
        parseChoices(currentQuestion)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Eduk Challenge", style = MaterialTheme.typography.headlineMedium, color = ChallengeNavy, fontWeight = FontWeight.ExtraBold)
        Text("Learn, earn, unlock.", style = MaterialTheme.typography.bodyMedium, color = ChallengeMuted)
        Spacer(Modifier.height(24.dp))

        when {
            loading -> LoadingChallenge()
            currentQuestion == null -> EmptyChallenge(
                message = errorMessage ?: "No approved learning challenge is ready yet.",
                onRetry = ::loadChallenge
            )
            else -> ChallengeQuestionContent(
                challenge = challenge!!,
                question = currentQuestion,
                questionNumber = questionIndex + 1,
                options = options,
                selectedAnswer = selectedAnswer,
                result = result,
                isSubmitting = submitting,
                errorMessage = errorMessage,
                onSelect = { if (result == null) selectedAnswer = it },
                onSubmit = {
                    val selected = selectedAnswer ?: return@ChallengeQuestionContent
                    val token = sessionStore.studentToken() ?: run {
                        errorMessage = "This phone is no longer linked. Pair it again before earning time."
                        return@ChallengeQuestionContent
                    }
                    submitting = true
                    errorMessage = null
                    scope.launch {
                        val elapsedSeconds = ((System.currentTimeMillis() - questionStartedAt) / 1000).toInt().coerceAtLeast(0)
                        runCatching {
                            EdukCloudRepository.submitChallengeAttempt(
                                token,
                                ChallengeAttemptRequest(challenge!!.id, currentQuestion.id, selected, elapsedSeconds)
                            )
                        }.onSuccess { response ->
                            result = response
                            if (response.wasCorrect && response.minutesAwarded > 0) {
                                AppMonitoringService.grantAccess(response.minutesAwarded)
                            }
                        }.onFailure { errorMessage = "Your answer could not be verified. Reconnect and submit it again." }
                        submitting = false
                    }
                },
                onContinue = {
                    val latest = result ?: return@ChallengeQuestionContent
                    when {
                        latest.challengeCompleted && latest.minutesAwarded > 0 -> onCorrect()
                        latest.challengeCompleted -> loadChallenge()
                        latest.wasCorrect && questionIndex + 1 < challenge!!.questions.size -> {
                            questionIndex += 1
                            selectedAnswer = null
                            result = null
                            questionStartedAt = System.currentTimeMillis()
                        }
                        else -> {
                            selectedAnswer = null
                            result = null
                            questionStartedAt = System.currentTimeMillis()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ChallengeQuestionContent(
    challenge: ActiveStudentChallenge,
    question: StudentChallengeQuestion,
    questionNumber: Int,
    options: List<String>,
    selectedAnswer: String?,
    result: ChallengeAttemptResponse?,
    isSubmitting: Boolean,
    errorMessage: String?,
    onSelect: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinue: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color(0xFFF4F6FA)) {
        Column(Modifier.padding(20.dp)) {
            Text(challenge.title, style = MaterialTheme.typography.labelLarge, color = ChallengeOrange, fontWeight = FontWeight.Bold)
            Text("Question $questionNumber · ${question.subject}", style = MaterialTheme.typography.labelSmall, color = ChallengeMuted)
            Spacer(Modifier.height(8.dp))
            Text(question.questionText, style = MaterialTheme.typography.titleLarge, color = ChallengeInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        }
    }
    Spacer(Modifier.height(18.dp))
    if (options.isEmpty()) {
        EmptyChallenge("This approved question is missing its choices. Ask a parent to review a new one.", onContinue)
        return
    }
    options.forEach { option ->
        OutlinedButton(
            onClick = { onSelect(option) },
            enabled = result == null,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = if (selectedAnswer == option) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFE6D4)) else ButtonDefaults.outlinedButtonColors(),
            shape = RoundedCornerShape(16.dp)
        ) { Text(option, color = ChallengeInk, fontWeight = FontWeight.SemiBold) }
    }
    Spacer(Modifier.height(18.dp))
    if (result == null) {
        Button(
            onClick = onSubmit,
            enabled = selectedAnswer != null && !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChallengeOrange),
            shape = RoundedCornerShape(18.dp)
        ) {
            if (isSubmitting) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Verify answer", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        ChallengeResult(result = result, onContinue = onContinue)
    }
    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Assertive }) }
}

@Composable
private fun ChallengeResult(result: ChallengeAttemptResponse, onContinue: () -> Unit) {
    val isRewarded = result.wasCorrect && result.minutesAwarded > 0
    val color = if (result.wasCorrect) Color(0xFF147A50) else MaterialTheme.colorScheme.error
    val message = when {
        isRewarded && result.challengeCompleted -> "Correct — ${result.minutesAwarded} minutes are ready."
        result.wasCorrect && result.challengeCompleted -> "Correct — you reached today’s earning limit."
        result.wasCorrect -> "Correct — keep going to complete this challenge."
        else -> "Not quite. Read the explanation and try again."
    }
    Surface(color = color.copy(alpha = .10f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        Column(Modifier.padding(16.dp)) {
            Text(message, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            result.explanation?.let { explanation ->
                Spacer(Modifier.height(8.dp))
                Text(explanation, color = ChallengeInk, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = ChallengeNavy), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text(if (result.challengeCompleted && isRewarded) "Unlock apps" else if (result.wasCorrect) "Next question" else "Try again")
            }
        }
    }
}

@Composable
private fun LoadingChallenge() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
        CircularProgressIndicator(color = ChallengeOrange)
        Spacer(Modifier.height(14.dp))
        Text("Getting an approved challenge…", color = ChallengeMuted)
    }
}

@Composable
private fun EmptyChallenge(message: String, onRetry: () -> Unit) {
    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Challenge not ready", color = ChallengeNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(8.dp))
            Text(message, color = ChallengeMuted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ChallengeOrange), shape = RoundedCornerShape(14.dp)) { Text("Check again") }
        }
    }
}

private fun parseChoices(question: StudentChallengeQuestion?): List<String> {
    val raw = question?.choicesJson ?: return emptyList()
    return runCatching { Gson().fromJson(raw, Array<String>::class.java).toList() }
        .getOrDefault(emptyList())
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
}
