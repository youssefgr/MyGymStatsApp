package com.example.mygymstatsapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.mygymstatsapp.data.local.entities.TemplateExerciseEntity
import com.example.mygymstatsapp.data.local.entities.TemplateWithExercises
import com.example.mygymstatsapp.data.local.entities.WorkoutTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Transaction
    @Query("SELECT * FROM workout_templates ORDER BY createdAt")
    fun observeTemplates(): Flow<List<TemplateWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplate(id: String): TemplateWithExercises?

    @Query("SELECT * FROM workout_templates WHERE id = :id")
    suspend fun getTemplateEntity(id: String): WorkoutTemplateEntity?

    @Upsert
    suspend fun upsertTemplate(template: WorkoutTemplateEntity)

    @Insert
    suspend fun insertTemplateExercises(exercises: List<TemplateExerciseEntity>)

    @Query("DELETE FROM template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercises(templateId: String)

    @Query("DELETE FROM workout_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)

    @Query("UPDATE workout_templates SET lastUsedAt = :now WHERE id = :id")
    suspend fun markUsed(id: String, now: Long)

    @Query("SELECT * FROM workout_templates WHERE programId = :programId ORDER BY programPosition, createdAt")
    suspend fun getProgramTemplates(programId: String): List<WorkoutTemplateEntity>

    @Query("SELECT COALESCE(MAX(programPosition), -1) FROM workout_templates WHERE programId = :programId")
    suspend fun getMaxProgramPosition(programId: String): Int

    @Query("UPDATE workout_templates SET programId = :programId, programPosition = :position, updatedAt = :now WHERE id = :id")
    suspend fun assignToProgram(id: String, programId: String?, position: Int, now: Long)

    @Query("UPDATE workout_templates SET programPosition = :position WHERE id = :id")
    suspend fun updateProgramPosition(id: String, position: Int)

    @Query("DELETE FROM workout_templates WHERE programId = :programId")
    suspend fun deleteProgramTemplates(programId: String)
}
