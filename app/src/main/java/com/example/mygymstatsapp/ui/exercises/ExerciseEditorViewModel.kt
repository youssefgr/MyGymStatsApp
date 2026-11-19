package com.example.mygymstatsapp.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.navigation.ExerciseEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseEditorUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val muscleGroup: MuscleGroup = MuscleGroup.CHEST,
    val equipment: Equipment = Equipment.BARBELL,
    val trackingType: TrackingType = TrackingType.WEIGHT_REPS,
    val description: String = "",
    val showNameError: Boolean = false,
    val isSaved: Boolean = false,
)

class ExerciseEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {

    private val exerciseId = savedStateHandle.toRoute<ExerciseEditorRoute>().exerciseId
    private val _uiState = MutableStateFlow(ExerciseEditorUiState(isEditing = exerciseId != null))
    val uiState: StateFlow<ExerciseEditorUiState> = _uiState.asStateFlow()

    init {
        if (exerciseId != null) {
            viewModelScope.launch {
                val exercise = exerciseRepository.observeExercise(exerciseId).first() ?: return@launch
                _uiState.update {
                    it.copy(
                        name = exercise.name,
                        muscleGroup = exercise.muscleGroup,
                        equipment = exercise.equipment,
                        trackingType = exercise.trackingType,
                        description = exercise.description,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, showNameError = false) }
    fun onMuscleGroupChange(value: MuscleGroup) = _uiState.update { it.copy(muscleGroup = value) }
    fun onEquipmentChange(value: Equipment) = _uiState.update { it.copy(equipment = value) }
    fun onTrackingTypeChange(value: TrackingType) = _uiState.update { it.copy(trackingType = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            exerciseRepository.saveCustomExercise(
                id = exerciseId,
                name = state.name,
                muscleGroup = state.muscleGroup,
                equipment = state.equipment,
                trackingType = state.trackingType,
                description = state.description,
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
