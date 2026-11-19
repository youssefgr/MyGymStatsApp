package com.example.mygymstatsapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.TrainingCalendar
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.utils.FIRST_DAY_OF_WEEK
import com.example.mygymstatsapp.utils.toLocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class MonthTotals(val workoutCount: Int, val trainedDays: Int, val durationSeconds: Long, val volumeKg: Double)

data class CalendarUiState(
    val isLoading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    /** Null: the list shows the whole month. */
    val selectedDay: LocalDate? = null,
    /** Completed workouts of the displayed month by day, most recent first within a day. */
    val workoutsByDay: Map<LocalDate, List<WorkoutSummary>> = emptyMap(),
    val totals: MonthTotals = MonthTotals(0, 0, 0, 0.0),
    val streaks: TrainingCalendar.Streaks = TrainingCalendar.Streaks(0, 0),
    val weightUnit: WeightUnit = WeightUnit.KG,
) {
    val canGoForward: Boolean get() = month < YearMonth.from(today)

    /** Workouts listed under the calendar: the selected day, or the whole month. */
    val listedWorkouts: List<WorkoutSummary>
        get() = selectedDay?.let { workoutsByDay[it].orEmpty() }
            ?: workoutsByDay.values.flatten().sortedByDescending { it.startedAt }
}

/** Month calendar of completed workouts, with monthly totals and weekly streaks. */
class CalendarViewModel(
    workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDay = MutableStateFlow<LocalDate?>(null)

    private val workoutsByDay = workoutRepository.observeCompletedWorkouts()
        .map { workouts -> workouts.groupBy { it.startedAt.toLocalDate() } }

    val uiState: StateFlow<CalendarUiState> = combine(
        workoutsByDay,
        month,
        selectedDay,
        preferencesRepository.profile,
    ) { byDay, month, selected, profile ->
        val today = LocalDate.now()
        val inMonth = byDay.filterKeys { YearMonth.from(it) == month }
        val workouts = inMonth.values.flatten()
        CalendarUiState(
            isLoading = false,
            month = month,
            today = today,
            selectedDay = selected,
            workoutsByDay = inMonth,
            totals = MonthTotals(
                workoutCount = workouts.size,
                trainedDays = inMonth.size,
                durationSeconds = workouts.sumOf { it.durationSeconds },
                volumeKg = workouts.sumOf { it.volumeKg },
            ),
            streaks = TrainingCalendar.weekStreaks(byDay.keys, today, FIRST_DAY_OF_WEEK),
            weightUnit = profile.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun previousMonth() = changeMonth(month.value.minusMonths(1))

    fun nextMonth() {
        if (month.value < YearMonth.now()) changeMonth(month.value.plusMonths(1))
    }

    fun goToToday() = changeMonth(YearMonth.now())

    /** Tapping the selected day again goes back to the whole month. */
    fun selectDay(day: LocalDate) = selectedDay.update { if (it == day) null else day }

    private fun changeMonth(value: YearMonth) {
        month.value = value
        selectedDay.value = null
    }
}
