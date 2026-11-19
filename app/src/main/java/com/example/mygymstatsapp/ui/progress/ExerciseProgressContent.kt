package com.example.mygymstatsapp.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseProgressPoint
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.BarChart
import com.example.mygymstatsapp.ui.components.BarEntry
import com.example.mygymstatsapp.ui.components.ChartPoint
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.LineChart
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.components.SegmentedTabs
import com.example.mygymstatsapp.ui.components.StatTile
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.theme.Danger
import com.example.mygymstatsapp.utils.formatCompact
import com.example.mygymstatsapp.utils.formatDecimal
import com.example.mygymstatsapp.utils.formatRelativeDate
import com.example.mygymstatsapp.utils.formatShortDate
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeight
import com.example.mygymstatsapp.utils.fromKg
import com.example.mygymstatsapp.utils.toEpochMillis
import com.example.mygymstatsapp.utils.weekStartOf

@Composable
fun ExerciseProgressContent(
    viewModel: ExerciseProgressViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val exercise = state.selectedExercise

    if (!state.isLoading && exercise == null) {
        EmptyState(
            icon = Icons.AutoMirrored.Rounded.ShowChart,
            title = stringResource(R.string.progress_empty_title),
            message = stringResource(R.string.progress_empty_message),
        )
        return
    }
    if (exercise == null) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "selector") {
            GymCard(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MuscleGroupBadge(exercise.muscleGroup, size = 40.dp)
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.progress_change_exercise), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.UnfoldMore, null)
                }
            }
        }
        item(key = "range") {
            SegmentedTabs(
                options = ProgressRange.entries.map { stringResource(it.labelRes) },
                selectedIndex = state.range.ordinal,
                onSelect = { viewModel.selectRange(ProgressRange.entries[it]) },
            )
        }
        item(key = "metrics") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.metrics) { metric ->
                    FilterChip(
                        selected = metric == state.metric,
                        onClick = { viewModel.selectMetric(metric) },
                        label = { Text(stringResource(metric.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
        }
        item(key = "chart") { ChartCard(state.points, state.metric, state.weightUnit) }
        state.record?.let { record ->
            if (exercise.trackingType == TrackingType.WEIGHT_REPS) {
                item(key = "records") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(stringResource(R.string.progress_best_one_rm), formatWeight(record.bests.bestOneRmKg, state.weightUnit), Modifier.weight(1f))
                            StatTile(stringResource(R.string.record_max_weight), formatWeight(record.bests.maxWeightKg, state.weightUnit), Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(stringResource(R.string.metric_reps), "${record.bests.maxReps}", Modifier.weight(1f), accent = MaterialTheme.colorScheme.tertiary)
                            StatTile(stringResource(R.string.progress_best_volume), formatVolume(record.bests.bestVolumeKg, state.weightUnit), Modifier.weight(1f), accent = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
        if (state.points.isNotEmpty()) {
            item(key = "sessions_header") {
                Text(stringResource(R.string.progress_sessions, state.points.size), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            }
            items(state.points.asReversed(), key = { it.workoutId }) { point ->
                SessionRow(point, exercise.trackingType, state.weightUnit)
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = state.performedExercises,
            onSelect = {
                viewModel.selectExercise(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun ChartCard(points: List<ExerciseProgressPoint>, metric: ProgressMetric, unit: WeightUnit) {
    GymCard(Modifier.fillMaxWidth()) {
        if (points.isEmpty()) {
            Text(
                stringResource(R.string.progress_no_session_in_range),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 48.dp),
            )
            return@GymCard
        }
        if (metric == ProgressMetric.FREQUENCY) {
            val weeks = remember(points) {
                points.groupBy { weekStartOf(it.date) }.toSortedMap().map { (week, sessions) ->
                    BarEntry(formatShortDate(week.toEpochMillis()), sessions.size.toDouble())
                }
            }
            Text(stringResource(metric.labelRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(R.string.progress_per_week, formatDecimal(points.size.toDouble() / weeks.size, 1)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            val resources = LocalResources.current
            BarChart(entries = weeks, formatValue = { resources.getQuantityString(R.plurals.count_workouts, it.toInt(), it.toInt()) })
            return@GymCard
        }

        val values = points.map { ChartPoint(it.date, metricValue(it, metric, unit)) }
        val first = values.first().y
        val last = values.last().y
        val delta = last - first
        Text(stringResource(metric.labelRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatMetric(last, metric, unit), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            if (values.size > 1 && delta != 0.0) {
                Text(
                    (if (delta > 0) "+" else "") + formatMetric(delta, metric, unit),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (delta > 0) MaterialTheme.colorScheme.primary else Danger,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        LineChart(
            points = values,
            formatY = { formatAxis(it, metric, unit) },
            formatX = ::formatShortDate,
            color = if (metric == ProgressMetric.VOLUME) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
        )
    }
}

/** Value in display unit (lb when selected). */
private fun metricValue(point: ExerciseProgressPoint, metric: ProgressMetric, unit: WeightUnit): Double = when (metric) {
    ProgressMetric.MAX_WEIGHT -> unit.fromKg(point.maxWeightKg)
    ProgressMetric.ONE_RM -> unit.fromKg(point.bestOneRmKg)
    ProgressMetric.VOLUME -> unit.fromKg(point.volumeKg)
    ProgressMetric.REPS -> point.maxReps.toDouble()
    ProgressMetric.DURATION -> point.totalDurationSeconds / 60.0
    ProgressMetric.FREQUENCY -> 1.0
}

@Composable
private fun formatMetric(value: Double, metric: ProgressMetric, unit: WeightUnit): String = when (metric) {
    ProgressMetric.MAX_WEIGHT, ProgressMetric.ONE_RM -> "${formatDecimal(value, 1)} ${unit.label}"
    ProgressMetric.VOLUME -> "${formatDecimal(value, 0)} ${unit.label}"
    ProgressMetric.REPS -> stringResource(R.string.value_reps_decimal, formatDecimal(value, 0))
    ProgressMetric.DURATION -> stringResource(R.string.value_minutes, formatDecimal(value, 1))
    ProgressMetric.FREQUENCY -> formatDecimal(value, 0)
}

private fun formatAxis(value: Double, metric: ProgressMetric, unit: WeightUnit): String = when (metric) {
    ProgressMetric.VOLUME -> formatCompact(value)
    else -> formatDecimal(value, if (value >= 100) 0 else 1)
}

@Composable
private fun SessionRow(point: ExerciseProgressPoint, trackingType: TrackingType, unit: WeightUnit) {
    ListItem(
        headlineContent = { Text(formatRelativeDate(point.date)) },
        supportingContent = {
            Text(
                when (trackingType) {
                    TrackingType.WEIGHT_REPS -> pluralStringResource(R.plurals.count_sets, point.setCount, point.setCount) + " · " +
                        stringResource(R.string.progress_session_weight, formatWeight(point.maxWeightKg, unit), formatWeight(point.bestOneRmKg, unit))
                    TrackingType.REPS_ONLY -> pluralStringResource(R.plurals.count_sets, point.setCount, point.setCount) + " · " +
                        stringResource(R.string.progress_session_reps, point.totalReps)
                    TrackingType.DURATION -> pluralStringResource(R.plurals.count_sets, point.setCount, point.setCount) + " · " +
                        stringResource(R.string.value_minutes, formatDecimal(point.totalDurationSeconds / 60.0, 1))
                }
            )
        },
        trailingContent = {
            if (trackingType == TrackingType.WEIGHT_REPS) {
                Text(formatVolume(point.volumeKg, unit), style = MaterialTheme.typography.titleSmall)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Text(
            stringResource(R.string.progress_performed_exercises),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(Modifier.navigationBarsPadding()) {
            items(exercises, key = { it.id }) { exercise ->
                ListItem(
                    headlineContent = { Text(exercise.name) },
                    supportingContent = { Text(stringResource(exercise.muscleGroup.labelRes) + " · " + stringResource(exercise.equipment.labelRes)) },
                    leadingContent = { MuscleGroupBadge(exercise.muscleGroup, size = 36.dp) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSelect(exercise.id) },
                )
            }
        }
    }
}
