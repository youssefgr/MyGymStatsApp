package com.example.mygymstatsapp.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A group of sessions ([WorkoutTemplateEntity.programId]). No foreign key on the sessions side:
 * adding one would rebuild workout_templates in the migration; the repository deletes a program's
 * sessions itself.
 */
@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey val id: String,
    val name: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
)
