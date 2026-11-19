package com.example.mygymstatsapp.ui.programs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.GeneratorOptions
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.NewSession
import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.ProgramGenerator
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.ui.LabelResolver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionPreview(
    val name: String,
    val notes: String,
    /** Exercise + its targets, in order. */
    val exercises: List<Pair<Exercise, PlannedExercise>>,
)

data class ProgramGeneratorUiState(
    val isLoading: Boolean = true,
    val options: GeneratorOptions = GeneratorOptions(),
    val name: String = "",
    val sessions: List<SessionPreview> = emptyList(),
    val isSaving: Boolean = false,
)

/** Live preview of the generated program; every change of options rebuilds it. */
class ProgramGeneratorViewModel(
    exerciseRepository: ExerciseRepository,
    private val programRepository: ProgramRepository,
    preferencesRepository: UserPreferencesRepository,
    private val labels: LabelResolver,
) : ViewModel() {

    /** Null until the profile goal is known (it is the default objective). */
    private val options = MutableStateFlow<GeneratorOptions?>(null)
    /** Name typed by the user; null = follow the suggested name. */
    private val customName = MutableStateFlow<String?>(null)
    private val isSaving = MutableStateFlow(false)

    private val _created = Channel<String>(Channel.BUFFERED)
    /** Id of the created program, to open it. */
    val created: Flow<String> = _created.receiveAsFlow()

    init {
        viewModelScope.launch {
            val goal = preferencesRepository.profile.first().goal
            options.value = GeneratorOptions(sessionsPerWeek = DEFAULT_SESSIONS, goal = goal)
        }
    }

    val uiState: StateFlow<ProgramGeneratorUiState> = combine(
        options.filterNotNull(),
        exerciseRepository.observeExercises(),
        customName,
        isSaving,
    ) { options, exercises, name, saving ->
        val byId = exercises.associateBy { it.id }
        ProgramGeneratorUiState(
            isLoading = false,
            options = options,
            name = name ?: labels.generatedProgramName(options),
            sessions = ProgramGenerator.generate(options, exercises).map { session ->
                SessionPreview(
                    name = labels.sessionName(session),
                    notes = labels.sessionNotes(session),
                    exercises = session.exercises.mapNotNull { planned -> byId[planned.exerciseId]?.let { it to planned } },
                )
            },
            isSaving = saving,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgramGeneratorUiState())

    fun toggleFocus(group: MuscleGroup) = updateOptions { options ->
        val focus = if (group in options.focus) options.focus - group else options.focus + group
        options.copy(focus = focus, focusOnly = options.focusOnly && focus.isNotEmpty())
    }

    fun setFocusOnly(value: Boolean) = updateOptions { it.copy(focusOnly = value && it.focus.isNotEmpty()) }

    fun setSessionsPerWeek(value: Int) = updateOptions { it.copy(sessionsPerWeek = value) }

    fun setGoal(value: Goal) = updateOptions { it.copy(goal = value) }

    fun setName(value: String) {
        customName.value = value
    }

    fun create() {
        val state = uiState.value
        if (state.isLoading || state.isSaving || state.sessions.isEmpty()) return
        isSaving.value = true
        viewModelScope.launch {
            val id = programRepository.createProgram(
                name = state.name.ifBlank { labels.generatedProgramName(state.options) },
                notes = labels.generatedProgramNotes(state.options),
                sessions = state.sessions.map { session ->
                    NewSession(session.name, session.notes, session.exercises.map { it.second })
                },
            )
            _created.send(id)
        }
    }

    private fun updateOptions(transform: (GeneratorOptions) -> GeneratorOptions) {
        options.update { it?.let(transform) }
    }

    private companion object {
        const val DEFAULT_SESSIONS = 3
    }
}
