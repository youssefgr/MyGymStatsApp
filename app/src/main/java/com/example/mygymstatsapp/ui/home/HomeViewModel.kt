package com.example.mygymstatsapp.ui.home

import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.ActiveWorkoutHeader
import com.example.mygymstatsapp.domain.model.PeriodStats
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.ui.programs.standaloneSessions
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherViewModel
import com.example.mygymstatsapp.utils.startOfWeekMillis
import com.example.mygymstatsapp.utils.toLocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val weekStats: PeriodStats = PeriodStats.EMPTY,
    val trainedDaysThisWeek: Set<LocalDate> = emptySet(),
    val recentWorkouts: List<WorkoutSummary> = emptyList(),
    val programs: List<Program> = emptyList(),
    val standaloneSessions: List<WorkoutTemplate> = emptyList(),
    val activeWorkout: ActiveWorkoutHeader? = null,
) {
    val lastWorkout: WorkoutSummary? get() = recentWorkouts.firstOrNull()

    /** One tap to start: each program's next session, then the standalone sessions. */
    val quickStart: List<QuickStartItem>
        get() = (programs.mapNotNull { program -> program.nextSession?.let { QuickStartItem(it, program.name) } } +
            standaloneSessions.map { QuickStartItem(it, null) })
            .filter { it.session.exercises.isNotEmpty() }
}

data class QuickStartItem(val session: WorkoutTemplate, val programName: String?)

class HomeViewModel(
    preferencesRepository: UserPreferencesRepository,
    workoutRepository: WorkoutRepository,
    statsRepository: StatsRepository,
    templateRepository: TemplateRepository,
    programRepository: ProgramRepository,
    startWorkoutUseCase: StartWorkoutUseCase,
) : WorkoutLauncherViewModel(startWorkoutUseCase) {

    private val sessions = combine(programRepository.observePrograms(), templateRepository.observeTemplates()) { programs, templates ->
        programs to standaloneSessions(templates, programs)
    }

    private val week = combine(
        statsRepository.observePeriodStats(from = startOfWeekMillis()),
        workoutRepository.observeCompletedStartTimes(from = startOfWeekMillis()),
    ) { stats, startTimes -> stats to startTimes.mapTo(HashSet()) { it.toLocalDate() } }

    val uiState: StateFlow<HomeUiState> = combine(
        preferencesRepository.profile,
        week,
        workoutRepository.observeCompletedWorkouts(limit = RECENT_COUNT),
        sessions,
        workoutRepository.observeActiveWorkoutHeader(),
    ) { profile, (weekStats, trainedDays), recent, (programs, standalone), active ->
        HomeUiState(
            isLoading = false,
            userName = profile.name,
            weightUnit = profile.weightUnit,
            weekStats = weekStats,
            trainedDaysThisWeek = trainedDays,
            recentWorkouts = recent,
            programs = programs,
            standaloneSessions = standalone,
            activeWorkout = active,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private companion object {
        const val RECENT_COUNT = 6
    }
}
