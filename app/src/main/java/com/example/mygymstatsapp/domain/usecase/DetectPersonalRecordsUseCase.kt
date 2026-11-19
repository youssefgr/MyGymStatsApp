package com.example.mygymstatsapp.domain.usecase

import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseBests
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.RecordType
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.bests
import com.example.mygymstatsapp.domain.repository.StatsRepository

/**
 * Compares performances with everything done before. An exercise performed for the first
 * time never produces a record (there is nothing to beat yet).
 */
class DetectPersonalRecordsUseCase(private val statsRepository: StatsRepository) {

    /** Records beaten in a workout: at most one per exercise, the most significant one. */
    suspend operator fun invoke(workout: Workout): List<NewPersonalRecord> {
        val previous = statsRepository.getBestsBefore(
            exerciseIds = workout.exercises.map { it.exercise.id }.toSet(),
            excludeWorkoutId = workout.id,
            before = workout.startedAt,
        )
        return workout.exercises.mapNotNull { exercise ->
            previous[exercise.exercise.id]?.let { detect(exercise, it) }
        }
    }

    /** Previous bests for the live check during a workout. */
    suspend fun previousBests(workout: Workout): Map<String, ExerciseBests> =
        statsRepository.getBestsBefore(
            exerciseIds = workout.exercises.map { it.exercise.id }.toSet(),
            excludeWorkoutId = workout.id,
            before = Long.MAX_VALUE,
        )

    /** Live check of a single set that has just been completed. */
    fun checkSet(exercise: Exercise, set: WorkoutSet, best: ExerciseBests): NewPersonalRecord? {
        if (set.setType == SetType.WARMUP) return null
        val weight = set.weightKg ?: 0.0
        val reps = set.reps ?: 0
        return when {
            exercise.trackingType == TrackingType.WEIGHT_REPS && weight > best.maxWeightKg + EPSILON ->
                record(exercise, RecordType.MAX_WEIGHT, weight, best.maxWeightKg, weight, reps)
            exercise.trackingType == TrackingType.WEIGHT_REPS &&
                set.estimatedOneRepMaxKg > best.bestOneRmKg + EPSILON ->
                record(exercise, RecordType.ONE_RM, set.estimatedOneRepMaxKg, best.bestOneRmKg, weight, reps)
            exercise.trackingType == TrackingType.REPS_ONLY && reps > best.maxReps ->
                record(exercise, RecordType.MAX_REPS, reps.toDouble(), best.maxReps.toDouble(), null, reps)
            else -> null
        }
    }

    private fun detect(workoutExercise: WorkoutExercise, previous: ExerciseBests): NewPersonalRecord? {
        val current = workoutExercise.bests() ?: return null
        val exercise = workoutExercise.exercise
        val working = workoutExercise.sets.filter { it.completed && it.setType != SetType.WARMUP }
        return when (exercise.trackingType) {
            TrackingType.WEIGHT_REPS -> when {
                current.maxWeightKg > previous.maxWeightKg + EPSILON -> {
                    val top = working.filter { it.weightKg == current.maxWeightKg }.maxBy { it.reps ?: 0 }
                    record(exercise, RecordType.MAX_WEIGHT, current.maxWeightKg, previous.maxWeightKg, top.weightKg, top.reps)
                }
                current.bestOneRmKg > previous.bestOneRmKg + EPSILON -> {
                    val top = working.maxBy { it.estimatedOneRepMaxKg }
                    record(exercise, RecordType.ONE_RM, current.bestOneRmKg, previous.bestOneRmKg, top.weightKg, top.reps)
                }
                current.bestVolumeKg > previous.bestVolumeKg + EPSILON ->
                    record(exercise, RecordType.VOLUME, current.bestVolumeKg, previous.bestVolumeKg, null, null)
                else -> null
            }
            TrackingType.REPS_ONLY -> if (current.maxReps > previous.maxReps) {
                record(exercise, RecordType.MAX_REPS, current.maxReps.toDouble(), previous.maxReps.toDouble(), null, current.maxReps)
            } else {
                null
            }
            TrackingType.DURATION -> null
        }
    }

    private fun record(
        exercise: Exercise,
        type: RecordType,
        value: Double,
        previous: Double,
        weightKg: Double?,
        reps: Int?,
    ) = NewPersonalRecord(exercise.id, exercise.name, type, value, previous, weightKg, reps)

    private companion object {
        const val EPSILON = 0.01
    }
}
