package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eduk.app.cloud.AppRuleRequest
import com.eduk.app.cloud.CloudAppRule
import com.eduk.app.cloud.CloudChild
import com.eduk.app.cloud.CloudInstalledApp
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.LearningPreferencesRequest
import com.eduk.app.cloud.ParentPolicyResponse
import com.eduk.app.cloud.RewardRuleRequest
import com.eduk.app.cloud.ScheduleRequest
import kotlinx.coroutines.launch

private val RulesNavy = Color(0xFF0B1F3A)
private val RulesOrange = Color(0xFFFF7A1A)
private val RulesInk = Color(0xFF172B44)
private val RulesMuted = Color(0xFF52677F)
private val RulesCanvas = Color(0xFFF5F7FB)

@Composable
fun ChildPolicyManagerSheet(
    child: CloudChild,
    parentToken: String?,
    onDismiss: () -> Unit,
    onPolicyChanged: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var policyState by remember { mutableStateOf<ParentPolicyResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var savingKey by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var selectedApp by remember { mutableStateOf<CloudInstalledApp?>(null) }
    var minutesPerAnswer by remember { mutableStateOf("10") }
    var dailyMaxMinutes by remember { mutableStateOf("60") }
    var subjectsText by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf("adaptive") }
    var learningGoals by remember { mutableStateOf("") }
    var motionPulse by remember { mutableStateOf(0) }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = RulesInk,
        unfocusedTextColor = RulesInk,
        disabledTextColor = RulesInk,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = RulesOrange,
        unfocusedBorderColor = Color(0xFF74869B),
        focusedLabelColor = RulesOrange,
        unfocusedLabelColor = RulesMuted,
        cursorColor = RulesOrange,
        focusedPlaceholderColor = Color(0xFF6A7D92),
        unfocusedPlaceholderColor = Color(0xFF6A7D92)
    )

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
                policy.rewardRules.firstOrNull()?.let { reward ->
                    minutesPerAnswer = reward.correctAnswerMinutes.toString()
                    dailyMaxMinutes = reward.dailyMaxEarnedMinutes.toString()
                }
                message = null
            }.onFailure {
                message = "We could not load these controls. Check your connection and try again."
            }
            loading = false
        }
    }

    fun saveAppRule(app: CloudInstalledApp, mode: String, limitMinutes: Int?) {
        if (parentToken == null) return
        savingKey = "app:${app.packageName}"
        scope.launch {
            runCatching {
                EdukCloudRepository.saveAppRule(
                    parentToken,
                    child.id,
                    AppRuleRequest(
                        packageName = app.packageName,
                        displayName = app.displayName,
                        category = if (app.isSystemApp) "System" else "Installed app",
                        accessMode = mode,
                        dailyLimitMinutes = limitMinutes
                    )
                )
            }.onSuccess {
                message = "${app.displayName} rule saved and will sync to ${child.displayName}'s phone."
                selectedApp = null
                reload()
                onPolicyChanged()
            }.onFailure {
                message = "That app rule could not be saved. Check your connection and try again."
            }
            savingKey = null
        }
    }

    fun removeAppRule(rule: CloudAppRule) {
        if (parentToken == null) return
        savingKey = "remove:${rule.id}"
        scope.launch {
            runCatching { EdukCloudRepository.deleteAppRule(parentToken, child.id, rule.id) }
                .onSuccess { message = "${rule.displayName ?: rule.packageName} now uses no custom rule."; reload(); onPolicyChanged() }
                .onFailure { message = "That app rule could not be removed." }
            savingKey = null
        }
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
        savingKey = "learning"
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

    fun saveSchoolHours() {
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

    LaunchedEffect(child.id) { reload() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            color = RulesCanvas,
            shape = RoundedCornerShape(30.dp)
        ) {
            Box(Modifier.fillMaxSize()) {
                FlowingControlBackdrop(pulse = motionPulse)
                Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${child.displayName}'s controls", color = RulesNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                        Text("Clear rules for this exact phone", color = RulesMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onDismiss) { Text("Close", color = RulesOrange, fontWeight = FontWeight.Bold) }
                }
                HorizontalDivider(color = Color(0xFFE0E6EF))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (loading) {
                        item {
                            Column(Modifier.fillMaxWidth().padding(vertical = 42.dp).semantics { liveRegion = LiveRegionMode.Polite }, horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RulesOrange)
                                Spacer(Modifier.height(12.dp))
                                Text("Loading ${child.displayName}'s controls…", color = RulesMuted)
                            }
                        }
                    }
                    message?.let { status -> item { StatusCard(status) } }
                    policyState?.let { current ->
                        item {
                            Surface(color = RulesNavy, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp)) {
                                    Text("How Eduk protects app time", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Choose an app below. You can allow it, block it, or allow daily minutes before Eduk asks an approved learning question.", color = Color.White.copy(alpha = .78f), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        item { SectionHeading("Apps on ${child.displayName}'s phone", "These apps are reported by the paired child device. Tap any app to set its rule.") }
                        val visibleApps = current.installedApps.filterNot { it.isSystemApp }
                        if (visibleApps.isEmpty()) {
                            item {
                                Surface(color = Color.White, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.padding(20.dp)) {
                                        Text("Waiting for the child phone", color = RulesInk, fontWeight = FontWeight.ExtraBold)
                                        Spacer(Modifier.height(5.dp))
                                        Text("Open Eduk in Student Mode on ${child.displayName}'s paired phone. It will securely send the visible app list here; no random popular-app suggestions are needed.", color = RulesMuted, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        } else {
                            items(visibleApps, key = { it.id }) { app ->
                                val rule = current.appRules.firstOrNull { it.packageName == app.packageName }
                                InstalledAppCard(app = app, rule = rule, onConfigure = { motionPulse += 1; selectedApp = app })
                            }
                        }
                        if (current.appRules.isNotEmpty()) {
                            item { SectionHeading("Configured rules", "Every active rule stays visible so you always know what Eduk will enforce.") }
                            items(current.appRules, key = { "configured-${it.id}" }) { rule ->
                                ConfiguredRuleCard(rule = rule, isSaving = savingKey == "remove:${rule.id}", onRemove = { removeAppRule(rule) })
                            }
                        }
                        item { HorizontalDivider(color = Color(0xFFE0E6EF)) }
                        item { SectionHeading("Learning time reward", "Set how much screen time a correct answer earns, and the total that can be earned each day.") }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = minutesPerAnswer,
                                    onValueChange = { minutesPerAnswer = it.filter(Char::isDigit) },
                                    label = { Text("Minutes per correct answer") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = inputColors
                                )
                                OutlinedTextField(
                                    value = dailyMaxMinutes,
                                    onValueChange = { dailyMaxMinutes = it.filter(Char::isDigit) },
                                    label = { Text("Daily earned-time cap") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = inputColors
                                )
                            }
                        }
                        item {
                            Button(onClick = ::saveRewardRule, enabled = savingKey == null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = RulesOrange)) {
                                Text(if (savingKey == "reward") "Saving reward…" else "Save learning reward", fontWeight = FontWeight.Bold)
                            }
                        }
                        item { SectionHeading("Adaptive learning", "Set what Eduk should prioritize when it prepares validated questions.") }
                        item {
                            OutlinedTextField(
                                value = subjectsText,
                                onValueChange = { subjectsText = it },
                                label = { Text("Subjects, separated by commas") },
                                placeholder = { Text("Math, Science, Reading") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = inputColors
                            )
                        }
                        item {
                            Text("Question difficulty", color = RulesInk, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                listOf("foundation" to "Build up", "adaptive" to "Adaptive", "stretch" to "Stretch").forEach { (value, label) ->
                                    FilterChip(
                                        selected = selectedDifficulty == value,
                                        onClick = { selectedDifficulty = value },
                                        label = { Text(label, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RulesNavy, selectedLabelColor = Color.White)
                                    )
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
                                minLines = 2,
                                colors = inputColors
                            )
                        }
                        item {
                            Button(onClick = ::saveLearningPreferences, enabled = savingKey == null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = RulesNavy)) {
                                Text(if (savingKey == "learning") "Saving preferences…" else "Save learning preferences", fontWeight = FontWeight.Bold)
                            }
                        }
                        item { SectionHeading("School routine", "Apply a clear weekday routine to entertainment apps.") }
                        item {
                            OutlinedButton(onClick = ::saveSchoolHours, enabled = savingKey == null, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Text(if (savingKey == "school") "Saving school hours…" else "Block entertainment · Mon–Fri · 8:00 AM–3:00 PM", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                }
            }
        }
    }

    selectedApp?.let { app ->
        val currentRule = policyState?.appRules?.firstOrNull { it.packageName == app.packageName }
        AppRuleEditor(
            app = app,
            currentRule = currentRule,
            saving = savingKey == "app:${app.packageName}",
            onDismiss = { selectedApp = null },
            onSave = { mode, limit -> saveAppRule(app, mode, limit) }
        )
    }
}

@Composable
private fun InstalledAppCard(app: CloudInstalledApp, rule: CloudAppRule?, onConfigure: () -> Unit) {
    Surface(onClick = onConfigure, color = Color.White, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = RulesNavy.copy(alpha = .08f), shape = RoundedCornerShape(14.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(app.displayName.take(1).uppercase(), color = RulesNavy, fontWeight = FontWeight.ExtraBold) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.displayName, color = RulesInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                Text(ruleSummary(rule), color = RulesMuted, style = MaterialTheme.typography.bodySmall)
            }
            RuleStatusBadge(rule)
        }
    }
}

@Composable
private fun ConfiguredRuleCard(rule: CloudAppRule, isSaving: Boolean, onRemove: () -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.displayName ?: rule.packageName, color = RulesInk, fontWeight = FontWeight.ExtraBold)
                Text(ruleSummary(rule), color = RulesMuted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRemove, enabled = !isSaving) { Text(if (isSaving) "Removing" else "Remove", color = Color(0xFF9B2C1C)) }
        }
    }
}

@Composable
private fun AppRuleEditor(
    app: CloudInstalledApp,
    currentRule: CloudAppRule?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Int?) -> Unit
) {
    var mode by remember(app.packageName) { mutableStateOf(currentRule?.accessMode ?: "learning_gate") }
    var limitText by remember(app.packageName) { mutableStateOf(currentRule?.dailyLimitMinutes?.toString() ?: "30") }
    var validation by remember { mutableStateOf<String?>(null) }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = RulesInk, unfocusedTextColor = RulesInk, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
        focusedBorderColor = RulesOrange, unfocusedBorderColor = Color(0xFF74869B), focusedLabelColor = RulesOrange, unfocusedLabelColor = RulesMuted, cursorColor = RulesOrange
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = { Text("Control ${app.displayName}", color = RulesNavy, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose exactly what happens when ${app.displayName} is opened on ${app.packageName}.", color = RulesMuted, style = MaterialTheme.typography.bodyMedium)
                listOf(
                    "allow" to "Allow anytime",
                    "learning_gate" to "Allow daily time, then ask a question",
                    "block" to "Block all day"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = mode == value,
                        onClick = { mode = value },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RulesNavy, selectedLabelColor = Color.White)
                    )
                }
                if (mode == "learning_gate") {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter(Char::isDigit) },
                        label = { Text("Minutes allowed each day before a question") },
                        supportingText = { Text("Use 0 to require a question immediately.") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = colors
                    )
                }
                validation?.let { Text(it, color = Color(0xFF9B2C1C), style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val limit = if (mode == "learning_gate") limitText.toIntOrNull() else null
                if (mode == "learning_gate" && (limit == null || limit !in 0..1440)) {
                    validation = "Choose a daily limit from 0 to 1,440 minutes."
                } else onSave(mode, limit)
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = RulesOrange)) {
                Text(if (saving) "Saving…" else "Save rule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = RulesNavy) } }
    )
}

private fun ruleSummary(rule: CloudAppRule?): String = when (rule?.accessMode) {
    "allow" -> "Allowed anytime"
    "block" -> "Blocked all day"
    "learning_gate" -> rule.dailyLimitMinutes?.let { "$it min/day, then a learning question" } ?: "Learning question required before use"
    else -> "No custom rule yet"
}

@Composable
private fun RuleStatusBadge(rule: CloudAppRule?) {
    val (label, color) = when (rule?.accessMode) {
        "allow" -> "Allowed" to Color(0xFF147A50)
        "block" -> "Blocked" to Color(0xFF9B2C1C)
        "learning_gate" -> "Rule set" to Color(0xFF9A4F17)
        else -> "Set rule" to RulesNavy
    }
    Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(99.dp)) {
        Text(label, color = color, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun SectionHeading(title: String, description: String) {
    Column {
        Text(title, color = RulesNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        Text(description, color = RulesMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatusCard(message: String) {
    Surface(color = Color(0xFFFFE8E1), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Assertive }) {
        Text(message, color = Color(0xFF8F3C2C), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
    }
}
