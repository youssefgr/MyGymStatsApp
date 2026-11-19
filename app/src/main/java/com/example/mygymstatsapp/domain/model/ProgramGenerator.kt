package com.example.mygymstatsapp.domain.model

import kotlin.math.ceil

/** Kind of a generated session; the UI turns it into a localized name. */
enum class SessionKind { FULL_BODY, UPPER, LOWER, PUSH, PULL, LEGS, FOCUS }

data class GeneratorOptions(
    /** Muscle groups to prioritize: extra exercises and sets, trained first. Empty = balanced program. */
    val focus: Set<MuscleGroup> = emptySet(),
    /** Sessions train only the focus groups (specialization block). */
    val focusOnly: Boolean = false,
    val sessionsPerWeek: Int = 3,
    val goal: Goal = Goal.MUSCLE_GAIN,
)

data class GeneratedSession(
    val kind: SessionKind,
    /** 0 for the first session of its kind, 1 for the second… (A, B). */
    val variant: Int,
    /** True when the program has several sessions of this kind, so names need A / B. */
    val repeated: Boolean,
    /** Muscle groups trained, in session order. */
    val groups: List<MuscleGroup>,
    val exercises: List<PlannedExercise>,
)

/**
 * Builds a program from the exercise library: a classic split for the number of sessions
 * (full body, upper / lower, push / pull / legs), sets, reps and rest from the goal, and
 * extra work placed first for the focus muscle groups. Repeated sessions rotate exercises (A / B).
 */
object ProgramGenerator {
    const val MIN_SESSIONS = 1
    const val MAX_SESSIONS = 6
    private const val MAX_EXERCISES = 8
    private const val MAX_SETS = 5

    /** Never moved to the front of a session, even in focus: they would tire the big lifts. */
    private val TRAILING = setOf(MuscleGroup.ABS, MuscleGroup.CARDIO)

    /** Movement patterns, each with the library exercises that fit it, best first. */
    private enum class Pool(val group: MuscleGroup, val compound: Boolean, val exerciseIds: List<String>) {
        CHEST_PRESS(
            MuscleGroup.CHEST, true,
            listOf(
                "ex_bench_press", "ex_incline_dumbbell_press", "ex_incline_bench_press", "ex_dumbbell_bench_press",
                "ex_machine_chest_press", "ex_smith_incline_press", "ex_dips",
            ),
        ),
        CHEST_FLY(MuscleGroup.CHEST, false, listOf("ex_pec_deck", "ex_cable_crossover", "ex_low_to_high_cable_fly", "ex_chest_fly")),
        VERTICAL_PULL(MuscleGroup.BACK, true, listOf("ex_pull_ups", "ex_lat_pulldown", "ex_chin_ups", "ex_close_grip_pulldown")),
        ROW(
            MuscleGroup.BACK, true,
            listOf("ex_chest_supported_row", "ex_barbell_row", "ex_seated_cable_row", "ex_dumbbell_row", "ex_t_bar_row"),
        ),
        SHOULDER_PRESS(
            MuscleGroup.SHOULDERS, true,
            listOf("ex_overhead_press", "ex_dumbbell_shoulder_press", "ex_machine_shoulder_press", "ex_arnold_press"),
        ),
        LATERAL(MuscleGroup.SHOULDERS, false, listOf("ex_lateral_raise", "ex_cable_lateral_raise")),
        REAR_DELT(MuscleGroup.SHOULDERS, false, listOf("ex_face_pull", "ex_rear_delt_fly")),
        BICEPS(
            MuscleGroup.BICEPS, false,
            listOf(
                "ex_ez_bar_curl", "ex_incline_dumbbell_curl", "ex_hammer_curl", "ex_preacher_curl",
                "ex_cable_curl", "ex_dumbbell_curl", "ex_barbell_curl", "ex_concentration_curl",
            ),
        ),
        TRICEPS(
            MuscleGroup.TRICEPS, false,
            listOf(
                "ex_overhead_cable_extension", "ex_rope_pushdown", "ex_skull_crusher", "ex_triceps_pushdown",
                "ex_dumbbell_overhead_extension", "ex_close_grip_bench_press",
            ),
        ),
        SQUAT(MuscleGroup.LEGS, true, listOf("ex_squat", "ex_leg_press", "ex_hack_squat", "ex_front_squat", "ex_goblet_squat")),
        HINGE(MuscleGroup.LEGS, true, listOf("ex_romanian_deadlift", "ex_deadlift")),
        LUNGE(MuscleGroup.LEGS, true, listOf("ex_bulgarian_split_squat", "ex_lunges")),
        LEG_ISOLATION(MuscleGroup.LEGS, false, listOf("ex_leg_curl", "ex_leg_extension")),
        GLUTE_THRUST(MuscleGroup.GLUTES, true, listOf("ex_hip_thrust", "ex_glute_bridge")),
        GLUTE_ISOLATION(MuscleGroup.GLUTES, false, listOf("ex_hip_abduction", "ex_cable_kickback")),
        CALF(MuscleGroup.CALVES, false, listOf("ex_calf_raise", "ex_seated_calf_raise", "ex_leg_press_calf_raise")),
        CORE(MuscleGroup.ABS, false, listOf("ex_hanging_leg_raise", "ex_cable_crunch", "ex_ab_wheel", "ex_plank", "ex_crunch")),
        FOREARM(MuscleGroup.FOREARMS, false, listOf("ex_wrist_curl", "ex_reverse_curl", "ex_reverse_wrist_curl")),
        CARDIO(
            MuscleGroup.CARDIO, false,
            listOf("ex_rowing_machine", "ex_stationary_bike", "ex_elliptical", "ex_stair_climber", "ex_treadmill", "ex_jump_rope"),
        ),
    }

