package com.eduk.app.ui

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.eduk.app.cloud.EdukCloudException
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.InstalledAppInventoryReporter
import com.eduk.app.cloud.LearningProgressResponse
import com.eduk.app.cloud.StudentLocationReportRequest
import com.eduk.app.cloud.StudentStateResponse
import com.eduk.app.service.AppMonitoringService
import com.eduk.app.service.ChildPolicyStore
import com.eduk.app.service.ConsentedLocationService
import com.eduk.app.service.EdukDeviceAdminReceiver
import com.eduk.app.service.GateOverlayService
import com.eduk.app.ui.theme.EdukTheme
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch

private val HomeNavy = Color(0xFF0B1F3A)
private val HomeOrange = Color(0xFFFF7A1A)

class MainActivity : ComponentActivity() {
    private var questionRequestNonce = 0
    private var restrictedAppPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("TRIGGER_QUESTION", false)) {
            questionRequestNonce += 1
            restrictedAppPackage = intent.getStringExtra("RESTRICTED_APP")
        }
        val hasStoredStudentSession = EdukSessionStore(applicationContext).studentToken() != null
        setContent {
            EdukTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    EdukApp(
                        startDestination = studentStartDestination(
                            hasPendingGate = questionRequestNonce > 0,
                            hasStoredStudentSession = hasStoredStudentSession
                        ),
                        questionRequestNonce = questionRequestNonce,
                        restrictedAppPackage = restrictedAppPackage
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        GateOverlayService.hide(this)
    }

    override fun onStop() {
        super.onStop()
        val hasPendingGate = intent.getBooleanExtra("TRIGGER_QUESTION", false)
        if (hasPendingGate && !ChildPolicyStore(this).isAccessCurrentlyEarned()) {
            GateOverlayService.show(this, restrictedAppPackage)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("TRIGGER_QUESTION", false)) {
            questionRequestNonce += 1
            restrictedAppPackage = intent.getStringExtra("RESTRICTED_APP")
            setContent {
                EdukTheme {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        EdukApp(
                            startDestination = "localization",
                            questionRequestNonce = questionRequestNonce,
                            restrictedAppPackage = restrictedAppPackage
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EdukApp(startDestination: String, questionRequestNonce: Int = 0, restrictedAppPackage: String? = null) {
    val navController = rememberNavController()
    val motionEnabled = rememberEdukMotionEnabled()
    var selectedCountry by remember { mutableStateOf("United States") }
    var selectedLanguage by remember { mutableStateOf("English") }
    LaunchedEffect(questionRequestNonce) {
        if (questionRequestNonce > 0) {
            navController.navigate("question") { launchSingleTop = true }
        }
    }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (motionEnabled) slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { it / 14 } + fadeIn(tween(180)) else EnterTransition.None
        },
        exitTransition = {
            if (motionEnabled) slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -it / 20 } + fadeOut(tween(140)) else ExitTransition.None
        },
        popEnterTransition = {
            if (motionEnabled) slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 18 } + fadeIn(tween(160)) else EnterTransition.None
        },
        popExitTransition = {
            if (motionEnabled) slideOutHorizontally(tween(160, easing = FastOutSlowInEasing)) { it / 24 } + fadeOut(tween(120)) else ExitTransition.None
        }
    ) {
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
            SubscriptionPaywallScreen(onContinue = { navController.navigate("add_child") })
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
            ChildHomeScreen(
                onOpenScanner = { navController.navigate("scanner") },
                onSessionInvalidated = {
                    navController.navigate("student_access") {
                        popUpTo("child_home") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("question") {
            QuestionScreen(restrictedAppPackage = restrictedAppPackage, onCorrect = {
                navController.navigate("child_home") { popUpTo("question") { inclusive = true } }
            })
        }
        composable("scanner") {
            BookScannerScreen(onScanComplete = { navController.popBackStack() })
        }
    }
}

@Composable
fun ChildHomeScreen(onOpenScanner: () -> Unit, onSessionInvalidated: () -> Unit) {
    val context = LocalContext.current
    val sessionStore = remember { EdukSessionStore(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<StudentStateResponse?>(null) }
    var learningProgress by remember { mutableStateOf<LearningProgressResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLocationSharingEnabled by remember { mutableStateOf(false) }
    var isContinuousLocationSharingActive by remember { mutableStateOf(sessionStore.isLocationSharingActive()) }
    var locationStatus by remember { mutableStateOf<String?>(null) }
    var showLocationConsentDialog by remember { mutableStateOf(false) }
    var motionPulse by remember { mutableStateOf(0) }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    fun shareCurrentLocation() {
        val token = sessionStore.studentToken()
        if (token == null) {
            locationStatus = "This device is not paired to a student account."
            return
        }
        locationStatus = "Getting your current location…"
        locationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    locationStatus = "Location is unavailable right now. Turn on device location and try again."
                    return@addOnSuccessListener
                }
                scope.launch {
                    runCatching {
                        EdukCloudRepository.reportStudentLocation(
                            token,
                            StudentLocationReportRequest(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracyMeters = location.accuracy.coerceAtLeast(0f).toInt(),
                                batteryPercent = null
                            )
                        )
                    }.onSuccess { locationStatus = "Location shared securely with your family." }
                        .onFailure { locationStatus = "Your location could not be shared. Check your connection and try again." }
                }
            }
            .addOnFailureListener { locationStatus = "Location is unavailable right now. Please try again." }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showLocationConsentDialog = true
        else locationStatus = "Notification permission is required before periodic location sharing can start, so its status always stays visible."
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else showLocationConsentDialog = true
        }
        else locationStatus = "Location permission was not granted. You can enable it later in Android settings."
    }

    fun refresh() {
        val token = sessionStore.studentToken()
        if (token == null) {
            errorMessage = "This phone is not paired to a student account yet."
            return
        }
        scope.launch {
            runCatching {
                val activeToken = if (sessionStore.shouldRefreshStudentSession()) {
                    runCatching { EdukCloudRepository.refreshStudentSession(token) }
                        .onSuccess { refreshed -> sessionStore.replaceStudentToken(refreshed.token, refreshed.expiresAt) }
                        .getOrNull()?.token ?: token
                } else token
                val status = EdukCloudRepository.getStudentState(activeToken)
                val policy = EdukCloudRepository.getStudentPolicy(activeToken)
                val progress = EdukCloudRepository.getStudentLearningProgress(activeToken)
                val locationSettings = EdukCloudRepository.getStudentLocationSettings(activeToken)
                Triple(status, policy, progress to locationSettings)
            }.onSuccess { (response, policy, progressAndLocation) ->
                    val (progress, locationSettings) = progressAndLocation
                    state = response
                    learningProgress = progress
                    isLocationSharingEnabled = locationSettings.isSharingEnabled
                    if (!locationSettings.isSharingEnabled && sessionStore.isLocationSharingActive()) {
                        sessionStore.setLocationSharingActive(false)
                        ConsentedLocationService.stop(context)
                    }
                    isContinuousLocationSharingActive = locationSettings.isSharingEnabled && sessionStore.isLocationSharingActive()
                    AppMonitoringService.applyRemotePolicy(context, policy)
                    scope.launch {
                        val inventoryToken = sessionStore.studentToken() ?: return@launch
                        runCatching {
                            EdukCloudRepository.reportInstalledApps(
                                inventoryToken,
                                InstalledAppInventoryReporter.collectVisibleLaunchableApps(context)
                            )
                        }
                    }
                }
                .onFailure { error ->
                    if (error is EdukCloudException && error.statusCode in setOf(401, 403)) {
                        sessionStore.clearStudentSession()
                        ConsentedLocationService.stop(context)
                        onSessionInvalidated()
                    } else {
                        errorMessage = "We could not sync your Eduk status. Check your connection and try again."
                    }
                }
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(containerColor = Color(0xFFF6F7FB)) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            FlowingControlBackdrop(pulse = motionPulse)
            Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(22.dp))
            state?.let { current ->
                EdukGlowHero(
                    overline = "Eduk student mode",
                    title = "Hi, ${current.child.displayName}.",
                    description = "Learn, earn your time, and keep your progress moving forward.",
                    icon = Icons.Default.School
                )
                Spacer(Modifier.height(14.dp))
                Surface(color = HomeOrange, shape = RoundedCornerShape(24.dp), shadowElevation = 7.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color.White.copy(alpha = 0.20f), shape = RoundedCornerShape(17.dp), modifier = Modifier.size(54.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Timer, null, tint = Color.White, modifier = Modifier.size(29.dp)) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("TIME READY", color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                            Text("${current.child.timeAvailableMinutes} min", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("EARN\nMORE", color = Color.White.copy(alpha = 0.90f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(color = if (current.child.isBlockingEnabled) HomeNavy else Color(0xFF5A3840), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (current.child.isBlockingEnabled) Icons.Default.Security else Icons.Default.Lock, null, tint = HomeOrange, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (current.child.isBlockingEnabled) "Protection is active" else "Protection is paused", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyMedium)
                            Text(if (current.child.isBlockingEnabled) "Your parent’s app rules are ready on this phone." else "Ask your parent before changing this setting.", color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                learningProgress?.let { progress ->
                    StudentProgressCard(progress)
                    Spacer(Modifier.height(20.dp))
                }
                if (!AppMonitoringService.isServiceRunning() || !GateOverlayService.canDrawOverlays(context)) {
                    Surface(color = Color(0xFFFFF6EE), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Finish protection setup", color = HomeNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (!AppMonitoringService.isServiceRunning()) "To apply your parent’s app rules, enable Eduk in Android Accessibility. Eduk only checks which app is open; it does not read messages or page content."
                                else "Allow Eduk to display above other apps so a pending learning question cannot be bypassed with Home or Recents.",
                                color = Color(0xFF624833), style = MaterialTheme.typography.bodySmall
                            )
                            if (!AppMonitoringService.isServiceRunning()) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                                ) { Text("Open Accessibility settings", fontWeight = FontWeight.Bold) }
                            }
                            if (!AppMonitoringService.isServiceRunning() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "If Eduk is greyed out or says “Restricted settings”, open Eduk app info, tap the three-dot menu, choose “Allow restricted settings”, then return here.",
                                    color = Color(0xFF624833),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.fromParts("package", context.packageName, null))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                ) { Text("Open Eduk app info", fontWeight = FontWeight.Bold) }
                            }
                            if (!GateOverlayService.canDrawOverlays(context)) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                                ) { Text("Allow display over other apps", fontWeight = FontWeight.Bold) }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    val component = ComponentName(context, EdukDeviceAdminReceiver::class.java)
                                    context.startActivity(
                                        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                                            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
                                            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Allows Eduk to apply the device protection your parent configured.")
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)
                            ) { Text("Enable device protection", fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                if (isLocationSharingEnabled) {
                    Surface(color = Color(0xFFEAF3FF), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Family location sharing", color = HomeNavy, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (isContinuousLocationSharingActive) "Sharing is active. A visible Android notification remains on while Eduk sends periodic updates to your family."
                                else "Your parent enabled location sharing for this paired device. You decide when to start sharing, and can stop at any time.",
                                color = Color(0xFF4F6078), style = MaterialTheme.typography.bodySmall
                            )
                            locationStatus?.let {
                                Spacer(Modifier.height(10.dp))
                                Text(it, color = HomeNavy, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = {
                                    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (isContinuousLocationSharingActive) {
                                        sessionStore.setLocationSharingActive(false)
                                        isContinuousLocationSharingActive = false
                                        ConsentedLocationService.stop(context)
                                        locationStatus = "Location sharing is off on this device."
                                    } else if (hasLocationPermission) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else showLocationConsentDialog = true
                                    }
                                    else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Security, null, tint = HomeNavy)
                                Spacer(Modifier.width(9.dp))
                                Text(if (isContinuousLocationSharingActive) "Stop location sharing" else "Start location sharing", color = HomeNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
                Surface(color = Color.White, shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Earn more time", color = HomeNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
                        Spacer(Modifier.height(6.dp))
                        Text("Scan the book you are studying. After a parent approves the questions, verified answers earn the time your parent configured.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { motionPulse += 1; onOpenScanner() }, modifier = Modifier.fillMaxWidth().height(58.dp), colors = ButtonDefaults.buttonColors(containerColor = HomeOrange), shape = RoundedCornerShape(17.dp)) {
                            Icon(Icons.Default.CameraAlt, null)
                            Spacer(Modifier.width(10.dp))
                            Text("Scan book & earn time", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            } ?: run {
                Spacer(Modifier.height(88.dp))
                if (errorMessage == null) CircularProgressIndicator(color = HomeOrange)
                else {
                    Icon(Icons.Default.Lock, null, tint = HomeOrange, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(errorMessage!!, color = HomeNavy, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive })
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(onClick = ::refresh) { Text("Try again") }
                }
                Spacer(Modifier.height(88.dp))
            }
            }
        }
    }
    if (showLocationConsentDialog) {
        AlertDialog(
            onDismissRequest = { showLocationConsentDialog = false },
            title = { Text("Share location with your family?") },
            text = { Text("Eduk will show a visible Android notification while it periodically shares this device’s location with your parent. You can stop sharing here at any time.") },
            confirmButton = {
                TextButton(onClick = {
                    sessionStore.setLocationSharingActive(true)
                    isContinuousLocationSharingActive = true
                    showLocationConsentDialog = false
                    ConsentedLocationService.start(context)
                    shareCurrentLocation()
                }) { Text("Start sharing") }
            },
            dismissButton = { TextButton(onClick = { showLocationConsentDialog = false }) { Text("Not now") } }
        )
    }
}

@Composable
private fun StudentProgressCard(progress: LearningProgressResponse) {
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Your learning progress", color = HomeNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, modifier = Modifier.semantics { heading() })
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HomeProgressMetric("XP", progress.xp.toString(), Modifier.weight(1f))
                HomeProgressMetric("Streak", "${progress.currentStreak} day${if (progress.currentStreak == 1) "" else "s"}", Modifier.weight(1f))
                HomeProgressMetric("Accuracy", if (progress.totalAttempts == 0) "—" else "${progress.accuracyPercent}%", Modifier.weight(1f))
            }
            if (progress.bySubject.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                val leadingSubject = progress.bySubject.first()
                Text("Most practiced subject", color = Color(0xFF62738A), style = MaterialTheme.typography.labelSmall)
                Text("${leadingSubject.subject} · ${leadingSubject.accuracyPercent}% accuracy", color = HomeNavy, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                progress.byTopic.firstOrNull()?.let { focusTopic ->
                    Spacer(Modifier.height(10.dp))
                    Text("Next focus", color = Color(0xFF62738A), style = MaterialTheme.typography.labelSmall)
                    Text("${focusTopic.topic} · let’s build confidence", color = HomeOrange, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(14.dp))
                Text("Complete an approved challenge to start your streak and subject progress.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HomeProgressMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(15.dp), modifier = modifier) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 10.dp)) {
            Text(label, color = Color(0xFF62738A), style = MaterialTheme.typography.labelSmall)
            Text(value, color = HomeNavy, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        }
    }
}
