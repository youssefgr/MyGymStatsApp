package com.example.mygymstatsapp.domain.usecase

import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WarmupPlanner
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.first
import java.time.LocalTime

sealed interface StartRequest {
    data object Empty : StartRequest
    data class FromTemplate(val templateId: String) : StartRequest
    data class Repeat(val workoutId: String) : StartRequest
}

sealed interface StartResult {
    data class Started(val workoutId: String) : StartResult
    data object ActiveWorkoutExists : StartResult
    data object NotFound : StartResult
}

/** Name given to an empty workout, depending on the time of day (localized by the UI layer). */
fun interface DefaultWorkoutName {
    fun forHour(hour: Int): String
}

/** Starts a workout (empty, from a template, or repeating a past one). Only one can be in progress. */
class StartWorkoutUseCase(
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val planExercises: PlanExercisesUseCase,
    private val defaultName: DefaultWorkoutName,
) {
    suspend operator fun invoke(request: StartRequest, replaceActive: Boolean = false): StartResult {
        val activeId = workoutRepository.getActiveWorkoutId()
        if (activeId != null) {
            if (!replaceActive) return StartResult.ActiveWorkoutExists
            workoutRepository.deleteWorkout(activeId)
        }

        val autoWarmup = preferencesRepository.profile.first().autoWarmup

        val workoutId = when (request) {
            StartRequest.Empty -> workoutRepository.createWorkout(defaultName.forHour(LocalTime.now().hour), null, emptyList())

            is StartRequest.FromTemplate -> {
                val template = templateRepository.getTemplate(request.templateId) ?: return StartResult.NotFound
                templateRepository.markUsed(template.id)
                workoutRepository.createWorkout(
                    name = template.name,
                    templateId = template.id,
                    exercises = planExercises(
                        template.exercises.mapIndexed { index, it ->
                            PlannedExercise(
                                exerciseId = it.exercise.id,
                                targetSets = it.targetSets,
                                targetRepsMin = it.targetRepsMin,
                                targetRepsMax = it.targetRepsMax,
                                restSeconds = it.restSeconds,
                                notes = it.notes,
                                supersetGroup = it.supersetGroup,
                                warmupSets = warmupCount(autoWarmup, index, it.exercise.trackingType),
                            )
                        },
                        template.id,
                    ),
                )
            }

            is StartRequest.Repeat -> {
                val past = workoutRepository.getWorkout(request.workoutId) ?: return StartResult.NotFound
                workoutRepository.createWorkout(
                    name = past.name,
                    templateId = past.templateId,
                    exercises = planExercises(
                        past.exercises.mapIndexed { index, it ->
                            PlannedExercise(
                                exerciseId = it.exercise.id,
                                targetSets = it.sets.count { set -> set.completed && set.setType != SetType.WARMUP }
                                    .coerceAtLeast(1),
                                targetRepsMin = it.targetRepsMin,
                                targetRepsMax = it.targetRepsMax,
                                restSeconds = it.restSeconds,
                                notes = it.notes,
                                supersetGroup = it.supersetGroup,
                                warmupSets = warmupCount(autoWarmup, index, it.exercise.trackingType),
                            )
                        },
                        past.templateId,
                    ),
                )
            }
        }
        return StartResult.Started(workoutId)
    }

    /** The user's program: 3 warm-up sets on the first exercise, 1 light set on the others. */
    private fun warmupCount(enabled: Boolean, index: Int, trackingType: TrackingType): Int = when {
        !enabled || trackingType != TrackingType.WEIGHT_REPS -> 0
        index == 0 -> WarmupPlanner.FIRST_EXERCISE_WARMUPS
        else -> WarmupPlanner.OTHER_EXERCISE_WARMUPS
    }
}
