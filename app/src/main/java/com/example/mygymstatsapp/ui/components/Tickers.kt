package com.example.mygymstatsapp.ui.components

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.mygymstatsapp.timer.RestTimer
import kotlinx.coroutines.delay

/*
 * Ticking values are read only by small leaf composables, so a running clock never
 * recomposes the surrounding screen.
 */

/** Seconds elapsed since [startedAt] (epoch ms), updated every second. */
@Composable
fun rememberElapsedSeconds(startedAt: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000 - now % 1_000)
        }
    }
    return ((now - startedAt) / 1_000).coerceAtLeast(0)
}

/** Current elapsedRealtime, refreshed while the rest timer is running. */
@Composable
fun rememberRestClock(state: RestTimer.State): Long {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state) {
        now = SystemClock.elapsedRealtime()
        while (state.isActive && !state.isPaused) {
            delay(100)
            now = SystemClock.elapsedRealtime()
        }
    }
    return now
}
