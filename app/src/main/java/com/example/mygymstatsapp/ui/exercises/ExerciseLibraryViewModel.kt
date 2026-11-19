package com.example.mygymstatsapp.ui.exercises

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.navigation.ExerciseLibraryRoute
import com.example.mygymstatsapp.ui.LabelResolver
import com.example.mygymstatsapp.utils.normalizedForSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LibraryFilter {
    data object All : LibraryFilter
    data object Favorites : LibraryFilter
    data class Group(val group: MuscleGroup) : LibraryFilter
}

data class ExerciseLibraryUiState(
    val isLoading: Boolean = true,
    val pickMode: Boolean = false,
    /** Replace mode: one tap picks the replacement of this item (see ExerciseLibraryRoute). */
    val replaceKey: String? = null,
    val query: String = "",
    val filter: LibraryFilter = LibraryFilter.All,
    val exercises: List<Exercise> = emptyList(),
    /** Ordered: exercises are added to the workout in the order they were picked. */
    val selectedIds: List<String> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultIncrementKg: Double = 2.5,
) {
    /** Grouped by muscle group only when browsing everything. */
    val showGroupHeaders: Boolean get() = filter == LibraryFilter.All && query.isBlank()
}

class ExerciseLibraryViewModel(
    savedStateHandle: SavedStateHandle,
    private val exerciseRepository: ExerciseRepository,
    preferencesRepository: UserPreferencesRepository,
    private val labels: LabelResolver,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<ExerciseLibraryRoute>()
    private val pickMode = route.pickMode
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow<LibraryFilter>(LibraryFilter.All)
    private val selectedIds = MutableStateFlow<List<String>>(emptyList())

    val uiState: StateFlow<ExerciseLibraryUiState> = combine(
        exerciseRepository.observeExercises(),
        query,
        filter,
        selectedIds,
        preferencesRepository.profile,
    ) { exercises, query, filter, selected, profile ->
        val needle = query.trim().normalizedForSearch()
        val filtered = exercises
            .asSequence()
            .filter {
                when (filter) {
                    LibraryFilter.All -> true
                    LibraryFilter.Favorites -> it.isFavorite
                    is LibraryFilter.Group -> it.muscleGroup == filter.group
                }
            }
            .filter {
                needle.isEmpty() ||
                    it.name.normalizedForSearch().contains(needle) ||
                    labels.muscleGroup(it.muscleGroup).normalizedForSearch().contains(needle) ||
                    labels.equipment(it.equipment).normalizedForSearch().contains(needle)
            }
            .sortedWith(compareBy<Exercise> { it.muscleGroup.ordinal }.thenBy { it.name.lowercase() })
            .toList()
        ExerciseLibraryUiState(
            isLoading = false,
            pickMode = pickMode,
            replaceKey = route.replaceKey,
            query = query,
            filter = filter,
            exercises = filtered,
            selectedIds = selected,
            weightUnit = profile.weightUnit,
            defaultIncrementKg = profile.weightIncrementKg,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseLibraryUiState(pickMode = pickMode, replaceKey = route.replaceKey))

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onFilterChange(value: LibraryFilter) {
        filter.value = value
    }

    fun toggleSelection(exerciseId: String) {
        selectedIds.update { if (exerciseId in it) it - exerciseId else it + exerciseId }
    }

    fun toggleFavorite(exerciseId: String) {
        viewModelScope.launch { exerciseRepository.toggleFavorite(exerciseId) }
    }

    fun setProgression(exerciseId: String, enabled: Boolean, incrementKg: Double?) {
        viewModelScope.launch { exerciseRepository.updateProgression(exerciseId, enabled, incrementKg) }
    }

    fun deleteExercise(exerciseId: String) {
        selectedIds.update { it - exerciseId }
        viewModelScope.launch { exerciseRepository.deleteExercise(exerciseId) }
    }
}
