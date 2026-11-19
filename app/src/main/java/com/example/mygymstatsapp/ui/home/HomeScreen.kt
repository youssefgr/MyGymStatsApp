package com.example.mygymstatsapp.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.DayBubble
import com.example.mygymstatsapp.ui.components.FonteWordmark
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.InlineStat
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.components.SectionHeader
import com.example.mygymstatsapp.ui.components.StatTile
import com.example.mygymstatsapp.ui.components.WorkoutSummaryCard
import com.example.mygymstatsapp.ui.components.rememberElapsedSeconds
import com.example.mygymstatsapp.ui.components.weekdayLetters
import com.example.mygymstatsapp.ui.workout.StartWorkoutSheet
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherEffects
import com.example.mygymstatsapp.ui.workout.templateSummary
import com.example.mygymstatsapp.utils.formatClock
import com.example.mygymstatsapp.utils.formatDuration
import com.example.mygymstatsapp.utils.formatRelativeDate
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeekdayDayMonth
import com.example.mygymstatsapp.utils.weekStart
import java.time.LocalDate

@Composable
fun HomeScreen(
    onOpenActiveWorkout: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCalendar: () -> Unit,
    onCreateTemplate: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showStartSheet by rememberSaveable { mutableStateOf(false) }

    WorkoutLauncherEffects(viewModel, onOpenActiveWorkout)
    if (showStartSheet) {
        StartWorkoutSheet(
            programs = state.programs,
            sessions = state.standaloneSessions,
            onDismiss = { showStartSheet = false },
            onStart = viewModel::startWorkout,
            onCreateTemplate = onCreateTemplate,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") { Greeting(state.userName) }

        item(key = "cta") {
            val active = state.activeWorkout
            if (active != null) {
                ResumeWorkoutCard(name = active.name, startedAt = active.startedAt, onClick = onOpenActiveWorkout)
            } else {
                Button(
                    onClick = { showStartSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_start_workout), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        item(key = "week") { WeekStats(state, onOpenCalendar) }

        state.lastWorkout?.let { last ->
            item(key = "last") {
                SectionHeader(stringResource(R.string.home_last_workout))
                Spacer(Modifier.height(8.dp))
                LastWorkoutCard(last, state.weightUnit, onOpen = { onOpenWorkout(last.id) })
            }
        }

        if (state.quickStart.isNotEmpty() && state.activeWorkout == null) {
            item(key = "programs") {
                SectionHeader(stringResource(R.string.home_quick_start))
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.quickStart, key = { it.session.id }) { item ->
                        QuickStartCard(item) { viewModel.startWorkout(StartRequest.FromTemplate(item.session.id)) }
                    }
                }
            }
        }

        if (state.recentWorkouts.size > 1) {
            item(key = "recent_header") {
                SectionHeader(stringResource(R.string.home_recent), actionLabel = stringResource(R.string.home_see_all), onAction = onOpenHistory)
            }
            items(state.recentWorkouts.drop(1), key = { it.id }) { workout ->
                WorkoutSummaryCard(workout, state.weightUnit, onClick = { onOpenWorkout(workout.id) })
            }
        }

        if (!state.isLoading && state.recentWorkouts.isEmpty()) {
            item(key = "empty") {
                GymCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.home_welcome_title), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.home_welcome_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String) {
    val today = LocalDate.now()
    val taglines = stringArrayResource(R.array.home_taglines)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            FonteWordmark(markSize = 26.dp)
            Text(formatWeekdayDayMonth(today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (name.isBlank()) stringResource(R.string.home_hello) else stringResource(R.string.home_hello_name, name),
            style = MaterialTheme.typography.headlineLarge,
        )
        // One line per day, so it changes without feeling random.
        Text(
            taglines[today.dayOfYear % taglines.size],
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResumeWorkoutCard(name: String, startedAt: Long, onClick: () -> Unit) {
    GymCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.primary) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_workout_in_progress), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatClock(rememberElapsedSeconds(startedAt)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.onPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.PlayArrow, stringResource(R.string.action_resume), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
private fun WeekStats(state: HomeUiState, onOpenCalendar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(
            stringResource(R.string.home_this_week),
            actionLabel = stringResource(R.string.home_calendar),
            onAction = onOpenCalendar,
        )
        WeekStrip(state.trainedDaysThisWeek, onClick = onOpenCalendar)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.label_workouts), state.weekStats.workoutCount.toString(), Modifier.weight(1f), Icons.Rounded.CalendarMonth)
            StatTile(
                stringResource(R.string.label_volume),
                formatVolume(state.weekStats.volumeKg, state.weightUnit),
                Modifier.weight(1f),
                Icons.Rounded.Scale,
                accent = MaterialTheme.colorScheme.secondary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.label_exercises), state.weekStats.exerciseCount.toString(), Modifier.weight(1f), Icons.Rounded.FitnessCenter, accent = MaterialTheme.colorScheme.tertiary)
            StatTile(
                stringResource(R.string.home_total_time),
                formatDuration(state.weekStats.totalDurationSeconds),
                Modifier.weight(1f),
                Icons.Rounded.Schedule,
                accent = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Monday to Sunday, trained days filled: the week at a glance, opens the calendar. */
@Composable
private fun WeekStrip(trainedDays: Set<LocalDate>, onClick: () -> Unit) {
    val today = LocalDate.now()
    val start = today.weekStart()
    val letters = remember { weekdayLetters() }
    GymCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = 10.dp) {
        Row {
            letters.forEachIndexed { offset, letter ->
                val day = start.plusDays(offset.toLong())
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(letter, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    DayBubble(
                        dayOfMonth = day.dayOfMonth,
                        trained = day in trainedDays,
                        isToday = day == today,
                        isFuture = day.isAfter(today),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LastWorkoutCard(workout: WorkoutSummary, unit: WeightUnit, onOpen: () -> Unit) {
    GymCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Text(workout.name, style = MaterialTheme.typography.titleLarge)
        Text(
            formatRelativeDate(workout.startedAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InlineStat(stringResource(R.string.label_exercises), workout.exerciseCount.toString())
            InlineStat(stringResource(R.string.label_duration), formatDuration(workout.durationSeconds))
            InlineStat(stringResource(R.string.label_volume), formatVolume(workout.volumeKg, unit))
        }
        Spacer(Modifier.height(16.dp))
        FilledTonalButton(shape = MaterialTheme.shapes.small, onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_see_workout)) }
    }
}

@Composable
private fun QuickStartCard(item: QuickStartItem, onStart: () -> Unit) {
    val template = item.session
    GymCard(onClick = onStart, modifier = Modifier.width(200.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            template.exercises.map { it.exercise.muscleGroup }.distinct().take(3).forEach {
                MuscleGroupBadge(it, size = 30.dp)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Next session of a program: the program name tells which rotation it belongs to.
        Text(
            item.programName?.uppercase().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(template.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            templateSummary(template),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            shape = MaterialTheme.shapes.small,
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.action_start))
        }
    }
}
