package com.example.mygymstatsapp.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The phone and the watch are separate apps: what one encodes, the other must decode. */
class WearProtocolTest {

    @Test
    fun `every command survives a round trip`() {
        val commands = listOf(
            WatchCommand.RequestSync,
            WatchCommand.StartWorkout(null),
            WatchCommand.StartWorkout("tpl_1"),
            WatchCommand.SetValues("set_1", 82.5, 8, null),
            WatchCommand.ToggleSet("we_1", "set_1"),
            WatchCommand.AddSet("we_1"),
            WatchCommand.RestAdjust(-15),
            WatchCommand.RestPause,
            WatchCommand.RestResume,
            WatchCommand.RestSkip,
            WatchCommand.Finish("w_1"),
            WatchCommand.HeartRate("w_1", bpm = 132, avg = 118, max = 161),
            WatchCommand.Following(true),
        )
        commands.forEach { assertEquals(it, decodeWatchCommand(it.encode())) }
    }

    @Test
    fun `every event survives a round trip`() {
        val record = PhoneEvent.Record("Bench Press", WearRecordType.MAX_WEIGHT, 92.5, 90.0, 92.5, 5)
        val events = listOf(
            record,
            PhoneEvent.NeedsValue(WearTracking.REPS_ONLY),
            PhoneEvent.Finished(FinishedSummary("w_1", "Push", 3120, 8450.0, 20, 6, 118, 161, listOf(record))),
            PhoneEvent.FinishRefused,
            PhoneEvent.StartRefused,
        )
        events.forEach { assertEquals(it, decodePhoneEvent(it.encode())) }
    }

    @Test
    fun `state round trip keeps the workout, rest and sessions`() {
        val state = PhoneState(
            workout = LiveWorkout(
                id = "w_1",
                name = "Push",
                startedAt = 1_000,
                exercises = listOf(
                    LiveExercise(
                        id = "we_1",
                        exerciseId = "ex_bench_press",
                        name = "Bench Press",
                        muscle = WearMuscle.CHEST,
                        tracking = WearTracking.WEIGHT_REPS,
                        targetRepsMin = 8,
                        targetRepsMax = 12,
                        superset = "A",
                        sets = listOf(
                            LiveSet("s1", 1, null, WearSetType.WARMUP, 40.0, 10, completed = true),
                            LiveSet("s2", 2, 1, WearSetType.NORMAL, previousWeightKg = 80.0, previousReps = 8),
                        ),
                    )
                ),
            ),
            rest = LiveRest(totalMs = 90_000, endsAt = 5_000, pausedRemainingMs = null),
            unit = WearUnit.LB,
            sessions = listOf(StartableSession("tpl_1", "Pull", "Programme 4 jours", isNext = true, exerciseCount = 6, setCount = 20)),
            publishedAt = 42,
        )
        assertEquals(state, decodePhoneState(state.toJson()))
        assertEquals(1, state.workout!!.completedSets)
        assertEquals(2, state.workout!!.totalSets)
    }

    @Test
    fun `unknown fields from a newer app are ignored`() {
        val json = """{"type":"rest_adjust","deltaSeconds":15,"addedLater":true}"""
        assertEquals(WatchCommand.RestAdjust(15), decodeWatchCommand(json.encodeToByteArray()))
    }

    @Test
    fun `garbage decodes to null instead of crashing`() {
        assertNull(decodeWatchCommand("not json".encodeToByteArray()))
        assertNull(decodePhoneState("{"))
    }

    @Test
    fun `rest remaining uses the wall clock and the paused value`() {
        assertEquals(4_000, LiveRest(totalMs = 90_000, endsAt = 10_000).remainingMs(now = 6_000))
        assertEquals(0, LiveRest(totalMs = 90_000, endsAt = 10_000).remainingMs(now = 12_000))
        assertEquals(30_000, LiveRest(totalMs = 90_000, endsAt = 10_000, pausedRemainingMs = 30_000).remainingMs(now = 99_999))
    }
}
