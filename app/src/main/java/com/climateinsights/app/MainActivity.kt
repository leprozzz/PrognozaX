package com.climateinsights.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.climateinsights.app.ui.theme.PrognozaXTheme
import com.climateinsights.app.ui.screens.MainScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.climateinsights.app.ui.viewmodels.MainViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import com.climateinsights.app.ui.screens.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
            val useDarkTheme = isDarkTheme ?: isSystemInDarkTheme()
            
            PrognozaXTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = hasCompletedOnboarding,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(350)) togetherWith
                                fadeOut(animationSpec = tween(250))
                        },
                        label = "OnboardingTransition"
                    ) { completed ->
                        if (completed) {
                            MainScreen(viewModel)
                        } else {
                            WelcomeScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}