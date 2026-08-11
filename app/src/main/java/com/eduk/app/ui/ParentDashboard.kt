package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Parent Dashboard") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text("Student: Alex", style = MaterialTheme.typography.headlineSmall)
                Text("Grade: 7th Grade", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                StatCard(title = "Accuracy", value = "85%", icon = Icons.Default.CheckCircle)
                StatCard(title = "Time Earned", value = "120 min", icon = Icons.Default.Timer)
                StatCard(title = "Top Subject", value = "Science", icon = Icons.Default.School)
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                ListItem(
                    headlineContent = { Text("Manage Restricted Apps") },
                    supportingContent = { Text("3 apps restricted") },
                    leadingContent = { Icon(Icons.Default.Block, contentDescription = null) }
                )
                ListItem(
                    headlineContent = { Text("AI Book Scan") },
                    supportingContent = { Text("Scan new study material") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                )
                ListItem(
                    headlineContent = { Text("Parental Reports") },
                    supportingContent = { Text("Sent to manager@example.com") },
                    leadingContent = { Icon(Icons.Default.Email, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.labelMedium)
                Text(text = value, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

@Composable fun OnboardingScreen(onComplete: () -> Unit) { /* Placeholder */ }
@Composable fun StatsScreen() { /* Placeholder */ }
