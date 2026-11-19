package com.example.mygymstatsapp.data.mapper

import com.example.mygymstatsapp.data.local.entities.ActiveWorkoutHeaderRow
import com.example.mygymstatsapp.data.local.entities.ExerciseBestsRow
import com.example.mygymstatsapp.data.local.entities.ExerciseEntity
import com.example.mygymstatsapp.data.local.entities.ExerciseProgressRow
import com.example.mygymstatsapp.data.local.entities.PeriodStatsRow
import com.example.mygymstatsapp.data.local.entities.PersonalRecordRow
import com.example.mygymstatsapp.data.local.entities.ProgramWithSessions
import com.example.mygymstatsapp.data.local.entities.TemplateWithExercises
import com.example.mygymstatsapp.data.local.entities.WorkoutExerciseWithSets
import com.example.mygymstatsapp.data.local.entities.WorkoutSetEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutSummaryRow
import com.example.mygymstatsapp.data.local.entities.WorkoutWithExercises
import com.example.mygymstatsapp.domain.model.ActiveWorkoutHeader
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseBests
import com.example.mygymstatsapp.domain.model.ExerciseProgressPoint
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.PeriodStats
import com.example.mygymstatsapp.domain.model.PersonalRecord
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.TemplateExercise
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.model.WorkoutTemplate

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    equipment = equipment,
    trackingType = trackingType,
    description = description,
    isCustom = isCustom,
    isFavorite = isFavorite,
    progressionEnabled = progressionEnabled,
    progressionIncrementKg = progressionIncrementKg,
)

fun ProgramWithSessions.toDomain() = Program(
    id = program.id,
    name = program.name,
    notes = program.notes,
    sessions = sessions
        .sortedWith(compareBy({ it.template.programPosition }, { it.template.createdAt }))
        .map { it.toDomain() },
)

fun TemplateWithExercises.toDomain() = WorkoutTemplate(
    id = template.id,
    name = template.name,
    notes = template.notes,
    programId = template.programId,
    lastUsedAt = template.lastUsedAt,
    exercises = exercises
        .sortedBy { it.templateExercise.position }
        .map {
            TemplateExercise(
                id = it.templateExercise.id,
                exercise = it.exercise.toDomain(),
                targetSets = it.templateExercise.targetSets,
                targetRepsMin = it.templateExercise.targetRepsMin,
                targetRepsMax = it.templateExercise.targetRepsMax,
                restSeconds = it.templateExercise.restSeconds,
                notes = it.templateExercise.notes,
                supersetGroup = it.templateExercise.supersetGroup,
            )
        },
)

fun WorkoutSetEntity.toDomain() = WorkoutSet(
    id = id,
    workoutExerciseId = workoutExerciseId,
    setNumber = setNumber,
    weightKg = weightKg,
    reps = reps,
    durationSeconds = durationSeconds,
    rpe = rpe,
    setType = setType,
    completed = completed,
    completedAt = completedAt,
    restSeconds = restSeconds,
)

fun WorkoutExerciseWithSets.toDomain() = WorkoutExercise(
    id = workoutExercise.id,
    workoutId = workoutExercise.workoutId,
    exercise = exercise.toDomain(),
    position = workoutExercise.position,
    notes = workoutExercise.notes,
    restSeconds = workoutExercise.restSeconds,
    targetRepsMin = workoutExercise.targetRepsMin,
    targetRepsMax = workoutExercise.targetRepsMax,
    supersetGroup = workoutExercise.supersetGroup,
    sets = sets.sortedBy { it.setNumber }.map { it.toDomain() },
)

fun WorkoutWithExercises.toDomain() = Workout(
    id = workout.id,
    name = workout.name,
    templateId = workout.templateId,
    startedAt = workout.startedAt,
    endedAt = workout.endedAt,
    durationSeconds = workout.durationSeconds,
    status = workout.status,
    notes = workout.notes,
    exercises = exercises.sortedBy { it.workoutExercise.position }.map { it.toDomain() },
    avgHeartRate = workout.avgHeartRate,
    maxHeartRate = workout.maxHeartRate,
)

fun WorkoutSummaryRow.toDomain() = WorkoutSummary(
    id = id,
    name = name,
    templateId = templateId,
    startedAt = startedAt,
    durationSeconds = durationSeconds,
    exerciseCount = exerciseCount,
    setCount = setCount,
    volumeKg = volumeKg,
    muscleGroups = muscleGroups.orEmpty()
        .split(',')
        .mapNotNull { name -> MuscleGroup.entries.firstOrNull { it.name == name } }
        .toSet(),
)

fun ActiveWorkoutHeaderRow.toDomain() = ActiveWorkoutHeader(id, name, startedAt)

fun PeriodStatsRow.toDomain() = PeriodStats(
    workoutCount = workoutCount,
    totalDurationSeconds = totalDurationSeconds,
    exerciseCount = exerciseCount,
    setCount = setCount,
    volumeKg = volumeKg,
)

fun ExerciseProgressRow.toDomain() = ExerciseProgressPoint(
    workoutId = workoutId,
    date = date,
    maxWeightKg = maxWeightKg ?: 0.0,
    maxReps = maxReps ?: 0,
    volumeKg = volumeKg,
    bestOneRmKg = bestOneRmKg ?: 0.0,
    totalReps = totalReps ?: 0,
    setCount = setCount,
    totalDurationSeconds = totalDurationSeconds ?: 0,
)

fun ExerciseBestsRow.toDomain() = ExerciseBests(
    maxWeightKg = maxWeightKg ?: 0.0,
    maxReps = maxReps ?: 0,
    bestOneRmKg = bestOneRmKg ?: 0.0,
    bestVolumeKg = bestVolumeKg ?: 0.0,
)

fun PersonalRecordRow.toDomain() = PersonalRecord(
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    muscleGroup = muscleGroup,
    trackingType = trackingType,
    bests = ExerciseBests(
        maxWeightKg = maxWeightKg ?: 0.0,
        maxReps = maxReps ?: 0,
        bestOneRmKg = bestOneRmKg ?: 0.0,
        bestVolumeKg = bestVolumeKg ?: 0.0,
    ),
    lastPerformedAt = lastPerformedAt,
)
