package com.climateinsights.app.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OpenMeteoCurrent(
    @Json(name = "temperature_2m") val temperature2m: Double,
    @Json(name = "precipitation") val precipitation: Double
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    @Json(name = "time") val time: List<String>,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double>
)

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "current") val current: OpenMeteoCurrent,
    @Json(name = "daily") val daily: OpenMeteoDaily
)

@JsonClass(generateAdapter = true)
data class OpenMeteoArchiveDaily(
    @Json(name = "time") val time: List<String>,
    @Json(name = "temperature_2m_mean") val temperature2mMean: List<Double?>? = null,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double?>? = null,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double?>? = null,
    @Json(name = "precipitation_sum") val precipitationSum: List<Double?>? = null
)

@JsonClass(generateAdapter = true)
data class OpenMeteoArchiveResponse(
    @Json(name = "daily") val daily: OpenMeteoArchiveDaily
)

interface OpenMeteoApi {
    @GET("v1/forecast")
    suspend fun getForecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,precipitation",
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse

    @GET("https://archive-api.open-meteo.com/v1/archive")
    suspend fun getArchive(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "temperature_2m_mean,temperature_2m_max,temperature_2m_min,precipitation_sum",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoArchiveResponse
}
