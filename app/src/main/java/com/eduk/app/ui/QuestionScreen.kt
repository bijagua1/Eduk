package com.eduk.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
private val ChallengeDeepBlue = Color(0xFF173D6C)
private val ChallengeOrange = Color(0xFFFF7A1A)
private val ChallengeGold = Color(0xFFFFC94A)
private val ChallengeInk = Color(0xFF182C45)
private val ChallengeMuted = Color(0xFF62738A)
private val ChallengeSuccess = Color(0xFF1FAE72)

@Composable
fun QuestionScreen(restrictedAppPackage: String? = null, onCorrect: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val motionEnabled = rememberEdukMotionEnabled()
    var challenge by remember { mutableStateOf<ActiveStudentChallenge?>(null) }
    var questionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<ChallengeAttemptResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var questionStartedAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    BackHandler(enabled = true) { /* A pending parent-authorized gate cannot be dismissed with Back. */ }

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
    val totalQuestions = challenge?.questions?.size ?: 0
    val options = remember(currentQuestion?.id, currentQuestion?.choicesJson) {
        parseChoices(currentQuestion)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ChallengeNavy, ChallengeDeepBlue, ChallengeNavy)))
    ) {
        Surface(
            color = ChallengeOrange.copy(alpha = 0.14f),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).size(190.dp).padding(top = (-60).dp, end = (-60).dp)
        ) {}
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LockedHeader()
            Spacer(Modifier.height(14.dp))
            if (!loading && currentQuestion != null && totalQuestions > 0) {
                GateProgressBar(current = questionIndex + 1, total = totalQuestions, motionEnabled = motionEnabled)
                Spacer(Modifier.height(18.dp))
            }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFFF6F8FC),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp)) {
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
                            onSelect = {
                                if (result == null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedAnswer = it
                                }
                            },
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
                                            ChallengeAttemptRequest(
                                                challengeId = challenge!!.id,
                                                questionId = currentQuestion.id,
                                                answer = selected,
                                                responseTimeSeconds = elapsedSeconds,
                                                accessPackageName = restrictedAppPackage
                                            )
                                        )
                                    }.onSuccess { response ->
                                        result = response
                                        haptics.performHapticFeedback(
                                            if (response.wasCorrect) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                                        )
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
        }
    }
}

@Composable
private fun LockedHeader() {
    Surface(color = Color.White.copy(alpha = 0.10f), shape = RoundedCornerShape(99.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, tint = ChallengeGold, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text("APP LOCKED · ANSWER TO CONTINUE", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
    Spacer(Modifier.height(16.dp))
    Text("Eduk Challenge", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
    Text("Learn, earn, unlock.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
}

@Composable
private fun GateProgressBar(current: Int, total: Int, motionEnabled: Boolean) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Question $current of $total", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 1..total) {
                val isDone = i < current
                val isActive = i == current
                val fraction by animateFloatAsState(
                    targetValue = if (isDone || isActive) 1f else 0.25f,
                    animationSpec = tween(if (motionEnabled) 180 else 0),
                    label = "segment"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(7.dp)
                        .background(
                            (if (isDone) ChallengeSuccess else if (isActive) ChallengeOrange else Color.White).copy(alpha = if (isActive || isDone) fraction else 0.20f),
                            RoundedCornerShape(99.dp)
                        )
                )
            }
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
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = ChallengeOrange.copy(alpha = 0.08f)) {
        Column(Modifier.padding(16.dp)) {
            Text(challenge.title, style = MaterialTheme.typography.labelLarge, color = ChallengeOrange, fontWeight = FontWeight.Bold)
            Text(question.subject, style = MaterialTheme.typography.labelSmall, color = ChallengeMuted)
            Spacer(Modifier.height(8.dp))
            Text(question.questionText, style = MaterialTheme.typography.titleLarge, color = ChallengeInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
        }
    }
    Spacer(Modifier.height(16.dp))
    if (options.isEmpty()) {
        EmptyChallenge("This approved question is missing its choices. Ask a parent to review a new one.", onContinue)
        return
    }
    options.forEach { option ->
        AnswerOption(
            text = option,
            selected = selectedAnswer == option,
            locked = result != null,
            isCorrectReveal = result != null && option == selectedAnswer,
            wasCorrect = result?.wasCorrect,
            onClick = { onSelect(option) }
        )
        Spacer(Modifier.height(8.dp))
    }
    Spacer(Modifier.height(10.dp))
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
    } else ChallengeResult(result = result, onContinue = onContinue)
    errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Assertive }) }
}

@Composable
private fun AnswerOption(
    text: String,
    selected: Boolean,
    locked: Boolean,
    isCorrectReveal: Boolean,
    wasCorrect: Boolean?,
    onClick: () -> Unit
) {
    val revealColor = when {
        !locked -> null
        isCorrectReveal && wasCorrect == true -> ChallengeSuccess
        isCorrectReveal && wasCorrect == false -> MaterialTheme.colorScheme.error
        else -> null
    }
    val borderWidth by animateDpAsState(if (selected || revealColor != null) 2.dp else 1.dp, label = "borderWidth")
    val borderColor = revealColor ?: if (selected) ChallengeOrange else Color(0xFFDCE3EE)
    val backgroundColor = revealColor?.copy(alpha = 0.10f)
        ?: if (selected) ChallengeOrange.copy(alpha = 0.10f) else Color.White

    Surface(
        onClick = onClick,
        enabled = !locked,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text, color = ChallengeInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (isCorrectReveal && wasCorrect != null) {
                Icon(
                    if (wasCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = revealColor ?: ChallengeMuted
                )
            }
        }
    }
}

@Composable
private fun ChallengeResult(result: ChallengeAttemptResponse, onContinue: () -> Unit) {
    val isRewarded = result.wasCorrect && result.minutesAwarded > 0
    val color = if (result.wasCorrect) ChallengeSuccess else MaterialTheme.colorScheme.error
    val message = when {
        isRewarded && result.challengeCompleted -> "Correct! ${result.minutesAwarded} minutes are ready."
        result.wasCorrect && result.challengeCompleted -> "Correct — you reached today's earning limit."
        result.wasCorrect -> "Correct — keep going to complete this challenge."
        else -> "Not quite. Read the explanation and try again."
    }
    Surface(color = color.copy(alpha = .10f), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isRewarded || result.wasCorrect) Icons.Default.CheckCircle else Icons.Default.Close,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(message, color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            }
            result.explanation?.let { explanation ->
                Spacer(Modifier.height(10.dp))
                Text(explanation, color = ChallengeInk, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue, colors = ButtonDefaults.buttonColors(containerColor = ChallengeNavy), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Text(if (result.challengeCompleted && isRewarded) "Unlock apps" else if (result.wasCorrect) "Next question" else "Try again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun LoadingChallenge() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
        CircularProgressIndicator(color = ChallengeOrange)
        Spacer(Modifier.height(14.dp))
        Text("Getting an approved challenge…", color = ChallengeMuted)
    }
}

@Composable
private fun EmptyChallenge(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Challenge not ready", color = ChallengeNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        Text(message, color = ChallengeMuted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = ChallengeOrange), shape = RoundedCornerShape(14.dp)) { Text("Check again") }
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
