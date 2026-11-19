package com.example.mygymstatsapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.TrackingType

/**
 * Predefined (isCustom = false) and user-created exercises.
 * Exercises used in a workout are archived instead of deleted to keep history intact.
 */
@Entity(tableName = "exercises", indices = [Index("muscleGroup")])
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipment: Equipment,
    val trackingType: TrackingType,
    val description: String,
    val isCustom: Boolean,
    val isFavorite: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    @ColumnInfo(defaultValue = "1") val progressionEnabled: Boolean = true,
    val progressionIncrementKg: Double? = null,
)
