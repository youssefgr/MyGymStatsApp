package com.example.mygymstatsapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mygymstatsapp.domain.model.SetType

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutExerciseId")],
)
data class WorkoutSetEntity(
    @PrimaryKey val id: String,
    val workoutExerciseId: String,
    val setNumber: Int,
    /** Always stored in kilograms; converted to pounds for display only. */
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int?,
    val rpe: Float?,
    val setType: SetType,
    val completed: Boolean,
    val completedAt: Long?,
    val restSeconds: Int?,
)
