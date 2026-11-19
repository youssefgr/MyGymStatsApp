package com.example.mygymstatsapp.wear

import android.content.Context
import android.os.SystemClock
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.SupersetGrouping
import com.example.mygymstatsapp.domain.model.UserProfile
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.model.fallbackSet
import com.example.mygymstatsapp.domain.model.matchPreviousSets
import com.example.mygymstatsapp.domain.model.workingSetNumbers
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.DetectPersonalRecordsUseCase
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.domain.usecase.StartResult
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.session.ActiveWorkoutController
import com.example.mygymstatsapp.session.SessionEvent
import com.example.mygymstatsapp.shared.FinishedSummary
import com.example.mygymstatsapp.shared.LiveExercise
import com.example.mygymstatsapp.shared.LiveRest
import com.example.mygymstatsapp.shared.LiveSet
import com.example.mygymstatsapp.shared.LiveWorkout
import com.example.mygymstatsapp.shared.PastExercise
import com.example.mygymstatsapp.shared.PastSet
import com.example.mygymstatsapp.shared.PastWorkout
import com.example.mygymstatsapp.shared.PhoneEvent
import com.example.mygymstatsapp.shared.PhoneState
import com.example.mygymstatsapp.shared.RecordLine
import com.example.mygymstatsapp.shared.StartableSession
import com.example.mygymstatsapp.shared.WatchCommand
import com.example.mygymstatsapp.shared.WatchHistory
import com.example.mygymstatsapp.shared.WearMuscle
import com.example.mygymstatsapp.shared.WearPaths
import com.example.mygymstatsapp.shared.WearRecordType
import com.example.mygymstatsapp.shared.WearSetType
import com.example.mygymstatsapp.shared.WearTracking
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.shared.decodeWatchCommand
import com.example.mygymstatsapp.shared.encode
import com.example.mygymstatsapp.shared.toJson
import com.example.mygymstatsapp.timer.RestTimer
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Phone side of the watch link (Wearable Data Layer). The phone stays the only owner of the data:
 * - it publishes the live state (workout in progress, rest timer, sessions to start) and a
 *   history snapshot as DataItems, which the system keeps in sync on the watch;
 * - it applies the watch's commands through the same [ActiveWorkoutController] as the workout
 *   screen, and forwards the session's events (record, finished…) to the watch.
 *
 * Nothing runs until a watch with the Fonte app has been seen, so phones without a watch (or
 * without Google Play services) pay nothing.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class PhoneWearSync(
    context: Context,
    private val scope: CoroutineScope,
    private val workoutRepository: WorkoutRepository,
    private val templateRepository: TemplateRepository,
    private val programRepository: ProgramRepository,
    private val statsRepository: StatsRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val restTimer: RestTimer,
    private val controller: ActiveWorkoutController,
    private val startWorkout: StartWorkoutUseCase,
    private val detectRecords: DetectPersonalRecordsUseCase,
) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    /** A watch with the Fonte app is paired (seen once is enough: DataItems sync when it reconnects). */
    private val watchInstalled = MutableStateFlow(false)

    /** Bumped to publish again even if nothing changed (the watch app was just opened). */
    private val resync = MutableStateFlow(0)

    @Volatile
    private var lastFollowingAt = Long.MIN_VALUE / 2

    private val _liveHeartRate = MutableStateFlow<Int?>(null)
    private var heartRateTimeout: Job? = null

    /** Last heart rate received from the watch, null when none arrived for a while. */
    val liveHeartRate: StateFlow<Int?> = _liveHeartRate.asStateFlow()

    /**
     * True while the watch follows the workout and gives the end-of-rest alert itself: the phone's
     * "rest is over" notification then stays on the phone instead of buzzing the wrist twice.
     */
    fun isWatchFollowing(): Boolean = SystemClock.elapsedRealtime() - lastFollowingAt < FOLLOWING_TIMEOUT_MS

    fun start() {
        scope.launch { watchWatchCapability() }
        scope.launch {
            watchInstalled.flatMapLatest { installed -> if (installed) stateFlow() else emptyFlow() }
                .collect { put(WearPaths.STATE, it.toJson()) }
        }
        scope.launch {
            watchInstalled.flatMapLatest { installed -> if (installed) historyFlow() else emptyFlow() }
                .collect { put(WearPaths.HISTORY, it.toJson()) }
        }
        scope.launch { controller.events.collect { forward(it) } }
    }

    /** A command from the watch (see PhoneWearListenerService). */
    suspend fun handle(bytes: ByteArray) {
        val command = decodeWatchCommand(bytes) ?: return
        watchInstalled.value = true
        when (command) {
            WatchCommand.RequestSync -> resync.update { it + 1 }
            is WatchCommand.StartWorkout -> {
                val request = command.templateId?.let { StartRequest.FromTemplate(it) } ?: StartRequest.Empty
                if (startWorkout(request) is StartResult.ActiveWorkoutExists) send(PhoneEvent.StartRefused)
            }
            is WatchCommand.SetValues ->
                controller.setValues(command.setId, command.weightKg, command.reps, command.durationSeconds)
            is WatchCommand.ToggleSet -> controller.toggleCompleted(command.workoutExerciseId, command.setId)
            is WatchCommand.AddSet -> controller.enqueue { workoutRepository.addSet(command.workoutExerciseId) }
            is WatchCommand.RestAdjust -> restTimer.adjust(command.deltaSeconds)
            WatchCommand.RestPause -> restTimer.pause()
            WatchCommand.RestResume -> restTimer.resume()
            WatchCommand.RestSkip -> restTimer.skip()
            is WatchCommand.Finish -> controller.finish(command.workoutId)
            is WatchCommand.HeartRate -> {
                onHeartRate(command.bpm)
                controller.enqueue { workoutRepository.updateHeartRate(command.workoutId, command.avg, command.max) }
            }
            is WatchCommand.Following ->
                lastFollowingAt = if (command.following) SystemClock.elapsedRealtime() else Long.MIN_VALUE / 2
        }
    }

    private fun onHeartRate(bpm: Int) {
        _liveHeartRate.value = bpm
        heartRateTimeout?.cancel()
        heartRateTimeout = scope.launch {
            delay(HEART_RATE_TIMEOUT_MS)
            _liveHeartRate.value = null
        }
    }

    private suspend fun watchWatchCapability() {
        runCatching {
            val info = capabilityClient.getCapability(WearPaths.CAPABILITY_WATCH, CapabilityClient.FILTER_ALL).await()
            if (info.nodes.isNotEmpty()) watchInstalled.value = true
            capabilityClient.addListener({ changed -> if (changed.nodes.isNotEmpty()) watchInstalled.value = true }, WearPaths.CAPABILITY_WATCH)
        }
    }

    // ---- Live state ---------------------------------------------------------------------------

    private fun stateFlow(): Flow<PhoneState> {
        val workout = workoutRepository.observeActiveWorkout()
        // Last time's sets, as in the workout screen ("Précédent" and the empty-field fallback).
        val previous = workout
            .map { it?.templateId to it?.exercises?.map { e -> e.exercise.id }?.toSet().orEmpty() }
            .distinctUntilChanged()
            .mapLatest { (templateId, ids) -> ids.associateWith { workoutRepository.getLastPerformance(it, templateId) } }
            .onStart { emit(emptyMap()) }
        val sessions = combine(programRepository.observePrograms(), templateRepository.observeTemplates(), ::startableSessions)
        val live = combine(workout, previous) { w, p -> w?.toLive(p) }

        return combine(live, restTimer.state, preferencesRepository.profile, sessions, resync) { liveWorkout, rest, profile, startable, sync ->
            sync to PhoneState(
                workout = liveWorkout,
                rest = rest.toLive(),
                unit = profile.wearUnit(),
                weightStepKg = profile.weightIncrementKg,
                defaultRestSeconds = profile.defaultRestSeconds,
                sessions = startable,
            )
        }
            .debounce(PUBLISH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .map { (_, state) -> state.copy(publishedAt = System.currentTimeMillis()) }
    }

    private fun Workout.toLive(previous: Map<String, List<WorkoutSet>>): LiveWorkout {
        val letters = SupersetGrouping.letters(exercises.map { it.supersetGroup })
        return LiveWorkout(
            id = id,
            name = name,
            startedAt = startedAt,
            avgHeartRate = avgHeartRate,
            maxHeartRate = maxHeartRate,
            exercises = exercises.map { exercise ->
                val matched = matchPreviousSets(exercise.sets, previous[exercise.exercise.id].orEmpty())
                val numbers = workingSetNumbers(exercise.sets)
                LiveExercise(
                    id = exercise.id,
                    exerciseId = exercise.exercise.id,
                    name = exercise.exercise.name,
                    muscle = exercise.exercise.muscleGroup.toWear(),
                    tracking = WearTracking.valueOf(exercise.exercise.trackingType.name),
                    targetRepsMin = exercise.targetRepsMin,
                    targetRepsMax = exercise.targetRepsMax,
                    restSeconds = exercise.restSeconds,
                    superset = exercise.supersetGroup?.let { letters[it]?.toString() },
                    sets = exercise.sets.map { set ->
                        val last = fallbackSet(exercise.sets, set, matched[set.id])
                        LiveSet(
                            id = set.id,
                            setNumber = set.setNumber,
                            displayNumber = numbers[set.id],
                            type = WearSetType.valueOf(set.setType.name),
                            weightKg = set.weightKg,
                            reps = set.reps,
                            durationSeconds = set.durationSeconds,
                            completed = set.completed,
                            previousWeightKg = last?.weightKg,
                            previousReps = last?.reps,
                            previousDurationSeconds = last?.durationSeconds,
                        )
                    },
                )
            },
        )
    }

    /** The rest timer runs on elapsedRealtime; the watch needs wall-clock time. */
    private fun RestTimer.State.toLive(): LiveRest? {
        if (!isActive) return null
        val remaining = remainingMs()
        return LiveRest(
            totalMs = totalMs,
            endsAt = System.currentTimeMillis() + remaining,
            pausedRemainingMs = pausedRemainingMs,
        )
    }

    /** Each program's next session first, then its other sessions, then the standalone ones. */
    private fun startableSessions(programs: List<Program>, templates: List<WorkoutTemplate>): List<StartableSession> {
        val fromPrograms = programs.filter { it.sessions.isNotEmpty() }.flatMap { program ->
            val next = program.nextSession
            (listOfNotNull(next) + program.sessions.filter { it.id != next?.id })
                .map { it.toStartable(program.name, isNext = it.id == next?.id) }
        }
        val standalone = templates.filter { it.programId == null && it.exercises.isNotEmpty() }
            .sortedByDescending { it.lastUsedAt ?: 0 }
            .map { it.toStartable(programName = null, isNext = false) }
        return fromPrograms + standalone
    }

    private fun WorkoutTemplate.toStartable(programName: String?, isNext: Boolean) = StartableSession(
        templateId = id,
        name = name,
        programName = programName,
        isNext = isNext,
        exerciseCount = exercises.size,
        setCount = totalSets,
        muscles = exercises.map { it.exercise.muscleGroup.toWear() }.distinct(),
    )

    // ---- History ------------------------------------------------------------------------------

    private fun historyFlow(): Flow<WatchHistory> {
        val unit = preferencesRepository.profile.map { it.wearUnit() }.distinctUntilChanged()
        return combine(
            workoutRepository.observeCompletedWorkouts(HISTORY_SIZE),
            statsRepository.observePersonalRecords(),
            unit,
            resync,
        ) { workouts, records, wearUnit, sync -> Triple(workouts, records, wearUnit) to sync }
            .debounce(PUBLISH_DEBOUNCE_MS)
            .mapLatest { (data, _) ->
                val (summaries, records, wearUnit) = data
                WatchHistory(
                    workouts = summaries.mapNotNull { summary ->
                        val workout = workoutRepository.getWorkout(summary.id) ?: return@mapNotNull null
                        PastWorkout(
                            id = workout.id,
                            name = workout.name,
                            startedAt = workout.startedAt,
                            durationSeconds = workout.durationSeconds,
                            volumeKg = workout.volumeKg,
                            setCount = summary.setCount,
                            muscles = summary.muscleGroups.map { it.toWear() },
                            avgHeartRate = workout.avgHeartRate,
                            maxHeartRate = workout.maxHeartRate,
                            exercises = workout.exercises.map { exercise ->
                                PastExercise(
                                    name = exercise.exercise.name,
                                    muscle = exercise.exercise.muscleGroup.toWear(),
                                    tracking = WearTracking.valueOf(exercise.exercise.trackingType.name),
                                    sets = exercise.sets
                                        .filter { it.completed && it.setType != SetType.WARMUP }
                                        .map { PastSet(it.weightKg, it.reps, it.durationSeconds) },
                                )
                            },
                        )
                    },
                    records = records.sortedByDescending { it.lastPerformedAt }.take(RECORDS_SIZE).map {
                        RecordLine(
                            exerciseId = it.exerciseId,
                            name = it.exerciseName,
                            muscle = it.muscleGroup.toWear(),
                            tracking = WearTracking.valueOf(it.trackingType.name),
                            maxWeightKg = it.bests.maxWeightKg,
                            maxReps = it.bests.maxReps,
                            bestOneRmKg = it.bests.bestOneRmKg,
                            lastPerformedAt = it.lastPerformedAt,
                        )
                    },
                    unit = wearUnit,
                    publishedAt = System.currentTimeMillis(),
                )
            }
    }

    // ---- Events and transport -----------------------------------------------------------------

    private suspend fun forward(event: SessionEvent) {
        if (!watchInstalled.value) return
        val message = when (event) {
            is SessionEvent.RecordAchieved -> event.record.toWear()
            is SessionEvent.NeedsValue -> PhoneEvent.NeedsValue(WearTracking.valueOf(event.trackingType.name))
            is SessionEvent.Finished -> workoutRepository.getWorkout(event.workoutId)?.let { workout ->
                PhoneEvent.Finished(
                    FinishedSummary(
                        workoutId = workout.id,
                        name = workout.name,
                        durationSeconds = workout.durationSeconds,
                        volumeKg = workout.volumeKg,
                        setCount = workout.completedSetCount,
                        exerciseCount = workout.exercises.size,
                        avgHeartRate = workout.avgHeartRate,
                        maxHeartRate = workout.maxHeartRate,
                        records = runCatching { detectRecords(workout) }.getOrDefault(emptyList()).map { it.toWear() },
                    )
                )
            }
            SessionEvent.FinishRefused -> PhoneEvent.FinishRefused
            SessionEvent.Discarded -> null
        }
        message?.let { send(it) }
    }

    private suspend fun send(event: PhoneEvent) {
        runCatching {
            val nodes = capabilityClient.getCapability(WearPaths.CAPABILITY_WATCH, CapabilityClient.FILTER_REACHABLE).await().nodes
            val bytes = event.encode()
            nodes.forEach { node -> messageClient.sendMessage(node.id, WearPaths.EVENT, bytes).await() }
        }
    }

    private suspend fun put(path: String, json: String) {
        runCatching {
            val request = PutDataMapRequest.create(path)
                .apply { dataMap.putString(WearPaths.KEY_JSON, json) }
                .asPutDataRequest()
                .setUrgent()
            dataClient.putDataItem(request).await()
        }
    }

    private fun MuscleGroup.toWear() = WearMuscle.valueOf(name)

    private fun UserProfile.wearUnit() = WearUnit.valueOf(weightUnit.name)

    private fun NewPersonalRecord.toWear() = PhoneEvent.Record(
        exerciseName = exerciseName,
        recordType = WearRecordType.valueOf(type.name),
        value = value,
        previousValue = previousValue,
        weightKg = weightKg,
        reps = reps,
    )

    private companion object {
        const val PUBLISH_DEBOUNCE_MS = 150L
        const val HISTORY_SIZE = 10
        const val RECORDS_SIZE = 40
        /** The watch says it is following every 30 s; past this, the phone alerts again. */
        const val FOLLOWING_TIMEOUT_MS = 75_000L
        const val HEART_RATE_TIMEOUT_MS = 45_000L
    }
}
