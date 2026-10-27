package com.climateinsights.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.climateinsights.app.data.models.DailyRecord
import com.climateinsights.app.ui.viewmodels.MainUiState
import com.climateinsights.app.ui.viewmodels.MainViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import com.climateinsights.app.ui.utils.formatGodina
import com.climateinsights.app.ui.utils.formatDanMjesec
import com.climateinsights.app.ui.utils.formatPuniDatum

enum class StatsTab(val label: String, val icon: ImageVector) {
    SEASONAL("Klimatski indeksi", Icons.Default.LocalFireDepartment),
    EXTREMES("Tabele dana", Icons.Default.TableChart),
    DECADES("Dekadni trend", Icons.Default.TrendingUp),
    CHART("Grafikon", Icons.Default.ShowChart),
    OVERVIEW("Ova godina", Icons.Default.CompareArrows)
}

enum class ExtremesFilter(val label: String) {
    HOTTEST("🔥 Najtoplije (Max)"),
    COLDEST("❄️ Najhladnije (Min)"),
    WARMEST_AVG("☀️ Najtopliji prosjek"),
    COLDEST_AVG("🥶 Najhladniji prosjek"),
    RAINIEST("🌧️ Najviše kiše")
}

enum class ClimateIndexType(val title: String, val description: String, val unit: String, val icon: String) {
    TROPICAL_DAYS("Tropski dani (Tmax ≥ 30°C)", "Broj dana sa temperaturom od 30°C i više", "dana", "🔥"),
    SUMMER_DAYS("Ljetni dani (Tmax ≥ 25°C)", "Broj dana sa temperaturom od 25°C i više", "dana", "☀️"),
    TROPICAL_NIGHTS("Tropske noći (Tmin ≥ 20°C)", "Noći kada temperatura nije pala ispod 20°C", "noći", "🌙"),
    FROST_DAYS("Mrazni dani (Tmin < 0°C)", "Dani kada je minimalna temperatura pala ispod 0°C", "dana", "❄️"),
    ICE_DAYS("Ledeni dani (Tmax < 0°C)", "Dani kada temperatura cijeli dan nije prešla 0°C", "dana", "🧊"),
    DROUGHT_STREAK("Najduža suša (niz bez kiše)", "Maksimalan broj uzastopnih dana bez padavina", "dana", "🌵"),
    HEAVY_RAIN_DAYS("Obilne padavine (≥ 20mm)", "Dani sa 20 mm ili više padavina", "dana", "🌧️")
}

