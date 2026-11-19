package com.example.mygymstatsapp.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Messages exchanged between the phone and the watch. Weights are always in kg, as in the
 * phone's database; the watch converts for display with [WearUnit]. Times are wall-clock epoch
 * milliseconds: the two devices' elapsedRealtime clocks are unrelated.
 */

// Enums mirror the phone's domain enums by name.
enum class WearMuscle { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, LEGS, GLUTES, CALVES, ABS, FOREARMS, CARDIO }

enum class WearTracking { WEIGHT_REPS, REPS_ONLY, DURATION }

enum class WearSetType { WARMUP, NORMAL, DROP, FAILURE }

enum class WearUnit(val label: String, val perKg: Double) {
    KG("kg", 1.0),
    LB("lb", 2.2046226218),
}

enum class WearRecordType { MAX_WEIGHT, ONE_RM, MAX_REPS, VOLUME }

// ---- Phone -> watch: live state (DataItem /fonte/state) -------------------------------------

@Serializable
data class PhoneState(
    val workout: LiveWorkout? = null,
    val rest: LiveRest? = null,
    val unit: WearUnit = WearUnit.KG,
    /** Load step of the profile (progressive overload), used by the watch's +/- buttons. */
    val weightStepKg: Double = 2.5,
    val defaultRestSeconds: Int = 90,
    /** Sessions that can be started from the watch: each program's next session first. */
    val sessions: List<StartableSession> = emptyList(),
    /** Changes on every publish so a forced resync always reaches the watch. */
    val publishedAt: Long = 0,
)

@Serializable
data class LiveWorkout(
    val id: String,
    val name: String,
    val startedAt: Long,
    val exercises: List<LiveExercise>,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
) {
    val completedSets: Int get() = exercises.sumOf { e -> e.sets.count { it.completed } }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
}

@Serializable
data class LiveExercise(
    /** workout_exercises.id */
    val id: String,
    val exerciseId: String,
    val name: String,
    val muscle: WearMuscle,
    val tracking: WearTracking,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val restSeconds: Int? = null,
    /** Letter of the superset this exercise belongs to ("A"), null when alone. */
    val superset: String? = null,
    val sets: List<LiveSet>,
)

@Serializable
data class LiveSet(
    val id: String,
    val setNumber: Int,
    /** Working set number (warm-ups not counted), null for a warm-up. */
    val displayNumber: Int?,
    val type: WearSetType,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val completed: Boolean = false,
    /** Same set last time, shown as "Précédent" and used when a field is empty. */
    val previousWeightKg: Double? = null,
    val previousReps: Int? = null,
    val previousDurationSeconds: Int? = null,
)

@Serializable
data class LiveRest(
    val totalMs: Long,
    /** Wall-clock time the rest ends at, while running. */
    val endsAt: Long,
    /** Remaining time while paused, null while running. */
    val pausedRemainingMs: Long? = null,
) {
    val isPaused: Boolean get() = pausedRemainingMs != null

    fun remainingMs(now: Long = System.currentTimeMillis()): Long =
        pausedRemainingMs ?: (endsAt - now).coerceAtLeast(0)
}

@Serializable
data class StartableSession(
    val templateId: String,
    val name: String,
    val programName: String? = null,
    /** The program's next session ("prochaine"). */
    val isNext: Boolean = false,
    val exerciseCount: Int,
    val setCount: Int,
    val muscles: List<WearMuscle> = emptyList(),
)

// ---- Phone -> watch: history (DataItem /fonte/history) --------------------------------------

@Serializable
data class WatchHistory(
    val workouts: List<PastWorkout> = emptyList(),
    val records: List<RecordLine> = emptyList(),
    val unit: WearUnit = WearUnit.KG,
    val publishedAt: Long = 0,
)

@Serializable
data class PastWorkout(
    val id: String,
    val name: String,
    val startedAt: Long,
    val durationSeconds: Long,
    val volumeKg: Double,
    val setCount: Int,
    val muscles: List<WearMuscle> = emptyList(),
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val exercises: List<PastExercise> = emptyList(),
)

