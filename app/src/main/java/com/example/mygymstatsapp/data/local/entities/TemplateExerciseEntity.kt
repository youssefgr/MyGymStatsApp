package com.example.mygymstatsapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("templateId"), Index("exerciseId")],
)
data class TemplateExerciseEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val exerciseId: String,
    val position: Int,
    val targetSets: Int,
    val targetRepsMin: Int?,
    val targetRepsMax: Int?,
    val restSeconds: Int?,
    val notes: String,
    /** Same value on consecutive exercises = superset (added in schema v2). */
    val supersetGroup: Int?,
)
