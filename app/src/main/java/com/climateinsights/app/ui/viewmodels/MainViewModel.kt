package com.climateinsights.app.ui.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.climateinsights.app.data.models.City
import com.climateinsights.app.data.models.DailyRecord
import com.climateinsights.app.data.models.LiveSnapshot
import com.climateinsights.app.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers

data class DailyComparison(
    val currentValue: Double,
    val historicalAverage: Double,
    val deltaFromAverage: Double,
    val isRecordCandidate: Boolean,
    val allTimeMax: RecordValue?,
    val allTimeMin: RecordValue?,
    val sampleSize: Int
)

data class RecordValue(val value: Double, val year: Int)

data class AnalysisResult(
    val date: LocalDate,
    val hasObservedData: Boolean,
    val observedTavg: Double?,
    val observedTmax: Double?,
    val observedTmin: Double?,
    val observedPrcp: Double?,
    val maxPrecipitationEver: RecordValue?,
    val historicalAvgTemp: Double?,
    val historicalRecordsForDay: List<DailyRecord>,
    val tmaxComparison: DailyComparison?,
    val tminComparison: DailyComparison?,
    val tavgComparison: DailyComparison?,
    val coldestForDay: DailyRecord?
)

sealed class MainUiState {
    object Loading : MainUiState()
    data class Success(
        val cities: List<City>,
        val selectedCity: City,
        val selectedDate: LocalDate,
        val isToday: Boolean,
        val liveSnapshot: LiveSnapshot?,
        val analysis: AnalysisResult?,
        val allHistoricalRecords: List<DailyRecord>,
        val isSyncing: Boolean = false
    ) : MainUiState()
    data class Error(val message: String) : MainUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    private val prefs = application.getSharedPreferences("prognozax_prefs", Context.MODE_PRIVATE)

    private val _hasCompletedOnboarding = MutableStateFlow(
        prefs.getBoolean("has_completed_onboarding", false)
    )
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _isDarkTheme = MutableStateFlow<Boolean?>(null)
    val isDarkTheme: StateFlow<Boolean?> = _isDarkTheme.asStateFlow()

    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var cities: List<City> = emptyList()
    private var selectedCity: City? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private var liveSnapshot: LiveSnapshot? = null

    init {
        viewModelScope.launch {
            try {
                cities = repository.getCities()
                val savedCityId = prefs.getString("default_city_id", null)
                selectedCity = cities.find { it.id == savedCityId } ?: cities.firstOrNull()
                if (selectedCity != null) {
                    loadData()
                } else {
                    _uiState.value = MainUiState.Error("Nema dostupnih gradova")
                }
            } catch (e: Exception) {
                _uiState.value = MainUiState.Error(e.message ?: "Greška pri učitavanju gradova")
            }
        }
    }

    fun completeOnboarding(city: City) {
        selectedCity = city
        prefs.edit()
            .putBoolean("has_completed_onboarding", true)
            .putString("default_city_id", city.id)
            .apply()
        _hasCompletedOnboarding.value = true
        viewModelScope.launch {
            loadData()
        }
    }

    fun resetOnboarding() {
        prefs.edit()
            .putBoolean("has_completed_onboarding", false)
            .apply()
        _hasCompletedOnboarding.value = false
    }

    fun selectCity(city: City) {
        selectedCity = city
        prefs.edit()
            .putString("default_city_id", city.id)
            .apply()
        viewModelScope.launch {
            loadData()
        }
    }

    fun setDarkTheme(isDark: Boolean?) {
        _isDarkTheme.value = isDark
    }

    fun selectDate(date: LocalDate) {
        selectedDate = date
        viewModelScope.launch {
            loadData()
        }
    }

    private var currentSyncJob: kotlinx.coroutines.Job? = null

    fun syncCurrentCityData() {
        val city = selectedCity ?: return
        val currentState = _uiState.value
        if (currentState is MainUiState.Success) {
            _uiState.value = currentState.copy(isSyncing = true)
        }
        currentSyncJob?.cancel()
        currentSyncJob = viewModelScope.launch {
            try {
                val updatedRecords = withContext(Dispatchers.IO) {
                    repository.smartSyncHistoricalData(city.id) ?: repository.getLocalHistoricalDataFast(city.id)
                }
                updateUiWithNewRecords(city.id, updatedRecords)
            } catch (e: Exception) {
                e.printStackTrace()
                val state = _uiState.value
                if (state is MainUiState.Success) {
                    _uiState.value = state.copy(isSyncing = false)
                }
            }
        }
    }

