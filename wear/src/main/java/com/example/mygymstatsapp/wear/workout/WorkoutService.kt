package com.example.mygymstatsapp.wear.workout

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.startExercise
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.mygymstatsapp.shared.LiveRest
import com.example.mygymstatsapp.shared.LiveWorkout
import com.example.mygymstatsapp.shared.WatchCommand
import com.example.mygymstatsapp.wear.MainActivity
import com.example.mygymstatsapp.wear.R
import com.example.mygymstatsapp.wear.container
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Runs on the watch while a workout is in progress and the heart-rate permission is granted:
 * - an exercise session in Health Services (heart rate, also with the screen off), whose average
 *   and max are sent to the phone and saved with the workout;
 * - the end-of-rest alert on the wrist (the phone then keeps its own alert to itself);
 * - an ongoing activity, so the workout stays one tap away from the watch face.
 * It stops by itself when the phone says the workout is over.
 */
class WorkoutService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val link by lazy { container.link }
    private val exerciseClient: ExerciseClient by lazy { HealthServices.getClient(this).exerciseClient }

    private var running = false
    private var session: Job? = null
    private var workoutId: String? = null
    private var ownsExercise = false
    private var heartRateSum = 0L
    private var heartRateCount = 0
    private var heartRateMax = 0
    private var latestHeartRate: Int? = null

    private val updateCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            update.latestMetrics.getData(DataType.HEART_RATE_BPM).forEach { sample ->
                val bpm = sample.value.roundToInt()
                if (bpm in 30..250) {
                    heartRateSum += bpm
                    heartRateCount++
                    heartRateMax = maxOf(heartRateMax, bpm)
                    latestHeartRate = bpm
                }
            }
            container.heartRate.value = latestHeartRate
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit
        override fun onRegistered() = Unit
        override fun onRegistrationFailed(throwable: Throwable) = Unit
        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) = Unit
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val workout = link.state.value?.workout
        if (workout == null || !hasHeartRatePermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification(workout), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        if (!running) {
            running = true
            follow()
        }
        return START_NOT_STICKY
    }

    private fun follow() {
        session?.cancel()
        session = scope.launch { followWorkout() }
    }

    private fun CoroutineScope.followWorkout() {
        // The workout changes or ends on the phone (or from this watch).
        launch {
            link.state.map { it?.workout }.distinctUntilChanged { a, b -> a?.id == b?.id }.collect { workout ->
                if (workout == null) {
                    stop()
                } else if (workout.id != workoutId) {
                    workoutId = workout.id
                    resetHeartRate()
                    startForeground(NOTIFICATION_ID, buildNotification(workout), ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
                    startHeartRate()
                }
            }
        }
        // Tell the phone the wrist gives the rest alert (renewed before the phone's timeout).
        launch {
            while (true) {
                link.send(WatchCommand.Following(true))
                delay(FOLLOWING_PERIOD_MS)
            }
        }
        // End of rest: buzz the wrist.
        launch {
            link.state.map { it?.rest }.distinctUntilChanged().collectLatest { rest -> alertAtEnd(rest) }
        }
        // Heart rate to the phone: live value, plus average and max for the workout.
        launch {
            while (true) {
                delay(HEART_RATE_PERIOD_MS)
                sendHeartRate()
            }
        }
    }

    private suspend fun alertAtEnd(rest: LiveRest?) {
        if (rest == null || rest.isPaused) return
        val remaining = rest.endsAt - System.currentTimeMillis()
        if (remaining <= 0) return
        delay(remaining)
        getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?.vibrate(VibrationEffect.createWaveform(REST_VIBRATION, -1))
    }

    private suspend fun startHeartRate() {
        runCatching {
            val info = exerciseClient.getCurrentExerciseInfo()
            when (info.exerciseTrackedStatus) {
                // Another app (e.g. Samsung Health) is recording a workout: leave it alone.
                ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS -> return
                // Our session survived a restart of the app: just listen again.
                ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS -> {
                    exerciseClient.setUpdateCallback(updateCallback)
                    ownsExercise = true
                    return
                }
            }
            val capabilities = exerciseClient.getCapabilities()
            val type = listOf(ExerciseType.WEIGHTLIFTING, ExerciseType.STRENGTH_TRAINING, ExerciseType.WORKOUT)
                .firstOrNull { it in capabilities.supportedExerciseTypes } ?: return
            if (DataType.HEART_RATE_BPM !in capabilities.getExerciseTypeCapabilities(type).supportedDataTypes) return
            exerciseClient.setUpdateCallback(updateCallback)
            exerciseClient.startExercise(
                ExerciseConfig.builder(type)
                    .setDataTypes(setOf(DataType.HEART_RATE_BPM))
                    .setIsAutoPauseAndResumeEnabled(false)
                    .setIsGpsEnabled(false)
                    .build()
            )
            ownsExercise = true
        }
    }

    private suspend fun sendHeartRate() {
        val id = workoutId ?: return
        val bpm = latestHeartRate ?: return
        if (heartRateCount == 0) return
        link.send(WatchCommand.HeartRate(id, bpm, (heartRateSum / heartRateCount).toInt(), heartRateMax))
    }

    private fun resetHeartRate() {
        heartRateSum = 0
        heartRateCount = 0
        heartRateMax = 0
        latestHeartRate = null
        container.heartRate.value = null
    }

    private suspend fun stop() {
        // Final figures for the workout that just ended (saved with it on the phone).
        sendHeartRate()
        link.send(WatchCommand.Following(false))
        if (ownsExercise) {
            runCatching {
                exerciseClient.endExercise()
                exerciseClient.clearUpdateCallbackAsync(updateCallback)
            }
            ownsExercise = false
        }
        container.heartRate.value = null
        workoutId = null
        running = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        session?.cancel()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(workout: LiveWorkout) = run {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_workout), NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_fonte_mark)
            .setColor(getColor(R.color.fonte))
            .setContentTitle(getString(R.string.notification_workout))
            .setContentText(workout.name)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp)
        // Elapsed time on the watch face, in the elapsedRealtime base.
        val startedElapsed = SystemClock.elapsedRealtime() - (System.currentTimeMillis() - workout.startedAt)
        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_fonte_mark)
            .setTouchIntent(openApp)
            .setStatus(
                Status.Builder()
                    .addTemplate("#name# · #time#")
                    .addPart("name", Status.TextPart(workout.name))
                    .addPart("time", Status.StopwatchPart(startedElapsed))
                    .build()
            )
            .build()
            .apply(applicationContext)
        builder.build()
    }

    companion object {
        private const val CHANNEL_ID = "workout"
        private const val NOTIFICATION_ID = 2001
        private const val FOLLOWING_PERIOD_MS = 30_000L
        private const val HEART_RATE_PERIOD_MS = 15_000L
        private val REST_VIBRATION = longArrayOf(0, 250, 150, 250, 150, 400)

        fun start(context: Context) {
            if (!hasHeartRatePermission(context)) return
            runCatching { ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java)) }
        }
    }
}
