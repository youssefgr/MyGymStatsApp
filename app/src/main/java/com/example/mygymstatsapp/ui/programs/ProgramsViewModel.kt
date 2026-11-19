package com.example.mygymstatsapp.ui.programs

import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgramsUiState(
    val isLoading: Boolean = true,
    val programs: List<Program> = emptyList(),
    /** Sessions outside any program. */
    val standaloneSessions: List<WorkoutTemplate> = emptyList(),
)

class ProgramsViewModel(
    private val templateRepository: TemplateRepository,
    private val programRepository: ProgramRepository,
    startWorkoutUseCase: StartWorkoutUseCase,
) : WorkoutLauncherViewModel(startWorkoutUseCase) {

    val uiState: StateFlow<ProgramsUiState> = combine(
        programRepository.observePrograms(),
        templateRepository.observeTemplates(),
    ) { programs, templates ->
        ProgramsUiState(isLoading = false, programs = programs, standaloneSessions = standaloneSessions(templates, programs))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramsUiState())

    private val _createdProgram = Channel<String>(Channel.BUFFERED)
    /** Id of a program just created by hand, to open it. */
    val createdProgram: Flow<String> = _createdProgram.receiveAsFlow()

    fun createProgram(name: String, notes: String) {
        viewModelScope.launch { _createdProgram.send(programRepository.createProgram(name, notes)) }
    }

    fun deleteSession(id: String) {
        viewModelScope.launch { templateRepository.deleteTemplate(id) }
    }

    fun addSessionToProgram(sessionId: String, programId: String) {
        viewModelScope.launch { programRepository.assignSession(sessionId, programId) }
    }
}

/** Sessions shown as standalone: no program, or a program that no longer exists. */
fun standaloneSessions(templates: List<WorkoutTemplate>, programs: List<Program>): List<WorkoutTemplate> {
    val programIds = programs.mapTo(HashSet()) { it.id }
    return templates.filter { it.programId == null || it.programId !in programIds }
}
