package com.example.mygymstatsapp.data.repository

import androidx.room.withTransaction
import com.example.mygymstatsapp.data.local.dao.TemplateDao
import com.example.mygymstatsapp.data.local.database.GymDatabase
import com.example.mygymstatsapp.data.local.entities.TemplateExerciseEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutTemplateEntity
import com.example.mygymstatsapp.data.mapper.toDomain
import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OfflineTemplateRepository(
    private val database: GymDatabase,
    private val dao: TemplateDao,
) : TemplateRepository {

    override fun observeTemplates(): Flow<List<WorkoutTemplate>> =
        dao.observeTemplates().map { list -> list.map { it.toDomain() } }

    override suspend fun getTemplate(id: String): WorkoutTemplate? = dao.getTemplate(id)?.toDomain()

    override suspend fun saveTemplate(
        id: String?,
        name: String,
        notes: String,
        exercises: List<PlannedExercise>,
        programId: String?,
    ): String {
        val templateId = id ?: UUID.randomUUID().toString()
        database.withTransaction {
            val existing = dao.getTemplateEntity(templateId)
            // An existing session keeps its place; a new one goes last in its program.
            val program = existing?.programId ?: programId
            val position = existing?.programPosition ?: program?.let { dao.getMaxProgramPosition(it) + 1 } ?: 0
            writeTemplate(
                dao = dao,
                id = templateId,
                name = name,
                notes = notes,
                exercises = exercises,
                programId = program,
                programPosition = position,
                existing = existing,
            )
        }
        return templateId
    }

    override suspend fun deleteTemplate(id: String) = dao.deleteTemplate(id)

    override suspend fun markUsed(id: String) = dao.markUsed(id, System.currentTimeMillis())
}

private const val DEFAULT_TARGET_SETS = 3

/** Upserts a session and replaces its exercises. Call inside a transaction. */
internal suspend fun writeTemplate(
    dao: TemplateDao,
    id: String,
    name: String,
    notes: String,
    exercises: List<PlannedExercise>,
    programId: String?,
    programPosition: Int,
    existing: WorkoutTemplateEntity? = null,
) {
    val now = System.currentTimeMillis()
    dao.upsertTemplate(
        WorkoutTemplateEntity(
            id = id,
            name = name.trim(),
            notes = notes.trim(),
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            lastUsedAt = existing?.lastUsedAt,
            programId = programId,
            programPosition = programPosition,
        )
    )
    dao.deleteTemplateExercises(id)
    dao.insertTemplateExercises(
        exercises.mapIndexed { index, planned ->
            TemplateExerciseEntity(
                id = UUID.randomUUID().toString(),
                templateId = id,
                exerciseId = planned.exerciseId,
                position = index,
                targetSets = planned.targetSets ?: DEFAULT_TARGET_SETS,
                targetRepsMin = planned.targetRepsMin,
                targetRepsMax = planned.targetRepsMax,
                restSeconds = planned.restSeconds,
                notes = planned.notes,
                supersetGroup = planned.supersetGroup,
            )
        }
    )
}
