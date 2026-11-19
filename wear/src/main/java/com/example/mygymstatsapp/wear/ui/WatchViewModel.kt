package com.example.mygymstatsapp.wear.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.shared.FinishedSummary
import com.example.mygymstatsapp.shared.LiveExercise
import com.example.mygymstatsapp.shared.LiveSet
import com.example.mygymstatsapp.shared.LiveWorkout
import com.example.mygymstatsapp.shared.PhoneEvent
import com.example.mygymstatsapp.shared.PhoneState
import com.example.mygymstatsapp.shared.WatchCommand
import com.example.mygymstatsapp.shared.WatchHistory
import com.example.mygymstatsapp.shared.WearTracking
import com.example.mygymstatsapp.wear.R
import com.example.mygymstatsapp.wear.link.PhoneLink
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WatchUiState(
    val phone: PhoneState? = null,
    val history: WatchHistory? = null,
    val phoneReachable: Boolean? = null,
    val heartRate: Int? = null,
) {
    val workout: LiveWorkout? get() = phone?.workout
}

sealed interface WatchMessage {
    data class Record(val record: PhoneEvent.Record) : WatchMessage
    data class Text(@get:StringRes val textRes: Int) : WatchMessage
    data class Finished(val summary: FinishedSummary) : WatchMessage
}

/**
 * A value tapped on the watch, shown right away while the phone applies it (a round trip through
 * the phone takes a fraction of a second). Dropped once the phone's state agrees, or after a delay.
 */
private data class SetOverride(
    val weightKg: Double? = null,
    val reps: Int? = null,
    val durationSeconds: Int? = null,
    val valuesSet: Boolean = false,
    val completed: Boolean? = null,
    val token: Long,
)

class WatchViewModel(
    private val link: PhoneLink,
    heartRate: StateFlow<Int?>,
) : ViewModel() {

    private val overrides = MutableStateFlow<Map<String, SetOverride>>(emptyMap())

    val uiState: StateFlow<WatchUiState> = combine(
        link.state,
        link.history,
        link.phoneReachable,
        heartRate,
        overrides,
    ) { phone, history, reachable, bpm, pending ->
        WatchUiState(
            phone = phone?.let { it.copy(workout = it.workout?.withOverrides(pending)) },
            history = history,
            phoneReachable = reachable,
            heartRate = bpm,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WatchUiState())

    private val _messages = MutableSharedFlow<WatchMessage>(extraBufferCapacity = 8)
    val messages: Flow<WatchMessage> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            link.events.collect { event ->
                when (event) {
                    is PhoneEvent.Record -> _messages.emit(WatchMessage.Record(event))
                    is PhoneEvent.NeedsValue -> {
                        // The phone refused the ✓: undo the optimistic check marks.
                        overrides.update { map -> map.mapValues { it.value.copy(completed = null) } }
                        _messages.emit(
                            WatchMessage.Text(if (event.tracking == WearTracking.DURATION) R.string.msg_needs_duration else R.string.msg_needs_reps)
                        )
                    }
                    is PhoneEvent.Finished -> _messages.emit(WatchMessage.Finished(event.summary))
                    PhoneEvent.FinishRefused -> _messages.emit(WatchMessage.Text(R.string.msg_finish_needs_set))
                    PhoneEvent.StartRefused -> _messages.emit(WatchMessage.Text(R.string.msg_start_refused))
                }
            }
        }
    }

    fun start(templateId: String?) = send(WatchCommand.StartWorkout(templateId))

    fun toggle(exercise: LiveExercise, set: LiveSet) {
        override(set.id) { it.copy(completed = !set.completed) }
        send(WatchCommand.ToggleSet(exercise.id, set.id))
    }

    fun setValues(set: LiveSet, weightKg: Double?, reps: Int?, durationSeconds: Int?) {
        override(set.id) { it.copy(weightKg = weightKg, reps = reps, durationSeconds = durationSeconds, valuesSet = true) }
        send(WatchCommand.SetValues(set.id, weightKg, reps, durationSeconds))
    }

    fun addSet(exercise: LiveExercise) = send(WatchCommand.AddSet(exercise.id))

    fun adjustRest(deltaSeconds: Int) = send(WatchCommand.RestAdjust(deltaSeconds))
    fun pauseRest() = send(WatchCommand.RestPause)
    fun resumeRest() = send(WatchCommand.RestResume)
    fun skipRest() = send(WatchCommand.RestSkip)

    fun finish(workoutId: String) = send(WatchCommand.Finish(workoutId))

    private fun send(command: WatchCommand, reportFailure: Boolean = true) {
        link.sendAsync(command) {
            overrides.value = emptyMap()
            if (reportFailure) _messages.tryEmit(WatchMessage.Text(R.string.msg_phone_unreachable))
        }
    }

    private fun override(setId: String, change: (SetOverride) -> SetOverride) {
        val token = System.nanoTime()
        overrides.update { map -> map + (setId to change(map[setId] ?: SetOverride(token = token)).copy(token = token)) }
        viewModelScope.launch {
            delay(OVERRIDE_TTL_MS)
            overrides.update { map -> if (map[setId]?.token == token) map - setId else map }
        }
    }

    private fun LiveWorkout.withOverrides(pending: Map<String, SetOverride>): LiveWorkout {
        if (pending.isEmpty()) return this
        return copy(
            exercises = exercises.map { exercise ->
                exercise.copy(
                    sets = exercise.sets.map { set ->
                        val o = pending[set.id] ?: return@map set
                        var result = set
                        if (o.valuesSet) result = result.copy(weightKg = o.weightKg, reps = o.reps, durationSeconds = o.durationSeconds)
                        o.completed?.let { result = result.copy(completed = it) }
                        result
                    },
                )
            },
        )
    }

    private companion object {
        const val OVERRIDE_TTL_MS = 4_000L
    }
}
