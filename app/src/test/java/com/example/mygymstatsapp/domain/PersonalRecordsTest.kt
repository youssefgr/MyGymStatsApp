package com.example.mygymstatsapp.domain

import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseBests
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.OneRepMax
import com.example.mygymstatsapp.domain.model.RecordType
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.bests
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.usecase.DetectPersonalRecordsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Proxy

class PersonalRecordsTest {

    private val bench = Exercise("ex_bench_press", "Bench Press", MuscleGroup.CHEST, Equipment.BARBELL, TrackingType.WEIGHT_REPS, "", false, false)
    // checkSet() is pure: the repository is never called.
    private val detector = DetectPersonalRecordsUseCase(statsRepository = throwingStatsRepository())

    @Test
    fun epley_matchesSpecExample() {
        assertEquals(80.0, OneRepMax.epley(60.0, 10), 0.001)
        assertEquals(100.0, OneRepMax.epley(100.0, 1), 0.001)
        assertEquals(0.0, OneRepMax.epley(60.0, 0), 0.001)
    }

    @Test
    fun volume_excludesWarmupsAndUnfinishedSets() {
        val exercise = workoutExercise(
            set(1, 40.0, 10, SetType.WARMUP),
            set(2, 60.0, 10),
            set(3, 65.0, 8),
            set(4, 65.0, 7, completed = false),
        )
        assertEquals(60.0 * 10 + 65.0 * 8, exercise.volumeKg, 0.001)
        val bests = exercise.bests()!!
        assertEquals(65.0, bests.maxWeightKg, 0.001)
        assertEquals(OneRepMax.epley(65.0, 8), bests.bestOneRmKg, 0.001)
    }

    @Test
    fun heavierSet_isWeightRecord() {
        val previous = ExerciseBests(maxWeightKg = 62.5, maxReps = 10, bestOneRmKg = 82.0, bestVolumeKg = 2000.0)
        val record = detector.checkSet(bench, set(1, 67.5, 8), previous)!!
        assertEquals(RecordType.MAX_WEIGHT, record.type)
        assertEquals(67.5, record.value, 0.001)
    }

    @Test
    fun moreRepsAtSameWeight_isOneRepMaxRecord() {
        val previous = ExerciseBests(maxWeightKg = 65.0, maxReps = 8, bestOneRmKg = OneRepMax.epley(65.0, 8), bestVolumeKg = 2000.0)
        val record = detector.checkSet(bench, set(1, 65.0, 10), previous)!!
        assertEquals(RecordType.ONE_RM, record.type)
    }

    @Test
    fun warmupOrWeakerSet_isNotARecord() {
        val previous = ExerciseBests(maxWeightKg = 65.0, maxReps = 10, bestOneRmKg = 90.0, bestVolumeKg = 2000.0)
        assertNull(detector.checkSet(bench, set(1, 100.0, 5, SetType.WARMUP), previous))
        assertNull(detector.checkSet(bench, set(1, 60.0, 8), previous))
    }

    private fun set(number: Int, weight: Double, reps: Int, type: SetType = SetType.NORMAL, completed: Boolean = true) =
        WorkoutSet("s$number", "we", number, weight, reps, null, null, type, completed, null, null)

    private fun workoutExercise(vararg sets: WorkoutSet) =
        WorkoutExercise("we", "w", bench, 0, "", null, null, null, supersetGroup = null, sets = sets.toList())

    private fun throwingStatsRepository(): StatsRepository = Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(StatsRepository::class.java),
    ) { _, method, _ -> error("Unexpected repository call: ${method.name}") } as StatsRepository
}
