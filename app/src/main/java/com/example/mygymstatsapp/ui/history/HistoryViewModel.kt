package com.example.mygymstatsapp.ui.history

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.utils.daysAgoMillis
import com.example.mygymstatsapp.utils.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class HistoryPeriod(@get:StringRes val labelRes: Int, val days: Long?) {
    ALL(R.string.history_period_all, null),
    WEEK(R.string.history_period_week, 7),
    MONTH(R.string.history_period_month, 30),
    QUARTER(R.string.history_period_quarter, 90),
    YEAR(R.string.history_period_year, 365),
}

enum class DurationFilter(@get:StringRes val labelRes: Int, val minSeconds: Long, val maxSeconds: Long) {
    ANY(R.string.history_duration_any, 0, Long.MAX_VALUE),
    SHORT(R.string.history_duration_short, 0, 45 * 60 - 1),
    MEDIUM(R.string.history_duration_medium, 45 * 60, 75 * 60),
    LONG(R.string.history_duration_long, 75 * 60 + 1, Long.MAX_VALUE),
}

data class HistoryFilters(
    val period: HistoryPeriod = HistoryPeriod.ALL,
    val workoutName: String? = null,
    val muscleGroup: MuscleGroup? = null,
    val duration: DurationFilter = DurationFilter.ANY,
) {
    val isActive: Boolean get() = this != HistoryFilters()
}

data class HistoryMonth(val month: LocalDate, val workouts: List<WorkoutSummary>) {
    val totalVolumeKg: Double get() = workouts.sumOf { it.volumeKg }
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val months: List<HistoryMonth> = emptyList(),
    val filters: HistoryFilters = HistoryFilters(),
    val workoutNames: List<String> = emptyList(),
    val totalCount: Int = 0,
    val weightUnit: WeightUnit = WeightUnit.KG,
)

class HistoryViewModel(
    workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(HistoryFilters())

    val uiState: StateFlow<HistoryUiState> = combine(
        workoutRepository.observeCompletedWorkouts(),
        filters,
        preferencesRepository.profile,
    ) { workouts, filters, profile ->
        val since = filters.period.days?.let { daysAgoMillis(it) } ?: 0L
        val filtered = workouts.filter { workout ->
            workout.startedAt >= since &&
                (filters.workoutName == null || workout.name == filters.workoutName) &&
                (filters.muscleGroup == null || filters.muscleGroup in workout.muscleGroups) &&
                workout.durationSeconds in filters.duration.minSeconds..filters.duration.maxSeconds
        }
        HistoryUiState(
            isLoading = false,
            months = filtered
                .groupBy { it.startedAt.toLocalDate().withDayOfMonth(1) }
                .map { (month, items) -> HistoryMonth(month, items) },
            filters = filters,
            workoutNames = workouts.map { it.name }.distinct().sorted(),
            totalCount = filtered.size,
            weightUnit = profile.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setPeriod(value: HistoryPeriod) = filters.update { it.copy(period = value) }
    fun setWorkoutName(value: String?) = filters.update { it.copy(workoutName = value) }
    fun setMuscleGroup(value: MuscleGroup?) = filters.update { it.copy(muscleGroup = value) }
    fun setDuration(value: DurationFilter) = filters.update { it.copy(duration = value) }
    fun clearFilters() = filters.update { HistoryFilters() }
}
