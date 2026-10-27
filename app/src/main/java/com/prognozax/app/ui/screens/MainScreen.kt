package com.prognozax.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.prognozax.app.ui.navigation.About
import com.prognozax.app.ui.navigation.Monthly
import com.prognozax.app.ui.navigation.OnThisDay
import com.prognozax.app.ui.navigation.Settings
import com.prognozax.app.ui.navigation.Statistics
import com.prognozax.app.ui.navigation.topLevelRoutes
import com.prognozax.app.ui.viewmodels.MainViewModel

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                topLevelRoutes.forEach { topLevelRoute ->
                    NavigationBarItem(
                        icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name) },
                        label = { Text(topLevelRoute.name) },
                        selected = currentDestination?.hierarchy?.any { 
                            it.route?.endsWith(topLevelRoute.route.javaClass.simpleName) == true 
                        } == true,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = OnThisDay,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<OnThisDay> { OnThisDayScreen(viewModel) }
                composable<Monthly> { MonthlyScreen(viewModel) }
                composable<Statistics> { StatisticsScreen(viewModel) }
                composable<Settings> { SettingsScreen(viewModel) }
                composable<About> { AboutScreen() }
            }
        }
    }
}
