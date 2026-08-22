package com.eduk.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eduk.app.model.LocalizationData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalizationSelectionScreen(onComplete: (countryCode: String, languageCode: String) -> Unit) {
    var step by remember { mutableStateOf(1) }
    var selectedCountry by remember { mutableStateOf(LocalizationData.countries[0]) }
    var selectedLanguage by remember { mutableStateOf(LocalizationData.languages[0]) }
    var countryQuery by remember { mutableStateOf("") }
    var languageQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(countryQuery) {
        LocalizationData.countries.filter { country ->
            country.name.contains(countryQuery, ignoreCase = true) ||
                country.code.contains(countryQuery, ignoreCase = true)
        }
    }
    val filteredLanguages = remember(languageQuery) {
        LocalizationData.languages.filter { language ->
            language.name.contains(languageQuery, ignoreCase = true) ||
                language.code.contains(languageQuery, ignoreCase = true)
        }
    }
    val searchFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color(0xFF0B1F3A),
        unfocusedTextColor = Color(0xFF0B1F3A),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFFFF7A1A),
        unfocusedBorderColor = Color(0xFF64778C),
        focusedLabelColor = Color(0xFFFF7A1A),
        unfocusedLabelColor = Color(0xFF52677F),
        cursorColor = Color(0xFFFF7A1A)
    )

    Box(modifier = Modifier.fillMaxSize()) {
        FlowingControlBackdrop(pulse = step)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1F3A).copy(alpha = 0.78f), Color(0xFF2E6CB8).copy(alpha = 0.38f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 220.dp)
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(Color(0xFFF7FAFF).copy(alpha = 0.94f))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (step == 1) "Choose your Region" else "Choose your Language",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1F3A)
            )
            Text(
                text = "Eduk is tailored to your local curriculum.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF52677F),
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
                OutlinedTextField(
                    value = countryQuery,
                    onValueChange = { countryQuery = it },
                    label = { Text("Search country") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = searchFieldColors,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (filteredCountries.isEmpty()) {
                        item { EmptySearchResult("No country matches that search.") }
                    }
                    items(filteredCountries, key = { it.code }) { country ->
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
                OutlinedTextField(
                    value = languageQuery,
                    onValueChange = { languageQuery = it },
                    label = { Text("Search language") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    colors = searchFieldColors,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                )
                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (filteredLanguages.isEmpty()) {
                        item { EmptySearchResult("No language matches that search.") }
                    }
                    items(filteredLanguages, key = { it.code }) { language ->
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

@Composable
private fun EmptySearchResult(message: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(
            text = message,
            color = Color(0xFF52677F),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(20.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
