package com.example.mygymstatsapp.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mygymstatsapp.MainActivity
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.utils.formatClock

/**
 * Rest timer notifications: a silent ongoing countdown (rendered by the system, so it keeps
 * ticking even if the app process is gone) and an alerting "rest is over" notification.
 */
class RestTimerNotifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    /** When true, the "rest is over" alert is not bridged to the watch (the watch alerts on its own). */
    var keepOnPhone: () -> Boolean = { false }

    fun createChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_RUNNING, context.getString(R.string.channel_rest_running), NotificationManager.IMPORTANCE_LOW)
                .apply { setShowBadge(false) }
        )
        system.createNotificationChannel(
            NotificationChannel(CHANNEL_FINISHED, context.getString(R.string.channel_rest_finished), NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    enableVibration(true)
                    vibrationPattern = VIBRATION_PATTERN
                }
        )
    }

    fun canNotify(): Boolean =
        manager.areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun showRunning(remainingMs: Long, paused: Boolean) {
        if (!canNotify()) return
        val builder = NotificationCompat.Builder(context, CHANNEL_RUNNING)
            .setSmallIcon(R.drawable.ic_fonte_mark)
            .setColor(context.getColor(R.color.fonte))
            .setContentTitle(context.getString(R.string.notification_rest_title))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())
            .addAction(0, context.getString(R.string.action_skip), skipIntent())
        if (paused) {
            builder.setContentText(context.getString(R.string.notification_rest_paused, formatClock((remainingMs + 999) / 1000)))
        } else {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + remainingMs)
        }
        notify(ID_RUNNING, builder)
    }

    fun showFinished() {
        notify(
            ID_FINISHED,
            NotificationCompat.Builder(context, CHANNEL_FINISHED)
                .setSmallIcon(R.drawable.ic_fonte_mark)
                .setColor(context.getColor(R.color.fonte))
                .setContentTitle(context.getString(R.string.notification_rest_finished_title))
                .setContentText(context.getString(R.string.notification_rest_finished_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(VIBRATION_PATTERN)
                .setAutoCancel(true)
                .setLocalOnly(keepOnPhone())
                .setTimeoutAfter(FINISHED_TIMEOUT_MS)
                .setContentIntent(openAppIntent()),
        )
    }

    fun cancelRunning() = manager.cancel(ID_RUNNING)

    fun cancelFinished() = manager.cancel(ID_FINISHED)

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        if (canNotify()) {
            @Suppress("MissingPermission") // checked by canNotify()
            manager.notify(id, builder.build())
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun skipIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_SKIP,
        Intent(context, RestTimerReceiver::class.java).setAction(RestTimerReceiver.ACTION_SKIP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val CHANNEL_RUNNING = "rest_timer_running"
        const val CHANNEL_FINISHED = "rest_timer_finished"
        const val ID_RUNNING = 1001
        const val ID_FINISHED = 1002
        const val REQUEST_SKIP = 11
        const val FINISHED_TIMEOUT_MS = 60_000L
        val VIBRATION_PATTERN = longArrayOf(0, 250, 150, 250, 150, 400)
    }
}
