package com.example.mygymstatsapp.timer

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * App-wide rest timer. It stores an end timestamp (elapsedRealtime), not a counter, so it stays
 * exact across screens, tabs, configuration changes and screen-off.
 *
 * Outside the app, a system notification shows the countdown and an alarm triggers the
 * "rest is over" alert, even if Android killed the process in the meantime.
 */
class RestTimer(
    context: Context,
    private val scope: CoroutineScope,
) {
    data class State(
        val totalMs: Long = 0,
        /** elapsedRealtime at which the rest ends (when running). */
        val endAtMs: Long = 0,
        /** Remaining time while paused, null when running. */
        val pausedRemainingMs: Long? = null,
        val isActive: Boolean = false,
    ) {
        val isPaused: Boolean get() = pausedRemainingMs != null

        fun remainingMs(now: Long = SystemClock.elapsedRealtime()): Long = when {
            !isActive -> 0
            pausedRemainingMs != null -> pausedRemainingMs
            else -> (endAtMs - now).coerceAtLeast(0)
        }

        fun progress(now: Long = SystemClock.elapsedRealtime()): Float =
            if (totalMs <= 0) 0f else (remainingMs(now).toFloat() / totalMs).coerceIn(0f, 1f)
    }

    val notifier = RestTimerNotifier(context)
    val alarms = RestAlarmScheduler(context)
    private val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private var completionJob: Job? = null
    /** End timestamp of the last rest already announced (the alarm and the coroutine may both fire). */
    private var lastFinishedEndAt: Long? = null

    @Synchronized
    fun start(seconds: Int) {
        if (seconds <= 0) return
        val totalMs = seconds * 1000L
        _state.value = State(totalMs = totalMs, endAtMs = now() + totalMs, isActive = true)
        notifier.cancelFinished()
        onRunningChanged()
    }

    @Synchronized
    fun adjust(deltaSeconds: Int) {
        val current = _state.value
        if (!current.isActive) return
        val delta = deltaSeconds * 1000L
        val remaining = current.remainingMs() + delta
        if (remaining <= 0) {
            skip()
            return
        }
        _state.value = current.copy(
            totalMs = (current.totalMs + delta).coerceAtLeast(remaining),
            endAtMs = if (current.isPaused) current.endAtMs else current.endAtMs + delta,
            pausedRemainingMs = current.pausedRemainingMs?.let { remaining },
        )
        if (current.isPaused) notifier.showRunning(remaining, paused = true) else onRunningChanged()
    }

    @Synchronized
    fun pause() {
        val current = _state.value
        if (!current.isActive || current.isPaused) return
        val remaining = current.remainingMs()
        _state.value = current.copy(pausedRemainingMs = remaining)
        completionJob?.cancel()
        alarms.cancel()
        notifier.showRunning(remaining, paused = true)
    }

    @Synchronized
    fun resume() {
        val current = _state.value
        val paused = current.pausedRemainingMs ?: return
        _state.value = current.copy(endAtMs = now() + paused, pausedRemainingMs = null)
        onRunningChanged()
    }

    @Synchronized
    fun skip() {
        completionJob?.cancel()
        alarms.cancel()
        notifier.cancelRunning()
        _state.value = State()
    }

    /** Called by the in-app coroutine or by the alarm, whichever comes first. */
    @Synchronized
    fun onRestFinished(endAtMs: Long) {
        if (lastFinishedEndAt == endAtMs) return
        val current = _state.value
        // A stale alarm for a rest that was replaced by a newer one: ignore it.
        if (current.isActive && current.endAtMs != endAtMs) return
        lastFinishedEndAt = endAtMs
        _state.value = State()
        alarms.cancel()
        notifier.cancelRunning()
        if (notifier.canNotify()) {
            notifier.showFinished()
        } else {
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 150, 250, 150, 400), -1))
        }
    }

    private fun onRunningChanged() {
        val current = _state.value
        completionJob?.cancel()
        completionJob = scope.launch {
            delay(current.remainingMs())
            onRestFinished(current.endAtMs)
        }
        alarms.schedule(current.endAtMs)
        notifier.showRunning(current.remainingMs(), paused = false)
    }

    private fun now() = SystemClock.elapsedRealtime()
}
