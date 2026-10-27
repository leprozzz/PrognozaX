package com.prognozax.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import com.prognozax.app.data.models.DailyRecord
import com.prognozax.app.data.models.ForecastDay
import com.prognozax.app.ui.viewmodels.MainUiState
import androidx.compose.material.icons.filled.ArrowDropDown
import com.prognozax.app.ui.components.CitySelectionBottomSheet
import com.prognozax.app.ui.components.CitySelectorCard
import com.prognozax.app.ui.viewmodels.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.prognozax.app.ui.utils.formatGodina
import com.prognozax.app.ui.utils.formatDanMjesec
import com.prognozax.app.ui.utils.formatPuniDatum
import com.prognozax.app.ui.utils.formatDanSedmiceIDatum

enum class TableSortOrder(val label: String) {
    TMAX_DESC("🔥 Max T"),
    TMIN_ASC("❄️ Min T"),
    PRCP_DESC("🌧️ Padavine"),
    YEAR_DESC("Godina (najnovije)"),
    YEAR_ASC("Godina (najstarije)")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OnThisDayScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var isOptionsExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var tableSortOrder by remember { mutableStateOf(TableSortOrder.TMAX_DESC) }

    when (val state = uiState) {
        is MainUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is MainUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Greška",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        is MainUiState.Success -> {
            val recordsForDay = state.analysis?.historicalRecordsForDay ?: emptyList()
            val analysis = state.analysis

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
                // 1. TOP DISCRETE LINE: City & Date with drop-down expand arrow
                Surface(
                    onClick = { isOptionsExpanded = !isOptionsExpanded },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                onClick = { showCityPicker = true },
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = state.selectedCity.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Promijeni grad",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "• ${formatPuniDatum(state.selectedDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.isToday) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Uživo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = if (isOptionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isOptionsExpanded) "Sakrij opcije" else "Otvori opcije za promjenu grada i datuma",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // EXPANDABLE PANEL: City Switcher, Date Picker & Quick Year Jump
                AnimatedVisibility(
                    visible = isOptionsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section A: City Switcher
                            Text(
                                text = "Aktivni grad:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            CitySelectorCard(
                                selectedCity = state.selectedCity,
                                onClick = { showCityPicker = true }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Section B: Date Navigation & Calendar
                            Text(
                                text = "Izbor datuma:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                IconButton(onClick = { 
                                    viewModel.selectDate(state.selectedDate.minusDays(1))
                                    isOptionsExpanded = false
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prethodni dan")
                                }

                                Surface(
                                    onClick = { showDatePicker = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = formatPuniDatum(state.selectedDate),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                IconButton(onClick = { 
                                    viewModel.selectDate(state.selectedDate.plusDays(1))
                                    isOptionsExpanded = false
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Sljedeći dan")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            // Section C: Quick Year Jump
                            Text(
                                text = "Brzi skok kroz godine:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val today = LocalDate.now()
                                FilterChip(
                                    selected = state.isToday,
                                    onClick = { 
                                        viewModel.selectDate(today)
                                        isOptionsExpanded = false
                                    },
                                    label = { Text("Danas (${today.year})", style = MaterialTheme.typography.labelSmall) }
                                )
                                listOf(1, 2, 3, 5, 10, 20).forEach { yearsBack ->
                                    val targetYear = today.year - yearsBack
                                    val isSelected = state.selectedDate.year == targetYear &&
                                            state.selectedDate.monthValue == today.monthValue &&
                                            state.selectedDate.dayOfMonth == today.dayOfMonth

                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.selectDate(
                                                state.selectedDate.withYear(targetYear)
                                            )
                                            isOptionsExpanded = false
                                        },
                                        label = { Text("-$yearsBack god ($targetYear)", style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. CURRENT WEATHER CONDITIONS (Trenutna vremenska prilika)
                if (state.isToday && state.liveSnapshot != null) {
                    val snap = state.liveSnapshot
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Trenutne vremenske prilike",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = state.selectedCity.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", snap.current)}°C",
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Danas: Max ${String.format(Locale.US, "%.1f", snap.todayMax)}°C  •  Min ${String.format(Locale.US, "%.1f", snap.todayMin)}°C",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.WaterDrop,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", snap.precipitation)} mm",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = "Padavine danas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            if (snap.forecast.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Prognoza za naredne dane",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(snap.forecast) { f ->
                                        ForecastChip(f)
                                    }
                                }
                            }
                        }
                    }
                } else if (!state.isToday && analysis != null) {
                    // Selected historical date specific observation card
                    val obsRecord = recordsForDay.find { it.date == state.selectedDate.toString() }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Zabilježeno na datum: ${formatPuniDatum(state.selectedDate)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (obsRecord != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = obsRecord.tavg?.let { "${String.format(Locale.US, "%.1f", it)}°C" } ?: "--",
                                            style = MaterialTheme.typography.displayMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Prosječna dnevna temperatura",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Max: ${obsRecord.tmax?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}°C",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFD32F2F)
                                        )
                                        Text(
                                            text = "Min: ${obsRecord.tmin?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}°C",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1976D2)
                                        )
                                        Text(
                                            text = "Kiša: ${obsRecord.prcp?.let { String.format(Locale.US, "%.1f", it) } ?: "0.0"} mm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "U bazi nema pojedinačnog mjerenja za tačnu godinu ${state.selectedDate.year}. U nastavku možete vidjeti istorijske rekorde i sve zabilježene godine.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 3. CLIMATE CONTEXT & HISTORICAL RECORDS (Klimatski kontekst sa istorijskim rekordima)
                if (analysis != null && recordsForDay.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Klimatski kontekst za ${formatDanMjesec(state.selectedDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Delta comparison banner
                        val currentTime = java.time.LocalTime.now()
                        val isDaytimeToday = state.isToday && currentTime.isAfter(java.time.LocalTime.of(8, 0)) && currentTime.isBefore(java.time.LocalTime.of(22, 0))
                        
                        val comp = if (isDaytimeToday) analysis.tmaxComparison else analysis.tavgComparison
                        val avgComparisonLabel = if (isDaytimeToday) "prosječnu maksimalnu dnevnu temperaturu" else "prosječnu dnevnu temperaturu"
                        val avgStatLabel = if (isDaytimeToday) "Prosječna maksimalna dnevna temperatura za ovaj dan:" else "Prosječna dnevna temperatura za ovaj dan:"

                        comp?.let { comparison ->
                            val delta = comparison.deltaFromAverage
                            val isWarmer = delta >= 0
                            val deltaColor = if (isWarmer) Color(0xFFD32F2F) else Color(0xFF1976D2)

                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(deltaColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            if (isWarmer) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = deltaColor
                                        )
                                    }
                                    Column {
                                        val prefix = if (delta >= 0) "+" else ""
                                        Text(
                                            text = "$prefix${String.format(Locale.US, "%.1f", delta)}°C u odnosu na $avgComparisonLabel",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = deltaColor
                                        )
                                        Text(
                                            text = "$avgStatLabel ${String.format(Locale.US, "%.1f", comparison.historicalAverage)}°C (kroz ${formatGodina(recordsForDay.size)})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (comparison.isRecordCandidate) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "🔥 Blizu istorijskog rekorda za ovaj datum!",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Extremes Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Rekordi dnevne temperature (${formatGodina(recordsForDay.size)} analize)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    analysis.tmaxComparison?.allTimeMax?.let { max ->
                                        MetricBox(
                                            title = "Najtopliji dan\n(Maksimalna T)",
                                            value = "${String.format(Locale.US, "%.1f", max.value)}°C",
                                            subtitle = "Godina: ${max.year}.",
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    analysis.tmaxComparison?.allTimeMin?.let { min ->
                                        MetricBox(
                                            title = "Najhladniji dan\n(Najniža dnevna T)",
                                            value = "${String.format(Locale.US, "%.1f", min.value)}°C",
                                            subtitle = "Godina: ${min.year}.",
                                            color = Color(0xFF0288D1),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    analysis.tminComparison?.allTimeMin?.let { minTmin ->
                                        MetricBox(
                                            title = "Najniža jutarnja\ntemperatura",
                                            value = "${String.format(Locale.US, "%.1f", minTmin.value)}°C",
                                            subtitle = "Godina: ${minTmin.year}.",
                                            color = Color(0xFF303F9F),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    analysis.maxPrecipitationEver?.let { p ->
                                        MetricBox(
                                            title = "Najviše padavina\n(Dnevni rekord)",
                                            value = "${String.format(Locale.US, "%.1f", p.value)} mm",
                                            subtitle = "Godina: ${p.year}.",
                                            color = Color(0xFF00796B),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. DETAILED HISTORICAL TABLE (Detaljna tabela svih godina direktno ispod)
                if (recordsForDay.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📊 Detaljna tabela svih godina za ${formatDanMjesec(state.selectedDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Sorting chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(TableSortOrder.values()) { order ->
                                FilterChip(
                                    selected = tableSortOrder == order,
                                    onClick = { tableSortOrder = order },
                                    label = { Text(order.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        val sortedRecords = remember(recordsForDay, tableSortOrder) {
                            when (tableSortOrder) {
                                TableSortOrder.YEAR_DESC -> recordsForDay.sortedByDescending { it.date.take(4).toIntOrNull() ?: 0 }
                                TableSortOrder.YEAR_ASC -> recordsForDay.sortedBy { it.date.take(4).toIntOrNull() ?: 0 }
                                TableSortOrder.TMAX_DESC -> recordsForDay.sortedByDescending { it.tmax ?: -999.0 }
                                TableSortOrder.TMIN_ASC -> recordsForDay.sortedBy { it.tmin ?: 999.0 }
                                TableSortOrder.PRCP_DESC -> recordsForDay.sortedByDescending { it.prcp ?: -1.0 }
                            }
                        }

                        val maxTmax = recordsForDay.mapNotNull { it.tmax }.maxOrNull()
                        val minTmin = recordsForDay.mapNotNull { it.tmin }.minOrNull()
                        val maxPrcp = recordsForDay.mapNotNull { it.prcp }.filter { it > 0 }.maxOrNull()

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                // Table Header
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
                                        Text("Godina", modifier = Modifier.width(68.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("Prosjek", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("Min / Max", modifier = Modifier.weight(1.2f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                        Text("Padavine", modifier = Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Table Rows
                                sortedRecords.forEach { record ->
                                    val year = record.date.take(4).toIntOrNull() ?: 0
                                    val isSelected = year == state.selectedDate.year
                                    val isHottest = maxTmax != null && record.tmax == maxTmax
                                    val isColdest = minTmin != null && record.tmin == minTmin
                                    val isWettest = maxPrcp != null && record.prcp == maxPrcp

                                    Surface(
                                        onClick = {
                                            viewModel.selectDate(state.selectedDate.withYear(year))
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Year + badge
                                            Column(modifier = Modifier.width(68.dp)) {
                                                Text(
                                                    text = "$year",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isHottest) {
                                                    Text("🔥 Max", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                                } else if (isColdest) {
                                                    Text("❄️ Min", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                                                } else if (isWettest) {
                                                    Text("🌧️ Kiša", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00796B), fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Tavg
                                            Text(
                                                text = record.tavg?.let { "${String.format(Locale.US, "%.1f", it)}°" } ?: "--",
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.Center,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // Min / Max
                                            Row(
                                                modifier = Modifier.weight(1.2f),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = record.tmin?.let { "${String.format(Locale.US, "%.1f", it)}°" } ?: "--",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF1976D2),
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(" / ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(
                                                    text = record.tmax?.let { "${String.format(Locale.US, "%.1f", it)}°" } ?: "--",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFFD32F2F),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            // Prcp
                                            Text(
                                                text = record.prcp?.let { if (it > 0) "${String.format(Locale.US, "%.1f", it)} mm" else "0 mm" } ?: "0 mm",
                                                modifier = Modifier.weight(1f),
                                                textAlign = TextAlign.End,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if ((record.prcp ?: 0.0) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (recordsForDay.isEmpty()) {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Istorijska baza",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Za ${state.selectedCity.name} istorijski podaci još nisu učitani. Podaci uživo i vremenska prognoza funkcionišu uredno.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Material 3 Date Picker Dialog
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = state.selectedDate
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val selectedLocalDate = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    viewModel.selectDate(selectedLocalDate)
                                }
                                showDatePicker = false
                                isOptionsExpanded = false
                            }
                        ) {
                            Text("Potvrdi")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Odustani")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

@Composable
fun ForecastChip(forecast: ForecastDay) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val date = try {
                val parsed = LocalDate.parse(forecast.date)
                formatDanSedmiceIDatum(parsed)
            } catch (e: Exception) {
                forecast.date
            }
            Text(text = date, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${String.format(Locale.US, "%.0f", forecast.tmax)}° / ${String.format(Locale.US, "%.0f", forecast.tmin)}°",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            if (forecast.precipitation > 0) {
                Text(
                    text = "${String.format(Locale.US, "%.1f", forecast.precipitation)}mm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun MetricBox(
    title: String,
    value: String,
    subtitle: String,
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
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                minLines = 2,
                maxLines = 2
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
