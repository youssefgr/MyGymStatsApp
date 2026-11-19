package com.example.mygymstatsapp.wear.workout

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Wear OS 6 (API 36) replaced BODY_SENSORS with a dedicated heart-rate permission. */
fun heartRatePermission(): String =
    if (Build.VERSION.SDK_INT >= 36) "android.permission.health.READ_HEART_RATE" else Manifest.permission.BODY_SENSORS

fun hasHeartRatePermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, heartRatePermission()) == PackageManager.PERMISSION_GRANTED

/** The system prompt is shown once by itself; after that, only from the "Suivre le cœur" button. */
object HeartRatePrompt {
    private const val PREFS = "fonte_watch"
    private const val KEY_SHOWN = "heart_rate_prompt_shown"

    fun wasShown(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SHOWN, false)

    fun markShown(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SHOWN, true).apply()
    }
}
