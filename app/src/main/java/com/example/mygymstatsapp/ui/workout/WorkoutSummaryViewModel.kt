package com.example.mygymstatsapp.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.DetectPersonalRecordsUseCase
import com.example.mygymstatsapp.navigation.WorkoutSummaryRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutSummaryUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    val records: List<NewPersonalRecord> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
)

class WorkoutSummaryViewModel(
    savedStateHandle: SavedStateHandle,
    workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
    detectRecords: DetectPersonalRecordsUseCase,
) : ViewModel() {

    private val workoutId = savedStateHandle.toRoute<WorkoutSummaryRoute>().workoutId
    private val workout = MutableStateFlow<Workout?>(null)
    private val records = MutableStateFlow<List<NewPersonalRecord>>(emptyList())

    init {
        viewModelScope.launch {
            val loaded = workoutRepository.getWorkout(workoutId)
            if (loaded != null) records.value = detectRecords(loaded)
            workout.value = loaded
        }
    }

    val uiState: StateFlow<WorkoutSummaryUiState> = combine(
        workout,
        records,
        preferencesRepository.profile,
    ) { workout, records, profile ->
        WorkoutSummaryUiState(
            isLoading = workout == null,
            workout = workout,
            records = records,
            weightUnit = profile.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutSummaryUiState())
}
