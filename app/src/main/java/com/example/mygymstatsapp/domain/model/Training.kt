package com.example.mygymstatsapp.domain.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** Warm-up ramp computed from the first working set's load. */
object WarmupPlanner {
    const val FIRST_EXERCISE_WARMUPS = 3
    const val OTHER_EXERCISE_WARMUPS = 1
    private const val ROUNDING_STEP_KG = 2.5

    data class WarmupSet(val weightKg: Double?, val reps: Int)

    private val ramps = mapOf(
        1 to listOf(0.6 to 8),
        2 to listOf(0.5 to 8, 0.75 to 4),
        3 to listOf(0.5 to 8, 0.7 to 5, 0.85 to 3),
    )

    fun plan(workingWeightKg: Double?, count: Int): List<WarmupSet> {
        if (count <= 0) return emptyList()
        return ramps.getValue(count.coerceAtMost(3)).map { (ratio, reps) ->
            WarmupSet(
                weightKg = workingWeightKg
                    ?.let { (it * ratio / ROUNDING_STEP_KG).roundToInt() * ROUNDING_STEP_KG }
                    ?.takeIf { it > 0 },
                reps = reps,
            )
        }
    }
}

/** Working sets created when there is no target and no history. */
const val DEFAULT_SET_COUNT = 3

/** A set to create: last time's values, or the progression targets. */
data class PlannedSet(
    val weightKg: Double?,
    val reps: Int?,
    val durationSeconds: Int? = null,
    val type: SetType = SetType.NORMAL,
)

/**
 * Progressive overload by double progression (the rule of the user's program), set by set against last time:
 * - every working set reached the top of the rep range: add the load step and restart at the bottom of the range;
 * - otherwise: same load, one more rep than last time (never above the top of the range).
 */
object ProgressionPlanner {
    enum class Step { ADD_WEIGHT, ADD_REPS }

    /** [targets] follow last time's working sets in order; extra sets repeat the last target. */
    data class Plan(val step: Step, val targets: List<PlannedSet>, val incrementKg: Double, val repsMax: Int) {
        fun targetFor(rank: Int): PlannedSet = targets.getOrElse(rank) { targets.last() }

        /** True when every unfinished working set already carries its target. */
        fun isAppliedTo(sets: List<WorkoutSet>): Boolean =
            sets.filter { it.setType != SetType.WARMUP }.withIndex()
                .filterNot { it.value.completed }
                .all { (rank, set) ->
                    val target = targetFor(rank)
                    set.reps == target.reps && sameWeight(set.weightKg, target.weightKg)
                }
    }

    fun plan(
        previous: List<WorkoutSet>,
        repsMin: Int?,
        repsMax: Int?,
        incrementKg: Double,
        trackingType: TrackingType,
    ): Plan? {
        if (repsMax == null || trackingType == TrackingType.DURATION) return null
        val working = previous.filter { it.completed && it.setType != SetType.WARMUP && (it.reps ?: 0) > 0 }
        if (working.isEmpty()) return null
        val bottom = (repsMin ?: repsMax).coerceAtMost(repsMax)
        val loaded = trackingType == TrackingType.WEIGHT_REPS && incrementKg > 0 && working.all { (it.weightKg ?: 0.0) > 0 }
        val topReached = working.all { it.reps!! >= repsMax }
        return when {
            topReached && loaded -> Plan(
                Step.ADD_WEIGHT,
                working.map { PlannedSet(it.weightKg!! + incrementKg, bottom) },
                incrementKg,
                repsMax,
            )
            // Top of the range without load to add (bodyweight): keep last time's values.
            topReached -> null
            else -> Plan(
                Step.ADD_REPS,
                working.map { PlannedSet(it.weightKg, minOf(it.reps!! + 1, repsMax)) },
                incrementKg,
                repsMax,
            )
        }
    }

    private fun sameWeight(a: Double?, b: Double?): Boolean =
        if (a == null || b == null) a == b else abs(a - b) < 0.01
}

/** This exercise's progression against [previous], or null when it is turned off or cannot apply. */
fun Exercise.progressionPlan(
    previous: List<WorkoutSet>,
    repsMin: Int?,
    repsMax: Int?,
    defaultIncrementKg: Double,
): ProgressionPlanner.Plan? =
    if (!progressionEnabled) {
        null
    } else {
        ProgressionPlanner.plan(previous, repsMin, repsMax, progressionIncrementKg ?: defaultIncrementKg, trackingType)
    }

/**
 * Sets of an exercise at the start of a workout: last time's weight and reps set by set
 * (or the progression targets), preceded by the warm-up ramp.
 */
fun planSets(
    previous: List<WorkoutSet>,
    targetSets: Int?,
    warmupSets: Int,
    progression: ProgressionPlanner.Plan?,
): List<PlannedSet> {
    val lastWorking = previous.filter { it.setType != SetType.WARMUP }
    val count = targetSets ?: lastWorking.size.takeIf { it > 0 } ?: DEFAULT_SET_COUNT
    val working = List(count) { rank ->
        progression?.targetFor(rank)
            ?: (lastWorking.getOrNull(rank) ?: lastWorking.lastOrNull())?.let { PlannedSet(it.weightKg, it.reps, it.durationSeconds) }
            ?: PlannedSet(null, null)
    }
    val warmups = WarmupPlanner.plan(working.firstOrNull()?.weightKg, warmupSets)
        .map { PlannedSet(it.weightKg, it.reps, type = SetType.WARMUP) }
    return warmups + working
}

