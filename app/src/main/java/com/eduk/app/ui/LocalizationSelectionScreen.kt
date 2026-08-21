package com.eduk.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.R
import com.eduk.app.model.LocalizationData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalizationSelectionScreen(onComplete: (countryCode: String, languageCode: String) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedCountry by remember { mutableStateOf(LocalizationData.countries[0]) }
    var selectedLanguage by remember { mutableStateOf(LocalizationData.languages[0]) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Hero Image with Gradient Overlay
        Image(
            painter = painterResource(id = R.drawable.parent_child_hero),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(300.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 220.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (step == 1) "Choose your Region" else "Choose your Language",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Eduk is tailored to your local curriculum.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            LinearProgressIndicator(
                progress = if (step == 1) 0.5f else 1f,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (step == 1) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(LocalizationData.countries) { country ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedCountry = country },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCountry == country) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(country.name, fontWeight = FontWeight.SemiBold) },
                                leadingContent = { Text(country.flag, fontSize = 24.sp) },
                                trailingContent = {
                                    RadioButton(selected = selectedCountry == country, onClick = null)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Next Step", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(LocalizationData.languages) { language ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedLanguage = language },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedLanguage == language) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(language.name, fontWeight = FontWeight.SemiBold) },
                                leadingContent = { Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                trailingContent = {
                                    RadioButton(selected = selectedLanguage == language, onClick = null)
                                },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onComplete(selectedCountry.code, selectedLanguage.code) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
