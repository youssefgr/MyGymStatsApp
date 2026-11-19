package com.example.mygymstatsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.mygymstatsapp.data.local.entities.ActiveWorkoutHeaderRow
import com.example.mygymstatsapp.data.local.entities.RepRangeRow
import com.example.mygymstatsapp.data.local.entities.WorkoutEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutExerciseEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutSetEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutSummaryRow
import com.example.mygymstatsapp.data.local.entities.WorkoutWithExercises
import com.example.mygymstatsapp.domain.model.SetType
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // ---- Reads -------------------------------------------------------------------------------

    @Transaction
    @Query("SELECT * FROM workouts WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(): Flow<WorkoutWithExercises?>

    @Query("SELECT id, name, startedAt FROM workouts WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkoutHeader(): Flow<ActiveWorkoutHeaderRow?>

    @Query("SELECT id FROM workouts WHERE status = 'IN_PROGRESS' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveWorkoutId(): String?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: String): Flow<WorkoutWithExercises?>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: String): WorkoutWithExercises?

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkoutEntity(id: String): WorkoutEntity?

    /** One row per completed workout with its aggregates. [limit] = -1 means no limit. */
    @Query(
        """
        SELECT w.id AS id, w.name AS name, w.templateId AS templateId, w.startedAt AS startedAt,
            w.durationSeconds AS durationSeconds,
            (SELECT COUNT(*) FROM workout_exercises we WHERE we.workoutId = w.id) AS exerciseCount,
            (SELECT COUNT(*) FROM workout_sets s
                JOIN workout_exercises we ON we.id = s.workoutExerciseId
                WHERE we.workoutId = w.id AND s.completed = 1) AS setCount,
            (SELECT COALESCE(SUM(COALESCE(s.weightKg, 0) * COALESCE(s.reps, 0)), 0) FROM workout_sets s
                JOIN workout_exercises we ON we.id = s.workoutExerciseId
                WHERE we.workoutId = w.id AND s.completed = 1 AND s.setType != 'WARMUP') AS volumeKg,
            (SELECT GROUP_CONCAT(DISTINCT e.muscleGroup) FROM workout_exercises we
                JOIN exercises e ON e.id = we.exerciseId
                WHERE we.workoutId = w.id) AS muscleGroups
        FROM workouts w
        WHERE w.status = 'COMPLETED'
        ORDER BY w.startedAt DESC
        LIMIT :limit
        """
    )
    fun observeCompletedSummaries(limit: Int): Flow<List<WorkoutSummaryRow>>

    @Query("SELECT startedAt FROM workouts WHERE status = 'COMPLETED' AND startedAt >= :from ORDER BY startedAt")
    fun observeCompletedStartTimes(from: Long): Flow<List<Long>>

    /** Completed sets of the most recent completed workout containing this exercise. */
    @Query(
        """
        SELECT * FROM workout_sets
        WHERE completed = 1 AND workoutExerciseId = (
            SELECT we.id FROM workout_exercises we
            JOIN workouts w ON w.id = we.workoutId
            WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED'
            ORDER BY w.startedAt DESC, we.position DESC
            LIMIT 1
        )
        ORDER BY setNumber
        """
    )
    suspend fun getLastPerformanceSets(exerciseId: String): List<WorkoutSetEntity>

    /** Same, among workouts started from [templateId] (an exercise can be trained differently in two programs). */
    @Query(
        """
        SELECT * FROM workout_sets
        WHERE completed = 1 AND workoutExerciseId = (
            SELECT we.id FROM workout_exercises we
            JOIN workouts w ON w.id = we.workoutId
            WHERE we.exerciseId = :exerciseId AND w.status = 'COMPLETED' AND w.templateId = :templateId
            ORDER BY w.startedAt DESC, we.position DESC
            LIMIT 1
        )
        ORDER BY setNumber
        """
    )
    suspend fun getLastPerformanceSetsInTemplate(exerciseId: String, templateId: String): List<WorkoutSetEntity>

    /** Rep range the exercise was most recently trained in. */
    @Query(
        """
        SELECT we.targetRepsMin, we.targetRepsMax FROM workout_exercises we
        JOIN workouts w ON w.id = we.workoutId
        WHERE we.exerciseId = :exerciseId AND we.targetRepsMax IS NOT NULL
        ORDER BY w.startedAt DESC
        LIMIT 1
        """
    )
    suspend fun getLastWorkoutRepRange(exerciseId: String): RepRangeRow?

    /** Rep range of the exercise in the most recently used program. */
    @Query(
        """
        SELECT te.targetRepsMin, te.targetRepsMax FROM template_exercises te
        JOIN workout_templates t ON t.id = te.templateId
        WHERE te.exerciseId = :exerciseId AND te.targetRepsMax IS NOT NULL
        ORDER BY t.lastUsedAt DESC, t.updatedAt DESC
        LIMIT 1
        """
    )
    suspend fun getTemplateRepRange(exerciseId: String): RepRangeRow?

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getWorkoutExercises(workoutId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun getWorkoutExercise(id: String): WorkoutExerciseEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun getMaxExercisePosition(workoutId: String): Int

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber")
    suspend fun getSets(workoutExerciseId: String): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :id")
    suspend fun getSet(id: String): WorkoutSetEntity?

    @Query(
        """
        SELECT COUNT(*) FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        WHERE we.workoutId = :workoutId AND s.completed = 1
        """
    )
    suspend fun countCompletedSets(workoutId: String): Int

    // ---- Workouts ----------------------------------------------------------------------------

    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Query("UPDATE workouts SET name = :name, updatedAt = :now WHERE id = :id")
    suspend fun renameWorkout(id: String, name: String, now: Long)

    @Query("UPDATE workouts SET notes = :notes, updatedAt = :now WHERE id = :id")
    suspend fun updateWorkoutNotes(id: String, notes: String, now: Long)

    @Query(
        """
        UPDATE workouts SET startedAt = :startedAt, durationSeconds = :durationSeconds,
            endedAt = :startedAt + :durationSeconds * 1000, updatedAt = :now
        WHERE id = :id
        """
    )
    suspend fun updateWorkoutTiming(id: String, startedAt: Long, durationSeconds: Long, now: Long)

    @Query(
        """
        UPDATE workouts SET status = 'COMPLETED', endedAt = :endedAt,
            durationSeconds = :durationSeconds, updatedAt = :endedAt
        WHERE id = :id
        """
    )
    suspend fun markCompleted(id: String, endedAt: Long, durationSeconds: Long)

    @Query("UPDATE workouts SET avgHeartRate = :avg, maxHeartRate = :max, updatedAt = :now WHERE id = :id")
    suspend fun updateHeartRate(id: String, avg: Int, max: Int, now: Long)

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: String)

    // ---- Workout exercises -------------------------------------------------------------------

    @Insert
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Query("UPDATE workout_exercises SET position = :position WHERE id = :id")
    suspend fun updateExercisePosition(id: String, position: Int)

    @Query("UPDATE workout_exercises SET restSeconds = :restSeconds WHERE id = :id")
    suspend fun updateExerciseRest(id: String, restSeconds: Int?)

    @Query("UPDATE workout_exercises SET notes = :notes WHERE id = :id")
    suspend fun updateExerciseNotes(id: String, notes: String)

    @Query("UPDATE workout_exercises SET supersetGroup = :group WHERE id = :id")
    suspend fun updateExerciseSuperset(id: String, group: Int?)

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: String)

    @Query(
        """
        DELETE FROM workout_exercises WHERE workoutId = :workoutId
        AND id NOT IN (SELECT workoutExerciseId FROM workout_sets)
        """
    )
    suspend fun deleteExercisesWithoutSets(workoutId: String)

    // ---- Sets (field-level updates so concurrent edits never overwrite each other) -----------

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Query("UPDATE workout_sets SET weightKg = :weightKg WHERE id = :id")
    suspend fun updateSetWeight(id: String, weightKg: Double?)

    @Query("UPDATE workout_sets SET reps = :reps WHERE id = :id")
    suspend fun updateSetReps(id: String, reps: Int?)

    @Query("UPDATE workout_sets SET durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun updateSetDuration(id: String, durationSeconds: Int?)

    @Query("UPDATE workout_sets SET weightKg = :weightKg, reps = :reps, durationSeconds = :durationSeconds WHERE id = :id")
    suspend fun updateSetValues(id: String, weightKg: Double?, reps: Int?, durationSeconds: Int?)

    @Query("UPDATE workout_sets SET setType = :setType WHERE id = :id")
    suspend fun updateSetType(id: String, setType: SetType)

    @Query("UPDATE workout_sets SET rpe = :rpe WHERE id = :id")
    suspend fun updateSetRpe(id: String, rpe: Float?)

    @Query("UPDATE workout_sets SET completed = :completed, completedAt = :completedAt, restSeconds = :restSeconds WHERE id = :id")
    suspend fun updateSetCompletion(id: String, completed: Boolean, completedAt: Long?, restSeconds: Int?)

    @Query("UPDATE workout_sets SET setNumber = :setNumber WHERE id = :id")
    suspend fun updateSetNumber(id: String, setNumber: Int)

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: String)

    @Query(
        """
        DELETE FROM workout_sets WHERE completed = 0 AND workoutExerciseId IN
            (SELECT id FROM workout_exercises WHERE workoutId = :workoutId)
        """
    )
    suspend fun deleteIncompleteSets(workoutId: String)

    @Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId AND completed = 0")
    suspend fun deleteIncompleteSetsOf(workoutExerciseId: String)

    @Query("UPDATE workout_exercises SET exerciseId = :exerciseId WHERE id = :id")
    suspend fun updateWorkoutExerciseExercise(id: String, exerciseId: String)
}