/**
 * Superset groups are stored as an Int per exercise; a superset is a run of at least two
 * consecutive exercises with the same group number.
 */
object SupersetGrouping {

    fun linkWithNext(groups: List<Int?>, index: Int): List<Int?> {
        if (index !in 0 until groups.lastIndex) return groups
        val nextGroup = groups[index + 1]
        val group = groups[index] ?: nextGroup ?: ((groups.filterNotNull().maxOrNull() ?: 0) + 1)
        val result = groups.map { if (nextGroup != null && it == nextGroup) group else it }.toMutableList()
        result[index] = group
        result[index + 1] = group
        return normalize(result)
    }

    fun unlink(groups: List<Int?>, index: Int): List<Int?> {
        if (index !in groups.indices) return groups
        return normalize(groups.toMutableList().also { it[index] = null })
    }

    /** Keeps only runs of 2+ consecutive equal groups and renumbers them 1, 2, 3… */
    fun normalize(groups: List<Int?>): List<Int?> {
        val result = MutableList<Int?>(groups.size) { null }
        var nextId = 1
        var start = 0
        while (start < groups.size) {
            val group = groups[start]
            var end = start
            while (group != null && end + 1 < groups.size && groups[end + 1] == group) end++
            if (group != null && end > start) {
                for (i in start..end) result[i] = nextId
                nextId++
            }
            start = end + 1
        }
        return result
    }

    /** Letter (A, B, …) of each superset in order of appearance. */
    fun letters(groups: List<Int?>): Map<Int, Char> =
        groups.filterNotNull().distinct().withIndex().associate { (i, group) -> group to 'A' + i }
}

/** Pairs each set with the set of the same kind (warm-up / working) at the same rank last time. */
fun matchPreviousSets(sets: List<WorkoutSet>, previous: List<WorkoutSet>): Map<String, WorkoutSet?> {
    val previousWarmups = previous.filter { it.setType == SetType.WARMUP }
    val previousWorking = previous.filter { it.setType != SetType.WARMUP }
    var warmupRank = 0
    var workingRank = 0
    return sets.associate { set ->
        set.id to if (set.setType == SetType.WARMUP) {
            previousWarmups.getOrNull(warmupRank++)
        } else {
            previousWorking.getOrNull(workingRank++)
        }
    }
}

/**
 * Warm-ups follow each other's load, working sets (normal, failure) too; drop sets are lighter on
 * purpose and follow nothing.
 */
private enum class LoadGroup { WARMUP, WORKING }

private val SetType.loadGroup: LoadGroup?
    get() = when (this) {
        SetType.WARMUP -> LoadGroup.WARMUP
        SetType.NORMAL, SetType.FAILURE -> LoadGroup.WORKING
        SetType.DROP -> null
    }

private const val LOAD_EPSILON = 1e-6

/**
 * What the empty fields of [set] stand for (shown dimmed, used when the set is validated as is):
 * the same set last time ([lastTime]), else the closest earlier set of the same kind in this
 * workout that has values. Validating an empty set repeats the one before it.
 */
fun fallbackSet(sets: List<WorkoutSet>, set: WorkoutSet, lastTime: WorkoutSet?): WorkoutSet? {
    if (lastTime != null) return lastTime
    val group = set.setType.loadGroup
    return sets
        .filter { it.setNumber < set.setNumber && it.setType.loadGroup == group && it.setType.loadGroup != null }
        .filter { it.weightKg != null || it.reps != null || it.durationSeconds != null }
        .maxByOrNull { it.setNumber }
}

/**
 * Going up mid-exercise carries on. When the validated [set] is heavier ([weightKg]) than the set
 * before it of the same kind (for the first one: than the same set last time, [lastTimeWeightKg]),
 * the following unfinished sets of that kind that are lighter, or have no load yet, take its load.
 * Same or lighter changes nothing, and each set keeps its own reps.
 *
 * @return the new load (kg) per set id.
 */
fun raisedLoads(sets: List<WorkoutSet>, set: WorkoutSet, weightKg: Double?, lastTimeWeightKg: Double?): Map<String, Double> {
    val group = set.setType.loadGroup ?: return emptyMap()
    if (weightKg == null) return emptyMap()
    val before = sets
        .filter { it.setNumber < set.setNumber && it.setType.loadGroup == group && it.weightKg != null }
        .maxByOrNull { it.setNumber }
        ?.weightKg
    val reference = before ?: lastTimeWeightKg ?: return emptyMap()
    if (weightKg <= reference + LOAD_EPSILON) return emptyMap()
    return sets
        .filter { it.setNumber > set.setNumber && !it.completed && it.setType.loadGroup == group }
        .filter { (it.weightKg ?: 0.0) < weightKg - LOAD_EPSILON }
        .associate { it.id to weightKg }
}

/** Displayed number of each working set (warm-ups are not counted). */
fun workingSetNumbers(sets: List<WorkoutSet>): Map<String, Int> {
    var number = 0
    return sets.filter { it.setType != SetType.WARMUP }.associate { it.id to ++number }
}
