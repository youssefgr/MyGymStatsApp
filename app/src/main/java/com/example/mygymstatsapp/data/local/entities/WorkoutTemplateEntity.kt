package com.example.mygymstatsapp.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A session ("Push Day"), started to create a [WorkoutEntity]. May belong to a [ProgramEntity]. */
@Entity(tableName = "workout_templates", indices = [Index("programId")])
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?,
    val programId: String? = null,
    /** Day order inside the program. */
    @ColumnInfo(defaultValue = "0") val programPosition: Int = 0,
)
