package com.eduk.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eduk.app.cloud.CloudChild
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.LearningProgressResponse
import com.eduk.app.cloud.SubjectLearningProgress
import com.eduk.app.cloud.TopicLearningProgress
import kotlinx.coroutines.launch

private val AnalyticsNavy = Color(0xFF0B1F3A)
private val AnalyticsOrange = Color(0xFFFF7A1A)
private val AnalyticsInk = Color(0xFF182C45)
private val AnalyticsMuted = Color(0xFF62738A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLearningProgressSheet(child: CloudChild, parentToken: String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<LearningProgressResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadProgress() {
        if (parentToken == null) {
            loading = false
            errorMessage = "Your parent session has expired. Sign in again to view learning progress."
            return
        }
        loading = true
        scope.launch {
            runCatching { EdukCloudRepository.getParentLearningProgress(parentToken, child.id) }
                .onSuccess { response -> progress = response; errorMessage = null }
                .onFailure { errorMessage = "We could not load this learning report. Check your connection and try again." }
            loading = false
        }
    }

    LaunchedEffect(child.id) { loadProgress() }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)) {
            FlowingControlBackdrop(pulse = (progress?.totalAttempts ?: 0) + if (loading) 0 else 1)
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 42.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
            item {
                Text("${child.displayName}'s learning", color = AnalyticsNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text("This report reflects verified cloud attempts and earned time, not device-only estimates.", color = AnalyticsMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (loading) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AnalyticsOrange)
                    Spacer(Modifier.height(10.dp))
                    Text("Loading learning progress…", color = AnalyticsMuted)
                }
            }
            errorMessage?.let { message -> item { AnalyticsMessage(message) } }
            progress?.let { data ->
                item { AnalyticsSummary(data) }
                item { Text("Subject progress", color = AnalyticsNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                if (data.bySubject.isEmpty()) {
                    item {
                        Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                            Text("No verified answers yet. After a child completes approved questions, their subject progress will appear here.", color = AnalyticsMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(16.dp))
                        }
                    }
                } else {
                    items(data.bySubject, key = { it.subject }) { subject -> SubjectAnalyticsRow(subject) }
                }
                if (data.byTopic.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Recommended learning focus", color = AnalyticsNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Text("Eduk prioritizes these lower-accuracy topics when choosing approved questions automatically.", color = AnalyticsMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    items(data.byTopic.take(3), key = { "${it.subject}:${it.topic}" }) { topic -> TopicAnalyticsRow(topic) }
                }
            }
            }
        }
    }
}

@Composable
private fun AnalyticsSummary(progress: LearningProgressResponse) {
    Surface(color = AnalyticsNavy, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Verified learning summary", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                AnalyticsMetric("Accuracy", if (progress.totalAttempts == 0) "—" else "${progress.accuracyPercent}%", Modifier.weight(1f))
                AnalyticsMetric("XP", progress.xp.toString(), Modifier.weight(1f))
                AnalyticsMetric("Streak", "${progress.currentStreak}d", Modifier.weight(1f))
            }
            Spacer(Modifier.height(13.dp))
            Text("${progress.correctAttempts} correct out of ${progress.totalAttempts} attempts · ${progress.minutesEarned} minutes earned · ${progress.completedChallenges} challenges completed", color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AnalyticsMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color.White.copy(alpha = .12f), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(label, color = Color.White.copy(alpha = .68f), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun SubjectAnalyticsRow(subject: SubjectLearningProgress) {
    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(subject.subject, color = AnalyticsInk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text("${subject.correct}/${subject.attempts} correct · ${subject.minutesEarned} min earned", color = AnalyticsMuted, style = MaterialTheme.typography.bodySmall)
            }
            Surface(color = AnalyticsOrange.copy(alpha = .13f), shape = RoundedCornerShape(99.dp)) {
                Text("${subject.accuracyPercent}%", color = AnalyticsOrange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun TopicAnalyticsRow(topic: TopicLearningProgress) {
    Surface(color = Color(0xFFFFF3E8), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(topic.topic, color = AnalyticsInk, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                Text("${topic.subject} · ${topic.correct}/${topic.attempts} correct", color = AnalyticsMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("${topic.accuracyPercent}%", color = AnalyticsOrange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AnalyticsMessage(message: String) {
    Surface(color = Color(0xFFFFE8E1), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Text(message, color = Color(0xFF8F3C2C), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
    }
}
