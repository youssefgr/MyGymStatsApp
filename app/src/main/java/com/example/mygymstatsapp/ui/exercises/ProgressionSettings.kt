package com.example.mygymstatsapp.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.RepRange
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.utils.formatDecimal
import com.example.mygymstatsapp.utils.formatWeight
import com.example.mygymstatsapp.utils.toKg
import kotlin.math.abs

/** Load steps offered for progressive overload, in the display unit. */
fun loadStepOptions(unit: WeightUnit): List<Double> =
    if (unit == WeightUnit.KG) listOf(1.25, 2.5, 5.0) else listOf(2.5, 5.0, 10.0)

/**
 * Progressive overload of one exercise: on/off, load step and what the rule will do.
 * [range] is the rep range in use, null when unknown; [showMissingRange] explains why nothing moves without one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgressionSettings(
    exercise: Exercise,
    defaultIncrementKg: Double,
    unit: WeightUnit,
    range: RepRange?,
    onChange: (enabled: Boolean, incrementKg: Double?) -> Unit,
    modifier: Modifier = Modifier,
    showMissingRange: Boolean = false,
) {
    if (exercise.trackingType == TrackingType.DURATION) return
    val weighted = exercise.trackingType == TrackingType.WEIGHT_REPS
    val incrementKg = exercise.progressionIncrementKg ?: defaultIncrementKg

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.progression_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.progression_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = exercise.progressionEnabled,
                onCheckedChange = { onChange(it, exercise.progressionIncrementKg) },
            )
        }
        if (!exercise.progressionEnabled) return@Column

        if (weighted) {
            Text(stringResource(R.string.progression_step), style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                loadStepOptions(unit).forEach { value ->
                    val kg = unit.toKg(value)
                    FilterChip(
                        selected = abs(kg - incrementKg) < 0.01,
                        onClick = { onChange(true, kg) },
                        label = { Text("+${formatDecimal(value)} ${unit.label}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        val step = formatWeight(incrementKg, unit)
        Text(
            text = when {
                weighted && range != null ->
                    stringResource(R.string.progression_explain_weight, range.max, step, range.min ?: range.max)
                weighted -> stringResource(R.string.progression_explain_weight_generic, step)
                range != null -> stringResource(R.string.progression_explain_reps, range.max)
                else -> stringResource(R.string.progression_explain_reps_generic)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (showMissingRange && range == null) {
            Text(
                stringResource(R.string.progression_no_range),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
