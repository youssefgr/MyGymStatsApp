package com.example.mygymstatsapp.ui.programs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.navigation.ProgramDetailRoute
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgramDetailUiState(
    val isLoading: Boolean = true,
    val program: Program? = null,
    /** Sessions that can be added to this program. */
    val standaloneSessions: List<WorkoutTemplate> = emptyList(),
) {
    /** Deleted (here or elsewhere): the screen closes. */
    val isMissing: Boolean get() = !isLoading && program == null
}

class ProgramDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val programRepository: ProgramRepository,
    private val templateRepository: TemplateRepository,
    startWorkoutUseCase: StartWorkoutUseCase,
) : WorkoutLauncherViewModel(startWorkoutUseCase) {

    private val programId = savedStateHandle.toRoute<ProgramDetailRoute>().programId

    val uiState: StateFlow<ProgramDetailUiState> = combine(
        programRepository.observeProgram(programId),
        programRepository.observePrograms(),
        templateRepository.observeTemplates(),
    ) { program, programs, templates ->
        ProgramDetailUiState(
            isLoading = false,
            program = program,
            standaloneSessions = standaloneSessions(templates, programs),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramDetailUiState())

    fun updateProgram(name: String, notes: String) {
        viewModelScope.launch { programRepository.updateProgram(programId, name, notes) }
    }

    fun deleteProgram() {
        viewModelScope.launch { programRepository.deleteProgram(programId) }
    }

    fun moveSession(sessionId: String, offset: Int) {
        viewModelScope.launch { programRepository.moveSession(sessionId, offset) }
    }

    fun addExistingSession(sessionId: String) {
        viewModelScope.launch { programRepository.assignSession(sessionId, programId) }
    }

    /** The session stays available in "Séances libres". */
    fun removeSession(sessionId: String) {
        viewModelScope.launch { programRepository.assignSession(sessionId, null) }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch { templateRepository.deleteTemplate(sessionId) }
    }
}
