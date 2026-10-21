package com.climateinsights.app.ui.utils

fun formatGodina(count: Int): String {
    val lastDigit = count % 10
    val lastTwoDigits = count % 100
    
    if (lastTwoDigits in 11..14) return "$count godina"
    return when (lastDigit) {
        1 -> "$count godina" 
        2, 3, 4 -> "$count godine"
        else -> "$count godina"
    }
}
