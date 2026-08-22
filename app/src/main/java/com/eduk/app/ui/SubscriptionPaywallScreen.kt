package com.eduk.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.cloud.EdukCloudRepository
import com.eduk.app.cloud.EdukSessionStore
import com.eduk.app.cloud.EntitlementResponse
import kotlinx.coroutines.launch

private val PaywallNavy = Color(0xFF0B1F3A)
private val PaywallInk = Color(0xFF263E5B)
private val PaywallMuted = Color(0xFF52677F)
private val PaywallOrange = Color(0xFFFF7A1A)

@Composable
fun SubscriptionPaywallScreen(onContinue: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionStore = remember(context) { EdukSessionStore(context) }
    val parentToken = sessionStore.parentToken()
    var entitlement by remember { mutableStateOf<EntitlementResponse?>(null) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadEntitlement() {
        if (parentToken == null) {
            loading = false
            errorMessage = "Your parent session has expired. Please sign in again."
            return
        }
        loading = true
        errorMessage = null
        scope.launch {
            runCatching { EdukCloudRepository.getEntitlements(parentToken) }
                .onSuccess { entitlement = it }
                .onFailure { errorMessage = "We could not verify your plan. Check your connection and try again." }
            loading = false
        }
    }

    LaunchedEffect(parentToken) { loadEntitlement() }

    Box(modifier = Modifier.fillMaxSize()) {
        FlowingControlBackdrop(pulse = if (loading) 0 else 2)
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.13f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color(0xFFFFA000))
                }
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                color = Color(0xFFFEFEFF).copy(alpha = 0.97f),
                shape = RoundedCornerShape(28.dp),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        loading -> {
                            Spacer(Modifier.height(18.dp))
                            CircularProgressIndicator(color = PaywallOrange)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Verifying your Eduk Family access…",
                                color = PaywallNavy,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            )
                            Spacer(Modifier.height(18.dp))
                        }
                        errorMessage != null -> {
                            Text(
                                "Plan verification unavailable",
                                color = PaywallNavy,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.semantics { heading() }
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                errorMessage!!,
                                textAlign = TextAlign.Center,
                                color = PaywallInk,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedButton(onClick = { loadEntitlement() }) {
                                Icon(Icons.Default.Refresh, null, tint = PaywallNavy)
                                Spacer(Modifier.width(8.dp))
                                Text("Try again", color = PaywallNavy)
                            }
                        }
                        entitlement != null && entitlement!!.tier != "free" -> ActiveEntitlement(entitlement!!, onContinue)
                        else -> FreeEntitlement(onContinue = onContinue)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveEntitlement(entitlement: EntitlementResponse, onContinue: () -> Unit) {
    val isTrial = entitlement.sourceStatus == "trialing"
    Text(
        text = if (isTrial) "Your Plus trial is active" else "Your ${entitlement.tier.replaceFirstChar { it.uppercase() }} plan is active",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = PaywallNavy,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = if (isTrial) "Your three-day trial is active until ${entitlement.trialEndsAt?.take(10) ?: "its scheduled end"}. No card data is collected by Eduk." else "Your subscription access is verified securely by Eduk Family Cloud.",
        textAlign = TextAlign.Center,
        color = PaywallInk
    )
    Spacer(Modifier.height(28.dp))
    EntitlementFeature("Up to ${entitlement.limits.maxChildren} child profiles")
    EntitlementFeature("AI learning, rewards and app controls")
    if (entitlement.limits.locationSharing) EntitlementFeature("Consented location and safe places")
    Spacer(Modifier.height(36.dp))
    Button(
        onClick = onContinue,
        colors = ButtonDefaults.buttonColors(containerColor = PaywallOrange, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Continue to Family Setup", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun FreeEntitlement(onContinue: () -> Unit) {
    Text(
        "Start with Eduk Free",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = PaywallNavy,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "No paid plan is active. You can continue with one child profile and Eduk’s core learning and parental-control tools.",
        textAlign = TextAlign.Center,
        color = PaywallInk
    )
    Spacer(Modifier.height(28.dp))
    EntitlementFeature("One child profile")
    EntitlementFeature("Core learning challenges and earned screen time")
    EntitlementFeature("Basic app blocking and daily time controls")
    Spacer(Modifier.height(20.dp))
    Text(
        "Paid upgrades and purchase restoration will be delivered through Google Play. Eduk does not collect card details in this app.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = PaywallMuted
    )
    Spacer(Modifier.height(36.dp))
    Button(
        onClick = onContinue,
        colors = ButtonDefaults.buttonColors(containerColor = PaywallOrange, contentColor = Color.White),
        modifier = Modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text("Continue with Free", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun EntitlementFeature(text: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E9C58))
        Spacer(Modifier.width(12.dp))
        Text(text, color = PaywallNavy, style = MaterialTheme.typography.bodyLarge)
    }
}
