package com.example.mygymstatsapp.utils

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

/** Weeks start on Monday (French convention): stats, calendar and streaks. */
val FIRST_DAY_OF_WEEK: DayOfWeek = DayOfWeek.MONDAY

fun LocalDate.weekStart(): LocalDate = with(TemporalAdjusters.previousOrSame(FIRST_DAY_OF_WEEK))

/** Monday 00:00 of the current week. */
fun startOfWeekMillis(date: LocalDate = LocalDate.now()): Long = date.weekStart().toEpochMillis()

fun startOfMonthMillis(date: LocalDate = LocalDate.now()): Long = date.withDayOfMonth(1).toEpochMillis()

fun daysAgoMillis(days: Long): Long = LocalDate.now().minusDays(days).toEpochMillis()

fun monthsAgoMillis(months: Long): Long = LocalDate.now().minusMonths(months).toEpochMillis()

/** Monday of the week containing this timestamp. */
fun weekStartOf(epochMs: Long): LocalDate = epochMs.toLocalDate().weekStart()
