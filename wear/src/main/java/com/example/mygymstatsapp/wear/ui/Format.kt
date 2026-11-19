package com.example.mygymstatsapp.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.mygymstatsapp.shared.PastSet
import com.example.mygymstatsapp.shared.WearMuscle
import com.example.mygymstatsapp.shared.WearTracking
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.wear.R
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

/* Same formats as the phone (utils/Formatters.kt), following the device locale. */

private fun locale(): Locale = Locale.getDefault()

fun WearUnit.fromKg(kg: Double): Double = kg * perKg

fun WearUnit.toKg(value: Double): Double = value / perKg

/** "62,5", "60": at most [maxDecimals] decimals. */
fun formatDecimal(value: Double, maxDecimals: Int = 2): String {
    val pattern = if (maxDecimals > 0) "0." + "#".repeat(maxDecimals) else "0"
    return DecimalFormat(pattern, DecimalFormatSymbols(locale())).format(value)
}

fun formatWeight(kg: Double, unit: WearUnit): String = "${formatDecimal(unit.fromKg(kg))} ${unit.label}"

/** "8 450 kg". */
fun formatVolume(kg: Double, unit: WearUnit): String =
    "${NumberFormat.getIntegerInstance(locale()).format(unit.fromKg(kg).roundToLong())} ${unit.label}"

/** "01:32" or "1:05:09". */
fun formatClock(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3600
    val mmss = "${((seconds % 3600) / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
    return if (hours > 0) "$hours:$mmss" else mmss
}

/** Rest or timed-set durations: "1:30", "45 s". */
fun formatRest(seconds: Int): String =
    if (seconds < 60) "$seconds s" else "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

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

/** "Aujourd'hui", "Hier", "28 sept.". */
@Composable
fun formatRelativeDate(epochMs: Long): String {
    val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale()).format(date)
    }
}

/** One set as on the phone: "80 × 8", "12 reps", "1:30". */
fun setText(weightKg: Double?, reps: Int?, durationSeconds: Int?, tracking: WearTracking, unit: WearUnit): String =
    when (tracking) {
        WearTracking.WEIGHT_REPS -> "${formatDecimal(unit.fromKg(weightKg ?: 0.0))} × ${reps ?: 0}"
        WearTracking.REPS_ONLY -> "${reps ?: 0}"
        WearTracking.DURATION -> formatRest(durationSeconds ?: 0)
    }

fun PastSet.text(tracking: WearTracking, unit: WearUnit): String = setText(weightKg, reps, durationSeconds, tracking, unit)

/** Two-letter badge as on the phone (first letters of the French name). */
fun WearMuscle.badge(): String = when (this) {
    WearMuscle.CHEST -> "PE"
    WearMuscle.BACK -> "DO"
    WearMuscle.SHOULDERS -> "ÉP"
    WearMuscle.BICEPS -> "BI"
    WearMuscle.TRICEPS -> "TR"
    WearMuscle.LEGS -> "JA"
    WearMuscle.GLUTES -> "FE"
    WearMuscle.CALVES -> "MO"
    WearMuscle.ABS -> "AB"
    WearMuscle.FOREARMS -> "AV"
    WearMuscle.CARDIO -> "CA"
}
