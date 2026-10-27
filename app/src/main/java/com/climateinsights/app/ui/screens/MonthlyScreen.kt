package com.climateinsights.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.climateinsights.app.data.models.DailyRecord
import com.climateinsights.app.ui.components.CitySelectionBottomSheet
import com.climateinsights.app.ui.components.CitySelectorCard
import com.climateinsights.app.ui.utils.formatGodina
import com.climateinsights.app.ui.viewmodels.MainUiState
import com.climateinsights.app.ui.viewmodels.MainViewModel
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        is MainUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is MainUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Greška: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
        }
        is MainUiState.Success -> {
            val allRecords = state.allHistoricalRecords
            var selectedMonth by remember { mutableIntStateOf(state.selectedDate.monthValue) }

            val monthNames = listOf(
                "januar", "februar", "mart", "april", "maj", "juni",
                "juli", "avgust", "septembar", "oktobar", "novembar", "decembar"
            )

            var showCityPicker by remember { mutableStateOf(false) }

            if (showCityPicker) {
                CitySelectionBottomSheet(
                    cities = state.cities,
                    selectedCity = state.selectedCity,
                    onCitySelected = { city -> viewModel.selectCity(city) },
                    onDismissRequest = { showCityPicker = false }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top City Switcher
                CitySelectorCard(
                    selectedCity = state.selectedCity,
                    onClick = { showCityPicker = true }
                )

                // Month Selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Izaberite mjesec za analizu:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(12) { idx ->
                                val monthNum = idx + 1
                                FilterChip(
                                    selected = selectedMonth == monthNum,
                                    onClick = { selectedMonth = monthNum },
                                    label = { Text(monthNames[idx].replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // Compute stats for selected month across all years
                val monthRecords = remember(allRecords, selectedMonth) {
                    allRecords.filter {
                        try {
                            LocalDate.parse(it.date).monthValue == selectedMonth
                        } catch (e: Exception) {
                            false
                        }
                    }
                }

                if (monthRecords.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Nema istorijskih podataka za ovaj mjesec.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    // Group by year
                    data class MonthYearStat(
                        val year: Int,
                        val avgTemp: Double?,
                        val maxTemp: Double?,
                        val minTemp: Double?,
                        val totalPrcp: Double,
                        val hottestDay: String?,
                        val coldestDay: String?
                    )

                    val yearStats = remember(monthRecords) {
                        monthRecords.groupBy { it.date.take(4).toIntOrNull() ?: 0 }
                            .map { (year, recs) ->
                                val avg = recs.mapNotNull { it.tavg }.average().takeIf { !it.isNaN() }
                                val maxRec = recs.filter { it.tmax != null }.maxByOrNull { it.tmax!! }
                                val minRec = recs.filter { it.tmin != null }.minByOrNull { it.tmin!! }
                                val prcpSum = recs.mapNotNull { it.prcp }.sum()

                                MonthYearStat(
                                    year = year,
                                    avgTemp = avg,
                                    maxTemp = maxRec?.tmax,
                                    minTemp = minRec?.tmin,
                                    totalPrcp = prcpSum,
                                    hottestDay = maxRec?.date,
                                    coldestDay = minRec?.date
                                )
                            }.sortedByDescending { it.year }
                    }

                    val hottestMonth = yearStats.filter { it.avgTemp != null }.maxByOrNull { it.avgTemp!! }
                    val coldestMonth = yearStats.filter { it.avgTemp != null }.minByOrNull { it.avgTemp!! }
                    val absMaxRecord = remember(monthRecords) { monthRecords.filter { it.tmax != null }.maxByOrNull { it.tmax!! } }
                    val absMinRecord = remember(monthRecords) { monthRecords.filter { it.tmin != null }.minByOrNull { it.tmin!! } }
                    val wettestMonth = yearStats.maxByOrNull { it.totalPrcp }

                    val multiYearAvg = yearStats.mapNotNull { it.avgTemp }.average().takeIf { !it.isNaN() } ?: 0.0

                    // Highlight Cards with uniform height and dimensions
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            hottestMonth?.let { h ->
                                MonthlyHighlightCard(
                                    title = "Najtopliji ${monthNames[selectedMonth - 1]}",
                                    value = "${String.format(Locale.US, "%.1f", h.avgTemp)}°C",
                                    sub = "Godina: ${h.year}.",
                                    color = Color(0xFFD32F2F),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            coldestMonth?.let { c ->
                                MonthlyHighlightCard(
                                    title = "Najhladniji ${monthNames[selectedMonth - 1]}",
                                    value = "${String.format(Locale.US, "%.1f", c.avgTemp)}°C",
                                    sub = "Godina: ${c.year}.",
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            absMaxRecord?.let { m ->
                                MonthlyHighlightCard(
                                    title = "Apsolutni Max dana",
                                    value = "${String.format(Locale.US, "%.1f", m.tmax)}°C",
                                    sub = "Datum: ${formatReadableDate(m.date)}",
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            absMinRecord?.let { m ->
                                MonthlyHighlightCard(
                                    title = "Apsolutni Min dana",
                                    value = "${String.format(Locale.US, "%.1f", m.tmin)}°C",
                                    sub = "Datum: ${formatReadableDate(m.date)}",
                                    color = Color(0xFF303F9F),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }

                    // Table of all years for this month
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Rang lista svih godina za ${monthNames[selectedMonth - 1]}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Višegodišnji prosjek: ${String.format(Locale.US, "%.1f", multiYearAvg)}°C (${formatGodina(yearStats.size)} analize)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Godina", modifier = Modifier.width(65.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Prosjek T", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Min / Max", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Uk. kiša", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }

                            yearStats.forEach { stat ->
                                val isTopWarm = stat.year == hottestMonth?.year
                                val isTopCold = stat.year == coldestMonth?.year
                                val isTopWet = stat.year == wettestMonth?.year

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.width(65.dp)) {
                                        Text(
                                            text = "${stat.year}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isTopWarm) Text("🔥 Rekord", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                        else if (isTopCold) Text("❄️ Rekord", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                                        else if (isTopWet) Text("🌧️ Kiša", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00796B), fontWeight = FontWeight.Bold)
                                    }

                                    Text(
                                        text = stat.avgTemp?.let { "${String.format(Locale.US, "%.1f", it)}°" } ?: "--",
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        modifier = Modifier.weight(1.2f),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stat.minTemp?.let { "${String.format(Locale.US, "%.0f", it)}°" } ?: "--",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF1976D2)
                                        )
                                        Text(" / ", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = stat.maxTemp?.let { "${String.format(Locale.US, "%.0f", it)}°" } ?: "--",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }

                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", stat.totalPrcp)} mm",
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.End,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (stat.totalPrcp > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatReadableDate(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "--"
    return try {
        val parts = dateStr.split("-")
        if (parts.size == 3) {
            val d = parts[2].toIntOrNull() ?: parts[2]
            val m = parts[1].toIntOrNull() ?: parts[1]
            val y = parts[0]
            val months = listOf("jan", "feb", "mar", "apr", "maj", "jun", "jul", "avg", "sep", "okt", "nov", "dec")
            val monthName = if (m is Int && m in 1..12) months[m - 1] else "$m"
            "$d. $monthName $y."
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
fun MonthlyHighlightCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
