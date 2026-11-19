package com.example.mygymstatsapp.domain.usecase

import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.planSets
import com.example.mygymstatsapp.domain.model.progressionPlan
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first

/**
 * Fills exercises about to enter a workout with their sets: last time's weight and reps
 * (in the same program when possible), moved forward by progressive overload when the exercise has it on.
 */
class PlanExercisesUseCase(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository,
    private val preferencesRepository: UserPreferencesRepository,
) {
    suspend operator fun invoke(exercises: List<PlannedExercise>, templateId: String?): List<PlannedExercise> {
        val defaultIncrementKg = preferencesRepository.profile.first().weightIncrementKg
        val byId = exerciseRepository.getExercises(exercises.map { it.exerciseId }.distinct()).associateBy { it.id }
        return exercises.map { planned ->
            val exercise = byId[planned.exerciseId] ?: return@map planned
            val previous = workoutRepository.getLastPerformance(exercise.id, templateId)
            // Outside a program, the exercise keeps the rep range it was last trained in.
            val ranged = if (planned.targetRepsMax != null) {
                planned
            } else {
                workoutRepository.getLastRepRange(exercise.id)
                    ?.let { planned.copy(targetRepsMin = it.min, targetRepsMax = it.max) }
                    ?: planned
            }
            val progression = exercise.progressionPlan(previous, ranged.targetRepsMin, ranged.targetRepsMax, defaultIncrementKg)
            ranged.copy(sets = planSets(previous, ranged.targetSets, ranged.warmupSets, progression))
        }
    }
}
