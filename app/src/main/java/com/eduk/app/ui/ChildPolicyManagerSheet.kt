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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.eduk.app.cloud.AppRuleRequest
import com.eduk.app.cloud.CloudChild
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.LearningPreferencesRequest
import com.eduk.app.cloud.ParentPolicyResponse
import com.eduk.app.cloud.RewardRuleRequest
import com.eduk.app.cloud.ScheduleRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val RulesNavy = Color(0xFF0B1F3A)
private val RulesOrange = Color(0xFFFF7A1A)
private val RulesInk = Color(0xFF182C45)
private val RulesMuted = Color(0xFF62738A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildPolicyManagerSheet(
    child: CloudChild,
    parentToken: String?,
    onDismiss: () -> Unit,
    onPolicyChanged: () -> Unit
) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var policyState by remember { mutableStateOf<ParentPolicyResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var savingKey by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var minutesPerAnswer by remember { mutableStateOf("10") }
    var dailyMaxMinutes by remember { mutableStateOf("60") }
    var subjectsText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("adaptive") }
    var learningGoals by remember { mutableStateOf("") }
    var customPackageName by remember { mutableStateOf("") }
    var customAppName by remember { mutableStateOf("") }
    var customAppMode by remember { mutableStateOf("learning_gate") }

    fun reload() {
        if (parentToken == null) {
            loading = false
            message = "Your parent session has expired. Sign in again to manage rules."
            return
        }
        loading = true
        scope.launch {
            runCatching {
                EdukCloudRepository.getParentPolicy(parentToken, child.id) to
                    EdukCloudRepository.getLearningPreferences(parentToken, child.id)
            }.onSuccess { (policy, preferences) ->
                    policyState = policy
                    subjectsText = preferences.subjects.joinToString(", ")
                    selectedDifficulty = preferences.difficulty
                    learningGoals = preferences.goals.orEmpty()
                    val firstReward = policy.rewardRules.firstOrNull()
                    if (firstReward != null) {
                        minutesPerAnswer = firstReward.correctAnswerMinutes.toString()
                        dailyMaxMinutes = firstReward.dailyMaxEarnedMinutes.toString()
                    } else {
                        dailyMaxMinutes = policy.policy.dailyEarnedTimeCapMinutes.toString()
                    }
                    message = null
                }
                .onFailure { message = "We could not load these controls. Check your connection and try again." }
            loading = false
        }
    }

    fun saveAppPreset(key: String, request: AppRuleRequest) {
        if (parentToken == null) return
        savingKey = key
        scope.launch {
            runCatching { EdukCloudRepository.saveAppRule(parentToken, child.id, request) }
                .onSuccess { message = "${request.displayName} rule saved."; reload(); onPolicyChanged() }
                .onFailure { message = "That app rule could not be saved." }
            savingKey = null
        }
    }

    fun saveSchoolSchedule() {
        if (parentToken == null) return
        savingKey = "school"
        scope.launch {
            runCatching {
                EdukCloudRepository.createSchedule(
                    parentToken, child.id,
                    ScheduleRequest("School hours", listOf(1, 2, 3, 4, 5), 8 * 60, 15 * 60, "block_entertainment")
                )
            }.onSuccess { message = "School-hours rule saved."; reload(); onPolicyChanged() }
                .onFailure { message = "The school-hours rule could not be saved." }
            savingKey = null
        }
    }

    fun deleteAppRule(ruleId: String) {
        if (parentToken == null) return
        savingKey = "delete-app:$ruleId"
        scope.launch {
            runCatching { EdukCloudRepository.deleteAppRule(parentToken, child.id, ruleId) }
                .onSuccess { message = "App rule removed."; reload(); onPolicyChanged() }
                .onFailure { message = "That app rule could not be removed." }
            savingKey = null
        }
    }

    fun deleteSchedule(scheduleId: String) {
        if (parentToken == null) return
        savingKey = "delete-schedule:$scheduleId"
        scope.launch {
            runCatching { EdukCloudRepository.deleteSchedule(parentToken, child.id, scheduleId) }
                .onSuccess { message = "Schedule removed."; reload(); onPolicyChanged() }
                .onFailure { message = "That schedule could not be removed." }
            savingKey = null
        }
    }

    fun deleteRewardRule(ruleId: String) {
        if (parentToken == null) return
        savingKey = "delete-reward:$ruleId"
        scope.launch {
            runCatching { EdukCloudRepository.deleteRewardRule(parentToken, child.id, ruleId) }
                .onSuccess { message = "Reward rule removed."; reload(); onPolicyChanged() }
                .onFailure { message = "That reward rule could not be removed." }
            savingKey = null
        }
    }

    fun saveCustomAppRule() {
        if (customPackageName.trim().length < 3 || customAppName.trim().isBlank()) {
            message = "Enter the app name and Android package name to save a custom rule."
            return
        }
        saveAppPreset("custom-app", AppRuleRequest(customPackageName.trim(), customAppName.trim(), "Custom", customAppMode))
    }

    fun saveRewardRule() {
        val earned = minutesPerAnswer.toIntOrNull()
        val cap = dailyMaxMinutes.toIntOrNull()
        if (earned == null || earned !in 0..120 || cap == null || cap !in 0..1440) {
            message = "Use whole-number rewards from 0–120 minutes and a daily cap from 0–1,440 minutes."
            return
        }
        if (parentToken == null) return
        savingKey = "reward"
        scope.launch {
            runCatching {
                EdukCloudRepository.createRewardRule(
                    parentToken, child.id,
                    RewardRuleRequest("Daily learning reward", null, earned, 0, cap, 1)
                )
            }.onSuccess { message = "Learning reward saved."; reload(); onPolicyChanged() }
                .onFailure { message = "The learning reward could not be saved." }
            savingKey = null
        }
    }

    fun saveLearningPreferences() {
        if (parentToken == null) return
        val subjects = subjectsText.split(",").map(String::trim).filter(String::isNotBlank).distinct()
        if (subjects.size > 12) {
            message = "Use up to 12 subjects, separated by commas."
            return
        }
        savingKey = "learning-profile"
        scope.launch {
            runCatching {
                EdukCloudRepository.saveLearningPreferences(
                    parentToken, child.id,
                    LearningPreferencesRequest(subjects, selectedDifficulty, learningGoals.trim().ifBlank { null })
                )
            }.onSuccess { message = "Adaptive learning preferences saved." }
                .onFailure { message = "Learning preferences could not be saved." }
            savingKey = null
        }
    }

    LaunchedEffect(child.id) { reload() }

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
                Text("Rules for ${child.displayName}", color = RulesNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(5.dp))
                Text("Changes synchronize to their paired phone. Eduk keeps the last confirmed policy available offline.", color = RulesMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (loading) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 30.dp).semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RulesOrange)
                    Spacer(Modifier.height(10.dp))
                    Text("Loading protected settings…", color = RulesMuted)
                }
            }
            message?.let { status -> item { StatusCard(status) } }
            policyState?.let { current ->
                item {
                    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Protection status", color = RulesInk, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (current.policy.lockEntertainmentUntilLearning) "Entertainment apps require earned time." else "Learning gate is currently turned off.",
                                color = RulesMuted, style = MaterialTheme.typography.bodySmall
                            )
                            Text("Policy revision ${current.policy.revision}", color = RulesOrange, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    SectionHeading("Popular app controls", "Choose what happens when ${child.displayName} opens an app. These are real Android package rules.")
                }
                item {
                    QuickAppRuleRow(
                        title = "YouTube", subtitle = "Require learning time first", isSaving = savingKey == "youtube",
                        onSave = { saveAppPreset("youtube", AppRuleRequest("com.google.android.youtube", "YouTube", "Video", "learning_gate")) }
                    )
                }
                item {
                    QuickAppRuleRow(
                        title = "TikTok", subtitle = "Block at all times", isSaving = savingKey == "tiktok",
                        onSave = { saveAppPreset("tiktok", AppRuleRequest("com.zhiliaoapp.musically", "TikTok", "Social", "block")) }
                    )
                }
                item {
                    QuickAppRuleRow(
                        title = "Instagram", subtitle = "Require learning time first", isSaving = savingKey == "instagram",
                        onSave = { saveAppPreset("instagram", AppRuleRequest("com.instagram.android", "Instagram", "Social", "learning_gate")) }
                    )
                }
                item {
                    QuickAppRuleRow(
                        title = "Roblox", subtitle = "Require learning time first", isSaving = savingKey == "roblox",
                        onSave = { saveAppPreset("roblox", AppRuleRequest("com.roblox.client", "Roblox", "Games", "learning_gate")) }
                    )
                }
                item { SectionHeading("Custom app rule", "Add any Android package to the allowed, blocked, or learning-first list.") }
                item {
                    OutlinedTextField(
                        value = customAppName, onValueChange = { customAppName = it }, label = { Text("App name") },
                        placeholder = { Text("Minecraft") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it.lowercase().filter { char -> char.isLetterOrDigit() || char == '.' || char == '_' } },
                        label = { Text("Android package name") }, placeholder = { Text("com.mojang.minecraftpe") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("allow" to "Allow", "learning_gate" to "Learn first", "block" to "Block").forEach { (mode, label) ->
                            OutlinedButton(
                                onClick = { customAppMode = mode }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (customAppMode == mode) RulesNavy else Color.Transparent, contentColor = if (customAppMode == mode) Color.White else RulesNavy)
                            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
                item {
                    Button(onClick = ::saveCustomAppRule, enabled = savingKey == null, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RulesNavy), shape = RoundedCornerShape(14.dp)) {
                        Text(if (savingKey == "custom-app") "Saving app rule…" else "Save custom app rule", fontWeight = FontWeight.Bold)
                    }
                }
                if (current.appRules.isNotEmpty()) {
                    item { SectionHeading("Active app rules", "The paired student device enforces these apps using the latest policy.") }
                    items(current.appRules, key = { it.id }) { rule ->
                        Surface(color = Color.White, shape = RoundedCornerShape(16.dp), shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(rule.displayName ?: rule.packageName, color = RulesInk, fontWeight = FontWeight.Bold)
                                    Text(rule.packageName, color = RulesMuted, style = MaterialTheme.typography.labelSmall)
                                }
                                ModePill(rule.accessMode)
                                TextButton(onClick = { deleteAppRule(rule.id) }, enabled = savingKey == null) {
                                    Text(if (savingKey == "delete-app:${rule.id}") "Removing" else "Remove")
                                }
                            }
                        }
                    }
                }
                item { HorizontalDivider(color = Color(0xFFE4E8EF)) }
                item { SectionHeading("Adaptive learning", "Choose what Eduk should prioritize when it selects approved questions automatically.") }
                item {
                    OutlinedTextField(
                        value = subjectsText,
                        onValueChange = { subjectsText = it },
                        label = { Text("Subjects, separated by commas") },
                        placeholder = { Text("Math, Science, Reading") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Question difficulty", color = RulesInk, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("foundation" to "Build up", "adaptive" to "Adaptive", "stretch" to "Stretch").forEach { (value, label) ->
                            OutlinedButton(
                                onClick = { selectedDifficulty = value },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedDifficulty == value) RulesNavy else Color.Transparent,
                                    contentColor = if (selectedDifficulty == value) Color.White else RulesNavy
                                )
                            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = learningGoals,
                        onValueChange = { learningGoals = it },
                        label = { Text("Learning goal (optional)") },
                        placeholder = { Text("Prepare for the solar system test") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
                item {
                    Button(
                        onClick = ::saveLearningPreferences, enabled = savingKey == null,
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RulesNavy), shape = RoundedCornerShape(16.dp)
                    ) { Text(if (savingKey == "learning-profile") "Saving preferences…" else "Save learning preferences", fontWeight = FontWeight.Bold) }
                }
                item { HorizontalDivider(color = Color(0xFFE4E8EF)) }
                item { SectionHeading("Earned time", "Set how much screen time a correct answer earns, then cap it for the day.") }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = minutesPerAnswer, onValueChange = { minutesPerAnswer = it.filter(Char::isDigit) },
                            label = { Text("Minutes / answer") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dailyMaxMinutes, onValueChange = { dailyMaxMinutes = it.filter(Char::isDigit) },
                            label = { Text("Daily cap") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Button(
                        onClick = ::saveRewardRule, enabled = savingKey == null,
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = RulesOrange), shape = RoundedCornerShape(16.dp)
                    ) { Text(if (savingKey == "reward") "Saving reward…" else "Save learning reward", fontWeight = FontWeight.Bold) }
                }
                if (current.rewardRules.isNotEmpty()) {
                    item { SectionHeading("Active reward rules", "Rewards are calculated in the cloud before time is awarded.") }
                    items(current.rewardRules, key = { it.id }) { rule ->
                        Surface(color = Color(0xFFFFF6EE), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(rule.name, color = RulesInk, fontWeight = FontWeight.Bold)
                                    Text("${rule.correctAnswerMinutes} min per answer · ${rule.dailyMaxEarnedMinutes} min daily cap", color = RulesMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { deleteRewardRule(rule.id) }, enabled = savingKey == null) {
                                    Text(if (savingKey == "delete-reward:${rule.id}") "Removing" else "Remove")
                                }
                            }
                        }
                    }
                }
                item { HorizontalDivider(color = Color(0xFFE4E8EF)) }
                item { SectionHeading("Schedules", "Create a protected routine for school time, homework, and bedtime.") }
                item {
                    OutlinedButton(onClick = ::saveSchoolSchedule, enabled = savingKey == null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Text(if (savingKey == "school") "Saving school hours…" else "Block entertainment · Mon–Fri · 8:00 AM–3:00 PM", fontWeight = FontWeight.Bold)
                    }
                }
                if (current.schedules.isNotEmpty()) {
                    items(current.schedules, key = { it.id }) { schedule ->
                        Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(schedule.name, color = RulesInk, fontWeight = FontWeight.Bold)
                                    Text("${schedule.mode.replace('_', ' ')} · days ${schedule.daysOfWeek}", color = RulesMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                TextButton(onClick = { deleteSchedule(schedule.id) }, enabled = savingKey == null) {
                                    Text(if (savingKey == "delete-schedule:${schedule.id}") "Removing" else "Remove")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, description: String) {
    Column {
        Text(title, color = RulesNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(3.dp))
        Text(description, color = RulesMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun QuickAppRuleRow(title: String, subtitle: String, isSaving: Boolean, onSave: () -> Unit) {
    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = RulesInk, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = RulesMuted, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onSave, enabled = !isSaving, colors = ButtonDefaults.buttonColors(containerColor = RulesNavy), shape = RoundedCornerShape(12.dp)) {
                Text(if (isSaving) "Saving" else "Apply")
            }
        }
    }
}

@Composable
private fun ModePill(mode: String) {
    val label = when (mode) {
        "block" -> "Blocked"
        "learning_gate" -> "Learn first"
        else -> "Allowed"
    }
    val tint = when (mode) {
        "block" -> Color(0xFF9B2C1C)
        "learning_gate" -> Color(0xFF9A4F17)
        else -> Color(0xFF147A50)
    }
    Surface(color = tint.copy(alpha = .12f), shape = RoundedCornerShape(99.dp)) {
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun StatusCard(message: String) {
    Surface(color = Color(0xFFFFE8E1), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        Text(message, color = Color(0xFF8F3C2C), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
    }
}
