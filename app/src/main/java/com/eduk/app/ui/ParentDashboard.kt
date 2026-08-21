package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.data.EdukDatabase
import com.eduk.app.model.StudentProfile
import com.eduk.app.service.AppMonitoringService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(onAddChild: () -> Unit) {
    val context = LocalContext.current
    val db = remember { EdukDatabase.getDatabase(context) }
    val profilesState = db.profileDao().getAllProfiles().collectAsState(initial = emptyList())
    val profiles = profilesState.value
    
    var timeEarned by remember { mutableIntStateOf(120) }
    var showTimeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eduk Family Hub", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = onAddChild) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Child", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Children Profiles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (profiles.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No child profile added yet", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Add your child to start monitoring and managing screen time.", color = MaterialTheme.colorScheme.secondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(onClick = onAddChild, shape = RoundedCornerShape(12.dp)) {
                                Text("Add Child Now")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            } else {
                items(profiles) { profile ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Face, contentDescription = null, tint = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(profile.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Grade ${profile.grade} • Age ${profile.age} • Active", color = Color.White.copy(alpha = 0.8f))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Accuracy", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                                    Text("${profile.accuracy}%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Time Earned", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                                    Text("${profile.timeEarnedMinutes} min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Column {
                                    Text("Status", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                                    Text("Protected", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                Button(
                    onClick = { showTimeDialog = true },
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Grant Bonus Screen Time", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                Text("Smart Controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                ControlCard(
                    title = "App Restrictions",
                    subtitle = "Social media & Games locked",
                    icon = Icons.Default.Block,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                ControlCard(
                    title = "AI Book Scanner",
                    subtitle = "Generate questions from textbooks",
                    icon = Icons.Default.CameraAlt,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                ControlCard(
                    title = "Analytics Report",
                    subtitle = "View detailed learning progress",
                    icon = Icons.Default.BarChart,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showTimeDialog) {
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Adjust Screen Time", fontWeight = FontWeight.Bold) },
            text = { Text("Would you like to grant extra screen time or reduce it manually?") },
            confirmButton = {
                Button(onClick = { 
                    timeEarned += 10
                    AppMonitoringService.grantAccess(10)
                    showTimeDialog = false 
                }) {
                    Text("+10 Min")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { 
                    if (timeEarned >= 10) timeEarned -= 10
                    showTimeDialog = false 
                }) {
                    Text("-10 Min")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}