@Serializable
data class PastExercise(
    val name: String,
    val muscle: WearMuscle,
    val tracking: WearTracking,
    /** Completed working sets, in order. */
    val sets: List<PastSet>,
)

@Serializable
data class PastSet(val weightKg: Double? = null, val reps: Int? = null, val durationSeconds: Int? = null)

@Serializable
data class RecordLine(
    val exerciseId: String,
    val name: String,
    val muscle: WearMuscle,
    val tracking: WearTracking,
    val maxWeightKg: Double,
    val maxReps: Int,
    val bestOneRmKg: Double,
    val lastPerformedAt: Long,
)

// ---- Watch -> phone: commands (message /fonte/command) --------------------------------------

@Serializable
sealed interface WatchCommand {
    /** Asks the phone to publish its state again (watch app opened, tile shown). */
    @Serializable
    @SerialName("sync")
    data object RequestSync : WatchCommand

    /** [templateId] null = empty workout. Refused (ActiveWorkoutExists) if one is in progress. */
    @Serializable
    @SerialName("start")
    data class StartWorkout(val templateId: String?) : WatchCommand

    @Serializable
    @SerialName("set_values")
    data class SetValues(
        val setId: String,
        val weightKg: Double?,
        val reps: Int?,
        val durationSeconds: Int?,
    ) : WatchCommand

    /** Validates the set (same rules as the ✓ on the phone) or un-validates it. */
    @Serializable
    @SerialName("toggle_set")
    data class ToggleSet(val workoutExerciseId: String, val setId: String) : WatchCommand

    @Serializable
    @SerialName("add_set")
    data class AddSet(val workoutExerciseId: String) : WatchCommand

    @Serializable
    @SerialName("rest_adjust")
    data class RestAdjust(val deltaSeconds: Int) : WatchCommand

    @Serializable
    @SerialName("rest_pause")
    data object RestPause : WatchCommand

    @Serializable
    @SerialName("rest_resume")
    data object RestResume : WatchCommand

    @Serializable
    @SerialName("rest_skip")
    data object RestSkip : WatchCommand

    @Serializable
    @SerialName("finish")
    data class Finish(val workoutId: String) : WatchCommand

    /** Live heart rate, plus the running average and max since the workout started on the watch. */
    @Serializable
    @SerialName("heart_rate")
    data class HeartRate(val workoutId: String, val bpm: Int, val avg: Int, val max: Int) : WatchCommand

    /**
     * Sent regularly while the watch follows the workout (heart rate session running): the watch
     * then gives the end-of-rest alert itself and the phone stops bridging its own notification.
     */
    @Serializable
    @SerialName("following")
    data class Following(val following: Boolean) : WatchCommand
}

// ---- Phone -> watch: events (message /fonte/event) ------------------------------------------

@Serializable
sealed interface PhoneEvent {
    @Serializable
    @SerialName("record")
    data class Record(
        val exerciseName: String,
        val recordType: WearRecordType,
        val value: Double,
        val previousValue: Double,
        val weightKg: Double? = null,
        val reps: Int? = null,
    ) : PhoneEvent

    /** A set can't be validated without reps (or a duration for timed exercises). */
    @Serializable
    @SerialName("needs_value")
    data class NeedsValue(val tracking: WearTracking) : PhoneEvent

    @Serializable
    @SerialName("finished")
    data class Finished(val summary: FinishedSummary) : PhoneEvent

    /** Finishing needs at least one validated set. */
    @Serializable
    @SerialName("finish_refused")
    data object FinishRefused : PhoneEvent

    /** A workout is already in progress: the watch resumes it instead. */
    @Serializable
    @SerialName("start_refused")
    data object StartRefused : PhoneEvent
}

@Serializable
data class FinishedSummary(
    val workoutId: String,
    val name: String,
    val durationSeconds: Long,
    val volumeKg: Double,
    val setCount: Int,
    val exerciseCount: Int,
    val avgHeartRate: Int? = null,
    val maxHeartRate: Int? = null,
    val records: List<PhoneEvent.Record> = emptyList(),
)
