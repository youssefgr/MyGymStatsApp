package com.example.mygymstatsapp.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import com.example.mygymstatsapp.shared.PastWorkout
import com.example.mygymstatsapp.shared.WatchHistory
import com.example.mygymstatsapp.shared.WearMuscle
import com.example.mygymstatsapp.shared.WearTracking
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.wear.R
import com.example.mygymstatsapp.wear.ui.theme.color

/** The last workouts, as sent by the phone. */
@Composable
fun HistoryScreen(history: WatchHistory?, onOpen: (String) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(R.string.home_history)) } }
            val workouts = history?.workouts.orEmpty()
            if (workouts.isEmpty()) {
                item { EmptyText(stringResource(R.string.history_empty)) }
            }
            items(workouts, key = { it.id }) { workout ->
                TitleCard(
                    onClick = { onOpen(workout.id) },
                    modifier = Modifier.fillMaxWidth(),
                    title = { Text(workout.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    time = { Text(formatRelativeDate(workout.startedAt)) },
                    subtitle = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            workout.muscles.take(3).forEach { MuscleDot(it) }
                        }
                    },
                ) {
                    Text(
                        formatDuration(workout.durationSeconds) + " · " + formatVolume(workout.volumeKg, history!!.unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun PastWorkoutScreen(workout: PastWorkout, unit: WearUnit) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(workout.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        formatRelativeDate(workout.startedAt) + " · " + formatDuration(workout.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(formatVolume(workout.volumeKg, unit), style = MaterialTheme.typography.labelMedium)
                    if (workout.avgHeartRate != null && workout.maxHeartRate != null) {
                        Text(
                            stringResource(R.string.label_heart_rate_avg) + " " + stringResource(R.string.value_bpm, workout.avgHeartRate!!) +
                                " · " + stringResource(R.string.label_heart_rate_max) + " " + workout.maxHeartRate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(workout.exercises) { exercise ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MuscleDot(exercise.muscle)
                        Text(exercise.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        exercise.sets.joinToString("  ·  ") { it.text(exercise.tracking, unit) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Best performances per exercise, most recently trained first. */
@Composable
fun RecordsScreen(history: WatchHistory?) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(R.string.home_records)) } }
            val records = history?.records.orEmpty()
            if (records.isEmpty()) {
                item { EmptyText(stringResource(R.string.records_empty)) }
            }
            items(records, key = { it.exerciseId }) { record ->
                val unit = history!!.unit
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MuscleDot(record.muscle)
                        Text(record.name, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    when (record.tracking) {
                        WearTracking.WEIGHT_REPS -> {
                            Text(
                                stringResource(R.string.record_max_value, formatWeight(record.maxWeightKg, unit)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            Text(
                                stringResource(R.string.record_one_rm_value, formatWeight(record.bestOneRmKg, unit)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        WearTracking.REPS_ONLY -> Text(
                            stringResource(R.string.record_max_value, stringResource(R.string.value_reps, record.maxReps)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        WearTracking.DURATION -> Unit
                    }
                }
            }
        }
    }
}

/** Muscle-group hue as a small dot (badge letters would be unreadable at watch size). */
@Composable
private fun MuscleDot(muscle: WearMuscle) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(muscle.color, CircleShape),
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
    )
}
