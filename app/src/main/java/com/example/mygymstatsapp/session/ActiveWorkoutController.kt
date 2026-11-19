package com.example.mygymstatsapp.session

import com.example.mygymstatsapp.domain.model.ExerciseBests
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.fallbackSet
import com.example.mygymstatsapp.domain.model.matchPreviousSets
import com.example.mygymstatsapp.domain.model.raisedLoads
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.DetectPersonalRecordsUseCase
import com.example.mygymstatsapp.timer.RestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What the workout in progress reports to whoever drives it (the phone's workout screen, the watch). */
sealed interface SessionEvent {
    data class RecordAchieved(val record: NewPersonalRecord) : SessionEvent

    /** The set can't be validated: reps (or a duration) are missing, here and last time. */
    data class NeedsValue(val trackingType: TrackingType) : SessionEvent

    data class Finished(val workoutId: String) : SessionEvent

    /** Finishing needs at least one validated set. */
    data object FinishRefused : SessionEvent

    data object Discarded : SessionEvent
}

/**
 * The workout in progress, independent of any screen. The phone's workout screen and the watch
 * (through PhoneWearSync) drive the same session, so validating a set does the same thing
 * wherever it is tapped: fallback to last time's values, rest timer, load carried to the next
 * sets, live record check.
 *
 * Every database write goes through one queue, consumed in order on the application scope: a value
 * typed just before tapping ✓ is always saved before the set is completed, writes coming from the
 * phone and the watch never interleave, and pending writes survive leaving the screen.
 */
class ActiveWorkoutController(
    private val workoutRepository: WorkoutRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val restTimer: RestTimer,
    private val detectRecords: DetectPersonalRecordsUseCase,
    scope: CoroutineScope,
) {
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    /** Best performances so far per exercise id (null = never done: no record to celebrate). */
    private val runningBests = mutableMapOf<String, ExerciseBests?>()

    /** Workout the [runningBests] belong to: they start over with each workout. */
    private var bestsWorkoutId: String? = null

    init {
        scope.launch {
            for (write in writes) {
                runCatching { write() }
            }
        }
    }

    fun enqueue(block: suspend () -> Unit) {
        writes.trySend(block)
    }

    fun setValues(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?) = enqueue {
        workoutRepository.updateSetValues(setId, weightKg, reps, durationSeconds)
    }

    fun toggleCompleted(workoutExerciseId: String, setId: String) = enqueue {
        val set = workoutRepository.getSet(setId) ?: return@enqueue
        if (set.completed) {
            workoutRepository.setSetCompleted(setId, completed = false, restSeconds = null)
            return@enqueue
        }
        val workout = workoutRepository.getActiveWorkoutId()?.let { workoutRepository.getWorkout(it) } ?: return@enqueue
        val workoutExercise = workout.exercises.firstOrNull { it.id == workoutExerciseId } ?: return@enqueue
        val exercise = workoutExercise.exercise
        val lastTime = workoutRepository.getLastPerformance(exercise.id, workout.templateId)
        val previous = matchPreviousSets(workoutExercise.sets, lastTime)[setId]

        // Empty fields fall back to last time, else to the set before (see fallbackSet).
        val fallback = fallbackSet(workoutExercise.sets, set, previous)
        val weight = set.weightKg ?: fallback?.weightKg
        val reps = set.reps ?: fallback?.reps
        val duration = set.durationSeconds ?: fallback?.durationSeconds
        val isValid = when (exercise.trackingType) {
            TrackingType.WEIGHT_REPS, TrackingType.REPS_ONLY -> (reps ?: 0) > 0
            TrackingType.DURATION -> (duration ?: 0) > 0
        }
        if (!isValid) {
            _events.emit(SessionEvent.NeedsValue(exercise.trackingType))
            return@enqueue
        }
        if (weight != set.weightKg || reps != set.reps || duration != set.durationSeconds) {
            workoutRepository.updateSetValues(setId, weight, reps, duration)
        }
        // In a superset, rest only after the last exercise of the round.
        val exercises = workout.exercises
        val next = exercises.getOrNull(exercises.indexOfFirst { it.id == workoutExerciseId } + 1)
        val chainsIntoNext = workoutExercise.supersetGroup != null && next?.supersetGroup == workoutExercise.supersetGroup
        val defaultRest = preferencesRepository.profile.first().defaultRestSeconds
        val rest = if (chainsIntoNext) 0 else workoutExercise.restSeconds ?: defaultRest
        workoutRepository.setSetCompleted(setId, completed = true, restSeconds = rest)
        if (rest > 0) restTimer.start(rest)

        // Heavier than the set before: the following lighter sets go up too (see raisedLoads).
        raisedLoads(workoutExercise.sets, set, weight, previous?.weightKg).forEach { (followingId, load) ->
            workoutRepository.updateSetWeight(followingId, load)
        }

        val completedSet = set.copy(weightKg = weight, reps = reps, durationSeconds = duration, completed = true)
        checkRecord(workout, exercise.id, completedSet)
    }

    fun finish(workoutId: String) = enqueue {
        if (workoutRepository.completeWorkout(workoutId)) {
            restTimer.skip()
            _events.emit(SessionEvent.Finished(workoutId))
        } else {
            _events.emit(SessionEvent.FinishRefused)
        }
    }

    fun discard(workoutId: String) = enqueue {
        workoutRepository.deleteWorkout(workoutId)
        restTimer.skip()
        _events.emit(SessionEvent.Discarded)
    }

    private suspend fun checkRecord(workout: Workout, exerciseId: String, set: WorkoutSet) {
        if (bestsWorkoutId != workout.id) {
            bestsWorkoutId = workout.id
            runningBests.clear()
        }
        if (exerciseId !in runningBests) {
            val bests = detectRecords.previousBests(workout)
            workout.exercises.forEach { runningBests.putIfAbsent(it.exercise.id, bests[it.exercise.id]) }
        }
        val best = runningBests[exerciseId] ?: return
        val exercise = workout.exercises.first { it.exercise.id == exerciseId }.exercise
        val record = detectRecords.checkSet(exercise, set, best)
        if (record != null) _events.emit(SessionEvent.RecordAchieved(record))
        // Raise the bar so the same performance is not celebrated twice.
        runningBests[exerciseId] = best.copy(
            maxWeightKg = maxOf(best.maxWeightKg, set.weightKg ?: 0.0),
            maxReps = maxOf(best.maxReps, set.reps ?: 0),
            bestOneRmKg = maxOf(best.bestOneRmKg, set.estimatedOneRepMaxKg),
        )
    }
}
