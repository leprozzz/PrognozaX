package com.climateinsights.app.data.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class City(
    val id: String,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val csvPath: String,
    val timezone: String,
    val meteostatId: String? = null
)

data class DailyRecord(
    val date: String,
    val tavg: Double?,
    val tmin: Double?,
    val tmax: Double?,
    val prcp: Double?
)

data class ForecastDay(
    val date: String,
    val tmin: Double,
    val tmax: Double,
    val precipitation: Double
)

data class LiveSnapshot(
    val cityId: String,
    val current: Double,
    val todayMax: Double,
    val todayMin: Double,
    val precipitation: Double,
    val forecast: List<ForecastDay>,
    val fetchedAt: String
)