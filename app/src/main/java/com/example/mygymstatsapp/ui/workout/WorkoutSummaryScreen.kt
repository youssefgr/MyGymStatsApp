package com.example.mygymstatsapp.ui.workout

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.ChalkBurst
import com.example.mygymstatsapp.ui.components.FonteMark
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.components.StatTile
import com.example.mygymstatsapp.ui.history.setsSummary
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.utils.formatDuration
import com.example.mygymstatsapp.utils.formatFullDateTime
import com.example.mygymstatsapp.utils.formatVolume

@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onDone)
    val workout = state.workout
    if (workout == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val plateScale = remember { Animatable(0.3f) }
    LaunchedEffect(Unit) {
        plateScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val cheers = stringArrayResource(R.array.summary_cheers)
    val cheer = if (state.records.isNotEmpty()) {
        stringResource(R.string.summary_cheer_record)
    } else {
        cheers[Math.floorMod(workout.id.hashCode(), cheers.size)]
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .statusBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), MaterialTheme.colorScheme.background)
                            )
                        )
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // The plate spins in on a puff of chalk (brass dust when a record fell).
                    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        ChalkBurst(Modifier.matchParentSize(), color = if (state.records.isNotEmpty()) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface)
                        FonteMark(
                            size = 72.dp,
                            modifier = Modifier
                                .scale(plateScale.value)
                                .rotate((1f - plateScale.value) * -120f),
                        )
                    }
                    Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(workout.name, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
                    Text(
                        formatFullDateTime(workout.startedAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        cheer,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            item(key = "stats") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(stringResource(R.string.label_duration), formatDuration(workout.durationSeconds), Modifier.weight(1f), Icons.Rounded.Schedule)
                        StatTile(stringResource(R.string.label_exercises), "${workout.exercises.size}", Modifier.weight(1f), Icons.Rounded.FitnessCenter, MaterialTheme.colorScheme.tertiary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(stringResource(R.string.label_sets), "${workout.completedSetCount}", Modifier.weight(1f), Icons.Rounded.Repeat, MaterialTheme.colorScheme.error)
                        StatTile(stringResource(R.string.profile_total_volume), formatVolume(workout.volumeKg, state.weightUnit), Modifier.weight(1f), Icons.Rounded.Scale, MaterialTheme.colorScheme.secondary)
                    }
                    val avgHeartRate = workout.avgHeartRate
                    val maxHeartRate = workout.maxHeartRate
                    if (avgHeartRate != null && maxHeartRate != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatTile(stringResource(R.string.label_heart_rate_avg), stringResource(R.string.value_bpm, avgHeartRate), Modifier.weight(1f), Icons.Rounded.Favorite, MaterialTheme.colorScheme.error)
                            StatTile(stringResource(R.string.label_heart_rate_max), stringResource(R.string.value_bpm, maxHeartRate), Modifier.weight(1f), Icons.Rounded.MonitorHeart, MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            if (state.records.isNotEmpty()) {
                item(key = "records") { RecordsCard(state.records, state.weightUnit) }
            }
            item(key = "details_header") {
                Text(stringResource(R.string.summary_details), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            }
            items(workout.exercises, key = { it.id }) { exercise -> ExerciseRecap(exercise, state.weightUnit) }
        }
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp)
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) { Text(stringResource(R.string.action_finish).uppercase(), style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
fun RecordsCard(records: List<NewPersonalRecord>, unit: WeightUnit) {
    GymCard(Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)) {
        Text(stringResource(R.string.records_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        records.forEach { record ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(22.dp))
                Column(Modifier.padding(start = 12.dp)) {
                    Text(record.exerciseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(record.type.labelRes) + " · " + recordDescription(record, unit),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseRecap(exercise: WorkoutExercise, unit: WeightUnit) {
    GymCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MuscleGroupBadge(exercise.exercise.muscleGroup, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(exercise.exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    setsSummary(exercise, unit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (exercise.volumeKg > 0) {
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(formatVolume(exercise.volumeKg, unit), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
