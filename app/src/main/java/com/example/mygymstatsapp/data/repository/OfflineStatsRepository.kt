package com.example.mygymstatsapp.data.repository

import com.example.mygymstatsapp.data.local.dao.StatsDao
import com.example.mygymstatsapp.data.mapper.toDomain
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseBests
import com.example.mygymstatsapp.domain.model.ExerciseProgressPoint
import com.example.mygymstatsapp.domain.model.ExerciseUsage
import com.example.mygymstatsapp.domain.model.MuscleGroupShare
import com.example.mygymstatsapp.domain.model.PeriodStats
import com.example.mygymstatsapp.domain.model.PersonalRecord
import com.example.mygymstatsapp.domain.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class OfflineStatsRepository(private val dao: StatsDao) : StatsRepository {

    override fun observePeriodStats(from: Long, to: Long): Flow<PeriodStats> =
        dao.observePeriodStats(from, to).map { it.toDomain() }.distinctUntilChanged()

    override fun observeExerciseProgress(exerciseId: String, since: Long): Flow<List<ExerciseProgressPoint>> =
        dao.observeExerciseProgress(exerciseId, since).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override fun observePersonalRecords(): Flow<List<PersonalRecord>> =
        dao.observePersonalRecords().map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override fun observePerformedExercises(): Flow<List<Exercise>> =
        dao.observePerformedExercises().map { rows -> rows.map { it.exercise.toDomain() } }.distinctUntilChanged()

    override fun observeTopExercises(since: Long, limit: Int): Flow<List<ExerciseUsage>> =
        dao.observeTopExercises(since, limit)
            .map { rows -> rows.map { ExerciseUsage(it.exerciseId, it.name, it.count) } }
            .distinctUntilChanged()

    override fun observeMuscleGroupShares(since: Long): Flow<List<MuscleGroupShare>> =
        dao.observeMuscleGroupShares(since)
            .map { rows -> rows.map { MuscleGroupShare(it.muscleGroup, it.setCount) } }
            .distinctUntilChanged()

    override suspend fun getBestsBefore(
        exerciseIds: Collection<String>,
        excludeWorkoutId: String,
        before: Long,
    ): Map<String, ExerciseBests> {
        if (exerciseIds.isEmpty()) return emptyMap()
        return dao.getBestsBefore(exerciseIds.toList(), excludeWorkoutId, before)
            .associate { it.exerciseId to it.toDomain() }
    }
}
