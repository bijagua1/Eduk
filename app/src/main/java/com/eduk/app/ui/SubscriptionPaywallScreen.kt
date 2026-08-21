package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.config.DeveloperConfig

@Composable
fun SubscriptionPaywallScreen(onSubscribed: () -> Unit) {
    var showCreditCardInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!showCreditCardInput) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.1f),
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = Color(0xFFFFD700)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Premium Family Plan",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text(
                    text = "3-DAY FREE TRIAL",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }

            Text(
                text = "Then only $3.99 / month",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PremiumFeatureItem("AI-Powered Book Scanning", "Turn any textbook into a smart quiz.")
                PremiumFeatureItem("Real-Time App Blocking", "Control access to social media and games.")
                PremiumFeatureItem("Insightful Progress Reports", "Weekly data on your child's learning.")
                PremiumFeatureItem("Multi-Device Support", "One subscription for the whole family.")
            }

            Spacer(modifier = Modifier.weight(1f))

            if (DeveloperConfig.isDeveloperMode) {
                Button(
                    onClick = onSubscribed,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("DEV: Bypass & Continue", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { showCreditCardInput = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text("Start Free Trial", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Secure payment. Cancel anytime.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            CreditCardInputUI(onSuccess = onSubscribed)
        }
    }
}

@Composable
fun PremiumFeatureItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.CheckCircle, 
            null, 
            tint = Color(0xFF28A745),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun CreditCardInputUI(onSuccess: () -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Payment Details", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Your trial starts after confirmation", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = cardNumber,
            onValueChange = { if (it.length <= 16) cardNumber = it },
            label = { Text("Card Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.CreditCard, null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = expiry,
                onValueChange = { if (it.length <= 5) expiry = it },
                label = { Text("MM/YY") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = cvc,
                onValueChange = { if (it.length <= 3) cvc = it },
                label = { Text("CVC") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSuccess,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = cardNumber.length >= 16 || DeveloperConfig.isDeveloperMode
        ) {
            Text("Activate Trial", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        TextButton(onClick = { /* Go back */ }) {
            Text("Back to Plans", color = MaterialTheme.colorScheme.secondary)
        }
    }
}