    private val SPLITS = mapOf(
        1 to listOf(SessionKind.FULL_BODY),
        2 to listOf(SessionKind.UPPER, SessionKind.LOWER),
        3 to listOf(SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS),
        4 to listOf(SessionKind.UPPER, SessionKind.LOWER, SessionKind.UPPER, SessionKind.LOWER),
        5 to listOf(SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS, SessionKind.UPPER, SessionKind.LOWER),
        6 to listOf(SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS, SessionKind.PUSH, SessionKind.PULL, SessionKind.LEGS),
    )

    private val SESSION_SLOTS = mapOf(
        SessionKind.PUSH to listOf(Pool.CHEST_PRESS, Pool.CHEST_PRESS, Pool.CHEST_FLY, Pool.LATERAL, Pool.TRICEPS, Pool.TRICEPS),
        SessionKind.PULL to listOf(Pool.VERTICAL_PULL, Pool.ROW, Pool.ROW, Pool.REAR_DELT, Pool.BICEPS, Pool.BICEPS),
        SessionKind.LEGS to listOf(Pool.SQUAT, Pool.HINGE, Pool.SQUAT, Pool.LEG_ISOLATION, Pool.CALF, Pool.CORE),
        SessionKind.UPPER to listOf(
            Pool.CHEST_PRESS, Pool.ROW, Pool.SHOULDER_PRESS, Pool.VERTICAL_PULL, Pool.LATERAL, Pool.BICEPS, Pool.TRICEPS,
        ),
        SessionKind.LOWER to listOf(Pool.SQUAT, Pool.HINGE, Pool.LUNGE, Pool.LEG_ISOLATION, Pool.CALF, Pool.CORE),
        SessionKind.FULL_BODY to listOf(
            Pool.SQUAT, Pool.CHEST_PRESS, Pool.ROW, Pool.HINGE, Pool.LATERAL, Pool.BICEPS, Pool.TRICEPS,
        ),
    )

    /** Extra exercise given to a focus group in a balanced program. */
    private val EMPHASIS = mapOf(
        MuscleGroup.CHEST to Pool.CHEST_FLY,
        MuscleGroup.BACK to Pool.VERTICAL_PULL,
        MuscleGroup.SHOULDERS to Pool.LATERAL,
        MuscleGroup.BICEPS to Pool.BICEPS,
        MuscleGroup.TRICEPS to Pool.TRICEPS,
        MuscleGroup.LEGS to Pool.LEG_ISOLATION,
        MuscleGroup.GLUTES to Pool.GLUTE_THRUST,
        MuscleGroup.CALVES to Pool.CALF,
        MuscleGroup.ABS to Pool.CORE,
        MuscleGroup.FOREARMS to Pool.FOREARM,
        MuscleGroup.CARDIO to Pool.CARDIO,
    )

