package com.climateinsights.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Today
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
object OnThisDay

@Serializable
object Monthly

@Serializable
object Statistics

@Serializable
object Settings

@Serializable
object About

data class TopLevelRoute<T : Any>(val name: String, val route: T, val icon: ImageVector)

val topLevelRoutes = listOf(
    TopLevelRoute("Dnevne", OnThisDay, Icons.Default.Today),
    TopLevelRoute("Mjesečno", Monthly, Icons.Default.CalendarMonth),
    TopLevelRoute("Statistika", Statistics, Icons.Default.Timeline),
    TopLevelRoute("Postavke", Settings, Icons.Default.Settings),
    TopLevelRoute("O aplikaciji", About, Icons.Default.Info)
)