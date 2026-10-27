package com.climateinsights.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "O aplikaciji",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "PrognozaX v1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Šta je PrognozaX?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PrognozaX je moderna Android aplikacija razvijena u Jetpack Compose-u namijenjena praćenju, analizi i vizuelizaciji vremenskih i klimatskih podataka.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Aplikacija omogućava upoređivanje današnjih temperatura sa višedecenijskim istorijskim prosjecima na isti datum.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            text = "Ključne mogućnosti",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        FeatureRow(
            icon = Icons.Default.History,
            title = "Istorijski prosjek na današnji dan",
            description = "Proračun temperaturnog odstupanja (anomalije) u odnosu na 1980–2024."
        )

        FeatureRow(
            icon = Icons.Default.Thermostat,
            title = "Istorijski rekordi i ekstremi",
            description = "Prikaz najtoplijeg i najhladnijeg dana u istoriji mjerenja za svaki datum."
        )

        FeatureRow(
            icon = Icons.Default.Cloud,
            title = "Meteo podaci uživo",
            description = "Integracija sa Open-Meteo API servisom za trenutne uslove i 7-dnevnu prognozu."
        )

        FeatureRow(
            icon = Icons.Default.Dataset,
            title = "Grafički trendovi",
            description = "Interaktivni dijagrami kretanja prosječnih, maksimalnih i minimalnih temperatura."
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Baza obuhvata 20 gradova u regionu (Bosna i Hercegovina, Srbija, Hrvatska) sa višedecenijskom arhivom mjerenja (1990–2026, Sarajevo od 1901), kao i meteorološke podatke uživo i 7-dnevnu vremensku prognozu.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, description: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}