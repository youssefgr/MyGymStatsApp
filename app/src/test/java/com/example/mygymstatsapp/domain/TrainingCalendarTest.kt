package com.example.mygymstatsapp.domain

import com.example.mygymstatsapp.domain.model.TrainingCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class TrainingCalendarTest {

    @Test
    fun monthGrid_startsOnMondayInFullWeeks() {
        // October 2026 starts on a Thursday and has 31 days.
        val grid = TrainingCalendar.monthGrid(YearMonth.of(2026, 10))
        assertEquals(0, grid.size % 7)
        assertEquals(listOf(null, null, null), grid.take(3))
        assertEquals(LocalDate.of(2026, 10, 1), grid[3])
        assertEquals(LocalDate.of(2026, 10, 31), grid.filterNotNull().last())
        assertNull(grid.last())
        // February 2027 starts on a Monday: no padding before.
        assertEquals(LocalDate.of(2027, 2, 1), TrainingCalendar.monthGrid(YearMonth.of(2027, 2)).first())
    }

    @Test
    fun weekStreaks_countConsecutiveWeeksWithAWorkout() {
        val today = LocalDate.of(2026, 10, 7) // Wednesday
        val days = listOf(
            LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 15), // 3 weeks
            LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 5), // last week + this week
        )
        assertEquals(TrainingCalendar.Streaks(current = 2, best = 3), TrainingCalendar.weekStreaks(days, today))
    }

    @Test
    fun weekStreaks_anUntrainedCurrentWeekDoesNotBreakTheStreakYet() {
        val today = LocalDate.of(2026, 10, 7)
        val days = listOf(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 29))
        assertEquals(2, TrainingCalendar.weekStreaks(days, today).current)
        // Two weeks without training: the streak is over.
        assertEquals(0, TrainingCalendar.weekStreaks(listOf(LocalDate.of(2026, 9, 22)), today).current)
        assertEquals(TrainingCalendar.Streaks(0, 0), TrainingCalendar.weekStreaks(emptyList(), today))
    }
}
