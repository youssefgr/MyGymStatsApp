package com.example.mygymstatsapp.data.repository

import androidx.room.withTransaction
import com.example.mygymstatsapp.data.local.dao.ProgramDao
import com.example.mygymstatsapp.data.local.dao.TemplateDao
import com.example.mygymstatsapp.data.local.database.GymDatabase
import com.example.mygymstatsapp.data.local.entities.ProgramEntity
import com.example.mygymstatsapp.data.mapper.toDomain
import com.example.mygymstatsapp.domain.model.NewSession
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OfflineProgramRepository(
    private val database: GymDatabase,
    private val programDao: ProgramDao,
    private val templateDao: TemplateDao,
) : ProgramRepository {

    override fun observePrograms(): Flow<List<Program>> =
        programDao.observePrograms().map { list -> list.map { it.toDomain() } }

    override fun observeProgram(id: String): Flow<Program?> =
        programDao.observeProgram(id).map { it?.toDomain() }

    override suspend fun createProgram(name: String, notes: String): String = createProgram(name, notes, emptyList())

    override suspend fun createProgram(name: String, notes: String, sessions: List<NewSession>): String {
        val now = System.currentTimeMillis()
        val programId = UUID.randomUUID().toString()
        database.withTransaction {
            programDao.insert(ProgramEntity(programId, name.trim(), notes.trim(), now, now))
            sessions.forEachIndexed { position, session ->
                writeTemplate(
                    dao = templateDao,
                    id = UUID.randomUUID().toString(),
                    name = session.name,
                    notes = session.notes,
                    exercises = session.exercises,
                    programId = programId,
                    programPosition = position,
                )
            }
        }
        return programId
    }

    override suspend fun updateProgram(id: String, name: String, notes: String) =
        programDao.update(id, name.trim(), notes.trim(), System.currentTimeMillis())

    override suspend fun deleteProgram(id: String) {
        database.withTransaction {
            templateDao.deleteProgramTemplates(id)
            programDao.delete(id)
        }
    }

    override suspend fun assignSession(templateId: String, programId: String?) {
        database.withTransaction {
            val position = programId?.let { templateDao.getMaxProgramPosition(it) + 1 } ?: 0
            templateDao.assignToProgram(templateId, programId, position, System.currentTimeMillis())
        }
    }

    override suspend fun moveSession(templateId: String, offset: Int) {
        database.withTransaction {
            val programId = templateDao.getTemplateEntity(templateId)?.programId ?: return@withTransaction
            val ordered = templateDao.getProgramTemplates(programId).toMutableList()
            val from = ordered.indexOfFirst { it.id == templateId }
            val to = (from + offset).coerceIn(0, ordered.lastIndex)
            if (from == to) return@withTransaction
            ordered.add(to, ordered.removeAt(from))
            ordered.forEachIndexed { index, template ->
                if (template.programPosition != index) templateDao.updateProgramPosition(template.id, index)
            }
        }
    }
}
