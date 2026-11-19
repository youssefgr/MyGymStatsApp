package com.example.mygymstatsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.example.mygymstatsapp.data.local.entities.ExerciseBestsRow
import com.example.mygymstatsapp.data.local.entities.ExerciseProgressRow
import com.example.mygymstatsapp.data.local.entities.ExerciseUsageRow
import com.example.mygymstatsapp.data.local.entities.MuscleGroupShareRow
import com.example.mygymstatsapp.data.local.entities.PerformedExerciseRow
import com.example.mygymstatsapp.data.local.entities.PeriodStatsRow
import com.example.mygymstatsapp.data.local.entities.PersonalRecordRow
import kotlinx.coroutines.flow.Flow

/** Epley estimated 1RM of a set, same formula as OneRepMax.epley(). */
const val ONE_RM_SQL =
    "CASE WHEN s.weightKg > 0 AND s.reps > 0 THEN " +
        "(CASE WHEN s.reps = 1 THEN s.weightKg ELSE s.weightKg * (1 + s.reps / 30.0) END) END"

/** Completed working sets (warm-ups excluded) of completed workouts. */
private const val WORKING_SETS =
    " FROM workout_sets s" +
        " JOIN workout_exercises we ON we.id = s.workoutExerciseId" +
        " JOIN workouts w ON w.id = we.workoutId" +
        " WHERE s.completed = 1 AND s.setType != 'WARMUP' AND w.status = 'COMPLETED' "

private const val SET_VOLUME_SQL = "COALESCE(s.weightKg, 0) * COALESCE(s.reps, 0)"

/** Per workout-exercise bests, aggregated again by the callers. */
private const val BESTS_PER_SESSION =
    "SELECT we.exerciseId AS exerciseId, w.id AS workoutId, w.startedAt AS date," +
        " MAX(s.weightKg) AS maxWeight, MAX(s.reps) AS maxReps," +
        " MAX(" + ONE_RM_SQL + ") AS bestOneRm, SUM(" + SET_VOLUME_SQL + ") AS volume" +
        WORKING_SETS

@Dao
interface StatsDao {

    @Query(
        "SELECT " +
            "(SELECT COUNT(*) FROM workouts w WHERE w.status = 'COMPLETED' AND w.startedAt >= :from AND w.startedAt < :to) AS workoutCount, " +
            "(SELECT COALESCE(SUM(w.durationSeconds), 0) FROM workouts w WHERE w.status = 'COMPLETED' AND w.startedAt >= :from AND w.startedAt < :to) AS totalDurationSeconds, " +
            "(SELECT COUNT(*) FROM workout_exercises we JOIN workouts w ON w.id = we.workoutId WHERE w.status = 'COMPLETED' AND w.startedAt >= :from AND w.startedAt < :to) AS exerciseCount, " +
            "(SELECT COUNT(*)" + WORKING_SETS + "AND w.startedAt >= :from AND w.startedAt < :to) AS setCount, " +
            "(SELECT COALESCE(SUM(" + SET_VOLUME_SQL + "), 0)" + WORKING_SETS + "AND w.startedAt >= :from AND w.startedAt < :to) AS volumeKg"
    )
    fun observePeriodStats(from: Long, to: Long): Flow<PeriodStatsRow>

    @Query(
        "SELECT w.id AS workoutId, w.startedAt AS date, MAX(s.weightKg) AS maxWeightKg, MAX(s.reps) AS maxReps," +
            " COALESCE(SUM(" + SET_VOLUME_SQL + "), 0) AS volumeKg, MAX(" + ONE_RM_SQL + ") AS bestOneRmKg," +
            " SUM(s.reps) AS totalReps, COUNT(*) AS setCount, SUM(s.durationSeconds) AS totalDurationSeconds" +
            WORKING_SETS +
            "AND we.exerciseId = :exerciseId AND w.startedAt >= :since" +
            " GROUP BY w.id ORDER BY w.startedAt"
    )
    fun observeExerciseProgress(exerciseId: String, since: Long): Flow<List<ExerciseProgressRow>>

    @Query(
        "SELECT x.exerciseId AS exerciseId, e.name AS exerciseName, e.muscleGroup AS muscleGroup," +
            " e.trackingType AS trackingType, MAX(x.maxWeight) AS maxWeightKg, MAX(x.maxReps) AS maxReps," +
            " MAX(x.bestOneRm) AS bestOneRmKg, MAX(x.volume) AS bestVolumeKg, MAX(x.date) AS lastPerformedAt" +
            " FROM (" + BESTS_PER_SESSION + " GROUP BY we.id) x" +
            " JOIN exercises e ON e.id = x.exerciseId" +
            " GROUP BY x.exerciseId ORDER BY e.name COLLATE NOCASE"
    )
    fun observePersonalRecords(): Flow<List<PersonalRecordRow>>

    /** Best values per exercise in completed workouts started before [before], excluding one workout. */
    @Query(
        "SELECT x.exerciseId AS exerciseId, MAX(x.maxWeight) AS maxWeightKg, MAX(x.maxReps) AS maxReps," +
            " MAX(x.bestOneRm) AS bestOneRmKg, MAX(x.volume) AS bestVolumeKg" +
            " FROM (" + BESTS_PER_SESSION +
            "AND we.exerciseId IN (:exerciseIds) AND w.id != :excludeWorkoutId AND w.startedAt < :before" +
            " GROUP BY we.id) x" +
            " GROUP BY x.exerciseId"
    )
    suspend fun getBestsBefore(
        exerciseIds: List<String>,
        excludeWorkoutId: String,
        before: Long,
    ): List<ExerciseBestsRow>

    @Query(
        """
        SELECT e.*, MAX(w.startedAt) AS lastPerformedAt FROM exercises e
        JOIN workout_exercises we ON we.exerciseId = e.id
        JOIN workouts w ON w.id = we.workoutId
        WHERE w.status = 'COMPLETED'
        GROUP BY e.id
        ORDER BY lastPerformedAt DESC
        """
    )
    fun observePerformedExercises(): Flow<List<PerformedExerciseRow>>

    @Query(
        """
        SELECT e.id AS exerciseId, e.name AS name, COUNT(*) AS count FROM workout_exercises we
        JOIN workouts w ON w.id = we.workoutId
        JOIN exercises e ON e.id = we.exerciseId
        WHERE w.status = 'COMPLETED' AND w.startedAt >= :since
        GROUP BY e.id
        ORDER BY count DESC, e.name COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun observeTopExercises(since: Long, limit: Int): Flow<List<ExerciseUsageRow>>

    @Query(
        """
        SELECT e.muscleGroup AS muscleGroup, COUNT(*) AS setCount FROM workout_sets s
        JOIN workout_exercises we ON we.id = s.workoutExerciseId
        JOIN workouts w ON w.id = we.workoutId
        JOIN exercises e ON e.id = we.exerciseId
        WHERE s.completed = 1 AND s.setType != 'WARMUP' AND w.status = 'COMPLETED' AND w.startedAt >= :since
        GROUP BY e.muscleGroup
        ORDER BY setCount DESC
        """
    )
    fun observeMuscleGroupShares(since: Long): Flow<List<MuscleGroupShareRow>>
}