enum class ChartMetricType(val label: String, val unit: String) {
    TAVG("Prosječna", "°C"),
    TMAX("Maksimalna", "°C"),
    TMIN("Minimalna", "°C"),
    PRCP("Padavine", "mm")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(StatsTab.SEASONAL) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }

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
            val recordsForDay = state.analysis?.historicalRecordsForDay ?: emptyList()
            val allRecords = state.allHistoricalRecords

            if (showCityPicker) {
                com.climateinsights.app.ui.components.CitySelectionBottomSheet(
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
                // Header Card with City, Date picker & Sync (Compact, no extra base description)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showCityPicker = true }
                        ) {
                            Text(
                                text = "Klimatska statistika",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${state.selectedCity.name} ▾",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "• Datum: ${formatDanMjesec(state.selectedDate)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showCityPicker = true }) {
                                Icon(Icons.Default.LocationCity, contentDescription = "Promijeni grad", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.syncCurrentCityData() },
                                enabled = !state.isSyncing
                            ) {
                                if (state.isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = "Sinhronizuj podatke", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Izaberi datum",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                if (recordsForDay.isNotEmpty() || allRecords.isNotEmpty()) {
                    // Navigation Tabs
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        modifier = Modifier.fillMaxWidth(),
                        edgePadding = 0.dp
                    ) {
                        StatsTab.values().forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }

                    when (selectedTab) {
                        StatsTab.SEASONAL -> {
                            SeasonalAndIndicesSection(allRecords)
                        }
                        StatsTab.EXTREMES -> {
                            ExtremesTablesSection(recordsForDay)
                        }
                        StatsTab.DECADES -> {
                            DecadesSection(recordsForDay)
                        }
                        StatsTab.CHART -> {
                            ResponsiveChartSection(recordsForDay, state.selectedDate)
                        }
                        StatsTab.OVERVIEW -> {
                            OverviewSection(state, allRecords)
                        }
                    }
                } else {
                    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Nema istorijskih podataka",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Kliknite na dugme za sinhronizaciju (🔄) na vrhu kako bi se automatski preuzeli ERA5 podaci od 1990. do danas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

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

// -------------------------------------------------------------------------------------------------
// 1. SEASONAL & CLIMATE INDICES SECTION (KLIMATSKI INDEKSI I SEZONE)
// -------------------------------------------------------------------------------------------------
@Composable
fun SeasonalAndIndicesSection(allRecords: List<DailyRecord>) {
    var selectedIndex by remember { mutableStateOf(ClimateIndexType.TROPICAL_DAYS) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "⚡ Sezonski ekstremi i klimatski indeksi",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Index Filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ClimateIndexType.values()) { idx ->
                FilterChip(
                    selected = selectedIndex == idx,
                    onClick = { selectedIndex = idx },
                    label = { Text("${idx.icon} ${idx.title.substringBefore('(').trim()}", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Info Card for current selected index
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(selectedIndex.icon, fontSize = 28.sp)
                Column {
                    Text(
                        text = selectedIndex.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = selectedIndex.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Calculate count per year for the selected index
        data class YearIndexCount(val year: Int, val count: Int)

        val yearCounts = remember(allRecords, selectedIndex) {
            val byYear = allRecords.groupBy { it.date.take(4).toIntOrNull() ?: 0 }
            byYear.map { (year, recs) ->
                val cnt = when (selectedIndex) {
                    ClimateIndexType.TROPICAL_DAYS -> recs.count { (it.tmax ?: -999.0) >= 30.0 }
                    ClimateIndexType.SUMMER_DAYS -> recs.count { (it.tmax ?: -999.0) >= 25.0 }
                    ClimateIndexType.TROPICAL_NIGHTS -> recs.count { (it.tmin ?: -999.0) >= 20.0 }
                    ClimateIndexType.FROST_DAYS -> recs.count { (it.tmin ?: 999.0) < 0.0 }
                    ClimateIndexType.ICE_DAYS -> recs.count { (it.tmax ?: 999.0) < 0.0 }
                    ClimateIndexType.DROUGHT_STREAK -> {
                        var maxStreak = 0
                        var curr = 0
                        recs.sortedBy { it.date }.forEach { r ->
                            val prcp = r.prcp ?: 0.0
                            if (prcp <= 0.1) {
                                curr++
                                if (curr > maxStreak) maxStreak = curr
                            } else {
                                curr = 0
                            }
                        }
                        maxStreak
                    }
                    ClimateIndexType.HEAVY_RAIN_DAYS -> recs.count { (it.prcp ?: 0.0) >= 20.0 }
                }
                YearIndexCount(year, cnt)
            }.sortedByDescending { it.count }
        }

        val topYear = yearCounts.firstOrNull()
        val avgCount = yearCounts.map { it.count }.average().takeIf { !it.isNaN() } ?: 0.0

        // Highlight summary
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            topYear?.let { top ->
                RecordHighlightCard(
                    title = "Apsolutni rekord",
                    value = "${top.count} ${selectedIndex.unit}",
                    sub = "Godina: ${top.year}",
                    color = Color(0xFFD32F2F),
                    modifier = Modifier.weight(1f)
                )
            }
            RecordHighlightCard(
                title = "Godišnji prosjek",
                value = "${String.format(Locale.US, "%.1f", avgCount)} ${selectedIndex.unit}",
                sub = "kroz ${formatGodina(yearCounts.size)}",
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
        }

        // Leaderboard Table
        Text(
            text = "Rang lista godina (od najvećeg broja):",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 6.dp)
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                val maxVal = yearCounts.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

                yearCounts.take(15).forEachIndexed { index, item ->
                    val rankBadge = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rankBadge,
                            modifier = Modifier.width(36.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.year}.",
                            modifier = Modifier.width(60.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Visual progress bar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            val fraction = (item.count.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(16.dp)
                                    .background(
                                        when {
                                            index == 0 -> Color(0xFFD32F2F)
                                            index < 3 -> Color(0xFFE65100)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                        }

                        Text(
                            text = "${item.count} ${selectedIndex.unit}",
                            modifier = Modifier.width(75.dp),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordHighlightCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(text = sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 3. OVERVIEW SECTION (OVA GODINA - POREĐENJE GODIŠNJEG PROSJEKA TEMPERATURE)
// -------------------------------------------------------------------------------------------------
data class YearAgg(
    val year: Int,
    val avgT: Double,
    val avgTmax: Double,
    val avgTmin: Double,
    val totalPrcp: Double,
    val maxT: Double,
    val minT: Double,
    val count: Int
)

@Composable
fun OverviewSection(state: MainUiState.Success, allRecords: List<DailyRecord>) {
    val currentYear = state.selectedDate.year
    val dayOfYear = state.selectedDate.dayOfYear

    // 1. Agregacija za svaku godinu
    val yearlyStats = remember(allRecords) {
        allRecords
            .groupBy { it.date.take(4).toIntOrNull() ?: 0 }
            .filter { (yr, list) -> yr in 1990..2026 && list.any { it.tavg != null } }
            .mapValues { (yr, list) ->
                val tavgList = list.mapNotNull { it.tavg }
                val tmaxList = list.mapNotNull { it.tmax }
                val tminList = list.mapNotNull { it.tmin }
                val prcpList = list.mapNotNull { it.prcp }
                YearAgg(
                    year = yr,
                    avgT = if (tavgList.isNotEmpty()) tavgList.average() else 0.0,
                    avgTmax = if (tmaxList.isNotEmpty()) tmaxList.average() else 0.0,
                    avgTmin = if (tminList.isNotEmpty()) tminList.average() else 0.0,
                    totalPrcp = prcpList.sum(),
                    maxT = tmaxList.maxOrNull() ?: 0.0,
                    minT = tminList.minOrNull() ?: 0.0,
                    count = tavgList.size
                )
            }
    }

    // Istorijske kompletne godine (prethodne godine pre ove)
    val pastCompletedYears = remember(yearlyStats, currentYear) {
        yearlyStats.filter { (yr, stat) -> yr < currentYear && stat.count >= 200 }
    }

    // Višegodišnji godišnji prosjeci
    val histAnnualAvgTemp = remember(pastCompletedYears) {
        pastCompletedYears.values.map { it.avgT }.average().takeIf { !it.isNaN() } ?: 11.5
    }
    val histAnnualAvgTmax = remember(pastCompletedYears) {
        pastCompletedYears.values.map { it.avgTmax }.average().takeIf { !it.isNaN() } ?: 16.5
    }
    val histAnnualAvgTmin = remember(pastCompletedYears) {
        pastCompletedYears.values.map { it.avgTmin }.average().takeIf { !it.isNaN() } ?: 7.0
    }
    val histAnnualPrcp = remember(pastCompletedYears) {
        pastCompletedYears.values.map { it.totalPrcp }.average().takeIf { !it.isNaN() } ?: 850.0
    }

    // Podaci za ovu godinu
    val thisYearStat = yearlyStats[currentYear]
    val thisYearAvgTemp = thisYearStat?.avgT
    val thisYearAvgTmax = thisYearStat?.avgTmax
    val thisYearAvgTmin = thisYearStat?.avgTmin
    val thisYearPrcp = thisYearStat?.totalPrcp ?: 0.0

    // Istorijski prosjek za isti period u godini (do današnjeg datuma)
    val samePeriodPastAvg = remember(allRecords, currentYear, dayOfYear) {
        val subset = allRecords.filter {
            val yr = it.date.take(4).toIntOrNull() ?: 0
            if (yr in 1990 until currentYear) {
                try {
                    LocalDate.parse(it.date).dayOfYear <= dayOfYear
                } catch (e: Exception) { false }
            } else false
        }.mapNotNull { it.tavg }
        if (subset.isNotEmpty()) subset.average() else histAnnualAvgTemp
    }

    val deltaAnnual = if (thisYearAvgTemp != null) thisYearAvgTemp - histAnnualAvgTemp else 0.0
    val isWarmer = deltaAnnual >= 0
    val deltaColor = if (isWarmer) Color(0xFFD32F2F) else Color(0xFF1976D2)

    val sortedByWarmth = remember(yearlyStats) {
        yearlyStats.values.sortedByDescending { it.avgT }
    }
    val thisYearRank = sortedByWarmth.indexOfFirst { it.year == currentYear } + 1
    val warmestYear = sortedByWarmth.firstOrNull()
    val coldestYear = sortedByWarmth.lastOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Glavna kartica: Godišnje temperaturno odstupanje
        if (thisYearAvgTemp != null) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(deltaColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isWarmer) "🔥" else "❄️",
                            fontSize = 26.sp
                        )
                    }
                    Column {
                        val prefix = if (deltaAnnual >= 0) "+" else ""
                        Text(
                            text = "$prefix${String.format(java.util.Locale.US, "%.1f", deltaAnnual)}°C odstupanje godišnjeg prosjeka",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = deltaColor
                        )
                        Text(
                            text = "Godišnji prosjek $currentYear: ${String.format(java.util.Locale.US, "%.1f", thisYearAvgTemp)}°C • Višegodišnji (1990–): ${String.format(java.util.Locale.US, "%.1f", histAnnualAvgTemp)}°C",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (thisYearRank in 1..sortedByWarmth.size) {
                            Text(
                                text = "🏆 $thisYearRank. najtoplija godina od 1990. u ${state.selectedCity.name}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Tabela poređenja godišnjih parametara
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📊 Poređenje godišnjih klimatskih prosjeka",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ova godina ($currentYear) u poređenju sa višegodišnjim godišnjim prosjekom (1990–)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Godišnji parametar", modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Ova god.", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Prosjek (1990–)", modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Text("Razlika", modifier = Modifier.weight(0.9f), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 1. Prosječna godišnja temperatura
                AnnualComparisonRow(
                    label = "Godišnji prosjek T",
                    current = thisYearAvgTemp?.let { "${String.format(java.util.Locale.US, "%.1f", it)}°" } ?: "--",
                    average = "${String.format(java.util.Locale.US, "%.1f", histAnnualAvgTemp)}°",
                    diff = thisYearAvgTemp?.let {
                        val d = it - histAnnualAvgTemp
                        (if (d >= 0) "+${String.format(java.util.Locale.US, "%.1f", d)}" else String.format(java.util.Locale.US, "%.1f", d)) + "°"
                    } ?: "--",
                    isWarmer = deltaAnnual >= 0
                )

                // 2. Prosječna maksimalna godišnja
                AnnualComparisonRow(
                    label = "Prosječna Max T",
                    current = thisYearAvgTmax?.let { "${String.format(java.util.Locale.US, "%.1f", it)}°" } ?: "--",
                    average = "${String.format(java.util.Locale.US, "%.1f", histAnnualAvgTmax)}°",
                    diff = thisYearAvgTmax?.let {
                        val d = it - histAnnualAvgTmax
                        (if (d >= 0) "+${String.format(java.util.Locale.US, "%.1f", d)}" else String.format(java.util.Locale.US, "%.1f", d)) + "°"
                    } ?: "--",
                    isWarmer = (thisYearAvgTmax ?: 0.0) >= histAnnualAvgTmax
                )

                // 3. Prosječna minimalna godišnja
                AnnualComparisonRow(
                    label = "Prosječna Min T",
                    current = thisYearAvgTmin?.let { "${String.format(java.util.Locale.US, "%.1f", it)}°" } ?: "--",
                    average = "${String.format(java.util.Locale.US, "%.1f", histAnnualAvgTmin)}°",
                    diff = thisYearAvgTmin?.let {
                        val d = it - histAnnualAvgTmin
                        (if (d >= 0) "+${String.format(java.util.Locale.US, "%.1f", d)}" else String.format(java.util.Locale.US, "%.1f", d)) + "°"
                    } ?: "--",
                    isWarmer = (thisYearAvgTmin ?: 0.0) >= histAnnualAvgTmin
                )

                // 4. Padavine
                AnnualComparisonRow(
                    label = "Ukupno padavina",
                    current = "${String.format(java.util.Locale.US, "%.0f", thisYearPrcp)} mm",
                    average = "${String.format(java.util.Locale.US, "%.0f", histAnnualPrcp)} mm",
                    diff = "${if (thisYearPrcp >= histAnnualPrcp) "+" else ""}${String.format(java.util.Locale.US, "%.0f", thisYearPrcp - histAnnualPrcp)} mm",
                    isWarmer = null
                )

                // 5. Do današnjeg datuma (isti period godine)
                AnnualComparisonRow(
                    label = "Prosjek do današnjeg dana",
                    current = thisYearAvgTemp?.let { "${String.format(java.util.Locale.US, "%.1f", it)}°" } ?: "--",
                    average = "${String.format(java.util.Locale.US, "%.1f", samePeriodPastAvg)}°",
                    diff = thisYearAvgTemp?.let {
                        val d = it - samePeriodPastAvg
                        (if (d >= 0) "+${String.format(java.util.Locale.US, "%.1f", d)}" else String.format(java.util.Locale.US, "%.1f", d)) + "°"
                    } ?: "--",
                    isWarmer = (thisYearAvgTemp ?: 0.0) >= samePeriodPastAvg
                )
            }
        }

        // Kartica sa istorijskim godišnjim rekordima
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📈 Istorijski ekstremi po godinama (${state.selectedCity.name})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    warmestYear?.let { wy ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFD32F2F).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Najtoplija godina", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${wy.year}.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text("Godišnji prosjek: ${String.format(java.util.Locale.US, "%.1f", wy.avgT)}°C", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    coldestYear?.let { cy ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1976D2).copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color(0xFF1976D2).copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Najhladnija godina", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1976D2), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${cy.year}.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text("Godišnji prosjek: ${String.format(java.util.Locale.US, "%.1f", cy.avgT)}°C", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnualComparisonRow(label: String, current: String, average: String, diff: String, isWarmer: Boolean?) {
    val diffColor = when (isWarmer) {
        true -> Color(0xFFD32F2F)
        false -> Color(0xFF1976D2)
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(current, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(average, modifier = Modifier.weight(1.1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(diff, modifier = Modifier.weight(0.9f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = diffColor)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

// -------------------------------------------------------------------------------------------------
// 4. EXTREMES TABLES (TABELE EKSTREMA ZA ODABRANI DAN)
// -------------------------------------------------------------------------------------------------
@Composable
fun ExtremesTablesSection(records: List<DailyRecord>) {
    var filter by remember { mutableStateOf(ExtremesFilter.HOTTEST) }

    val sorted = remember(records, filter) {
        when (filter) {
            ExtremesFilter.HOTTEST -> records.filter { it.tmax != null }.sortedByDescending { it.tmax }
            ExtremesFilter.COLDEST -> records.filter { it.tmin != null }.sortedBy { it.tmin }
            ExtremesFilter.WARMEST_AVG -> records.filter { it.tavg != null }.sortedByDescending { it.tavg }
            ExtremesFilter.COLDEST_AVG -> records.filter { it.tavg != null }.sortedBy { it.tavg }
            ExtremesFilter.RAINIEST -> records.filter { it.prcp != null }.sortedByDescending { it.prcp }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "🏆 Istorijski ekstremi na ovaj dan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ExtremesFilter.values()) { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                sorted.take(10).forEachIndexed { index, record ->
                    val rankText = when (index) {
                        0 -> "🥇"
                        1 -> "🥈"
                        2 -> "🥉"
                        else -> "${index + 1}."
                    }
                    val year = record.date.take(4)

                    val mainValue = when (filter) {
                        ExtremesFilter.HOTTEST -> "Max: ${String.format(Locale.US, "%.1f", record.tmax)}°C"
                        ExtremesFilter.COLDEST -> "Min: ${String.format(Locale.US, "%.1f", record.tmin)}°C"
                        ExtremesFilter.WARMEST_AVG -> "Prosjek: ${String.format(Locale.US, "%.1f", record.tavg)}°C"
                        ExtremesFilter.COLDEST_AVG -> "Prosjek: ${String.format(Locale.US, "%.1f", record.tavg)}°C"
                        ExtremesFilter.RAINIEST -> "Kiša: ${String.format(Locale.US, "%.1f", record.prcp)} mm"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = rankText,
                            modifier = Modifier.width(36.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$year. god",
                            modifier = Modifier.width(80.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = mainValue,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (filter) {
                                ExtremesFilter.HOTTEST, ExtremesFilter.WARMEST_AVG -> Color(0xFFD32F2F)
                                ExtremesFilter.COLDEST, ExtremesFilter.COLDEST_AVG -> Color(0xFF1976D2)
                                ExtremesFilter.RAINIEST -> Color(0xFF00796B)
                            }
                        )
                        Text(
                            text = "Tavg: ${record.tavg?.let { "${String.format(Locale.US, "%.1f", it)}°" } ?: "--"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < 9) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// 5. DECADES TREND
// -------------------------------------------------------------------------------------------------
@Composable
fun DecadesSection(records: List<DailyRecord>) {
    val decades = listOf(1990, 2000, 2010, 2020)
    val decadeStats = decades.map { decadeStart ->
        val decadeRecords = records.filter {
            val y = it.date.take(4).toIntOrNull() ?: 0
            y in decadeStart..(decadeStart + 9)
        }
        val avg = decadeRecords.mapNotNull { it.tavg }.average().takeIf { !it.isNaN() }
        val max = decadeRecords.mapNotNull { it.tmax }.maxOrNull()
        val min = decadeRecords.mapNotNull { it.tmin }.minOrNull()
        val prcpAvg = decadeRecords.mapNotNull { it.prcp }.average().takeIf { !it.isNaN() }
        DecadeStat(decadeStart, avg, max, min, prcpAvg, decadeRecords.size)
    }.filter { it.sampleCount > 0 }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "📈 Kretanje prosjeka po decenijama na ovaj dan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        decadeStats.forEach { stat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stat.startYear}-e (${stat.startYear}–${stat.startYear + 9})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stat.sampleCount} god. mjerenja",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Prosjek T", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stat.tavg?.let { "${String.format(Locale.US, "%.1f", it)}°C" } ?: "--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column {
                            Text("Raspon (Min - Max)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${stat.tmin?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}° / ${stat.tmax?.let { String.format(Locale.US, "%.1f", it) } ?: "--"}°",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Prosj. kiša", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stat.prcpAvg?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "0.0 mm",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

data class DecadeStat(
    val startYear: Int,
    val tavg: Double?,
    val tmax: Double?,
    val tmin: Double?,
    val prcpAvg: Double?,
    val sampleCount: Int
)

// -------------------------------------------------------------------------------------------------
// 6. RESPONSIVE COMPACT CHART (GRAFIKON)
// -------------------------------------------------------------------------------------------------
@Composable
fun ResponsiveChartSection(records: List<DailyRecord>, selectedDate: LocalDate) {
    var chartMetric by remember { mutableStateOf(ChartMetricType.TAVG) }
    var selectedRecordIndex by remember { mutableStateOf<Int?>(null) }

    val sortedRecords = remember(records) {
        records.sortedBy { it.date.take(4).toIntOrNull() ?: 0 }
    }

    val values = remember(sortedRecords, chartMetric) {
        sortedRecords.map { r ->
            when (chartMetric) {
                ChartMetricType.TAVG -> r.tavg ?: 0.0
                ChartMetricType.TMAX -> r.tmax ?: 0.0
                ChartMetricType.TMIN -> r.tmin ?: 0.0
                ChartMetricType.PRCP -> r.prcp ?: 0.0
            }
        }
    }

    val avgValue = values.average().takeIf { !it.isNaN() } ?: 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "📊 Istorijski pregled kroz sve godine na ovaj dan",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ChartMetricType.values()) { metric ->
                FilterChip(
                    selected = chartMetric == metric,
                    onClick = {
                        chartMetric = metric
                        selectedRecordIndex = null
                    },
                    label = { Text(metric.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        // Details card on tap
        val tappedRecord = selectedRecordIndex?.let { sortedRecords.getOrNull(it) }
        if (tappedRecord != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Godina: ${tappedRecord.date.take(4)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Tavg: ${tappedRecord.tavg?.let { "${String.format(Locale.US, "%.1f", it)}°C" } ?: "--"}  •  Kiša: ${tappedRecord.prcp?.let { "${String.format(Locale.US, "%.1f", it)} mm" } ?: "0 mm"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = "Min: ${tappedRecord.tmin ?: "--"}°\nMax: ${tappedRecord.tmax ?: "--"}°",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Canvas Chart
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(sortedRecords) {
                            detectTapGestures { offset ->
                                val count = sortedRecords.size
                                if (count > 0) {
                                    val barSlotWidth = size.width / count
                                    val index = (offset.x / barSlotWidth).toInt().coerceIn(0, count - 1)
                                    selectedRecordIndex = index
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val count = values.size
                        if (count == 0) return@Canvas

                        val minVal = (values.minOrNull() ?: 0.0).coerceAtMost(0.0)
                        val maxVal = (values.maxOrNull() ?: 30.0).coerceAtLeast(minVal + 10.0)
                        val range = (maxVal - minVal).coerceAtLeast(1.0)

                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val slotWidth = canvasWidth / count
                        val barWidth = (slotWidth * 0.7f).coerceIn(4f, 24f)

                        // Baseline avg dashed line
                        val avgY = canvasHeight - ((avgValue - minVal) / range * canvasHeight).toFloat()
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.5f),
                            start = Offset(0f, avgY),
                            end = Offset(canvasWidth, avgY),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )

                        values.forEachIndexed { i, value ->
                            val x = i * slotWidth + (slotWidth - barWidth) / 2
                            val barHeight = ((value - minVal) / range * canvasHeight).toFloat().coerceAtLeast(4f)
                            val y = canvasHeight - barHeight

                            val isSelected = selectedRecordIndex == i
                            val barColor = when {
                                chartMetric == ChartMetricType.PRCP -> Color(0xFF00796B)
                                value >= avgValue -> Color(0xFFE53935)
                                else -> Color(0xFF1E88E5)
                            }

                            drawRoundRect(
                                color = if (isSelected) barColor else barColor.copy(alpha = 0.85f),
                                topLeft = Offset(x, y),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                // Years labels below chart
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = sortedRecords.firstOrNull()?.date?.take(4) ?: "1990",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Prosjek: ${String.format(Locale.US, "%.1f", avgValue)} ${chartMetric.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = sortedRecords.lastOrNull()?.date?.take(4) ?: "Danas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
