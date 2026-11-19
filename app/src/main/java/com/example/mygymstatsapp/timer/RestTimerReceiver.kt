package com.example.mygymstatsapp.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mygymstatsapp.GymApplication

/** Rest alarm and "Passer" notification action. Works even if the app process was restarted. */
class RestTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val restTimer = (context.applicationContext as GymApplication).container.restTimer
        when (intent.action) {
            ACTION_FINISHED -> restTimer.onRestFinished(intent.getLongExtra(EXTRA_END_AT, 0))
            ACTION_SKIP -> restTimer.skip()
        }
    }

    companion object {
        const val ACTION_FINISHED = "com.example.mygymstatsapp.action.REST_FINISHED"
        const val ACTION_SKIP = "com.example.mygymstatsapp.action.REST_SKIP"
        const val EXTRA_END_AT = "end_at"
    }
}
