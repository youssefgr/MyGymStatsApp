package com.example.mygymstatsapp.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.mygymstatsapp.domain.model.WorkoutStatus

/**
 * A performed workout. The workout in progress is persisted too (status = IN_PROGRESS),
 * so nothing is lost if Android kills the app during a session.
 */
@Entity(
    tableName = "workouts",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("templateId"), Index("status", "startedAt")],
)
data class WorkoutEntity(
    @PrimaryKey val id: String,
    val name: String,
    val templateId: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val durationSeconds: Long,
    val status: WorkoutStatus,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Measured by the watch during the workout (v5); null without a watch. */
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
)