    /** Where a focus group goes when the split has no session for it. */
    private val PARTNER = mapOf(
        MuscleGroup.GLUTES to MuscleGroup.LEGS,
        MuscleGroup.CALVES to MuscleGroup.LEGS,
        MuscleGroup.FOREARMS to MuscleGroup.BICEPS,
    )

    /** Session content when a group is trained on its own (specialization). */
    private val GROUP_SLOTS = mapOf(
        MuscleGroup.CHEST to listOf(Pool.CHEST_PRESS, Pool.CHEST_PRESS, Pool.CHEST_FLY, Pool.CHEST_FLY),
        MuscleGroup.BACK to listOf(Pool.VERTICAL_PULL, Pool.ROW, Pool.ROW, Pool.VERTICAL_PULL),
        MuscleGroup.SHOULDERS to listOf(Pool.SHOULDER_PRESS, Pool.LATERAL, Pool.REAR_DELT, Pool.LATERAL),
        MuscleGroup.BICEPS to List(4) { Pool.BICEPS },
        MuscleGroup.TRICEPS to List(4) { Pool.TRICEPS },
        MuscleGroup.LEGS to listOf(Pool.SQUAT, Pool.HINGE, Pool.LEG_ISOLATION, Pool.LUNGE),
        MuscleGroup.GLUTES to listOf(Pool.GLUTE_THRUST, Pool.GLUTE_ISOLATION, Pool.GLUTE_ISOLATION, Pool.GLUTE_THRUST),
        MuscleGroup.CALVES to List(3) { Pool.CALF },
        MuscleGroup.ABS to List(3) { Pool.CORE },
        MuscleGroup.FOREARMS to List(3) { Pool.FOREARM },
        MuscleGroup.CARDIO to List(2) { Pool.CARDIO },
    )

    private class Scheme(val sets: Int, val repsMin: Int, val repsMax: Int, val restSeconds: Int)

    private fun scheme(goal: Goal, compound: Boolean): Scheme = when (goal) {
        Goal.STRENGTH -> if (compound) Scheme(4, 4, 6, 180) else Scheme(3, 8, 12, 90)
        Goal.MUSCLE_GAIN -> if (compound) Scheme(4, 6, 10, 150) else Scheme(3, 10, 15, 75)
        Goal.FAT_LOSS -> if (compound) Scheme(3, 8, 12, 90) else Scheme(3, 12, 15, 60)
        Goal.GENERAL_FITNESS -> if (compound) Scheme(3, 8, 12, 120) else Scheme(3, 10, 15, 75)
    }

    private class Blueprint(val kind: SessionKind, val slots: List<Pool>, val focusGroups: List<MuscleGroup> = emptyList())

    fun generate(options: GeneratorOptions, available: List<Exercise>): List<GeneratedSession> {
        val count = options.sessionsPerWeek.coerceIn(MIN_SESSIONS, MAX_SESSIONS)
        val specialization = options.focusOnly && options.focus.isNotEmpty()
        val blueprints = if (specialization) focusBlueprints(options.focus, count) else splitBlueprints(count, options.focus)
        val byId = available.associateBy { it.id }
        val occurrences = blueprints.groupingBy { it.kind to it.focusGroups }.eachCount()
        val seen = mutableMapOf<Pair<SessionKind, List<MuscleGroup>>, Int>()

        return blueprints.map { blueprint ->
            val key = blueprint.kind to blueprint.focusGroups
            val variant = seen.getOrDefault(key, 0).also { seen[key] = it + 1 }
            val picked = pick(blueprint.slots, variant, byId, available)
            GeneratedSession(
                kind = blueprint.kind,
                variant = variant,
                repeated = occurrences.getValue(key) > 1,
                groups = picked.map { it.second.muscleGroup }.distinct(),
                exercises = picked.map { (pool, exercise) ->
                    planned(exercise, pool, options.goal, bonusSet = !specialization && exercise.muscleGroup in options.focus)
                },
            )
        }
    }

