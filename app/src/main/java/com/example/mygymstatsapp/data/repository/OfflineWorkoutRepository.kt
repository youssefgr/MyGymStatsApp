package com.example.mygymstatsapp.data.repository

import androidx.room.withTransaction
import com.example.mygymstatsapp.data.local.dao.WorkoutDao
import com.example.mygymstatsapp.data.local.database.GymDatabase
import com.example.mygymstatsapp.data.local.entities.WorkoutEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutExerciseEntity
import com.example.mygymstatsapp.data.local.entities.WorkoutSetEntity
import com.example.mygymstatsapp.data.mapper.toDomain
import com.example.mygymstatsapp.domain.model.ActiveWorkoutHeader
import com.example.mygymstatsapp.domain.model.DEFAULT_SET_COUNT
import com.example.mygymstatsapp.domain.model.PlannedExercise
import com.example.mygymstatsapp.domain.model.PlannedSet
import com.example.mygymstatsapp.domain.model.RepRange
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.SupersetGrouping
import com.example.mygymstatsapp.domain.model.WarmupPlanner
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.WorkoutStatus
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID

class OfflineWorkoutRepository(
    private val database: GymDatabase,
    private val dao: WorkoutDao,
) : WorkoutRepository {

    override fun observeActiveWorkout(): Flow<Workout?> =
        dao.observeActiveWorkout().map { it?.toDomain() }.distinctUntilChanged()

    override fun observeActiveWorkoutHeader(): Flow<ActiveWorkoutHeader?> =
        dao.observeActiveWorkoutHeader().map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun getActiveWorkoutId(): String? = dao.getActiveWorkoutId()

    override fun observeWorkout(id: String): Flow<Workout?> =
        dao.observeWorkout(id).map { it?.toDomain() }.distinctUntilChanged()

    override suspend fun getWorkout(id: String): Workout? = dao.getWorkout(id)?.toDomain()

    override fun observeCompletedWorkouts(limit: Int): Flow<List<WorkoutSummary>> =
        dao.observeCompletedSummaries(limit).map { rows -> rows.map { it.toDomain() } }.distinctUntilChanged()

    override fun observeCompletedStartTimes(from: Long): Flow<List<Long>> =
        dao.observeCompletedStartTimes(from).distinctUntilChanged()

    override suspend fun getLastPerformance(exerciseId: String, templateId: String?): List<WorkoutSet> {
        val inTemplate = templateId?.let { dao.getLastPerformanceSetsInTemplate(exerciseId, it) }.orEmpty()
        return inTemplate.ifEmpty { dao.getLastPerformanceSets(exerciseId) }.map { it.toDomain() }
    }

    override suspend fun getLastRepRange(exerciseId: String): RepRange? =
        (dao.getLastWorkoutRepRange(exerciseId) ?: dao.getTemplateRepRange(exerciseId))
            ?.let { RepRange(it.targetRepsMin, it.targetRepsMax) }

    override suspend fun getSet(setId: String): WorkoutSet? = dao.getSet(setId)?.toDomain()

    // ---- Workout lifecycle -------------------------------------------------------------------

    override suspend fun createWorkout(
        name: String,
        templateId: String?,
        exercises: List<PlannedExercise>,
    ): String {
        val now = System.currentTimeMillis()
        val workoutId = newId()
        database.withTransaction {
            dao.insertWorkout(
                WorkoutEntity(
                    id = workoutId,
                    name = name,
                    templateId = templateId,
                    startedAt = now,
                    endedAt = null,
                    durationSeconds = 0,
                    status = WorkoutStatus.IN_PROGRESS,
                    notes = "",
                    createdAt = now,
                    updatedAt = now,
                )
            )
            insertPlannedExercises(workoutId, exercises, firstPosition = 0)
        }
        return workoutId
    }

    override suspend fun addExercises(workoutId: String, exercises: List<PlannedExercise>) {
        database.withTransaction {
            val firstPosition = dao.getMaxExercisePosition(workoutId) + 1
            insertPlannedExercises(workoutId, exercises, firstPosition)
        }
    }

    /** Inserts the exercises with their planned sets (blank working sets when nothing was planned). */
    private suspend fun insertPlannedExercises(
        workoutId: String,
        exercises: List<PlannedExercise>,
        firstPosition: Int,
    ) {
        exercises.forEachIndexed { index, planned ->
            val workoutExerciseId = newId()
            dao.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    id = workoutExerciseId,
                    workoutId = workoutId,
                    exerciseId = planned.exerciseId,
                    position = firstPosition + index,
                    notes = planned.notes,
                    restSeconds = planned.restSeconds,
                    targetRepsMin = planned.targetRepsMin,
                    targetRepsMax = planned.targetRepsMax,
                    supersetGroup = planned.supersetGroup,
                )
            )
            insertPlannedSets(workoutExerciseId, planned)
        }
    }

    private suspend fun insertPlannedSets(workoutExerciseId: String, planned: PlannedExercise) {
        val sets = planned.sets.ifEmpty { List(planned.targetSets ?: DEFAULT_SET_COUNT) { PlannedSet(null, null) } }
        dao.insertSets(
            sets.mapIndexed { i, set ->
                newSet(workoutExerciseId, set.weightKg, set.reps, set.durationSeconds, set.type).copy(setNumber = i + 1)
            }
        )
    }

    override suspend fun replaceExercise(workoutExerciseId: String, replacement: PlannedExercise) {
        database.withTransaction {
            val current = dao.getWorkoutExercise(workoutExerciseId) ?: return@withTransaction
            val hasValidatedSets = dao.getSets(workoutExerciseId).any { it.completed }
            dao.deleteIncompleteSetsOf(workoutExerciseId)
            if (!hasValidatedSets) {
                // Nothing done yet: the new exercise takes the slot (place, targets, rest, notes, superset).
                dao.updateWorkoutExerciseExercise(workoutExerciseId, replacement.exerciseId)
                insertPlannedSets(workoutExerciseId, replacement)
                return@withTransaction
            }
            // What was done stays recorded under the old exercise; the new one follows it.
            dao.getWorkoutExercises(current.workoutId)
                .filter { it.position > current.position }
                .sortedByDescending { it.position }
                .forEach { dao.updateExercisePosition(it.id, it.position + 1) }
            insertPlannedExercises(current.workoutId, listOf(replacement), firstPosition = current.position + 1)
            val ordered = dao.getWorkoutExercises(current.workoutId)
            val groups = ordered.map { if (it.id == workoutExerciseId) null else it.supersetGroup }
            saveSupersets(ordered, SupersetGrouping.normalize(groups))
        }
    }

    private fun newSet(
        workoutExerciseId: String,
        weightKg: Double?,
        reps: Int?,
        durationSeconds: Int?,
        type: SetType,
    ) = WorkoutSetEntity(
        id = newId(),
        workoutExerciseId = workoutExerciseId,
        setNumber = 0,
        weightKg = weightKg,
        reps = reps,
        durationSeconds = durationSeconds,
        rpe = null,
        setType = type,
        completed = false,
        completedAt = null,
        restSeconds = null,
    )

    override suspend fun removeExercise(workoutExerciseId: String) {
        database.withTransaction {
            val removed = dao.getWorkoutExercise(workoutExerciseId) ?: return@withTransaction
            dao.deleteWorkoutExercise(workoutExerciseId)
            val remaining = dao.getWorkoutExercises(removed.workoutId)
            remaining.forEachIndexed { index, exercise ->
                if (exercise.position != index) dao.updateExercisePosition(exercise.id, index)
            }
            saveSupersets(remaining, SupersetGrouping.normalize(remaining.map { it.supersetGroup }))
        }
    }

    override suspend fun moveExercise(workoutExerciseId: String, offset: Int) {
        database.withTransaction {
            val moved = dao.getWorkoutExercise(workoutExerciseId) ?: return@withTransaction
            val ordered = dao.getWorkoutExercises(moved.workoutId).toMutableList()
            val from = ordered.indexOfFirst { it.id == workoutExerciseId }
            val to = (from + offset).coerceIn(0, ordered.lastIndex)
            if (from == to) return@withTransaction
            ordered.add(to, ordered.removeAt(from))
            ordered.forEachIndexed { index, exercise -> dao.updateExercisePosition(exercise.id, index) }
            saveSupersets(ordered, SupersetGrouping.normalize(ordered.map { it.supersetGroup }))
        }
    }

    override suspend fun linkSupersetWithNext(workoutExerciseId: String) =
        updateSupersets(workoutExerciseId) { groups, index -> SupersetGrouping.linkWithNext(groups, index) }

    override suspend fun unlinkSuperset(workoutExerciseId: String) =
        updateSupersets(workoutExerciseId) { groups, index -> SupersetGrouping.unlink(groups, index) }

    private suspend fun updateSupersets(
        workoutExerciseId: String,
        transform: (List<Int?>, Int) -> List<Int?>,
    ) {
        database.withTransaction {
            val target = dao.getWorkoutExercise(workoutExerciseId) ?: return@withTransaction
            val ordered = dao.getWorkoutExercises(target.workoutId)
            val index = ordered.indexOfFirst { it.id == workoutExerciseId }
            saveSupersets(ordered, transform(ordered.map { it.supersetGroup }, index))
        }
    }

    private suspend fun saveSupersets(exercises: List<WorkoutExerciseEntity>, groups: List<Int?>) {
        exercises.zip(groups).forEach { (exercise, group) ->
            if (exercise.supersetGroup != group) dao.updateExerciseSuperset(exercise.id, group)
        }
    }

    override suspend fun updateExerciseRest(workoutExerciseId: String, restSeconds: Int?) =
        dao.updateExerciseRest(workoutExerciseId, restSeconds)

    // ---- Sets --------------------------------------------------------------------------------

    override suspend fun addSet(workoutExerciseId: String) {
        database.withTransaction {
            val sets = dao.getSets(workoutExerciseId)
            val last = sets.lastOrNull { it.setType != SetType.WARMUP } ?: sets.lastOrNull()
            dao.insertSets(
                listOf(
                    WorkoutSetEntity(
                        id = newId(),
                        workoutExerciseId = workoutExerciseId,
                        setNumber = sets.size + 1,
                        weightKg = last?.weightKg,
                        reps = last?.reps,
                        durationSeconds = last?.durationSeconds,
                        rpe = null,
                        setType = if (last?.setType == SetType.DROP) SetType.DROP else SetType.NORMAL,
                        completed = false,
                        completedAt = null,
                        restSeconds = null,
                    )
                )
            )
        }
    }

    override suspend fun addWarmupSets(workoutExerciseId: String, count: Int) {
        database.withTransaction {
            val sets = dao.getSets(workoutExerciseId)
            val workingWeight = sets.firstOrNull { it.setType != SetType.WARMUP }?.weightKg
            val warmups = WarmupPlanner.plan(workingWeight, count)
            // Warm-ups go first: shift the existing sets down.
            sets.forEach { dao.updateSetNumber(it.id, it.setNumber + warmups.size) }
            dao.insertSets(
                warmups.mapIndexed { i, warmup ->
                    newSet(workoutExerciseId, warmup.weightKg, warmup.reps, null, SetType.WARMUP).copy(setNumber = i + 1)
                }
            )
        }
    }

    override suspend fun applySetTargets(workoutExerciseId: String, targets: List<PlannedSet>) {
        if (targets.isEmpty()) return
        database.withTransaction {
            val sets = dao.getSets(workoutExerciseId)
            sets.filter { it.setType != SetType.WARMUP }.forEachIndexed { rank, set ->
                if (set.completed) return@forEachIndexed
                val target = targets.getOrElse(rank) { targets.last() }
                dao.updateSetValues(set.id, target.weightKg, target.reps, target.durationSeconds ?: set.durationSeconds)
            }
            // Keep warm-ups proportional to the new working load (whole ramp, finished warm-ups untouched).
            val warmups = sets.filter { it.setType == SetType.WARMUP }
            WarmupPlanner.plan(targets.first().weightKg, warmups.size).zip(warmups).forEach { (plan, set) ->
                if (!set.completed) dao.updateSetValues(set.id, plan.weightKg, plan.reps, null)
            }
        }
    }

    override suspend fun deleteSet(setId: String) {
        database.withTransaction {
            val set = dao.getSet(setId) ?: return@withTransaction
            dao.deleteSet(setId)
            dao.getSets(set.workoutExerciseId).forEachIndexed { index, remaining ->
                if (remaining.setNumber != index + 1) dao.updateSetNumber(remaining.id, index + 1)
            }
        }
    }

    override suspend fun updateSetWeight(setId: String, weightKg: Double?) = dao.updateSetWeight(setId, weightKg)

    override suspend fun updateSetReps(setId: String, reps: Int?) = dao.updateSetReps(setId, reps)

    override suspend fun updateSetDuration(setId: String, durationSeconds: Int?) =
        dao.updateSetDuration(setId, durationSeconds)

    override suspend fun updateSetValues(setId: String, weightKg: Double?, reps: Int?, durationSeconds: Int?) =
        dao.updateSetValues(setId, weightKg, reps, durationSeconds)

    override suspend fun updateSetType(setId: String, type: SetType) = dao.updateSetType(setId, type)

    override suspend fun updateSetRpe(setId: String, rpe: Float?) = dao.updateSetRpe(setId, rpe)

    override suspend fun setSetCompleted(setId: String, completed: Boolean, restSeconds: Int?) =
        dao.updateSetCompletion(
            id = setId,
            completed = completed,
            completedAt = if (completed) System.currentTimeMillis() else null,
            restSeconds = if (completed) restSeconds else null,
        )

    // ---- Workout fields ----------------------------------------------------------------------

    override suspend fun renameWorkout(id: String, name: String) =
        dao.renameWorkout(id, name.trim(), System.currentTimeMillis())

    override suspend fun updateWorkoutNotes(id: String, notes: String) =
        dao.updateWorkoutNotes(id, notes, System.currentTimeMillis())

    override suspend fun updateWorkoutTiming(id: String, startedAt: Long, durationSeconds: Long) =
        dao.updateWorkoutTiming(id, startedAt, durationSeconds.coerceAtLeast(0), System.currentTimeMillis())

    override suspend fun updateHeartRate(id: String, avg: Int, max: Int) =
        dao.updateHeartRate(id, avg, max, System.currentTimeMillis())

    override suspend fun completeWorkout(id: String): Boolean = database.withTransaction {
        val workout = dao.getWorkoutEntity(id) ?: return@withTransaction false
        if (dao.countCompletedSets(id) == 0) return@withTransaction false
        dao.deleteIncompleteSets(id)
        dao.deleteExercisesWithoutSets(id)
        dao.getWorkoutExercises(id).forEachIndexed { index, exercise ->
            if (exercise.position != index) dao.updateExercisePosition(exercise.id, index)
        }
        val endedAt = System.currentTimeMillis()
        dao.markCompleted(id, endedAt, ((endedAt - workout.startedAt) / 1000).coerceAtLeast(0))
        true
    }

    override suspend fun deleteWorkout(id: String) = dao.deleteWorkout(id)

    private fun newId() = UUID.randomUUID().toString()
}
