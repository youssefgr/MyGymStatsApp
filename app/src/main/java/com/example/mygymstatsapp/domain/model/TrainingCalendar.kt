package com.example.mygymstatsapp.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** Month layout and training streaks for the calendar. Weeks start on Monday (French convention). */
object TrainingCalendar {

    /** Days of [month] in rows of 7 starting on [firstDayOfWeek]; null cells pad the first and last rows. */
    fun monthGrid(month: YearMonth, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<LocalDate?> {
        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val cells = List<LocalDate?>(leading) { null } + List(month.lengthOfMonth()) { first.plusDays(it.toLong()) }
        return cells + List((7 - cells.size % 7) % 7) { null }
    }

    /** Streaks counted in weeks with at least one workout. */
    data class Streaks(val current: Int, val best: Int)

    /**
     * [Streaks.current] ends this week, or last week while this one has no workout yet
     * (the week is not over, so the streak is not broken).
     */
    fun weekStreaks(
        trainedDays: Collection<LocalDate>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): Streaks {
        val weeks = trainedDays.map { it.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)) }.toSortedSet()
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (week in weeks) {
            run = if (previous != null && week == previous.plusWeeks(1)) run + 1 else 1
            best = maxOf(best, run)
            previous = week
        }
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
        var cursor = if (thisWeek in weeks) thisWeek else thisWeek.minusWeeks(1)
        var current = 0
        while (cursor in weeks) {
            current++
            cursor = cursor.minusWeeks(1)
        }
        return Streaks(current, best)
    }
}
