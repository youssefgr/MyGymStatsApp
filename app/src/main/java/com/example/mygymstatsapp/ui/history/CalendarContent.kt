package com.example.mygymstatsapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.TrainingCalendar
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.DayBubble
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.InlineStat
import com.example.mygymstatsapp.ui.components.SectionHeader
import com.example.mygymstatsapp.ui.components.WorkoutSummaryCard
import com.example.mygymstatsapp.ui.components.weekdayLetters
import com.example.mygymstatsapp.utils.FIRST_DAY_OF_WEEK
import com.example.mygymstatsapp.utils.formatDuration
import com.example.mygymstatsapp.utils.formatMonthYear
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeekdayDayMonth
import java.time.LocalDate
import java.time.YearMonth

/** Month calendar of completed workouts: trained days filled, streak, and the workouts of a day. */
@Composable
fun CalendarContent(
    onOpenWorkout: (String) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "month") {
            MonthCard(
                state = state,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onToday = viewModel::goToToday,
                onSelectDay = viewModel::selectDay,
            )
        }
        item(key = "streak") { StreakCard(state.streaks) }
        item(key = "list_header") {
            SectionHeader(
                title = state.selectedDay?.let { formatWeekdayDayMonth(it) } ?: formatMonthYear(state.month.atDay(1)),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        val listed = state.listedWorkouts
        if (!state.isLoading && listed.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(
                        when {
                            state.selectedDay == null -> R.string.calendar_empty_month
                            state.selectedDay == state.today -> R.string.calendar_empty_today
                            else -> R.string.calendar_empty_day
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(listed, key = { it.id }) { workout ->
            WorkoutSummaryCard(workout, state.weightUnit, onClick = { onOpenWorkout(workout.id) })
        }
    }
}

@Composable
private fun MonthCard(
    state: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    val grid = remember(state.month) { TrainingCalendar.monthGrid(state.month, FIRST_DAY_OF_WEEK) }
    val letters = remember { weekdayLetters() }

    GymCard(Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month))
            }
            Text(
                formatMonthYear(state.month.atDay(1)),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNext, enabled = state.canGoForward) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.calendar_next_month))
            }
        }
        Row(Modifier.padding(top = 4.dp)) {
            letters.forEach { letter ->
                Text(
                    letter,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        grid.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        DayBubble(
                            dayOfMonth = day.dayOfMonth,
                            trained = day in state.workoutsByDay,
                            isToday = day == state.today,
                            selected = day == state.selectedDay,
                            isFuture = day.isAfter(state.today),
                            onClick = { onSelectDay(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (state.month != YearMonth.from(state.today)) {
            TextButton(onClick = onToday, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.calendar_back_to_today))
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
        val totals = state.totals
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InlineStat(stringResource(R.string.label_workouts), "${totals.workoutCount}")
            InlineStat(stringResource(R.string.calendar_days), "${totals.trainedDays}")
            InlineStat(stringResource(R.string.label_duration), formatDuration(totals.durationSeconds))
            InlineStat(stringResource(R.string.label_volume), formatVolume(totals.volumeKg, state.weightUnit))
        }
    }
}

/** Weeks in a row with at least one workout: the regularity that matters more than any single day. */
@Composable
private fun StreakCard(streaks: TrainingCalendar.Streaks) {
    GymCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.LocalFireDepartment,
                contentDescription = null,
                tint = if (streaks.current > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    if (streaks.current > 0) {
                        pluralStringResource(R.plurals.calendar_streak, streaks.current, streaks.current)
                    } else {
                        stringResource(R.string.calendar_no_streak)
                    },
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    stringResource(R.string.calendar_streak_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (streaks.best > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.calendar_best),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        pluralStringResource(R.plurals.count_weeks, streaks.best, streaks.best),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}
