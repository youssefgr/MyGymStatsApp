package com.example.mygymstatsapp.domain.model

data class Exercise(
    val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val description: String,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    /** Progressive overload: next workout's sets move forward from last time (see ProgressionPlanner). */
    val progressionEnabled: Boolean = true,
    /** Load step for this exercise; null = the profile's default step. */
    val progressionIncrementKg: Double? = null,
)

/** A session ("Pecs lourd + triceps"): the exercises to do, started to create a [Workout]. */
data class WorkoutTemplate(
    val id: String,
    val name: String,
    val notes: String,
    val exercises: List<TemplateExercise>,
    /** Program the session belongs to; null for a standalone session. */
    val programId: String? = null,
    val lastUsedAt: Long? = null,
) {
    val totalSets: Int get() = exercises.sumOf { it.targetSets }
}

/** A group of sessions trained in rotation ("4-day program": one session per training day). */
data class Program(
    val id: String,
    val name: String,
    val notes: String,
    /** In program order: day 1, day 2… */
    val sessions: List<WorkoutTemplate>,
) {
    /** The session after the last one started; the first one when none was started yet. */
    val nextSession: WorkoutTemplate?
        get() {
            val last = sessions.withIndex()
                .filter { it.value.lastUsedAt != null }
                .maxByOrNull { it.value.lastUsedAt!! }
                ?: return sessions.firstOrNull()
            return sessions[(last.index + 1) % sessions.size]
        }
}

/** A session to create together with its program. */
data class NewSession(val name: String, val notes: String, val exercises: List<PlannedExercise>)

data class TemplateExercise(
    val id: String,
    val exercise: Exercise,
    val targetSets: Int,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val restSeconds: Int?,
    val notes: String,
    /** Exercises sharing the same group number are performed as a superset. */
    val supersetGroup: Int?,
)

/** An exercise to insert in a workout or a template, with optional targets. */
data class PlannedExercise(
    val exerciseId: String,
    val targetSets: Int? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val restSeconds: Int? = null,
    val notes: String = "",
    val supersetGroup: Int? = null,
    /** Warm-up sets to generate before the working sets (input of PlanExercisesUseCase). */
    val warmupSets: Int = 0,
    /** Sets to create, in order (output of PlanExercisesUseCase). Empty = blank working sets. */
    val sets: List<PlannedSet> = emptyList(),
)

/** A rep range ("8-12"); [min] is null for a fixed target ("10"). */
data class RepRange(val min: Int?, val max: Int)

data class Workout(
    val id: String,
    val name: String,
    val templateId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Long,
    val status: WorkoutStatus,
    val notes: String,
    val exercises: List<WorkoutExercise>,
    /** Measured by the watch; null when the workout was done without it. */
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
) {
    val volumeKg: Double get() = exercises.sumOf { it.volumeKg }
    val completedSetCount: Int get() = exercises.sumOf { e -> e.sets.count { it.completed } }
    val totalSetCount: Int get() = exercises.sumOf { it.sets.size }
}

data class WorkoutExercise(
    val id: String,
    val workoutId: String,
    val exercise: Exercise,
    val position: Int,
    val notes: String,
    val restSeconds: Int?,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val supersetGroup: Int?,
    val sets: List<WorkoutSet>,
) {
    val volumeKg: Double get() = sets.sumOf { it.volumeKg }
}

data class WorkoutSet(
    val id: String,
    val workoutExerciseId: String,
    val setNumber: Int,
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val rpe: Float?,
    val setType: SetType,
    val completed: Boolean,
    val completedAt: Long?,
    val restSeconds: Int?,
) {
    /** Volume = weight × reps, for completed working sets only (warm-ups excluded). */
    val volumeKg: Double
        get() = if (completed && setType != SetType.WARMUP) (weightKg ?: 0.0) * (reps ?: 0) else 0.0

    val estimatedOneRepMaxKg: Double get() = OneRepMax.epley(weightKg ?: 0.0, reps ?: 0)
}

data class WorkoutSummary(
    val id: String,
    val name: String,
    val templateId: String?,
    val startedAt: Long,
    val durationSeconds: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
    val muscleGroups: Set<MuscleGroup>,
)

data class ActiveWorkoutHeader(val id: String, val name: String, val startedAt: Long)

data class PeriodStats(
    val workoutCount: Int,
    val totalDurationSeconds: Long,
    val exerciseCount: Int,
    val setCount: Int,
    val volumeKg: Double,
) {
    val averageDurationSeconds: Long get() = if (workoutCount == 0) 0 else totalDurationSeconds / workoutCount

    companion object {
        val EMPTY = PeriodStats(0, 0, 0, 0, 0.0)
    }
}

/** Aggregated performance of one exercise in one workout. */
data class ExerciseProgressPoint(
    val workoutId: String,
    val date: Long,
    val maxWeightKg: Double,
    val maxReps: Int,
    val volumeKg: Double,
    val bestOneRmKg: Double,
    val totalReps: Int,
    val setCount: Int,
    val totalDurationSeconds: Int,
)

data class ExerciseBests(
    val maxWeightKg: Double,
    val maxReps: Int,
    val bestOneRmKg: Double,
    val bestVolumeKg: Double,
)

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val muscleGroup: MuscleGroup,
    val trackingType: TrackingType,
    val bests: ExerciseBests,
    val lastPerformedAt: Long,
)

/** A record beaten in a given workout (or set). */
data class NewPersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val type: RecordType,
    val value: Double,
    val previousValue: Double,
    val weightKg: Double?,
    val reps: Int?,
)

data class ExerciseUsage(val exerciseId: String, val name: String, val count: Int)

data class MuscleGroupShare(val muscleGroup: MuscleGroup, val setCount: Int)

data class UserProfile(
    val name: String = "",
    val bodyWeightKg: Double? = null,
    val heightCm: Int? = null,
    val goal: Goal = Goal.MUSCLE_GAIN,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = 90,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Load added when the progression rule is met (double progression). */
    val weightIncrementKg: Double = 2.5,
    /** Generate warm-up sets when a workout starts: 3 on the first exercise, 1 on the others. */
    val autoWarmup: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val notificationPromptShown: Boolean = false,
)
