package com.example.mygymstatsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.example.mygymstatsapp.data.local.entities.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {

    @Query("SELECT * FROM exercises WHERE isArchived = 0 ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeExercise(id: String): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id IN (:ids)")
    suspend fun getExercises(ids: List<String>): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Query("UPDATE exercises SET isFavorite = NOT isFavorite, updatedAt = :now WHERE id = :id")
    suspend fun toggleFavorite(id: String, now: Long)

    @Query(
        "UPDATE exercises SET progressionEnabled = :enabled, progressionIncrementKg = :incrementKg, updatedAt = :now WHERE id = :id"
    )
    suspend fun updateProgression(id: String, enabled: Boolean, incrementKg: Double?, now: Long)

    @Query("UPDATE exercises SET isArchived = 1, isFavorite = 0, updatedAt = :now WHERE id = :id")
    suspend fun archive(id: String, now: Long)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT COUNT(*) FROM workout_exercises WHERE exerciseId = :id")
    suspend fun countWorkoutUsages(id: String): Int
}
