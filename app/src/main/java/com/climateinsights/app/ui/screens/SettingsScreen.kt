package com.climateinsights.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.climateinsights.app.ui.viewmodels.MainUiState
import com.climateinsights.app.ui.viewmodels.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is MainUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is MainUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Greška: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is MainUiState.Success -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.padding(4.dp))
                            Text(
                                text = "Podešavanja",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Izaberite grad za analizu istorijskih i trenutnih klimatskih podataka",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                                }
                
                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Tema aplikacije", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.FilterChip(
                                selected = isDarkTheme == null,
                                onClick = { viewModel.setDarkTheme(null) },
                                label = { Text("Sistemska") },
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.FilterChip(
                                selected = isDarkTheme == false,
                                onClick = { viewModel.setDarkTheme(false) },
                                label = { Text("Svijetla") },
                                modifier = Modifier.weight(1f)
                            )
                            androidx.compose.material3.FilterChip(
                                selected = isDarkTheme == true,
                                onClick = { viewModel.setDarkTheme(true) },
                                label = { Text("Tamna") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Uvodni ekran (Welcome screen)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Prikažite početni ekran sa vodičem i selekcijom podrazumijevanog grada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.material3.OutlinedButton(
                            onClick = { viewModel.resetOnboarding() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Otvori")
                        }
                    }
                }

                var searchQuery by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
                var selectedCountryFilter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

                val availableCountries = androidx.compose.runtime.remember(state.cities) {
                    state.cities.map { it.country }.distinct()
                }

                val filteredCities = androidx.compose.runtime.remember(state.cities, searchQuery, selectedCountryFilter) {
                    state.cities.filter { city ->
                        val matchesCountry = selectedCountryFilter == null || city.country == selectedCountryFilter
                        val matchesSearch = searchQuery.isBlank() ||
                            city.name.contains(searchQuery.trim(), ignoreCase = true) ||
                            city.country.contains(searchQuery.trim(), ignoreCase = true)
                        matchesCountry && matchesSearch
                    }
                }

                Text(
                    text = "Dostupni gradovi (${state.cities.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Pretraži grad...") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Obriši")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        androidx.compose.material3.FilterChip(
                            selected = selectedCountryFilter == null,
                            onClick = { selectedCountryFilter = null },
                            label = { Text("Sve (${state.cities.size})") }
                        )
                    }
                    items(availableCountries) { country ->
                        val flag = when {
                            country.contains("Bosna", ignoreCase = true) -> "🇧🇦"
                            country.contains("Srbija", ignoreCase = true) -> "🇷🇸"
                            country.contains("Hrvatska", ignoreCase = true) -> "🇭🇷"
                            else -> "📍"
                        }
                        val count = state.cities.count { it.country == country }
                        androidx.compose.material3.FilterChip(
                            selected = selectedCountryFilter == country,
                            onClick = { selectedCountryFilter = if (selectedCountryFilter == country) null else country },
                            label = { Text("$flag $country ($count)") }
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredCities, key = { it.id }) { city ->
                        val isSelected = city.id == state.selectedCity.id
                        val flag = when {
                            city.country.contains("Bosna", ignoreCase = true) -> "🇧🇦"
                            city.country.contains("Srbija", ignoreCase = true) -> "🇷🇸"
                            city.country.contains("Hrvatska", ignoreCase = true) -> "🇭🇷"
                            else -> "📍"
                        }

                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectCity(city) },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                            ),
                            border = CardDefaults.outlinedCardBorder(enabled = isSelected)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        if (isSelected) Icons.Default.LocationOn else Icons.Default.LocationCity,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column {
                                        Text(
                                            text = city.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = "$flag ${city.country}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "1990–2026 istorijska baza + Uživo",
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Odabrano",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}