package com.eduk.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.StudentStateResponse
import com.eduk.app.service.AppMonitoringService
import com.eduk.app.ui.theme.EdukTheme
import kotlinx.coroutines.launch

private val HomeNavy = Color(0xFF0B1F3A)
private val HomeOrange = Color(0xFFFF7A1A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val triggerQuestion = intent.getBooleanExtra("TRIGGER_QUESTION", false)
        setContent {
            EdukTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EdukApp(startDestination = if (triggerQuestion) "question" else "localization")
                }
            }
        }
    }
}

@Composable
fun EdukApp(startDestination: String) {
    val navController = rememberNavController()
    var selectedCountry by remember { mutableStateOf("United States") }
    var selectedLanguage by remember { mutableStateOf("English") }
    NavHost(navController = navController, startDestination = startDestination) {
        composable("localization") {
            LocalizationSelectionScreen(onComplete = { country, language ->
                selectedCountry = country
                selectedLanguage = language
                navController.navigate("role_selection")
            })
        }
        composable("role_selection") {
            RoleSelectionScreen(onRoleSelected = { role ->
                navController.navigate(if (role == "parent") "auth" else "student_access")
            })
        }
        composable("auth") {
            ProfessionalAuthScreen(
                country = selectedCountry,
                language = selectedLanguage,
                onAuthSuccess = { navController.navigate("subscription") }
            )
        }
        composable("subscription") {
            SubscriptionPaywallScreen(onSubscribed = { navController.navigate("add_child") })
        }
        composable("add_child") {
            AddChildScreen(onChildCreated = {
                navController.navigate("parent_dashboard") { popUpTo("add_child") { inclusive = true } }
            })
        }
        composable("parent_dashboard") {
            ParentDashboardScreen(onAddChild = { navController.navigate("add_child") })
        }
        composable("student_access") {
            StudentDeviceScreen(onReady = {
                navController.navigate("child_home") { popUpTo("student_access") { inclusive = true } }
            })
        }
        composable("child_home") {
            ChildHomeScreen(onOpenScanner = { navController.navigate("scanner") })
        }
        composable("question") {
            QuestionScreen(onCorrect = {
                navController.navigate("child_home") { popUpTo("question") { inclusive = true } }
            })
        }
        composable("scanner") {
            BookScannerScreen(onScanComplete = { navController.popBackStack() })
        }
    }
}

@Composable
fun ChildHomeScreen(onOpenScanner: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<StudentStateResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        val token = sessionStore.studentToken()
        if (token == null) {
            errorMessage = "This phone is not paired to a student account yet."
            return
        }
        scope.launch {
            runCatching { EdukCloudRepository.getStudentState(token) }
                .onSuccess { response ->
                    state = response
                    AppMonitoringService.setBlockingEnabled(response.child.isBlockingEnabled)
                }
                .onFailure { errorMessage = "We could not sync your Eduk status. Check your connection and try again." }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(containerColor = Color(0xFFF6F7FB)) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(28.dp))
            Text("Eduk", color = HomeNavy, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Text("Student Mode", color = HomeOrange, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(28.dp))
            state?.let { current ->
                Surface(color = HomeNavy, shape = RoundedCornerShape(30.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(26.dp)) {
                        Text("Hi, ${current.child.displayName}", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(18.dp), modifier = Modifier.size(58.dp)) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Timer, null, tint = HomeOrange, modifier = Modifier.size(30.dp)) }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Screen time ready", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelLarge)
                                Text("${current.child.timeAvailableMinutes} min", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Surface(color = if (current.child.isBlockingEnabled) Color(0xFF143B51) else Color(0xFF5A3840), shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (current.child.isBlockingEnabled) Icons.Default.Security else Icons.Default.Lock, null, tint = HomeOrange, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(9.dp))
                                Text(if (current.child.isBlockingEnabled) "Protected apps are active" else "App protection is paused", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Surface(color = Color.White, shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Earn more time", color = HomeNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Scan the book you are studying. Eduk will create questions, and each correct answer earns 10 minutes.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = onOpenScanner, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = HomeOrange), shape = RoundedCornerShape(17.dp)) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(10.dp))
                            Text("Scan book & earn time", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            } ?: run {
                Spacer(Modifier.weight(1f))
                if (errorMessage == null) CircularProgressIndicator(color = HomeOrange)
                else {
                    Icon(Icons.Default.Lock, null, tint = HomeOrange, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(errorMessage!!, color = HomeNavy, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(onClick = ::refresh) { Text("Try again") }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
