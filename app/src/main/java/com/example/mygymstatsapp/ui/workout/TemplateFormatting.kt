package com.example.mygymstatsapp.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WorkoutTemplate

/** "6 exercices · 20 séries". */
@Composable
fun templateSummary(template: WorkoutTemplate): String =
    pluralStringResource(R.plurals.count_exercises, template.exercises.size, template.exercises.size) + " · " +
        pluralStringResource(R.plurals.count_sets, template.totalSets, template.totalSets)

/** "8-12", "10" or null. */
fun repsRangeLabel(repsMin: Int?, repsMax: Int?): String? = when {
    repsMin != null && repsMax != null && repsMin != repsMax -> "$repsMin-$repsMax"
    repsMin != null -> "$repsMin"
    repsMax != null -> "$repsMax"
    else -> null
}

/** "4 × 8-12", "3 × 10", "3 séries", "8-12 reps". */
@Composable
fun targetLabel(sets: Int?, repsMin: Int?, repsMax: Int?): String {
    val reps = repsRangeLabel(repsMin, repsMax)
    return when {
        sets != null && reps != null -> "$sets × $reps"
        sets != null -> pluralStringResource(R.plurals.count_sets, sets, sets)
        reps != null -> stringResource(R.string.value_reps_decimal, reps)
        else -> ""
    }
}
