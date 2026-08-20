package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color(0xFFFFD700)
            )
            Text(
                text = "Unlock Premium Access",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "3-Day Free Trial",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Then only $3.99 / month",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            FeatureItem("AI Book Scanning & Question Generation")
            FeatureItem("Unlimited Screen Time Management")
            FeatureItem("Detailed Weekly Progress Reports")
            FeatureItem("Multi-Child Device Support")

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { showCreditCardInput = true },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Start Free Trial Now", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Cancel anytime before trial ends.",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            CreditCardInputUI(onSuccess = onSubscribed)
        }
    }
}

@Composable
fun FeatureItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Check, null, tint = Color.Green)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun CreditCardInputUI(onSuccess: () -> Unit) {
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvc by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Payment Information", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Safe & Secure Checkout", style = MaterialTheme.typography.bodyMedium)
        
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = cardNumber,
            onValueChange = { cardNumber = it },
            label = { Text("Card Number") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.CreditCard, null) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = expiry,
                onValueChange = { expiry = it },
                label = { Text("MM/YY") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            OutlinedTextField(
                value = cvc,
                onValueChange = { cvc = it },
                label = { Text("CVC") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onSuccess,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            enabled = cardNumber.length >= 16
        ) {
            Text("Confirm & Start Trial", fontSize = 18.sp)
        }
    }
}
