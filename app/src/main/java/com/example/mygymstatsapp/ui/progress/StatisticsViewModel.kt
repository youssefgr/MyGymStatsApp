package com.example.mygymstatsapp.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.ExerciseUsage
import com.example.mygymstatsapp.domain.model.MuscleGroupShare
import com.example.mygymstatsapp.domain.model.PeriodStats
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.utils.daysAgoMillis
import com.example.mygymstatsapp.utils.startOfMonthMillis
import com.example.mygymstatsapp.utils.startOfWeekMillis
import com.example.mygymstatsapp.utils.weekStartOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class WeekBucket(val weekStart: LocalDate, val volumeKg: Double, val workoutCount: Int)

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val week: PeriodStats = PeriodStats.EMPTY,
    val month: PeriodStats = PeriodStats.EMPTY,
    val allTime: PeriodStats = PeriodStats.EMPTY,
    val weeks: List<WeekBucket> = emptyList(),
    val topExercises: List<ExerciseUsage> = emptyList(),
    val muscleShares: List<MuscleGroupShare> = emptyList(),
    val recordCount: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.KG,
)

class StatisticsViewModel(
    statsRepository: StatsRepository,
    workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val periods = combine(
        statsRepository.observePeriodStats(from = startOfWeekMillis()),
        statsRepository.observePeriodStats(from = startOfMonthMillis()),
        statsRepository.observePeriodStats(from = 0),
    ) { week, month, allTime -> Triple(week, month, allTime) }

    private val rankings = combine(
        statsRepository.observeTopExercises(since = 0, limit = 5),
        statsRepository.observeMuscleGroupShares(since = daysAgoMillis(30)),
        statsRepository.observePersonalRecords(),
    ) { top, shares, records -> Triple(top, shares, records.size) }

    val uiState: StateFlow<StatisticsUiState> = combine(
        periods,
        rankings,
        workoutRepository.observeCompletedWorkouts(),
        preferencesRepository.profile,
    ) { (week, month, allTime), (top, shares, recordCount), workouts, profile ->
        StatisticsUiState(
            isLoading = false,
            week = week,
            month = month,
            allTime = allTime,
            weeks = lastWeeks(workouts, WEEKS_SHOWN),
            topExercises = top,
            muscleShares = shares,
            recordCount = recordCount,
            weightUnit = profile.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    /** Volume and workout count for each of the last [count] weeks, empty weeks included. */
    private fun lastWeeks(workouts: List<WorkoutSummary>, count: Int): List<WeekBucket> {
        val byWeek = workouts.groupBy { weekStartOf(it.startedAt) }
        val currentWeek = weekStartOf(System.currentTimeMillis())
        return (count - 1 downTo 0).map { offset ->
            val week = currentWeek.minusWeeks(offset.toLong())
            val items = byWeek[week].orEmpty()
            WeekBucket(week, items.sumOf { it.volumeKg }, items.size)
        }
    }

    private companion object {
        const val WEEKS_SHOWN = 12
    }
}
