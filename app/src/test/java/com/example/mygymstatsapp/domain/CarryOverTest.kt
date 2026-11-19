package com.example.mygymstatsapp.domain

import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.fallbackSet
import com.example.mygymstatsapp.domain.model.raisedLoads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarryOverTest {

    @Test
    fun `heavier than the set before raises the following lighter sets`() {
        val sets = listOf(set(1, 80.0, 8, completed = true), set(2, 80.0, 8), set(3, 80.0, 8), set(4, 80.0, 8))
        assertEquals(mapOf("s3" to 85.0, "s4" to 85.0), raisedLoads(sets, sets[1], 85.0, lastTimeWeightKg = 80.0))
    }

    @Test
    fun `same or lighter than the set before changes nothing`() {
        val sets = listOf(set(1, 80.0, 8, completed = true), set(2, 80.0, 8), set(3, 80.0, 8))
        assertTrue(raisedLoads(sets, sets[1], 80.0, lastTimeWeightKg = 70.0).isEmpty())
        assertTrue(raisedLoads(sets, sets[1], 75.0, lastTimeWeightKg = 70.0).isEmpty())
    }

    @Test
    fun `first set compares with last time`() {
        val sets = listOf(set(1, 80.0, 8), set(2, 80.0, 8), set(3, 82.5, 8))
        // 85 > 80 last time: set 2 goes up, set 3 too (82.5 is lighter).
        assertEquals(mapOf("s2" to 85.0, "s3" to 85.0), raisedLoads(sets, sets[0], 85.0, lastTimeWeightKg = 80.0))
        // Nothing to compare with (new exercise): nothing is copied.
        assertTrue(raisedLoads(sets, sets[0], 85.0, lastTimeWeightKg = null).isEmpty())
    }

    @Test
    fun `heavier planned sets, validated sets and drop sets keep their load`() {
        val sets = listOf(
            set(1, 80.0, 8, completed = true),
            set(2, 85.0, 8),
            set(3, 90.0, 6),
            set(4, 60.0, 12, type = SetType.DROP),
            set(5, null, null),
            set(6, 70.0, 8, completed = true),
        )
        // Pyramid: 90 stays 90; the drop set stays light; the empty set gets the load; done sets untouched.
        assertEquals(mapOf("s5" to 85.0), raisedLoads(sets, sets[1], 85.0, lastTimeWeightKg = null))
    }

    @Test
    fun `warm-ups and working sets follow their own kind`() {
        val sets = listOf(
            set(1, 40.0, 8, type = SetType.WARMUP, completed = true),
            set(2, 55.0, 5, type = SetType.WARMUP),
            set(3, 50.0, 3, type = SetType.WARMUP),
            set(4, 80.0, 8),
        )
        assertEquals(mapOf("s3" to 55.0), raisedLoads(sets, sets[1], 55.0, lastTimeWeightKg = null))
        // A drop set raises nothing.
        val drops = listOf(set(1, 60.0, 10, type = SetType.DROP), set(2, 50.0, 10, type = SetType.DROP))
        assertTrue(raisedLoads(drops, drops[0], 65.0, lastTimeWeightKg = 60.0).isEmpty())
    }

    @Test
    fun `empty fields stand for last time, else the set before`() {
        val lastTime = set(9, 77.5, 10, id = "last")
        val sets = listOf(set(1, 60.0, 10, completed = true), set(2, null, null))
        assertEquals(lastTime, fallbackSet(sets, sets[1], lastTime))
        assertEquals(sets[0], fallbackSet(sets, sets[1], lastTime = null))
        // The first set of a new exercise has nothing to stand for.
        assertNull(fallbackSet(sets, sets[0], lastTime = null))
        // A warm-up never stands for a working set.
        val mixed = listOf(set(1, 40.0, 8, type = SetType.WARMUP, completed = true), set(2, null, null))
        assertNull(fallbackSet(mixed, mixed[1], lastTime = null))
    }

    private fun set(
        number: Int,
        weight: Double?,
        reps: Int?,
        type: SetType = SetType.NORMAL,
        id: String = "s$number",
        completed: Boolean = false,
    ) = WorkoutSet(id, "we", number, weight, reps, null, null, type, completed = completed, completedAt = null, restSeconds = null)
}
