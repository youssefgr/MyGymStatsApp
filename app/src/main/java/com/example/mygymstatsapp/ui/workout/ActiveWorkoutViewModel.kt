package com.example.mygymstatsapp.ui.workout

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.PlannedSet
import com.example.mygymstatsapp.domain.model.ProgressionPlanner
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.progressionPlan
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.PlanExercisesUseCase
import com.example.mygymstatsapp.session.ActiveWorkoutController
import com.example.mygymstatsapp.session.SessionEvent
import com.example.mygymstatsapp.timer.RestTimer
import com.example.mygymstatsapp.utils.parseDecimal
import com.example.mygymstatsapp.utils.toKg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ActiveWorkoutUiState(
    val isLoading: Boolean = true,
    val workout: Workout? = null,
    /** Last performance per exercise id, shown in the "Précédent" column. */
    val previous: Map<String, List<WorkoutSet>> = emptyMap(),
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = 90,
    val weightIncrementKg: Double = 2.5,
    val notificationPromptShown: Boolean = true,
    val isClosing: Boolean = false,
    /** Live heart rate from the watch, null when no watch is measuring it. */
    val heartRate: Int? = null,
)

sealed interface WorkoutAction {
    data class WeightChanged(val setId: String, val text: String) : WorkoutAction
    data class RepsChanged(val setId: String, val text: String) : WorkoutAction
    data class DurationChanged(val setId: String, val text: String) : WorkoutAction
    data class ToggleCompleted(val workoutExerciseId: String, val setId: String) : WorkoutAction
    data class CopyPrevious(val setId: String, val previous: WorkoutSet) : WorkoutAction
    data class ChangeSetType(val setId: String, val type: SetType) : WorkoutAction
    data class ChangeRpe(val setId: String, val rpe: Float?) : WorkoutAction
    data class AddSet(val workoutExerciseId: String) : WorkoutAction
    data class DeleteSet(val setId: String) : WorkoutAction
    data class MoveExercise(val workoutExerciseId: String, val offset: Int) : WorkoutAction
    data class RemoveExercise(val workoutExerciseId: String) : WorkoutAction
    data class ChangeRest(val workoutExerciseId: String, val seconds: Int?) : WorkoutAction
    data class LinkSuperset(val workoutExerciseId: String) : WorkoutAction
    data class UnlinkSuperset(val workoutExerciseId: String) : WorkoutAction
    data class AddWarmups(val workoutExerciseId: String, val count: Int) : WorkoutAction
    /** Moves the unfinished sets to the progression targets. */
    data class ApplyProgression(val workoutExerciseId: String) : WorkoutAction
    /** Puts last time's values back on the unfinished sets. */
    data class RevertProgression(val workoutExerciseId: String) : WorkoutAction
    data class UpdateProgression(val exerciseId: String, val enabled: Boolean, val incrementKg: Double?) : WorkoutAction
}

