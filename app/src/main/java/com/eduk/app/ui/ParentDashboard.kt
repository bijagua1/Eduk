package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eduk.app.service.AppMonitoringService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen() {
    var timeEarned by remember { mutableIntStateOf(120) }
    var showTimeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Eduk Parent Dashboard", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Student: Alex", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Grade: 7th Grade", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Device Status: Protected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        title = "Accuracy", 
                        value = "85%", 
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Time Earned", 
                        value = "$timeEarned min", 
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showTimeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adjust Student Time")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("Parental Controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                ControlItem(
                    title = "Manage Restricted Apps",
                    subtitle = "YouTube, TikTok, Instagram blocked",
                    icon = Icons.Default.Block
                )
                ControlItem(
                    title = "AI Book Scan",
                    subtitle = "Scan new study material to generate questions",
                    icon = Icons.Default.CameraAlt
                )
                ControlItem(
                    title = "Weekly Progress Report",
                    subtitle = "Next report: Sunday, Aug 23",
                    icon = Icons.Default.Assessment
                )
            }
        }
    }

    if (showTimeDialog) {
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text("Adjust Time") },
            text = { Text("Add or remove screen time for Alex.") },
            confirmButton = {
                TextButton(onClick = { 
                    timeEarned += 10
                    AppMonitoringService.grantAccess(10)
                    showTimeDialog = false 
                }) {
                    Text("Add 10 Min")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    if (timeEarned >= 10) timeEarned -= 10
                    showTimeDialog = false 
                }) {
                    Text("Remove 10 Min")
                }
            }
        )
    }
}

@Composable
fun ControlItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle) },
        leadingContent = { 
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
        },
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
