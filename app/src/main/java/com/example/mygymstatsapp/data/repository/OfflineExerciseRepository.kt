package com.example.mygymstatsapp.data.repository

import com.example.mygymstatsapp.data.local.dao.ExerciseDao
import com.example.mygymstatsapp.data.local.entities.ExerciseEntity
import com.example.mygymstatsapp.data.mapper.toDomain
import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OfflineExerciseRepository(private val dao: ExerciseDao) : ExerciseRepository {

    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeExercises().map { list -> list.map { it.toDomain() } }

    override fun observeExercise(id: String): Flow<Exercise?> =
        dao.observeExercise(id).map { it?.toDomain() }

    override suspend fun getExercises(ids: List<String>): List<Exercise> {
        val byId = dao.getExercises(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    override suspend fun saveCustomExercise(
        id: String?,
        name: String,
        muscleGroup: MuscleGroup,
        equipment: Equipment,
        trackingType: TrackingType,
        description: String,
    ): String {
        val now = System.currentTimeMillis()
        val existing = id?.let { dao.getExercise(it) }
        val entity = ExerciseEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            muscleGroup = muscleGroup,
            equipment = equipment,
            trackingType = trackingType,
            description = description.trim(),
            isCustom = true,
            isFavorite = existing?.isFavorite ?: false,
            isArchived = false,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            progressionEnabled = existing?.progressionEnabled ?: true,
            progressionIncrementKg = existing?.progressionIncrementKg,
        )
        dao.upsert(entity)
        return entity.id
    }

    override suspend fun toggleFavorite(id: String) = dao.toggleFavorite(id, System.currentTimeMillis())

    override suspend fun updateProgression(id: String, enabled: Boolean, incrementKg: Double?) =
        dao.updateProgression(id, enabled, incrementKg, System.currentTimeMillis())

    override suspend fun deleteExercise(id: String) {
        if (dao.countWorkoutUsages(id) > 0) dao.archive(id, System.currentTimeMillis()) else dao.delete(id)
    }
}
