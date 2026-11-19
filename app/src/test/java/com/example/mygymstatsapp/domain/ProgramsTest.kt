package com.example.mygymstatsapp.domain

import com.example.mygymstatsapp.data.local.database.SeedData
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.GeneratedSession
import com.example.mygymstatsapp.domain.model.GeneratorOptions
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.ProgramGenerator
import com.example.mygymstatsapp.domain.model.SessionKind
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramsTest {

    /** The built-in exercise library. */
    private val library = SeedData.exercises.map {
        Exercise(it.id, it.name, it.muscleGroup, it.equipment, it.trackingType, it.description, isCustom = false, isFavorite = false)
    }
    private val groupOf = library.associate { it.id to it.muscleGroup }

    private fun GeneratedSession.ids() = exercises.map { it.exerciseId }
    private fun GeneratedSession.groupsInOrder() = ids().map { groupOf.getValue(it) }

    @Test
    fun threeSessions_givePushPullLegsWithGoalTargets() {
        val sessions = ProgramGenerator.generate(GeneratorOptions(sessionsPerWeek = 3, goal = Goal.MUSCLE_GAIN), library)
        assertEquals(listOf(SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS), sessions.map { it.kind })
        val push = sessions.first()
        assertEquals(listOf("ex_bench_press", "ex_incline_dumbbell_press"), push.ids().take(2))
        // Compound 4 × 6-10, isolation 3 × 10-15.
        assertEquals(listOf(4, 6, 10), push.exercises.first().let { listOf(it.targetSets, it.targetRepsMin, it.targetRepsMax) })
        assertEquals(listOf(3, 10, 15), push.exercises.last().let { listOf(it.targetSets, it.targetRepsMin, it.targetRepsMax) })
        sessions.forEach { assertEquals(it.ids().distinct(), it.ids()) }
        assertTrue(sessions.none { it.repeated })
    }

    @Test
    fun repeatedSessions_rotateExercises() {
        val sessions = ProgramGenerator.generate(GeneratorOptions(sessionsPerWeek = 4), library)
        assertEquals(listOf(SessionKind.UPPER, SessionKind.LOWER, SessionKind.UPPER, SessionKind.LOWER), sessions.map { it.kind })
        assertEquals(listOf(0, 0, 1, 1), sessions.map { it.variant })
        assertTrue(sessions.all { it.repeated })
        assertNotEquals(sessions[0].ids().first(), sessions[2].ids().first())
    }

    @Test
    fun focus_addsWorkTrainedFirstWithAnExtraSet() {
        val sessions = ProgramGenerator.generate(GeneratorOptions(focus = setOf(MuscleGroup.BICEPS), sessionsPerWeek = 3), library)
        val pull = sessions[1]
        assertEquals(List(3) { MuscleGroup.BICEPS }, pull.groupsInOrder().take(3))
        assertEquals(4, pull.exercises.first().targetSets)
        // Push and legs have no biceps work: unchanged.
        assertEquals(6, sessions[0].exercises.size)
        assertTrue(MuscleGroup.BICEPS !in sessions[2].groupsInOrder())
    }

    @Test
    fun focusMissingFromTheSplit_goesWhereItBelongs() {
        val forearms = ProgramGenerator.generate(GeneratorOptions(focus = setOf(MuscleGroup.FOREARMS), sessionsPerWeek = 3), library)
        assertEquals(listOf(false, true, false), forearms.map { MuscleGroup.FOREARMS in it.groupsInOrder() })

        val cardio = ProgramGenerator.generate(GeneratorOptions(focus = setOf(MuscleGroup.CARDIO), sessionsPerWeek = 2), library)
        // Every session ends with a single cardio block.
        assertTrue(cardio.all { it.groupsInOrder().last() == MuscleGroup.CARDIO })
        assertTrue(cardio.all { it.exercises.last().targetSets == 1 && it.exercises.last().targetRepsMax == null })
    }

    @Test
    fun focusOnly_trainsOnlyTheSelectionAndRotates() {
        val arms = setOf(MuscleGroup.BICEPS, MuscleGroup.TRICEPS)
        val sessions = ProgramGenerator.generate(GeneratorOptions(focus = arms, focusOnly = true, sessionsPerWeek = 2), library)
        assertTrue(sessions.all { it.kind == SessionKind.FOCUS && it.repeated })
        sessions.forEach { session ->
            assertTrue(session.groupsInOrder().all { it in arms })
            assertEquals(6, session.exercises.size)
        }
        assertNotEquals(sessions[0].ids(), sessions[1].ids())
    }

    @Test
    fun missingExercises_areSkipped() {
        val withoutBench = library.filterNot { it.id == "ex_bench_press" }
        val push = ProgramGenerator.generate(GeneratorOptions(sessionsPerWeek = 3), withoutBench).first()
        assertEquals("ex_incline_dumbbell_press", push.ids().first())
    }

    @Test
    fun strength_usesHeavyLowReps() {
        val squat = ProgramGenerator.generate(GeneratorOptions(sessionsPerWeek = 1, goal = Goal.STRENGTH), library).first().exercises.first()
        assertEquals(listOf(4, 4, 6, 180), listOf(squat.targetSets, squat.targetRepsMin, squat.targetRepsMax, squat.restSeconds))
    }

    @Test
    fun nextSession_followsTheLastOneStartedAndWraps() {
        fun session(id: String, lastUsedAt: Long?) = WorkoutTemplate(id, id, "", emptyList(), programId = "p", lastUsedAt = lastUsedAt)
        fun program(vararg sessions: WorkoutTemplate) = Program("p", "Programme", "", sessions.toList())
        assertEquals("a", program(session("a", null), session("b", null)).nextSession?.id)
        assertEquals("c", program(session("a", 10), session("b", 20), session("c", null)).nextSession?.id)
        assertEquals("a", program(session("a", 10), session("b", 5), session("c", 30)).nextSession?.id)
        assertEquals(null, program().nextSession)
    }
}