    private suspend fun loadData() {
        val city = selectedCity ?: return
        _uiState.value = MainUiState.Loading
        try {
            val isToday = selectedDate == LocalDate.now()
            
            if (isToday) {
                try {
                    liveSnapshot = repository.getLiveSnapshot(city.id)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            val localRecords = repository.getLocalHistoricalDataFast(city.id)
            val month = selectedDate.monthValue
            val day = selectedDate.dayOfMonth
            
            val recordsForDay = localRecords.filter { record ->
                try {
                    val recordDate = LocalDate.parse(record.date)
                    recordDate.monthValue == month && recordDate.dayOfMonth == day
                } catch (e: Exception) {
                    false
                }
            }
            
            val analysis = analyze(recordsForDay, isToday)
            
            _uiState.value = MainUiState.Success(
                cities = cities,
                selectedCity = city,
                selectedDate = selectedDate,
                isToday = isToday,
                liveSnapshot = liveSnapshot,
                analysis = analysis,
                allHistoricalRecords = localRecords,
                isSyncing = false
            )

            triggerSmartSync(city.id, isToday, month, day)

        } catch (e: Exception) {
            _uiState.value = MainUiState.Error(e.message ?: "Greška pri učitavanju podataka")
        }
    }

    private fun triggerSmartSync(cityId: String, isToday: Boolean, month: Int, day: Int) {
        currentSyncJob?.cancel()
        currentSyncJob = viewModelScope.launch {
            val syncDeferred = async(Dispatchers.IO) {
                repository.smartSyncHistoricalData(cityId)
            }

            // Wait 5 seconds, if still running, show syncing indicator
            delay(5000)
            if (syncDeferred.isActive) {
                val state = _uiState.value
                if (state is MainUiState.Success && state.selectedCity.id == cityId) {
                    _uiState.value = state.copy(isSyncing = true)
                }
            }

            val updatedRecords = syncDeferred.await()
            if (updatedRecords != null) {
                updateUiWithNewRecords(cityId, updatedRecords)
            } else {
                val state = _uiState.value
                if (state is MainUiState.Success && state.selectedCity.id == cityId) {
                    _uiState.value = state.copy(isSyncing = false)
                }
            }
        }
    }

    private fun updateUiWithNewRecords(cityId: String, updatedRecords: List<DailyRecord>) {
        val state = _uiState.value
        if (state is MainUiState.Success && state.selectedCity.id == cityId) {
            val isToday = selectedDate == LocalDate.now()
            val month = selectedDate.monthValue
            val day = selectedDate.dayOfMonth
            val recordsForDay = updatedRecords.filter { record ->
                try {
                    val recordDate = LocalDate.parse(record.date)
                    recordDate.monthValue == month && recordDate.dayOfMonth == day
                } catch (e: Exception) {
                    false
                }
            }
            val newAnalysis = analyze(recordsForDay, isToday)
            _uiState.value = state.copy(
                analysis = newAnalysis,
                allHistoricalRecords = updatedRecords,
                isSyncing = false
            )
        }
    }

    private fun analyze(recordsForDay: List<DailyRecord>, isToday: Boolean): AnalysisResult {
        val observedRecord = recordsForDay.find { it.date == selectedDate.toString() }
        
        val observedTavg = if (isToday) liveSnapshot?.current else observedRecord?.tavg
        val observedTmax = if (isToday) liveSnapshot?.todayMax else observedRecord?.tmax
        val observedTmin = if (isToday) liveSnapshot?.todayMin else observedRecord?.tmin
        val observedPrcp = if (isToday) liveSnapshot?.precipitation else observedRecord?.prcp

        val hasObservedData = observedTavg != null || observedTmax != null || observedTmin != null

        val tavgComparison = observedTavg?.let { getComparison(it, recordsForDay, { it.tavg }) }
        val tmaxComparison = observedTmax?.let { getComparison(it, recordsForDay, { it.tmax }) }
        val tminComparison = observedTmin?.let { getComparison(it, recordsForDay, { it.tmin }) }

        var maxPrecip: RecordValue? = null
        recordsForDay.forEach { r ->
            if (r.prcp != null) {
                val year = r.date.take(4).toIntOrNull() ?: 0
                if (maxPrecip == null || r.prcp > maxPrecip!!.value) {
                    maxPrecip = RecordValue(r.prcp, year)
                }
            }
        }

        val coldestForDay = recordsForDay.minByOrNull { it.tmin ?: Double.MAX_VALUE }?.takeIf { it.tmin != null }

        val historicalAvgTemp = recordsForDay.mapNotNull { it.tavg }.average().takeIf { !it.isNaN() }

        return AnalysisResult(
            date = selectedDate,
            hasObservedData = hasObservedData,
            observedTavg = observedTavg,
            observedTmax = observedTmax,
            observedTmin = observedTmin,
            observedPrcp = observedPrcp,
            maxPrecipitationEver = maxPrecip,
            historicalAvgTemp = historicalAvgTemp,
            historicalRecordsForDay = recordsForDay,
            tmaxComparison = tmaxComparison,
            tminComparison = tminComparison,
            tavgComparison = tavgComparison,
            coldestForDay = coldestForDay
        )
    }

    private fun getComparison(
        current: Double,
        records: List<DailyRecord>,
        selector: (DailyRecord) -> Double?
    ): DailyComparison {
        val values = records.mapNotNull { selector(it) }
        val avg = values.average().takeIf { !it.isNaN() } ?: current
        val delta = current - avg
        
        var max: RecordValue? = null
        var min: RecordValue? = null
        
        records.forEach { r ->
            val v = selector(r)
            if (v != null) {
                val year = r.date.take(4).toIntOrNull() ?: 0
                if (max == null || v > max!!.value) {
                    max = RecordValue(v, year)
                }
                if (min == null || v < min!!.value) {
                    min = RecordValue(v, year)
                }
            }
        }

        val isRecord = max?.let { current >= it.value - 0.5 } ?: false

        return DailyComparison(
            currentValue = current,
            historicalAverage = avg,
            deltaFromAverage = delta,
            isRecordCandidate = isRecord,
            allTimeMax = max,
            allTimeMin = min,
            sampleSize = values.size
        )
    }
}
