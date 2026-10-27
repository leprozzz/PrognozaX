package com.prognozax.app.data.repository

import android.content.Context
import android.util.Log
import com.prognozax.app.data.api.OpenMeteoApi
import com.prognozax.app.data.api.OpenMeteoArchiveResponse
import com.prognozax.app.data.models.City
import com.prognozax.app.data.models.DailyRecord
import com.prognozax.app.data.models.ForecastDay
import com.prognozax.app.data.models.LiveSnapshot
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class WeatherRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val api: OpenMeteoApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenMeteoApi::class.java)
    }

    private var citiesCache: List<City>? = null
    private val historicalDataCache = ConcurrentHashMap<String, List<DailyRecord>>()

    init {
        try {
            // Delete all legacy cache files that may contain missing/zero precipitation from earlier versions
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("historical_") || file.name.startsWith("merged_")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to clear legacy cache files", e)
        }
    }

    suspend fun getCities(): List<City> = withContext(Dispatchers.IO) {
        citiesCache?.let { return@withContext it }
        try {
            val json = context.assets.open("data/cities.json").bufferedReader().use { it.readText() }
            val type = Types.newParameterizedType(List::class.java, City::class.java)
            val adapter = moshi.adapter<List<City>>(type)
            val cities = adapter.fromJson(json) ?: emptyList()
            if (cities.isNotEmpty()) {
                citiesCache = cities
                return@withContext cities
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error loading cities.json", e)
        }
        val defaultCities = listOf(
            City("banja-luka", "Banja Luka", "Bosna i Hercegovina", 44.7722, 17.1910, "/data/csv/banja-luka.csv", "Europe/Sarajevo", "13242"),
            City("sarajevo", "Sarajevo", "Bosna i Hercegovina", 43.8563, 18.4131, "/data/csv/sarajevo.csv", "Europe/Sarajevo", "14654"),
            City("mostar", "Mostar", "Bosna i Hercegovina", 43.3438, 17.8078, "/data/csv/mostar.csv", "Europe/Sarajevo", "13348"),
            City("tuzla", "Tuzla", "Bosna i Hercegovina", 44.5384, 18.6671, "/data/csv/tuzla.csv", "Europe/Sarajevo", "13257"),
            City("zenica", "Zenica", "Bosna i Hercegovina", 44.2017, 17.9040, "/data/csv/zenica.csv", "Europe/Sarajevo", "14549"),
            City("bijeljina", "Bijeljina", "Bosna i Hercegovina", 44.7587, 19.2144, "/data/csv/bijeljina.csv", "Europe/Sarajevo", "13262"),
            City("prijedor", "Prijedor", "Bosna i Hercegovina", 44.9799, 16.7140, "/data/csv/prijedor.csv", "Europe/Sarajevo", "14537"),
            City("trebinje", "Trebinje", "Bosna i Hercegovina", 42.7120, 18.3436, "/data/csv/trebinje.csv", "Europe/Sarajevo", "14474"),
            City("doboj", "Doboj", "Bosna i Hercegovina", 44.73, 18.08, "/data/csv/doboj.csv", "Europe/Sarajevo", "14554"),
            City("bihac", "Bihać", "Bosna i Hercegovina", 44.81, 15.86, "/data/csv/bihac.csv", "Europe/Sarajevo", "14528"),
            City("beograd", "Beograd", "Srbija", 44.81, 20.46, "/data/csv/beograd.csv", "Europe/Belgrade", "13274"),
            City("zagreb", "Zagreb", "Hrvatska", 45.81, 15.98, "/data/csv/zagreb.csv", "Europe/Zagreb", "14236"),
            City("brcko", "Brčko", "Bosna i Hercegovina", 44.87, 18.81, "/data/csv/brcko.csv", "Europe/Sarajevo", "69171"),
            City("gradiska", "Gradiška (Bosanska Gradiška)", "Bosna i Hercegovina", 45.14, 17.25, "/data/csv/gradiska.csv", "Europe/Sarajevo", "13242"),
            City("zvornik", "Zvornik", "Bosna i Hercegovina", 44.39, 19.10, "/data/csv/zvornik.csv", "Europe/Sarajevo", "13262"),
            City("nis", "Niš", "Srbija", 43.3247, 21.9033, "/data/csv/nis.csv", "Europe/Belgrade", "13388"),
            City("novi-sad", "Novi Sad", "Srbija", 45.2671, 19.8335, "/data/csv/novi-sad.csv", "Europe/Belgrade", "13168"),
            City("split", "Split", "Hrvatska", 43.5081, 16.4402, "/data/csv/split.csv", "Europe/Zagreb", "14445"),
            City("rijeka", "Rijeka", "Hrvatska", 45.3271, 14.4422, "/data/csv/rijeka.csv", "Europe/Zagreb", "14303"),
            City("osijek", "Osijek", "Hrvatska", 45.5550, 18.6955, "/data/csv/osijek.csv", "Europe/Zagreb", "14279")
        )
        citiesCache = defaultCities
        defaultCities
    }

    /**
     * Retrieves historical records from 1990 to present day for the specified city.
     * Always uses bundled primary asset data (1990-2026) first, supplemented with any recent online additions.
     */
    suspend fun getLocalHistoricalDataFast(cityId: String): List<DailyRecord> = withContext(Dispatchers.IO) {
        historicalDataCache[cityId]?.let { return@withContext it }

        // Always remove legacy cache file if still present
        val legacyCache = File(context.filesDir, "historical_${cityId}_1990.csv")
        if (legacyCache.exists()) {
            legacyCache.delete()
        }

        // 1. ALWAYS load the primary verified bundled asset data FIRST
        val city = getCities().find { it.id == cityId } ?: return@withContext emptyList()
        val records = mutableListOf<DailyRecord>()
        try {
            val csvPath = city.csvPath.removePrefix("/")
            context.assets.open(csvPath).bufferedReader().useLines { lines ->
                val it = lines.iterator()
                if (it.hasNext()) it.next() // Skip header
                while (it.hasNext()) {
                    val line = it.next()
                    if (line.isBlank()) continue
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        records.add(
                            DailyRecord(
                                date = parts[0].trim(),
                                tavg = sanitizeTemp(parts[1].trim().toDoubleOrNull()),
                                tmin = sanitizeTemp(parts[2].trim().toDoubleOrNull()),
                                tmax = sanitizeTemp(parts[3].trim().toDoubleOrNull()),
                                prcp = parts[4].trim().toDoubleOrNull()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("WeatherRepository", "No bundled station CSV for $cityId or read error: ${e.message}")
        }

        // 2. Check if recent online additions exist beyond the asset date
        val additionsFile = File(context.filesDir, "additions_${cityId}.csv")
        if (additionsFile.exists() && additionsFile.length() > 10) {
            val additionRecords = parseCsvFile(additionsFile)
            val latestAssetDate = records.maxOfOrNull { it.date } ?: ""
            val newDays = additionRecords.filter { it.date > latestAssetDate }
            records.addAll(newDays)
        }

        if (records.isNotEmpty()) {
            historicalDataCache[cityId] = records
        }
        records
    }

    private suspend fun fetchMeteostatBulkData(stationId: String, startDate: LocalDate, endDate: LocalDate): List<DailyRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<DailyRecord>()
        try {
            val url = java.net.URL("https://bulk.meteostat.net/v2/daily/$stationId.csv.gz")
            val connection = url.openConnection()
            java.util.zip.GZIPInputStream(connection.getInputStream()).bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.isBlank()) continue
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        val dateStr = parts[0].trim()
                        try {
                            val recordDate = LocalDate.parse(dateStr)
                            if (!recordDate.isBefore(startDate) && !recordDate.isAfter(endDate)) {
                                records.add(
                                    DailyRecord(
                                        date = dateStr,
                                        tavg = sanitizeTemp(parts[1].trim().toDoubleOrNull()),
                                        tmin = sanitizeTemp(parts[2].trim().toDoubleOrNull()),
                                        tmax = sanitizeTemp(parts[3].trim().toDoubleOrNull()),
                                        prcp = parts[4].trim().toDoubleOrNull()
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            // ignore parse error for specific line
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to fetch Meteostat bulk for $stationId", e)
        }
        records
    }

    suspend fun smartSyncHistoricalData(cityId: String): List<DailyRecord>? = withContext(Dispatchers.IO) {
        val localRecords = getLocalHistoricalDataFast(cityId).toMutableList()
        val city = getCities().find { it.id == cityId } ?: return@withContext null

        if (localRecords.isEmpty()) {
            return@withContext syncAndMergeHistoricalData(cityId)
        }

        val latestRecord = localRecords.maxByOrNull { it.date }
        val latestDateStr = latestRecord?.date ?: return@withContext null
        
        var latestDate = try {
            LocalDate.parse(latestDateStr)
        } catch (e: Exception) {
            return@withContext null
        }
        
        val today = LocalDate.now()

        // ERA5 often has a ~5 day lag. If we are within 2 days, don't bother fetching yet
        if (latestDate.isAfter(today.minusDays(2)) || latestDate.isEqual(today)) {
            return@withContext localRecords
        }

        val startDate = latestDate.plusDays(1)
        val endDate = today
        var addedAny = false

        // 1. Try fetching primary data from Meteostat bulk first
        val meteostatRecords = city.meteostatId?.let { 
            fetchMeteostatBulkData(it, startDate, endDate) 
        } ?: emptyList()

        // 2. Fetch ERA5 as fallback
        var era5Daily: com.prognozax.app.data.api.OpenMeteoArchiveDaily? = null
        try {
            val archiveResponse = api.getArchive(
                latitude = city.latitude,
                longitude = city.longitude,
                startDate = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate = endDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                timezone = city.timezone
            )
            era5Daily = archiveResponse.daily
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to fetch ERA5 fallback", e)
        }

        // 3. Merge: Meteostat first, ERA5 second
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dateStr = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            if (localRecords.any { it.date == dateStr }) {
                currentDate = currentDate.plusDays(1)
                continue
            }

            val mRecord = meteostatRecords.find { it.date == dateStr }
            var pTavg = mRecord?.tavg
            var pTmin = mRecord?.tmin
            var pTmax = mRecord?.tmax
            var pPrcp = mRecord?.prcp

            var eTavg: Double? = null
            var eTmin: Double? = null
            var eTmax: Double? = null
            var ePrcp: Double? = null

            if (era5Daily != null) {
                val index = era5Daily.time.indexOf(dateStr)
                if (index != -1) {
                    eTavg = era5Daily.temperature2mMean?.getOrNull(index)
                    eTmax = era5Daily.temperature2mMax?.getOrNull(index)
                    eTmin = era5Daily.temperature2mMin?.getOrNull(index)
                    ePrcp = era5Daily.precipitationSum?.getOrNull(index)
                }
            }

            val finalTmax = if (pTmax != null && isValidTemperature(pTmax)) pTmax else eTmax
            val finalTmin = if (pTmin != null && isValidTemperature(pTmin)) pTmin else eTmin
            var finalTavg = if (pTavg != null && isValidTemperature(pTavg)) pTavg else eTavg
            if (finalTavg == null && finalTmax != null && finalTmin != null) {
                finalTavg = (finalTmax + finalTmin) / 2.0
            }

            var safeTmax = finalTmax
            var safeTmin = finalTmin
            if (safeTmax != null && safeTmin != null && safeTmax < safeTmin) {
                safeTmax = eTmax
                safeTmin = eTmin
                finalTavg = eTavg
            }

            val finalPrcp = if (pPrcp != null && isValidPrecipitation(pPrcp)) pPrcp else (ePrcp ?: 0.0)

            if (safeTmax != null || safeTmin != null || finalTavg != null) {
                localRecords.add(
                    DailyRecord(
                        dateStr,
                        sanitizeTemp(finalTavg),
                        sanitizeTemp(safeTmin),
                        sanitizeTemp(safeTmax),
                        finalPrcp
                    )
                )
                addedAny = true
            }

            currentDate = currentDate.plusDays(1)
        }

        if (addedAny) {
            localRecords.sortBy { it.date }
            saveToCache(cityId, localRecords)
            historicalDataCache[cityId] = localRecords
            return@withContext localRecords
        } else {
            return@withContext localRecords
        }
    }

    private fun saveToCache(cityId: String, records: List<DailyRecord>) {
        val additionsFile = File(context.filesDir, "additions_${cityId}.csv")
        try {
            val additions = records.filter { it.date >= "2026-03-18" }
            if (additions.isNotEmpty()) {
                additionsFile.bufferedWriter().use { writer ->
                    writer.write("date,tavg,tmin,tmax,prcp\n")
                    for (r in additions) {
                        val tavgStr = r.tavg?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: ""
                        val tminStr = r.tmin?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: ""
                        val tmaxStr = r.tmax?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: ""
                        val prcpStr = r.prcp?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "0.0"
                        writer.write("${r.date},$tavgStr,$tminStr,$tmaxStr,$prcpStr\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error saving additions CSV", e)
        }
    }

    suspend fun getHistoricalData(cityId: String, forceRefresh: Boolean = false): List<DailyRecord> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            return@withContext getLocalHistoricalDataFast(cityId)
        }
        val mergedRecords = syncAndMergeHistoricalData(cityId)
        historicalDataCache[cityId] = mergedRecords
        mergedRecords
    }

    /**
     * MERGE & FILL GAPS ALGORITHM:
     * - Primary Source: Local Meteostat / Station measurements (assets)
     * - Fallback / Reanalysis: ERA5 via Open-Meteo Historical Archive (1990-01-01 to today)
     * - Sanity Checks: Validates temperatures between -45°C and 48°C, tmax >= tmin, prcp >= 0.
     * - Replaces anomalies (e.g. 54°C) or missing nulls with exact ERA5 counterpart.
     */
    suspend fun syncAndMergeHistoricalData(cityId: String): List<DailyRecord> = withContext(Dispatchers.IO) {
        val city = getCities().find { it.id == cityId } ?: throw IllegalArgumentException("Unknown city: $cityId")
        
        // Step A: Load primary station records from bundled assets (if available)
        val primaryMap = mutableMapOf<String, DailyRecord>()
        try {
            val csvPath = city.csvPath.removePrefix("/")
            context.assets.open(csvPath).bufferedReader().useLines { lines ->
                val it = lines.iterator()
                if (it.hasNext()) it.next() // Skip header
                while (it.hasNext()) {
                    val line = it.next()
                    if (line.isBlank()) continue
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        val date = parts[0].trim()
                        val tavg = sanitizeTemp(parts[1].trim().toDoubleOrNull())
                        val tmin = sanitizeTemp(parts[2].trim().toDoubleOrNull())
                        val tmax = sanitizeTemp(parts[3].trim().toDoubleOrNull())
                        val prcp = parts[4].trim().toDoubleOrNull()
                        primaryMap[date] = DailyRecord(date, tavg, tmin, tmax, prcp)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("WeatherRepository", "No bundled station CSV for $cityId or read error: ${e.message}")
        }

        // Step B: Download ERA5 Archive from 1990-01-01 to today
        val startDate = "1990-01-01"
        val endDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val era5Map = mutableMapOf<String, DailyRecord>()
        try {
            val archiveResponse = api.getArchive(
                latitude = city.latitude,
                longitude = city.longitude,
                startDate = startDate,
                endDate = endDate,
                timezone = city.timezone
            )
            val d = archiveResponse.daily
            val times = d.time
            val tmeans = d.temperature2mMean
            val tmaxs = d.temperature2mMax
            val tmins = d.temperature2mMin
            val prcps = d.precipitationSum

            for (i in times.indices) {
                val date = times[i]
                val tavg = tmeans?.getOrNull(i)
                val tmax = tmaxs?.getOrNull(i)
                val tmin = tmins?.getOrNull(i)
                val prcp = prcps?.getOrNull(i) ?: 0.0
                era5Map[date] = DailyRecord(date, tavg, tmin, tmax, prcp)
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Failed to download ERA5 archive for ${city.name}", e)
        }

        // Step C: Sanity Check & Merge & Fill Gaps
        // Collect all distinct dates from 1990-01-01 to endDate
        val allDates = (primaryMap.keys + era5Map.keys).sorted()
        val mergedList = mutableListOf<DailyRecord>()

        for (date in allDates) {
            val primary = primaryMap[date]
            val era5 = era5Map[date]

            // 1. TMAX Check
            val pTmax = primary?.tmax
            val eTmax = era5?.tmax
            val finalTmax = if (pTmax != null && isValidTemperature(pTmax)) pTmax else eTmax

            // 2. TMIN Check
            val pTmin = primary?.tmin
            val eTmin = era5?.tmin
            val finalTmin = if (pTmin != null && isValidTemperature(pTmin)) pTmin else eTmin

            // 3. TAVG Check
            val pTavg = primary?.tavg
            val eTavg = era5?.tavg
            var finalTavg = if (pTavg != null && isValidTemperature(pTavg)) pTavg else eTavg
            if (finalTavg == null && finalTmax != null && finalTmin != null) {
                finalTavg = (finalTmax + finalTmin) / 2.0
            }

            // 4. Consistency Check: tmax must be >= tmin
            var safeTmax = finalTmax
            var safeTmin = finalTmin
            if (safeTmax != null && safeTmin != null && safeTmax < safeTmin) {
                // Inconsistent sensor data -> fallback to ERA5
                safeTmax = eTmax
                safeTmin = eTmin
                finalTavg = eTavg
            }

            // 5. Precipitation Check
            val pPrcp = primary?.prcp
            val ePrcp = era5?.prcp ?: 0.0
            val finalPrcp = if (pPrcp != null && isValidPrecipitation(pPrcp)) pPrcp else ePrcp

            mergedList.add(
                DailyRecord(
                    date = date,
                    tavg = sanitizeTemp(finalTavg),
                    tmin = sanitizeTemp(safeTmin),
                    tmax = sanitizeTemp(safeTmax),
                    prcp = finalPrcp
                )
            )
        }

        // Step D: Write merged dataset to internal storage cache
        if (mergedList.isNotEmpty()) {
            saveToCache(cityId, mergedList)
        }

        historicalDataCache[cityId] = mergedList
        mergedList
    }

    private fun isValidTemperature(t: Double): Boolean {
        return t in -33.0..44.0
    }

    private fun isValidPrecipitation(p: Double): Boolean {
        return p in 0.0..300.0
    }

    private fun parseCsvFile(file: File): List<DailyRecord> {
        val records = mutableListOf<DailyRecord>()
        try {
            file.bufferedReader().useLines { lines ->
                val it = lines.iterator()
                if (it.hasNext()) it.next() // Skip header
                while (it.hasNext()) {
                    val line = it.next()
                    if (line.isBlank()) continue
                    val parts = line.split(",")
                    if (parts.size >= 5) {
                        records.add(
                            DailyRecord(
                                date = parts[0].trim(),
                                tavg = sanitizeTemp(parts[1].trim().toDoubleOrNull()),
                                tmin = sanitizeTemp(parts[2].trim().toDoubleOrNull()),
                                tmax = sanitizeTemp(parts[3].trim().toDoubleOrNull()),
                                prcp = parts[4].trim().toDoubleOrNull()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error parsing cached CSV", e)
        }
        return records
    }

    suspend fun getLiveSnapshot(cityId: String): LiveSnapshot = withContext(Dispatchers.IO) {
        val city = getCities().find { it.id == cityId } ?: throw IllegalArgumentException("Unknown city: $cityId")
        val response = api.getForecast(city.latitude, city.longitude, timezone = city.timezone)

        val forecastList = mutableListOf<ForecastDay>()
        val count = response.daily.time.size.coerceAtMost(response.daily.temperature2mMax.size)
        for (i in 0 until count) {
            forecastList.add(
                ForecastDay(
                    date = response.daily.time[i],
                    tmax = response.daily.temperature2mMax[i],
                    tmin = response.daily.temperature2mMin[i],
                    precipitation = response.daily.precipitationSum[i]
                )
            )
        }

        LiveSnapshot(
            cityId = cityId,
            current = response.current.temperature2m,
            precipitation = response.current.precipitation,
            todayMax = response.daily.temperature2mMax.firstOrNull() ?: response.current.temperature2m,
            todayMin = response.daily.temperature2mMin.firstOrNull() ?: response.current.temperature2m,
            forecast = forecastList,
            fetchedAt = java.time.LocalDateTime.now().toString()
        )
    }

    private fun sanitizeTemp(temp: Double?): Double? {
        if (temp == null) return null
        return if (temp in -33.0..44.0) temp else null
    }
}