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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.eduk.app.cloud.LocationSettingsRequest
import kotlinx.coroutines.launch

private val LocationNavy = Color(0xFF0B1F3A)
private val LocationOrange = Color(0xFFFF7A1A)
private val LocationMuted = Color(0xFF62738A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildLocationSettingsSheet(child: CloudChild, parentToken: String?, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isSharingEnabled by remember { mutableStateOf(false) }
    var retentionDays by remember { mutableStateOf(7) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun load() {
        if (parentToken == null) {
            loading = false
            message = "Your parent session has expired. Sign in again to manage location sharing."
            return
        }
        loading = true
        scope.launch {
            runCatching { EdukCloudRepository.getLocationSettings(parentToken, child.id) }
                .onSuccess { settings -> isSharingEnabled = settings.isSharingEnabled; retentionDays = settings.retentionDays; message = null }
                .onFailure { message = "We could not load location privacy settings." }
            loading = false
        }
    }

    fun save() {
        if (parentToken == null) return
        saving = true
        scope.launch {
            runCatching { EdukCloudRepository.saveLocationSettings(parentToken, child.id, LocationSettingsRequest(isSharingEnabled, retentionDays)) }
                .onSuccess {
                    message = if (isSharingEnabled) "Location sharing is enabled. The student sees a clear sharing control before any location is sent." else "Location sharing is off and retained location reports were removed."
                }
                .onFailure { message = "Location privacy settings could not be saved." }
            saving = false
        }
    }

    LaunchedEffect(child.id) { load() }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp)
        ) {
            item {
                Text("${child.displayName}'s location privacy", color = LocationNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(5.dp))
                Text("Location is off by default. When enabled, Eduk asks for Android permission visibly and only shares a location when the student chooses the sharing control.", color = LocationMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (loading) item {
                Column(Modifier.fillMaxWidth().padding(vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = LocationOrange)
                    Spacer(Modifier.height(10.dp))
                    Text("Loading privacy settings…", color = LocationMuted)
                }
            }
            message?.let { status -> item {
                Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(status, color = LocationNavy, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(14.dp))
                }
            } }
            if (!loading) {
                item {
                    Surface(color = Color(0xFFF4F6FA), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Enable family location sharing", color = LocationNavy, fontWeight = FontWeight.ExtraBold)
                                Text("You can turn this off at any time. Turning it off deletes retained location reports and alerts.", color = LocationMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(checked = isSharingEnabled, onCheckedChange = { isSharingEnabled = it })
                        }
                    }
                }
                item {
                    Text("Retention period", color = LocationNavy, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(1, 7, 30).forEach { days ->
                            OutlinedButton(
                                onClick = { retentionDays = days },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (retentionDays == days) LocationNavy else Color.Transparent,
                                    contentColor = if (retentionDays == days) Color.White else LocationNavy
                                )
                            ) { Text("$days day${if (days == 1) "" else "s"}") }
                        }
                    }
                }
                item {
                    Button(onClick = ::save, enabled = !saving, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = LocationNavy), shape = RoundedCornerShape(16.dp)) {
                        Text(if (saving) "Saving privacy settings…" else "Save location privacy settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
