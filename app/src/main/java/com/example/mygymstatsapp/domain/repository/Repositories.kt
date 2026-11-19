package com.example.mygymstatsapp.domain.repository

import com.example.mygymstatsapp.domain.model.ActiveWorkoutHeader
import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseBests
import com.example.mygymstatsapp.domain.model.ExerciseProgressPoint
import com.example.mygymstatsapp.domain.model.ExerciseUsage
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.MuscleGroupShare
import com.example.mygymstatsapp.domain.model.NewSession
import com.example.mygymstatsapp.domain.model.PeriodStats
import com.example.mygymstatsapp.domain.model.PersonalRecord
import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.PlannedSet
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.RepRange
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.UserProfile
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import kotlinx.coroutines.flow.Flow

/*
 * Repository contracts used by ViewModels and use cases. The current implementations are
 * local (Room / DataStore); a cloud-synced implementation can be swapped in behind them.
 */

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>
    fun observeExercise(id: String): Flow<Exercise?>
    suspend fun getExercises(ids: List<String>): List<Exercise>
    suspend fun saveCustomExercise(
        id: String?,
        name: String,
        muscleGroup: MuscleGroup,
        equipment: Equipment,
        trackingType: TrackingType,
        description: String,
    ): String
    suspend fun toggleFavorite(id: String)
    /** Progressive overload settings of one exercise ([incrementKg] null = profile default). */
    suspend fun updateProgression(id: String, enabled: Boolean, incrementKg: Double?)
    /** Deletes a custom exercise, or archives it if it appears in the history. */
    suspend fun deleteExercise(id: String)
}

/** Sessions (workout templates), standalone or inside a program. */
interface TemplateRepository {
    fun observeTemplates(): Flow<List<WorkoutTemplate>>
    suspend fun getTemplate(id: String): WorkoutTemplate?
    /** Creates or updates a session. A new session with [programId] goes last in that program. */
    suspend fun saveTemplate(
        id: String?,
        name: String,
        notes: String,
        exercises: List<PlannedExercise>,
        programId: String? = null,
    ): String
    suspend fun deleteTemplate(id: String)
    suspend fun markUsed(id: String)
}

interface ProgramRepository {
    fun observePrograms(): Flow<List<Program>>
    fun observeProgram(id: String): Flow<Program?>
    suspend fun createProgram(name: String, notes: String): String
    /** Creates a program with its sessions in one transaction (generated programs). */
    suspend fun createProgram(name: String, notes: String, sessions: List<NewSession>): String
    suspend fun updateProgram(id: String, name: String, notes: String)
    /** Deletes the program and its sessions; workouts already done stay in the history. */
    suspend fun deleteProgram(id: String)
    /** Puts a session at the end of [programId], or makes it standalone when null. */
    suspend fun assignSession(templateId: String, programId: String?)
    /** Moves a session up (-1) or down (+1) inside its program. */
    suspend fun moveSession(templateId: String, offset: Int)
}

interface WorkoutRepository {
    fun observeActiveWorkout(): Flow<Workout?>
    fun observeActiveWorkoutHeader(): Flow<ActiveWorkoutHeader?>
    suspend fun getActiveWorkoutId(): String?
    fun observeWorkout(id: String): Flow<Workout?>
    suspend fun getWorkout(id: String): Workout?
    /** Completed workouts, most recent first. [limit] = -1 for all. */
    fun observeCompletedWorkouts(limit: Int = -1): Flow<List<WorkoutSummary>>
    /** Start times of the workouts completed since [from] (calendar dots). */
    fun observeCompletedStartTimes(from: Long): Flow<List<Long>>
    /**
     * Completed sets of the last completed workout containing this exercise, preferring
     * workouts started from [templateId] (same program, same targets).
     */
    suspend fun getLastPerformance(exerciseId: String, templateId: String? = null): List<WorkoutSet>
    /** Rep range the exercise was last trained in (workouts first, then programs). */
    suspend fun getLastRepRange(exerciseId: String): RepRange?
    suspend fun getSet(setId: String): WorkoutSet?

    /** Creates an in-progress workout with the planned sets (see PlanExercisesUseCase). */
    suspend fun createWorkout(name: String, templateId: String?, exercises: List<PlannedExercise>): String
    suspend fun addExercises(workoutId: String, exercises: List<PlannedExercise>)
    suspend fun removeExercise(workoutExerciseId: String)
    /**
     * Puts [replacement] (with its planned sets) in place of the exercise of [workoutExerciseId].
     * With nothing validated yet, it takes the same slot. Otherwise the validated sets stay with the
     * old exercise, which leaves its superset, and the new one comes right after it.
     */
    suspend fun replaceExercise(workoutExerciseId: String, replacement: PlannedExercise)
    suspend fun moveExercise(workoutExerciseId: String, offset: Int)
    suspend fun updateExerciseRest(workoutExerciseId: String, restSeconds: Int?)
    suspend fun linkSupersetWithNext(workoutExerciseId: String)
    suspend fun unlinkSuperset(workoutExerciseId: String)

    /** Adds a set duplicating the previous set's values. */
    suspend fun addSet(workoutExerciseId: String)
    /** Inserts [count] warm-up sets before the existing sets, based on the first working load. */
    suspend fun addWarmupSets(workoutExerciseId: String, count: Int)
    /**
     * Gives each unfinished working set its target (by rank among working sets, extra sets take
     * the last target) and rescales unfinished warm-ups on the first target's load.
     */
    suspend fun applySetTargets(workoutExerciseId: String, targets: List<PlannedSet>)
    suspend fun deleteSet(setId: String)
    suspend fun updateSetWeight(setId: String, weightKg: Double?)
    suspend fun updateSetReps(setId: String, reps: Int?)
    suspend fun updateSetDuration(setId: String, durationSeconds: Int?)
    suspend fun updateSetValues(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?)
    suspend fun updateSetType(setId: String, type: SetType)
    suspend fun updateSetRpe(setId: String, rpe: Float?)
    suspend fun setSetCompleted(setId: String, completed: Boolean, restSeconds: Int?)

    suspend fun renameWorkout(id: String, name: String)
    suspend fun updateWorkoutNotes(id: String, notes: String)
    suspend fun updateWorkoutTiming(id: String, startedAt: Long, durationSeconds: Long)
    /** Average and max heart rate measured by the watch so far (also after the workout ended). */
    suspend fun updateHeartRate(id: String, avg: Int, max: Int)
    /**
     * Drops unfinished sets and empty exercises, then marks the workout completed.
     * Returns false (and changes nothing) if no set was completed.
     */
    suspend fun completeWorkout(id: String): Boolean
    suspend fun deleteWorkout(id: String)
}

interface StatsRepository {
    fun observePeriodStats(from: Long, to: Long = Long.MAX_VALUE): Flow<PeriodStats>
    fun observeExerciseProgress(exerciseId: String, since: Long): Flow<List<ExerciseProgressPoint>>
    fun observePersonalRecords(): Flow<List<PersonalRecord>>
    /** Exercises present in at least one completed workout, most recently performed first. */
    fun observePerformedExercises(): Flow<List<Exercise>>
    fun observeTopExercises(since: Long, limit: Int): Flow<List<ExerciseUsage>>
    fun observeMuscleGroupShares(since: Long): Flow<List<MuscleGroupShare>>
    suspend fun getBestsBefore(
        exerciseIds: Collection<String>,
        excludeWorkoutId: String,
        before: Long,
    ): Map<String, ExerciseBests>
}

interface UserPreferencesRepository {
    val profile: Flow<UserProfile>
    suspend fun updateProfile(transform: (UserProfile) -> UserProfile)
}