    private fun splitBlueprints(count: Int, focus: Set<MuscleGroup>): List<Blueprint> {
        val sessions = SPLITS.getValue(count).map { it to SESSION_SLOTS.getValue(it).toMutableList() }
        for (group in focus.sortedBy { it.ordinal }) {
            val pool = EMPHASIS.getValue(group)
            val targets = sessions.filter { (_, slots) -> slots.any { it.group == group } }
                .ifEmpty { sessions.filter { (_, slots) -> slots.any { it.group == PARTNER[group] } } }
                .ifEmpty { sessions }
            targets.forEach { (_, slots) -> slots += pool }
        }
        return sessions.map { (kind, slots) ->
            // Focus first (priority principle), except abs and cardio which belong at the end.
            val ordered = slots.sortedBy { if (it.group in focus && it.group !in TRAILING) 0 else 1 }.toMutableList()
            while (ordered.size > MAX_EXERCISES) {
                val dropped = ordered.indexOfLast { it.group !in focus }.takeIf { it > 0 } ?: break
                ordered.removeAt(dropped)
            }
            Blueprint(kind, ordered)
        }
    }

    private fun focusBlueprints(focus: Set<MuscleGroup>, count: Int): List<Blueprint> {
        val groups = focus.sortedBy { it.ordinal }
        val bucketCount = ceil(groups.size / 3.0).toInt()
        val buckets = List(bucketCount) { b -> groups.filterIndexed { i, _ -> i % bucketCount == b } }
        return List(count) { day ->
            val bucket = buckets[day % bucketCount]
            val perGroup = when (bucket.size) {
                1 -> 4
                2 -> 3
                else -> 2
            }
            Blueprint(SessionKind.FOCUS, bucket.flatMap { GROUP_SLOTS.getValue(it).take(perGroup) }, bucket)
        }
    }

    /**
     * One exercise per slot, never twice in a session. Variant B starts further down each
     * pool so repeated sessions rotate exercises.
     */
    private fun pick(
        slots: List<Pool>,
        variant: Int,
        byId: Map<String, Exercise>,
        available: List<Exercise>,
    ): List<Pair<Pool, Exercise>> {
        val used = mutableSetOf<String>()
        val perPool = slots.groupingBy { it }.eachCount()
        val occurrence = mutableMapOf<Pool, Int>()
        return slots.mapNotNull { pool ->
            val n = occurrence.getOrDefault(pool, 0).also { occurrence[pool] = it + 1 }
            // Exercises removed from the library are skipped; an empty pool falls back to the muscle group.
            val candidates = pool.exerciseIds.mapNotNull { byId[it] }
                .ifEmpty { available.filter { it.muscleGroup == pool.group } }
            if (candidates.isEmpty()) return@mapNotNull null
            val start = variant * perPool.getValue(pool) + n
            val exercise = candidates.indices
                .map { candidates[(start + it) % candidates.size] }
                .firstOrNull { it.id !in used }
                ?: return@mapNotNull null
            used += exercise.id
            pool to exercise
        }
    }

    private fun planned(exercise: Exercise, pool: Pool, goal: Goal, bonusSet: Boolean): PlannedExercise {
        if (exercise.trackingType == TrackingType.DURATION) {
            val cardio = exercise.muscleGroup == MuscleGroup.CARDIO
            return PlannedExercise(exercise.id, targetSets = if (cardio) 1 else 3, restSeconds = if (cardio) 0 else 60)
        }
        val scheme = scheme(goal, pool.compound)
        return PlannedExercise(
            exerciseId = exercise.id,
            targetSets = (scheme.sets + if (bonusSet) 1 else 0).coerceAtMost(MAX_SETS),
            targetRepsMin = scheme.repsMin,
            targetRepsMax = scheme.repsMax,
            restSeconds = scheme.restSeconds,
        )
    }
}
