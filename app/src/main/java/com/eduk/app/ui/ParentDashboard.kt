package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.cloud.CloudChild
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.LearningHistoryEvent
import com.eduk.app.cloud.PairingCodeResponse
import kotlinx.coroutines.launch

private val DashboardNavy = Color(0xFF0B1F3A)
private val DashboardOrange = Color(0xFFFF7A1A)
private val DashboardCanvas = Color(0xFFF5F7FB)
private val DashboardInk = Color(0xFF182C45)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(onAddChild: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var children by remember { mutableStateOf<List<CloudChild>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedChild by remember { mutableStateOf<CloudChild?>(null) }
    var pairingCode by remember { mutableStateOf<PairingCodeResponse?>(null) }
    var history by remember { mutableStateOf<List<LearningHistoryEvent>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    var controlsChild by remember { mutableStateOf<CloudChild?>(null) }
    var reviewChild by remember { mutableStateOf<CloudChild?>(null) }
    var analyticsChild by remember { mutableStateOf<CloudChild?>(null) }
    var locationChild by remember { mutableStateOf<CloudChild?>(null) }

    fun refreshDashboard() {
        val token = sessionStore.parentToken()
        if (token == null) {
            isLoading = false
            errorMessage = "Your parent session has expired. Sign in again to continue."
            return
        }
        isLoading = true
        scope.launch {
            runCatching {
                val activeToken = if (sessionStore.shouldRefreshParentSession()) {
                    runCatching { EdukCloudRepository.refreshParentSession(token) }
                        .onSuccess { refreshed -> sessionStore.replaceParentToken(refreshed.token, refreshed.expiresAt) }
                        .getOrNull()?.token ?: token
                } else token
                EdukCloudRepository.getDashboard(activeToken)
            }
                .onSuccess { response -> children = response.children; errorMessage = null }
                .onFailure { errorMessage = "We could not refresh your family controls. Check your connection and try again." }
            isLoading = false
        }
    }

    fun adjustTime(child: CloudChild, delta: Int) {
        val token = sessionStore.parentToken() ?: return
        scope.launch {
            runCatching { EdukCloudRepository.updateTime(token, child.id, delta) }
                .onSuccess { refreshDashboard() }
                .onFailure { errorMessage = "The screen-time change could not be saved." }
        }
    }

    fun toggleBlocking(child: CloudChild, enabled: Boolean) {
        val token = sessionStore.parentToken() ?: return
        scope.launch {
            runCatching { EdukCloudRepository.updateBlocking(token, child.id, enabled) }
                .onSuccess { refreshDashboard() }
                .onFailure { errorMessage = "The protection setting could not be saved." }
        }
    }

    fun generatePairing(child: CloudChild) {
        val token = sessionStore.parentToken() ?: return
        scope.launch {
            runCatching { EdukCloudRepository.createPairingCode(token, child.id) }
                .onSuccess { pairingCode = it }
                .onFailure { errorMessage = "A new pairing code could not be created." }
        }
    }

    fun loadHistory(child: CloudChild) {
        val token = sessionStore.parentToken() ?: return
        scope.launch {
            runCatching { EdukCloudRepository.getLearningHistory(token, child.id) }
                .onSuccess { history = it.events; showHistory = true }
                .onFailure { errorMessage = "Learning history could not be loaded." }
        }
    }

    LaunchedEffect(Unit) { refreshDashboard() }

    Scaffold(
        containerColor = DashboardCanvas,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Eduk", color = DashboardNavy, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                        Text("Family Cloud", color = DashboardOrange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onAddChild, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DashboardNavy, contentColor = Color.White)) {
                        Icon(Icons.Default.Add, contentDescription = "Create child account")
                    }
                    Spacer(Modifier.width(12.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DashboardCanvas)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Surface(color = DashboardNavy, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Your family, clearly in control.", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(8.dp))
                        Text("Create a child account, pair their phone, and manage learning-based screen time from one secure place.", color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            errorMessage?.let { message ->
                item {
                    Surface(color = Color(0xFFFFE8E1), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(message, color = Color(0xFF9B2C1C), modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Children", color = DashboardInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Live family controls", color = Color(0xFF62738A), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onAddChild) { Text("Add child", color = DashboardOrange, fontWeight = FontWeight.Bold) }
                }
            }

            when {
                isLoading -> item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DashboardOrange)
                        Spacer(Modifier.height(12.dp))
                        Text("Loading your family controls…", color = Color(0xFF62738A))
                    }
                }
                children.isEmpty() -> item {
                    Surface(color = Color.White, shape = RoundedCornerShape(26.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.School, null, tint = DashboardOrange, modifier = Modifier.size(44.dp))
                            Spacer(Modifier.height(14.dp))
                            Text("Create the first child account", color = DashboardInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(6.dp))
                            Text("Their username, PIN, and one-time device code are created here by the parent.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = onAddChild, colors = ButtonDefaults.buttonColors(containerColor = DashboardOrange), shape = RoundedCornerShape(16.dp)) { Text("Create child account", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                else -> items(children, key = { it.id }) { child ->
                    ChildControlCard(child = child, onOpen = { selectedChild = child })
                }
            }
        }
    }

    selectedChild?.let { child ->
        ModalBottomSheet(
            onDismissRequest = { selectedChild = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
        ) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 36.dp)) {
                Text(child.displayName, color = DashboardNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("@${child.username} · Grade ${child.gradeLevel}", color = Color(0xFF62738A), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(24.dp))
                Text("Screen time", color = DashboardInk, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { adjustTime(child, -10) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp)) { Text("− 10 min") }
                    Button(onClick = { adjustTime(child, 10) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DashboardOrange), shape = RoundedCornerShape(16.dp)) { Text("+ 10 min") }
                }
                Spacer(Modifier.height(22.dp))
                Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = DashboardNavy)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("App blocking", color = DashboardInk, fontWeight = FontWeight.Bold)
                            Text(if (child.isBlockingEnabled) "Protected apps lock when time runs out." else "App blocking is currently paused.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = child.isBlockingEnabled, onCheckedChange = { toggleBlocking(child, it) })
                    }
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { selectedChild = null; reviewChild = child },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.School, null, tint = DashboardOrange)
                    Spacer(Modifier.width(10.dp))
                    Text("Review AI-generated questions", color = DashboardNavy, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { selectedChild = null; analyticsChild = child },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Timer, null, tint = DashboardOrange)
                    Spacer(Modifier.width(10.dp))
                    Text("View learning progress", color = DashboardNavy, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { selectedChild = null; locationChild = child },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Security, null, tint = DashboardOrange)
                    Spacer(Modifier.width(10.dp))
                    Text("Location sharing & privacy", color = DashboardNavy, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { selectedChild = null; controlsChild = child },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = DashboardNavy),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Lock, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Manage app & learning rules")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { generatePairing(child) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Link, null)
                    Spacer(Modifier.width(10.dp))
                    Text("Generate new pairing code")
                }
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { loadHistory(child) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.History, null, tint = DashboardOrange)
                    Spacer(Modifier.width(10.dp))
                    Text("View answered-question history", color = DashboardOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    controlsChild?.let { child ->
        ChildPolicyManagerSheet(
            child = child,
            parentToken = sessionStore.parentToken(),
            onDismiss = { controlsChild = null },
            onPolicyChanged = { refreshDashboard() }
        )
    }

    reviewChild?.let { child ->
        ParentQuestionReviewSheet(
            child = child,
            parentToken = sessionStore.parentToken(),
            onDismiss = { reviewChild = null }
        )
    }

    analyticsChild?.let { child ->
        ParentLearningProgressSheet(
            child = child,
            parentToken = sessionStore.parentToken(),
            onDismiss = { analyticsChild = null }
        )
    }

    locationChild?.let { child ->
        ChildLocationSettingsSheet(
            child = child,
            parentToken = sessionStore.parentToken(),
            onDismiss = { locationChild = null }
        )
    }

    pairingCode?.let { pairing ->
        AlertDialog(
            onDismissRequest = { pairingCode = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Pair a child’s phone", color = DashboardNavy, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("On the child’s device, choose Student Mode, enter their username and PIN, then enter this code.")
                    Spacer(Modifier.height(20.dp))
                    Text(pairing.code, color = DashboardOrange, fontSize = 36.sp, letterSpacing = 6.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(10.dp))
                    Text("Valid for 10 minutes. This code can be used only once.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { Button(onClick = { pairingCode = null }, colors = ButtonDefaults.buttonColors(containerColor = DashboardNavy)) { Text("Done") } }
        )
    }

    if (showHistory) {
        AlertDialog(
            onDismissRequest = { showHistory = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Learning history", color = DashboardNavy, fontWeight = FontWeight.ExtraBold) },
            text = {
                if (history.isEmpty()) Text("No answered questions have synchronized for this child yet.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 360.dp)) {
                    items(history, key = { it.id }) { event ->
                        Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(event.subject, color = DashboardOrange, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(event.questionText, color = DashboardInk, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                                Text(if (event.wasCorrect) "Correct · +${event.minutesAwarded} min" else "Not correct", color = if (event.wasCorrect) Color(0xFF147A50) else Color(0xFF8F3C2C), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHistory = false }) { Text("Close", color = DashboardOrange) } }
        )
    }
}

@Composable
private fun ChildControlCard(child: CloudChild, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 3.dp
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = DashboardNavy, shape = RoundedCornerShape(16.dp), modifier = Modifier.size(52.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text(child.displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(child.displayName, color = DashboardInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("@${child.username} · Grade ${child.gradeLevel}", color = Color(0xFF62738A), style = MaterialTheme.typography.bodySmall)
                }
                Surface(color = if (child.isDeviceLinked) Color(0xFFE3F6EB) else Color(0xFFFFE9D8), shape = RoundedCornerShape(99.dp)) {
                    Text(if (child.isDeviceLinked) "Linked" else "Pair device", color = if (child.isDeviceLinked) Color(0xFF147A50) else Color(0xFF9A4F17), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricBlock("Time ready", "${child.timeAvailableMinutes} min", Modifier.weight(1f))
                MetricBlock("Accuracy", if (child.questionsAnswered == 0) "—" else "${child.accuracyPercent}%", Modifier.weight(1f))
                MetricBlock("Protection", if (child.isBlockingEnabled) "On" else "Off", Modifier.weight(1f))
            }
            if (child.linkedDeviceLabel != null) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PhoneAndroid, null, tint = DashboardOrange, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(child.linkedDeviceLabel, color = Color(0xFF62738A), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Text(label, color = Color(0xFF62738A), style = MaterialTheme.typography.labelSmall)
            Text(value, color = DashboardNavy, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
