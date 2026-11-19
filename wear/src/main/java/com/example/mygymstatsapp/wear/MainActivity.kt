package com.example.mygymstatsapp.wear

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.mygymstatsapp.shared.WatchCommand
import com.example.mygymstatsapp.wear.ui.FonteWearApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleStartRequest(intent)
        setContent { FonteWearApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleStartRequest(intent)
    }

    override fun onResume() {
        super.onResume()
        // The phone publishes again: the watch shows fresh data even after a long time away.
        container.link.sendAsync(WatchCommand.RequestSync)
    }

    /** "Démarrer" on the tile opens the app with the session to start. */
    private fun handleStartRequest(intent: Intent?) {
        if (intent?.hasExtra(EXTRA_START_TEMPLATE) != true) return
        val templateId = intent.getStringExtra(EXTRA_START_TEMPLATE)?.takeIf { it.isNotEmpty() }
        intent.removeExtra(EXTRA_START_TEMPLATE)
        container.link.sendAsync(WatchCommand.StartWorkout(templateId))
    }

    companion object {
        /** Template id to start, empty for an empty workout. */
        const val EXTRA_START_TEMPLATE = "start_template_id"
    }
}
