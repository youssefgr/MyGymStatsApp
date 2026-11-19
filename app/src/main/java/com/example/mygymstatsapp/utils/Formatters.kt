package com.example.mygymstatsapp.utils

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WeightUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

/* Formatting follows the device locale (decimal separator, date order, month names). */

private const val KG_TO_LB = 2.2046226218

private fun locale(): Locale = Locale.getDefault()

// ---- Units -----------------------------------------------------------------------------------

fun WeightUnit.fromKg(kg: Double): Double = if (this == WeightUnit.LB) kg * KG_TO_LB else kg

fun WeightUnit.toKg(value: Double): Double = if (this == WeightUnit.LB) value / KG_TO_LB else value

/** Accepts both "62.5" and "62,5". */
fun parseDecimal(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

// ---- Numbers ---------------------------------------------------------------------------------

/** "62,5" / "62.5", "60" — at most [maxDecimals] decimals. */
fun formatDecimal(value: Double, maxDecimals: Int = 2): String {
    val pattern = if (maxDecimals > 0) "0." + "#".repeat(maxDecimals) else "0"
    return DecimalFormat(pattern, DecimalFormatSymbols(locale())).format(value)
}

/** Weight with unit: "62,5 kg", "61,25 kg". */
fun formatWeight(kg: Double, unit: WeightUnit): String = "${formatDecimal(unit.fromKg(kg), 2)} ${unit.label}"

/** Weight without unit for compact rows: "62,5". */
fun formatWeightValue(kg: Double, unit: WeightUnit): String = formatDecimal(unit.fromKg(kg), 2)

/** Volume rounded with thousands separator: "8 450 kg". */
fun formatVolume(kg: Double, unit: WeightUnit): String =
    "${NumberFormat.getIntegerInstance(locale()).format(unit.fromKg(kg).roundToLong())} ${unit.label}"

/** Compact number for chart axes: "8,4 k" / "950". */
fun formatCompact(value: Double): String =
    if (value >= 1000) "${formatDecimal(value / 1000, 1)} k" else formatDecimal(value, 0)

// ---- Durations -------------------------------------------------------------------------------

/** "52 min", "1 h 05". */
@Composable
fun formatDuration(seconds: Long): String {
    val totalMinutes = (seconds / 60).toInt()
    val hours = totalMinutes / 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours, hours, (totalMinutes % 60).toString().padStart(2, '0'))
    } else {
        stringResource(R.string.duration_minutes, totalMinutes)
    }
}

/** Stopwatch format: "01:32" or "1:05:09". */
fun formatClock(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val mmss = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    return if (hours > 0) "$hours:$mmss" else mmss
}

/** Rest durations: "1:30", "45 s". */
fun formatRest(seconds: Int): String =
    if (seconds < 60) "$seconds s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

// ---- Dates -----------------------------------------------------------------------------------

private fun pattern(skeleton: String): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale(), skeleton), locale())

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

/** "Aujourd'hui", "Hier", "28 septembre", "28 septembre 2025". */
@Composable
fun formatRelativeDate(epochMs: Long): String {
    val date = epochMs.toLocalDate()
    val today = LocalDate.now()
    return when {
        date == today -> stringResource(R.string.today)
        date == today.minusDays(1) -> stringResource(R.string.yesterday)
        date.year == today.year -> pattern("dMMMM").format(date)
        else -> formatDayMonthYear(date)
    }
}

/** "28 septembre 2026". */
fun formatDayMonthYear(date: LocalDate): String = pattern("dMMMMyyyy").format(date)

/** "Vendredi 2 octobre 2026 · 18:30". */
fun formatFullDateTime(epochMs: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale()).format(dateTime)
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale()).format(dateTime)
    return "${date.capitalizeFirst()} · $time"
}

/** "28 sept." for chart axes. */
fun formatShortDate(epochMs: Long): String = pattern("dMMM").format(epochMs.toLocalDate())

/** "Septembre 2026" for list headers. */
fun formatMonthYear(date: LocalDate): String = pattern("MMMMyyyy").format(date).capitalizeFirst()

/** "Vendredi 2 octobre". */
fun formatWeekdayDayMonth(date: LocalDate): String = pattern("EEEEdMMMM").format(date).capitalizeFirst()

fun String.capitalizeFirst(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale()) else it.toString() }
