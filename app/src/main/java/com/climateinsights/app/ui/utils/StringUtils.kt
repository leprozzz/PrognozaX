package com.climateinsights.app.ui.utils

import java.time.LocalDate

val MONTH_NAMES_NOMINATIVE = listOf(
    "januar", "februar", "mart", "april", "maj", "juni",
    "juli", "avgust", "septembar", "oktobar", "novembar", "decembar"
)

val MONTH_NAMES_GENITIVE = listOf(
    "januara", "februara", "marta", "aprila", "maja", "juna",
    "jula", "avgusta", "septembra", "oktobra", "novembra", "decembra"
)

val DAY_OF_WEEK_SHORT = listOf("pon", "uto", "sri", "čet", "pet", "sub", "ned")

fun getMonthNameNominative(month: Int): String {
    return if (month in 1..12) MONTH_NAMES_NOMINATIVE[month - 1] else "$month"
}

fun getMonthNameGenitive(month: Int): String {
    return if (month in 1..12) MONTH_NAMES_GENITIVE[month - 1] else "$month"
}

fun formatDanMjesec(date: LocalDate, inGenitive: Boolean = false): String {
    val day = date.dayOfMonth
    val m = if (inGenitive) getMonthNameGenitive(date.monthValue) else getMonthNameNominative(date.monthValue)
    return "$day. $m"
}

fun formatPuniDatum(date: LocalDate): String {
    val day = date.dayOfMonth
    val m = getMonthNameGenitive(date.monthValue)
    val year = date.year
    return "$day. $m $year."
}

fun formatDanSedmiceIDatum(date: LocalDate): String {
    val dayIdx = (date.dayOfWeek.value - 1).coerceIn(0, 6)
    val dayName = DAY_OF_WEEK_SHORT[dayIdx]
    return "$dayName ${date.dayOfMonth}"
}

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
