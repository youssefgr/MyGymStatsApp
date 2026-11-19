package com.example.mygymstatsapp.data.local.entities

import androidx.room.Embedded
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.TrackingType

/** Rows produced by aggregate queries (not tables). */

data class WorkoutSummaryRow(
    val id: String,
    val name: String,
    val templateId: String?,
    val startedAt: Long,
    val durationSeconds: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    /** Comma-separated [MuscleGroup] names (GROUP_CONCAT). */
    val muscleGroups: String?,
)

data class ActiveWorkoutHeaderRow(val id: String, val name: String, val startedAt: Long)

data class PeriodStatsRow(
    val workoutCount: Int,
    val totalDurationSeconds: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
)

data class ExerciseProgressRow(
    val workoutId: String,
    val date: Long,
    val maxWeightKg: Double?,
    val maxReps: Int?,
    val volumeKg: Double,
    val bestOneRmKg: Double?,
    val totalReps: Int?,
    val setCount: Int,
    val totalDurationSeconds: Int?,
)

data class ExerciseBestsRow(
    val exerciseId: String,
    val maxWeightKg: Double?,
    val maxReps: Int?,
    val bestOneRmKg: Double?,
    val bestVolumeKg: Double?,
)

data class PersonalRecordRow(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val trackingType: TrackingType,
    val maxWeightKg: Double?,
    val maxReps: Int?,
    val bestOneRmKg: Double?,
    val bestVolumeKg: Double?,
    val lastPerformedAt: Long,
)

data class PerformedExerciseRow(
    @Embedded val exercise: ExerciseEntity,
    val lastPerformedAt: Long,
)

data class RepRangeRow(val targetRepsMin: Int?, val targetRepsMax: Int)

data class ExerciseUsageRow(val exerciseId: String, val name: String, val count: Int)

data class MuscleGroupShareRow(val muscleGroup: MuscleGroup, val setCount: Int)
