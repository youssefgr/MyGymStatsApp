package com.example.mygymstatsapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.WorkoutSummaryCard
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.utils.formatMonthYear
import com.example.mygymstatsapp.utils.formatVolume

/** History list with filters (shown in the "Séances" tab). */
@Composable
fun HistoryContent(
    onOpenWorkout: (String) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                FilterDropdown(
                    label = stringResource(state.filters.period.takeIf { it != HistoryPeriod.ALL }?.labelRes ?: R.string.history_filter_date),
                    active = state.filters.period != HistoryPeriod.ALL,
                    options = HistoryPeriod.entries.map { stringResource(it.labelRes) to { viewModel.setPeriod(it) } },
                )
            }
            item {
                FilterDropdown(
                    label = state.filters.workoutName ?: stringResource(R.string.history_filter_workout),
                    active = state.filters.workoutName != null,
                    options = listOf<Pair<String, () -> Unit>>(stringResource(R.string.history_all_workouts) to { viewModel.setWorkoutName(null) }) +
                        state.workoutNames.map { name -> name to { viewModel.setWorkoutName(name) } },
                )
            }
            item {
                FilterDropdown(
                    label = stringResource(state.filters.muscleGroup?.labelRes ?: R.string.history_filter_muscle),
                    active = state.filters.muscleGroup != null,
                    options = listOf<Pair<String, () -> Unit>>(stringResource(R.string.history_all_muscles) to { viewModel.setMuscleGroup(null) }) +
                        MuscleGroup.entries.map { group -> stringResource(group.labelRes) to { viewModel.setMuscleGroup(group) } },
                )
            }
            item {
                FilterDropdown(
                    label = stringResource(state.filters.duration.takeIf { it != DurationFilter.ANY }?.labelRes ?: R.string.label_duration),
                    active = state.filters.duration != DurationFilter.ANY,
                    options = DurationFilter.entries.map { stringResource(it.labelRes) to { viewModel.setDuration(it) } },
                )
            }
            if (state.filters.isActive) {
                item {
                    TextButton(onClick = viewModel::clearFilters) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.action_clear), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        if (!state.isLoading && state.months.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.History,
                title = stringResource(if (state.filters.isActive) R.string.history_no_result else R.string.history_empty_title),
                message = if (state.filters.isActive) {
                    stringResource(R.string.history_no_result_message)
                } else {
                    stringResource(R.string.history_empty_message)
                },
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.months.forEach { month ->
                item(key = "month_${month.month}") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(formatMonthYear(month.month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Text(
                            pluralStringResource(R.plurals.count_workouts, month.workouts.size, month.workouts.size) +
                                " · " + formatVolume(month.totalVolumeKg, state.weightUnit),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(month.workouts, key = { it.id }) { workout ->
                    WorkoutSummaryCard(workout, state.weightUnit, onClick = { onOpenWorkout(workout.id) })
                }
            }
        }
    }
}

@Composable
private fun FilterDropdown(label: String, active: Boolean, options: List<Pair<String, () -> Unit>>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = active,
            onClick = { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null, modifier = Modifier.size(18.dp)) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; action() })
            }
        }
    }
}
