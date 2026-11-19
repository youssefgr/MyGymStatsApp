package com.example.mygymstatsapp.domain

import com.example.mygymstatsapp.domain.model.DEFAULT_SET_COUNT
import com.example.mygymstatsapp.domain.model.Equipment
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.ProgressionPlanner
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.SupersetGrouping
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WarmupPlanner
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.matchPreviousSets
import com.example.mygymstatsapp.domain.model.planSets
import com.example.mygymstatsapp.domain.model.progressionPlan
import com.example.mygymstatsapp.domain.model.workingSetNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTest {

    @Test
    fun warmupRamp_roundsToTwoAndAHalfKilos() {
        val plan = WarmupPlanner.plan(workingWeightKg = 100.0, count = 3)
        assertEquals(listOf(50.0, 70.0, 85.0), plan.map { it.weightKg })
        assertEquals(listOf(8, 5, 3), plan.map { it.reps })
        assertEquals(listOf(37.5), WarmupPlanner.plan(62.5, 1).map { it.weightKg })
        assertEquals(listOf<Double?>(null), WarmupPlanner.plan(null, 1).map { it.weightKg })
        assertEquals(0, WarmupPlanner.plan(100.0, 0).size)
    }

    @Test
    fun progression_addsLoadOnceEveryWorkingSetReachedTheTopOfTheRange() {
        val done = listOf(set(1, 30.0, 5, SetType.WARMUP), set(2, 60.0, 12), set(3, 60.0, 12), set(4, 60.0, 12))
        val plan = ProgressionPlanner.plan(done, repsMin = 8, repsMax = 12, incrementKg = 1.25, trackingType = TrackingType.WEIGHT_REPS)!!
        assertEquals(ProgressionPlanner.Step.ADD_WEIGHT, plan.step)
        assertEquals(listOf(61.25, 61.25, 61.25), plan.targets.map { it.weightKg })
        assertEquals(listOf(8, 8, 8), plan.targets.map { it.reps })
    }

    @Test
    fun progression_otherwiseAddsOneRepPerSetUpToTheTop() {
        val done = listOf(set(1, 60.0, 12), set(2, 60.0, 10), set(3, 60.0, 7))
        val plan = ProgressionPlanner.plan(done, 8, 12, 2.5, TrackingType.WEIGHT_REPS)!!
        assertEquals(ProgressionPlanner.Step.ADD_REPS, plan.step)
        assertEquals(listOf(60.0, 60.0, 60.0), plan.targets.map { it.weightKg })
        assertEquals(listOf(12, 11, 8), plan.targets.map { it.reps })
    }

    @Test
    fun progression_needsARangeAndStopsAtTheTopWithoutLoad() {
        val done = listOf(set(1, 60.0, 12), set(2, 60.0, 12))
        assertNull(ProgressionPlanner.plan(done, 8, repsMax = null, incrementKg = 2.5, trackingType = TrackingType.WEIGHT_REPS))
        assertNull(ProgressionPlanner.plan(done, 8, 12, 2.5, TrackingType.DURATION))
        val bodyweight = listOf(set(1, null, 15), set(2, null, 15))
        assertNull(ProgressionPlanner.plan(bodyweight, 10, 15, 2.5, TrackingType.REPS_ONLY))
        val notYet = listOf(set(1, null, 12), set(2, null, 15))
        assertEquals(listOf(13, 15), ProgressionPlanner.plan(notYet, 10, 15, 2.5, TrackingType.REPS_ONLY)!!.targets.map { it.reps })
    }

    @Test
    fun progression_canBeTurnedOffPerExerciseWithItsOwnStep() {
        val done = listOf(set(1, 20.0, 15), set(2, 20.0, 15))
        val exercise = Exercise("ex", "Curl", MuscleGroup.BICEPS, Equipment.DUMBBELL, TrackingType.WEIGHT_REPS, "", false, false)
        assertEquals(22.5, exercise.progressionPlan(done, 10, 15, defaultIncrementKg = 2.5)!!.targets.first().weightKg)
        assertEquals(21.25, exercise.copy(progressionIncrementKg = 1.25).progressionPlan(done, 10, 15, 2.5)!!.targets.first().weightKg)
        assertNull(exercise.copy(progressionEnabled = false).progressionPlan(done, 10, 15, 2.5))
    }

    @Test
    fun planSets_copiesLastTimeSetBySetOrUsesTheProgression() {
        val done = listOf(set(1, 60.0, 10), set(2, 62.5, 8))
        val copied = planSets(done, targetSets = 3, warmupSets = 1, progression = null)
        assertEquals(listOf(SetType.WARMUP, SetType.NORMAL, SetType.NORMAL, SetType.NORMAL), copied.map { it.type })
        assertEquals(listOf(35.0, 60.0, 62.5, 62.5), copied.map { it.weightKg })
        assertEquals(listOf(8, 10, 8, 8), copied.map { it.reps })

        val plan = ProgressionPlanner.plan(done, 8, 10, 2.5, TrackingType.WEIGHT_REPS)!!
        assertEquals(listOf(10, 9), planSets(done, null, 0, plan).map { it.reps })
        assertEquals(DEFAULT_SET_COUNT, planSets(emptyList(), null, 0, null).size)
    }

    @Test
    fun plan_isAppliedOnlyWhileUnfinishedSetsCarryTheTargets() {
        val done = listOf(set(1, 60.0, 12), set(2, 60.0, 12))
        val plan = ProgressionPlanner.plan(done, 8, 12, 2.5, TrackingType.WEIGHT_REPS)!!
        val current = listOf(set(1, 62.5, 8, completed = false), set(2, 62.5, 8, completed = false))
        assertTrue(plan.isAppliedTo(current))
        assertFalse(plan.isAppliedTo(listOf(set(1, 60.0, 12, completed = false), set(2, 62.5, 8, completed = false))))
        // A finished set no longer matters.
        assertTrue(plan.isAppliedTo(listOf(set(1, 60.0, 12), set(2, 62.5, 8, completed = false))))
    }

    @Test
    fun superset_linkUnlinkAndNormalize() {
        val linked = SupersetGrouping.linkWithNext(listOf(null, null, null, null), 1)
        assertEquals(listOf(null, 1, 1, null), linked)
        // Linking the last member extends the superset to the next exercise.
        assertEquals(listOf(null, 1, 1, 1), SupersetGrouping.linkWithNext(linked, 2))
        // Removing one of two members dissolves the superset.
        assertEquals(listOf<Int?>(null, null, null, null), SupersetGrouping.unlink(linked, 1))
        // Members separated by a move are no longer a superset; groups are renumbered.
        assertEquals(listOf(null, null, 1, 1), SupersetGrouping.normalize(listOf(7, null, 3, 3)))
        assertEquals(mapOf(1 to 'A', 2 to 'B'), SupersetGrouping.letters(listOf(1, 1, null, 2, 2)))
    }

    @Test
    fun previousSets_areMatchedByKindAndRank() {
        val current = listOf(set(1, null, null, SetType.WARMUP, id = "w1"), set(2, null, null, id = "s1"), set(3, null, null, id = "s2"))
        val previous = listOf(set(1, 40.0, 8, SetType.WARMUP), set(2, 60.0, 10), set(3, 62.5, 8))
        val matched = matchPreviousSets(current, previous)
        assertEquals(40.0, matched["w1"]?.weightKg)
        assertEquals(60.0, matched["s1"]?.weightKg)
        assertEquals(62.5, matched["s2"]?.weightKg)
        assertEquals(mapOf("s1" to 1, "s2" to 2), workingSetNumbers(current))
    }

    private fun set(
        number: Int,
        weight: Double?,
        reps: Int?,
        type: SetType = SetType.NORMAL,
        id: String = "s$number",
        completed: Boolean = true,
    ) = WorkoutSet(id, "we", number, weight, reps, null, null, type, completed = completed, completedAt = null, restSeconds = null)
}
