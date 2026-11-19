package com.example.mygymstatsapp.domain.model

object OneRepMax {
    /**
     * Epley formula: 1RM = weight × (1 + reps / 30). A single rep is already a 1RM.
     * Must stay in sync with [com.example.mygymstatsapp.data.local.dao.ONE_RM_SQL].
     */
    fun epley(weightKg: Double, reps: Int): Double = when {
        weightKg <= 0.0 || reps <= 0 -> 0.0
        reps == 1 -> weightKg
        else -> weightKg * (1 + reps / 30.0)
    }
}

/** Best values reached on this exercise in this workout, or null if no completed working set. */
fun WorkoutExercise.bests(): ExerciseBests? {
    val working = sets.filter { it.completed && it.setType != SetType.WARMUP }
    if (working.isEmpty()) return null
    return ExerciseBests(
        maxWeightKg = working.maxOf { it.weightKg ?: 0.0 },
        maxReps = working.maxOf { it.reps ?: 0 },
        bestOneRmKg = working.maxOf { it.estimatedOneRepMaxKg },
        bestVolumeKg = volumeKg,
    )
}
