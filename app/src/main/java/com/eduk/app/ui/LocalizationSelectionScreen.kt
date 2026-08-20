package com.eduk.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eduk.app.model.LocalizationData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalizationSelectionScreen(onComplete: (countryCode: String, languageCode: String) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedCountry by remember { mutableStateOf(LocalizationData.countries[0]) }
    var selectedLanguage by remember { mutableStateOf(LocalizationData.languages[0]) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Eduk Worldwide", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = if (step == 1) 0.5f else 1f,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            )

            if (step == 1) {
                Text("Select your Country", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(LocalizationData.countries) { country ->
                        ListItem(
                            headlineContent = { Text(country.name) },
                            leadingContent = { Text(country.flag, style = MaterialTheme.typography.headlineSmall) },
                            trailingContent = {
                                RadioButton(selected = selectedCountry == country, onClick = null)
                            },
                            modifier = Modifier.clickable { selectedCountry = country }
                        )
                    }
                }
                Button(
                    onClick = { step = 2 },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Next")
                }
            } else {
                Text("Select your Language", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(LocalizationData.languages) { language ->
                        ListItem(
                            headlineContent = { Text(language.name) },
                            leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                            trailingContent = {
                                RadioButton(selected = selectedLanguage == language, onClick = null)
                            },
                            modifier = Modifier.clickable { selectedLanguage = language }
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(
                        onClick = { step = 1 },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Back")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { onComplete(selectedCountry.code, selectedLanguage.code) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}
