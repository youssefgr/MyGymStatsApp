package com.example.mygymstatsapp.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.SupersetGrouping
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.navigation.TemplateEditorRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class EditableTemplateExercise(
    /** Local key: the same exercise may appear twice in a session. */
    val key: String,
    val exercise: Exercise,
    val sets: Int,
    val repsMin: String,
    val repsMax: String,
    val restSeconds: Int?,
    val notes: String,
    val supersetGroup: Int? = null,
)

data class TemplateEditorUiState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val name: String = "",
    val notes: String = "",
    val exercises: List<EditableTemplateExercise> = emptyList(),
    val showNameError: Boolean = false,
    val isSaved: Boolean = false,
)

class TemplateEditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val templateRepository: TemplateRepository,
    private val exerciseRepository: ExerciseRepository,
    startWorkoutUseCase: StartWorkoutUseCase,
) : WorkoutLauncherViewModel(startWorkoutUseCase) {

    private val route = savedStateHandle.toRoute<TemplateEditorRoute>()
    private var templateId: String? = route.templateId
    private val _uiState = MutableStateFlow(TemplateEditorUiState(isEditing = templateId != null))
    val uiState: StateFlow<TemplateEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val template = templateId?.let { templateRepository.getTemplate(it) }
            _uiState.update { state ->
                if (template == null) {
                    state.copy(isLoading = false)
                } else {
                    state.copy(
                        isLoading = false,
                        name = template.name,
                        notes = template.notes,
                        exercises = template.exercises.map {
                            EditableTemplateExercise(
                                key = newKey(),
                                exercise = it.exercise,
                                sets = it.targetSets,
                                repsMin = it.targetRepsMin?.toString().orEmpty(),
                                repsMax = it.targetRepsMax?.toString().orEmpty(),
                                restSeconds = it.restSeconds,
                                notes = it.notes,
                                supersetGroup = it.supersetGroup,
                            )
                        },
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, showNameError = false) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }

    fun addExercises(ids: List<String>) {
        viewModelScope.launch {
            val added = exerciseRepository.getExercises(ids).map {
                EditableTemplateExercise(newKey(), it, sets = 3, repsMin = "8", repsMax = "12", restSeconds = 90, notes = "")
            }
            _uiState.update { it.copy(exercises = it.exercises + added) }
        }
    }

    /** Another exercise in the same slot: sets, rep range, rest, notes and superset are kept. */
    fun replaceExercise(key: String, exerciseId: String) {
        viewModelScope.launch {
            val replacement = exerciseRepository.getExercises(listOf(exerciseId)).firstOrNull() ?: return@launch
            updateExercise(key) { it.copy(exercise = replacement) }
        }
    }

    fun updateExercise(key: String, transform: (EditableTemplateExercise) -> EditableTemplateExercise) =
        _uiState.update { state ->
            state.copy(exercises = state.exercises.map { if (it.key == key) transform(it) else it })
        }

    fun removeExercise(key: String) = _uiState.update { state ->
        state.copy(exercises = withGroups(state.exercises.filterNot { it.key == key }, SupersetGrouping::normalize))
    }

    fun moveExercise(key: String, offset: Int) = _uiState.update { state ->
        val list = state.exercises.toMutableList()
        val from = list.indexOfFirst { it.key == key }
        val to = (from + offset).coerceIn(0, list.lastIndex)
        if (from < 0 || from == to) return@update state
        list.add(to, list.removeAt(from))
        state.copy(exercises = withGroups(list, SupersetGrouping::normalize))
    }

    fun linkSupersetWithNext(key: String) = _uiState.update { state ->
        val index = state.exercises.indexOfFirst { it.key == key }
        state.copy(exercises = withGroups(state.exercises) { SupersetGrouping.linkWithNext(it, index) })
    }

    fun unlinkSuperset(key: String) = _uiState.update { state ->
        val index = state.exercises.indexOfFirst { it.key == key }
        state.copy(exercises = withGroups(state.exercises) { SupersetGrouping.unlink(it, index) })
    }

    private fun withGroups(
        exercises: List<EditableTemplateExercise>,
        transform: (List<Int?>) -> List<Int?>,
    ): List<EditableTemplateExercise> =
        exercises.zip(transform(exercises.map { it.supersetGroup })) { exercise, group -> exercise.copy(supersetGroup = group) }

    fun save(startAfterSave: Boolean = false) {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(showNameError = true) }
            return
        }
        viewModelScope.launch {
            val id = templateRepository.saveTemplate(
                id = templateId,
                name = state.name,
                notes = state.notes,
                exercises = state.exercises.map {
                    val min = it.repsMin.toIntOrNull()
                    val max = it.repsMax.toIntOrNull()
                    PlannedExercise(
                        exerciseId = it.exercise.id,
                        targetSets = it.sets,
                        targetRepsMin = if (min != null && max != null) minOf(min, max) else min,
                        targetRepsMax = if (min != null && max != null) maxOf(min, max) else max,
                        restSeconds = it.restSeconds,
                        notes = it.notes.trim(),
                        supersetGroup = it.supersetGroup,
                    )
                },
                programId = route.programId,
            )
            templateId = id
            if (startAfterSave) startWorkout(StartRequest.FromTemplate(id)) else _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun newKey() = UUID.randomUUID().toString()
}
