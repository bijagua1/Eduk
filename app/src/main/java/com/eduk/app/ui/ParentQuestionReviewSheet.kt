package com.eduk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eduk.app.cloud.CloudChild
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.ParentQuestion
import com.google.gson.Gson
import kotlinx.coroutines.launch

private val ReviewNavy = Color(0xFF0B1F3A)
private val ReviewOrange = Color(0xFFFF7A1A)
private val ReviewInk = Color(0xFF182C45)
private val ReviewMuted = Color(0xFF62738A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentQuestionReviewSheet(child: CloudChild, parentToken: String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var questions by remember { mutableStateOf<List<ParentQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var pendingQuestionId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        if (parentToken == null) {
            isLoading = false
            message = "Your parent session has expired. Sign in again to review learning material."
            return
        }
        isLoading = true
        scope.launch {
            runCatching { EdukCloudRepository.getParentQuestions(parentToken, child.id) }
                .onSuccess { response -> questions = response.questions; message = null }
                .onFailure { message = "We could not load AI-generated questions. Check your connection and try again." }
            isLoading = false
        }
    }

    fun review(question: ParentQuestion, decision: String) {
        if (parentToken == null) return
        pendingQuestionId = question.id
        scope.launch {
            runCatching { EdukCloudRepository.reviewParentQuestion(parentToken, child.id, question.id, decision) }
                .onSuccess { message = if (decision == "approved") "Question approved for ${child.displayName}." else "Question rejected."; reload() }
                .onFailure { message = "The review decision could not be saved." }
            pendingQuestionId = null
        }
    }

    LaunchedEffect(child.id) { reload() }
    val generated = questions.filter { it.reviewStatus == "generated" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)
        ) {
            item {
                Text("Review learning questions", color = ReviewNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(5.dp))
                Text("Approve only questions that accurately match ${child.displayName}'s material. Approved questions are the only ones delivered to the child.", color = ReviewMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (isLoading) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp).semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ReviewOrange)
                    Spacer(Modifier.height(10.dp))
                    Text("Loading review queue…", color = ReviewMuted)
                }
            }
            message?.let { status -> item { ReviewStatusCard(status) } }
            if (!isLoading && generated.isEmpty()) item {
                Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("No questions awaiting review", color = ReviewInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(6.dp))
                        Text("Use the child’s book scanner to generate a new set, then return here to approve each question before it appears in a challenge.", color = ReviewMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(generated, key = { it.id }) { question ->
                ReviewQuestionCard(question = question, isSaving = pendingQuestionId == question.id, onApprove = { review(question, "approved") }, onReject = { review(question, "rejected") })
            }
            if (!isLoading && questions.any { it.reviewStatus != "generated" }) {
                item {
                    Text("Reviewed questions", color = ReviewNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                }
                items(questions.filter { it.reviewStatus != "generated" }, key = { it.id }) { question ->
                    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(question.questionText, color = ReviewInk, maxLines = 2, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Text("${question.subject} · ${question.topic}", color = ReviewMuted, style = MaterialTheme.typography.labelSmall)
                            }
                            ReviewStatusPill(question.reviewStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewQuestionCard(question: ParentQuestion, isSaving: Boolean, onApprove: () -> Unit, onReject: () -> Unit) {
    val choices = remember(question.id, question.choicesJson) { parseReviewChoices(question.choicesJson) }
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("${question.subject} · ${question.topic}", color = ReviewOrange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(7.dp))
            Text(question.questionText, color = ReviewInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            choices.forEach { choice ->
                Text("• $choice", color = ReviewMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
            Spacer(Modifier.height(10.dp))
            Surface(color = Color(0xFFE3F6EB), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Correct answer: ${question.correctAnswer}", color = Color(0xFF147A50), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(11.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(question.explanation, color = ReviewMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onReject, enabled = !isSaving, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Reject", color = Color(0xFF9B2C1C), fontWeight = FontWeight.Bold) }
                Button(onClick = onApprove, enabled = !isSaving, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ReviewOrange), shape = RoundedCornerShape(14.dp)) {
                    Text(if (isSaving) "Saving…" else "Approve", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReviewStatusPill(status: String) {
    val (label, color) = when (status) {
        "approved" -> "Approved" to Color(0xFF147A50)
        "rejected" -> "Rejected" to Color(0xFF9B2C1C)
        else -> "Generated" to Color(0xFF9A4F17)
    }
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(99.dp)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun ReviewStatusCard(message: String) {
    Surface(color = Color(0xFFFFE8E1), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        Text(message, color = Color(0xFF8F3C2C), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
    }
}

private fun parseReviewChoices(raw: String?): List<String> = runCatching {
    Gson().fromJson(raw ?: "[]", Array<String>::class.java).toList()
}.getOrDefault(emptyList()).map(String::trim).filter(String::isNotBlank)
