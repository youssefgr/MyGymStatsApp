package com.example.mygymstatsapp.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.DetectPersonalRecordsUseCase
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.navigation.WorkoutDetailRoute
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherViewModel
import com.example.mygymstatsapp.utils.parseDecimal
import com.example.mygymstatsapp.utils.toKg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class WorkoutDetailUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val records: List<NewPersonalRecord> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val isDeleted: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
    detectRecords: DetectPersonalRecordsUseCase,
    startWorkoutUseCase: StartWorkoutUseCase,
) : WorkoutLauncherViewModel(startWorkoutUseCase) {

    private val workoutId = savedStateHandle.toRoute<WorkoutDetailRoute>().workoutId
    private val isDeleted = MutableStateFlow(false)

    /** Records are recomputed after each edit so they always match the stored sets. */
    private val workoutWithRecords = workoutRepository.observeWorkout(workoutId).mapLatest { workout ->
        workout to (workout?.let { detectRecords(it) } ?: emptyList())
    }

    val uiState: StateFlow<WorkoutDetailUiState> = combine(
        workoutWithRecords,
        preferencesRepository.profile,
        isDeleted,
    ) { (workout, records), profile, deleted ->
        WorkoutDetailUiState(
            isLoading = false,
            workout = workout,
            records = records,
            weightUnit = profile.weightUnit,
            isDeleted = deleted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutDetailUiState())

    fun rename(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { workoutRepository.renameWorkout(workoutId, name) }
    }

    fun updateNotes(notes: String) {
        viewModelScope.launch { workoutRepository.updateWorkoutNotes(workoutId, notes.trim()) }
    }

    fun updateTiming(startedAt: Long, durationSeconds: Long) {
        viewModelScope.launch { workoutRepository.updateWorkoutTiming(workoutId, startedAt, durationSeconds) }
    }

    fun editSet(setId: String, weightText: String, repsText: String, durationText: String, type: SetType) {
        val unit = uiState.value.weightUnit
        viewModelScope.launch {
            workoutRepository.updateSetValues(
                setId = setId,
                weightKg = parseDecimal(weightText)?.let { unit.toKg(it) },
                reps = repsText.toIntOrNull(),
                durationSeconds = parseDecimal(durationText)?.let { (it * 60).roundToInt() },
            )
            workoutRepository.updateSetType(setId, type)
        }
    }

    fun deleteSet(setId: String) {
        viewModelScope.launch { workoutRepository.deleteSet(setId) }
    }

    fun repeatWorkout() = startWorkout(StartRequest.Repeat(workoutId))

    fun deleteWorkout() {
        viewModelScope.launch {
            isDeleted.value = true
            workoutRepository.deleteWorkout(workoutId)
        }
    }
}
