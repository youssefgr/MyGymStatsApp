package com.example.mygymstatsapp.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * Wakes the app when the rest ends, even if its process was killed. Uses an exact alarm when
 * the user allowed it ("Alarmes et rappels"), otherwise an inexact one that Android may delay.
 */
class RestAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = alarmManager.canScheduleExactAlarms()

    /** [endAtElapsedMs] is in the SystemClock.elapsedRealtime() time base. */
    fun schedule(endAtElapsedMs: Long) {
        val intent = finishedIntent(endAtElapsedMs)
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAtElapsedMs, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAtElapsedMs, intent)
        }
    }

    fun cancel() = alarmManager.cancel(finishedIntent(0))

    private fun finishedIntent(endAtElapsedMs: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_FINISHED,
        Intent(context, RestTimerReceiver::class.java)
            .setAction(RestTimerReceiver.ACTION_FINISHED)
            .putExtra(RestTimerReceiver.EXTRA_END_AT, endAtElapsedMs),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_FINISHED = 10
    }
}