sealed interface ActiveWorkoutEvent {
    data class RecordAchieved(val record: NewPersonalRecord) : ActiveWorkoutEvent
    data class Message(@get:StringRes val textRes: Int) : ActiveWorkoutEvent
    data class Finished(val workoutId: String) : ActiveWorkoutEvent
    data object Discarded : ActiveWorkoutEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val planExercises: PlanExercisesUseCase,
    private val restTimer: RestTimer,
    private val controller: ActiveWorkoutController,
    liveHeartRate: Flow<Int?>,
) : ViewModel() {

    private val activeWorkout: Flow<Workout?> = workoutRepository.observeActiveWorkout()
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** Same source as the prefill: last time in this program when possible. */
    private val previousPerformances: Flow<Map<String, List<WorkoutSet>>> = activeWorkout
        .map { workout -> workout?.templateId to workout?.exercises?.map { it.exercise.id }?.toSet().orEmpty() }
        .distinctUntilChanged()
        .mapLatest { (templateId, ids) -> ids.associateWith { workoutRepository.getLastPerformance(it, templateId) } }
        .onStart { emit(emptyMap()) }

    private val isClosing = MutableStateFlow(false)

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        activeWorkout,
        previousPerformances,
        preferencesRepository.profile,
        isClosing,
        liveHeartRate,
    ) { workout, previous, profile, closing, heartRate ->
        ActiveWorkoutUiState(
            isLoading = false,
            workout = workout,
            previous = previous,
            weightUnit = profile.weightUnit,
            defaultRestSeconds = profile.defaultRestSeconds,
            weightIncrementKg = profile.weightIncrementKg,
            notificationPromptShown = profile.notificationPromptShown,
            isClosing = closing,
            heartRate = heartRate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    val restTimerState: StateFlow<RestTimer.State> = restTimer.state

    private val _events = Channel<ActiveWorkoutEvent>(Channel.BUFFERED)
    val events: Flow<ActiveWorkoutEvent> = _events.receiveAsFlow()

    init {
        // Events of the session, whether the action came from this screen or from the watch.
        viewModelScope.launch {
            controller.events.collect { event ->
                when (event) {
                    is SessionEvent.RecordAchieved -> _events.send(ActiveWorkoutEvent.RecordAchieved(event.record))
                    is SessionEvent.NeedsValue -> _events.send(
                        ActiveWorkoutEvent.Message(
                            if (event.trackingType == TrackingType.DURATION) R.string.msg_enter_duration else R.string.msg_enter_reps
                        )
                    )
                    is SessionEvent.Finished -> _events.send(ActiveWorkoutEvent.Finished(event.workoutId))
                    SessionEvent.FinishRefused -> {
                        isClosing.value = false
                        _events.send(ActiveWorkoutEvent.Message(R.string.msg_finish_needs_set))
                    }
                    SessionEvent.Discarded -> _events.send(ActiveWorkoutEvent.Discarded)
                }
            }
        }
    }

    /** Writes go through the session queue (shared with the watch), in order. */
    private fun enqueue(block: suspend () -> Unit) = controller.enqueue(block)

    fun onAction(action: WorkoutAction) {
        val unit = uiState.value.weightUnit
        when (action) {
            is WorkoutAction.WeightChanged -> enqueue {
                workoutRepository.updateSetWeight(action.setId, parseDecimal(action.text)?.let { unit.toKg(it) })
            }
            is WorkoutAction.RepsChanged -> enqueue {
                workoutRepository.updateSetReps(action.setId, action.text.toIntOrNull())
            }
            is WorkoutAction.DurationChanged -> enqueue {
                // Entered in minutes (decimals allowed), stored in seconds.
                workoutRepository.updateSetDuration(action.setId, parseDecimal(action.text)?.let { (it * 60).roundToInt() })
            }
            is WorkoutAction.CopyPrevious -> enqueue {
                workoutRepository.updateSetValues(
                    action.setId,
                    action.previous.weightKg,
                    action.previous.reps,
                    action.previous.durationSeconds,
                )
            }
            is WorkoutAction.ToggleCompleted -> controller.toggleCompleted(action.workoutExerciseId, action.setId)
            is WorkoutAction.ChangeSetType -> enqueue { workoutRepository.updateSetType(action.setId, action.type) }
            is WorkoutAction.ChangeRpe -> enqueue { workoutRepository.updateSetRpe(action.setId, action.rpe) }
            is WorkoutAction.AddSet -> enqueue { workoutRepository.addSet(action.workoutExerciseId) }
            is WorkoutAction.DeleteSet -> enqueue { workoutRepository.deleteSet(action.setId) }
            is WorkoutAction.MoveExercise -> enqueue {
                workoutRepository.moveExercise(action.workoutExerciseId, action.offset)
            }
            is WorkoutAction.RemoveExercise -> enqueue { workoutRepository.removeExercise(action.workoutExerciseId) }
            is WorkoutAction.ChangeRest -> enqueue {
                workoutRepository.updateExerciseRest(action.workoutExerciseId, action.seconds)
            }
            is WorkoutAction.LinkSuperset -> enqueue { workoutRepository.linkSupersetWithNext(action.workoutExerciseId) }
            is WorkoutAction.UnlinkSuperset -> enqueue { workoutRepository.unlinkSuperset(action.workoutExerciseId) }
            is WorkoutAction.AddWarmups -> enqueue {
                workoutRepository.addWarmupSets(action.workoutExerciseId, action.count)
            }
            is WorkoutAction.ApplyProgression -> enqueue {
                val state = uiState.value
                val workoutExercise = state.findExercise(action.workoutExerciseId) ?: return@enqueue
                planFor(workoutExercise, state)?.let { workoutRepository.applySetTargets(workoutExercise.id, it.targets) }
            }
            is WorkoutAction.RevertProgression -> enqueue {
                val state = uiState.value
                state.findExercise(action.workoutExerciseId)?.let { revertToLastTime(it, state) }
            }
            is WorkoutAction.UpdateProgression -> enqueue {
                exerciseRepository.updateProgression(action.exerciseId, action.enabled, action.incrementKg)
                // The unfinished sets of this workout follow the new settings right away.
                val state = uiState.value
                state.workout?.exercises?.filter { it.exercise.id == action.exerciseId }?.forEach { workoutExercise ->
                    val updated = workoutExercise.exercise.copy(
                        progressionEnabled = action.enabled,
                        progressionIncrementKg = action.incrementKg,
                    )
                    val plan = planFor(workoutExercise, state, updated)
                    if (plan != null) {
                        workoutRepository.applySetTargets(workoutExercise.id, plan.targets)
                    } else {
                        revertToLastTime(workoutExercise, state)
                    }
                }
            }
        }
    }

    private fun ActiveWorkoutUiState.findExercise(workoutExerciseId: String): WorkoutExercise? =
        workout?.exercises?.firstOrNull { it.id == workoutExerciseId }

    private fun planFor(
        workoutExercise: WorkoutExercise,
        state: ActiveWorkoutUiState,
        exercise: Exercise = workoutExercise.exercise,
    ): ProgressionPlanner.Plan? = exercise.progressionPlan(
        previous = state.previous[exercise.id].orEmpty(),
        repsMin = workoutExercise.targetRepsMin,
        repsMax = workoutExercise.targetRepsMax,
        defaultIncrementKg = state.weightIncrementKg,
    )

    private suspend fun revertToLastTime(workoutExercise: WorkoutExercise, state: ActiveWorkoutUiState) {
        val lastWorking = state.previous[workoutExercise.exercise.id].orEmpty().filter { it.setType != SetType.WARMUP }
        if (lastWorking.isEmpty()) return
        workoutRepository.applySetTargets(
            workoutExercise.id,
            lastWorking.map { PlannedSet(it.weightKg, it.reps, it.durationSeconds) },
        )
    }

    fun onNotificationPromptShown() {
        viewModelScope.launch { preferencesRepository.updateProfile { it.copy(notificationPromptShown = true) } }
    }

    fun addExercises(exerciseIds: List<String>) {
        val workout = uiState.value.workout ?: return
        enqueue {
            val planned = planExercises(exerciseIds.map { PlannedExercise(exerciseId = it) }, workout.templateId)
            workoutRepository.addExercises(workout.id, planned)
        }
    }

    /**
     * Another exercise in this slot, keeping its targets, rest, notes and superset, with sets planned
     * from the new exercise's own history. Sets already validated stay with the old exercise.
     */
    fun replaceExercise(workoutExerciseId: String, exerciseId: String) {
        val workout = uiState.value.workout ?: return
        val current = workout.exercises.firstOrNull { it.id == workoutExerciseId } ?: return
        if (current.exercise.id == exerciseId) return
        val remaining = current.sets.filter { !it.completed }
        enqueue {
            val planned = planExercises(
                listOf(
                    PlannedExercise(
                        exerciseId = exerciseId,
                        targetSets = remaining.count { it.setType != SetType.WARMUP }.coerceAtLeast(1),
                        targetRepsMin = current.targetRepsMin,
                        targetRepsMax = current.targetRepsMax,
                        restSeconds = current.restSeconds,
                        notes = current.notes,
                        supersetGroup = current.supersetGroup,
                        warmupSets = remaining.count { it.setType == SetType.WARMUP },
                    )
                ),
                workout.templateId,
            ).first()
            workoutRepository.replaceExercise(workoutExerciseId, planned)
        }
    }

    fun rename(name: String) {
        val workoutId = uiState.value.workout?.id ?: return
        if (name.isBlank()) return
        enqueue { workoutRepository.renameWorkout(workoutId, name) }
    }

    fun updateNotes(notes: String) {
        val workoutId = uiState.value.workout?.id ?: return
        enqueue { workoutRepository.updateWorkoutNotes(workoutId, notes) }
    }

    fun startRestTimer(seconds: Int) = restTimer.start(seconds)
    fun adjustRestTimer(deltaSeconds: Int) = restTimer.adjust(deltaSeconds)
    fun pauseRestTimer() = restTimer.pause()
    fun resumeRestTimer() = restTimer.resume()
    fun skipRestTimer() = restTimer.skip()

    fun finishWorkout() {
        val workoutId = uiState.value.workout?.id ?: return
        isClosing.value = true
        controller.finish(workoutId)
    }

    fun discardWorkout() {
        val workoutId = uiState.value.workout?.id ?: return
        isClosing.value = true
        controller.discard(workoutId)
    }
}
