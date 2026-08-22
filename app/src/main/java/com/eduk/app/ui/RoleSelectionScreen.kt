package com.eduk.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.R

private val RoleNavy = Color(0xFF0B1F3A)
private val RoleOrange = Color(0xFFFF7A1A)

@Composable
fun RoleSelectionScreen(onRoleSelected: (String) -> Unit) {
    var motionPulse by remember { mutableIntStateOf(0) }
    Box(modifier = Modifier.fillMaxSize()) {
        FlowingControlBackdrop(pulse = motionPulse)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(Modifier.height(34.dp))
        Surface(color = RoleNavy, shape = RoundedCornerShape(30.dp), modifier = Modifier.size(112.dp)) {
            Image(painter = painterResource(id = R.drawable.ic_eduk_shield), contentDescription = "Eduk shield", modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Eduk", color = RoleNavy, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
        Text("Learn. Earn. Unlock.", color = RoleOrange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text("Choose how you are using this device.", color = Color(0xFF62738A), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        RoleCard(
            title = "I’m a parent",
            description = "Create child accounts, pair phones, set time, and see learning progress.",
            icon = Icons.Default.SupervisorAccount,
            accentColor = RoleOrange,
            isPrimary = true,
            onClick = { motionPulse += 1; onRoleSelected("parent") }
        )
        Spacer(Modifier.height(16.dp))
        RoleCard(
            title = "I’m a student",
            description = "Sign in with the username and PIN your parent created, then pair this phone.",
            icon = Icons.Default.ChildCare,
            accentColor = RoleNavy,
            isPrimary = false,
            onClick = { motionPulse += 1; onRoleSelected("child") }
        )
        Spacer(Modifier.weight(1f))
        Text("Eduk Family Cloud keeps your family controls synchronized across paired devices.", color = Color(0xFF62738A), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    isPrimary: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPrimary) RoleNavy else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPrimary) 5.dp else 2.dp)
    ) {
        Row(Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = if (isPrimary) Color.White.copy(alpha = 0.14f) else accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(17.dp), modifier = Modifier.size(62.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (isPrimary) RoleOrange else accentColor, modifier = Modifier.size(31.dp)) }
            }
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (isPrimary) Color.White else RoleNavy, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text(description, color = if (isPrimary) Color.White.copy(alpha = 0.72f) else Color(0xFF62738A), style = MaterialTheme.typography.bodyMedium)
            }
            Text("›", color = if (isPrimary) RoleOrange else RoleNavy, fontSize = 32.sp, fontWeight = FontWeight.Light)
        }
    }
}
