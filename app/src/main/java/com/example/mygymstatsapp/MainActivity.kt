package com.example.mygymstatsapp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mygymstatsapp.domain.model.ThemeMode
import com.example.mygymstatsapp.navigation.GymApp
import com.example.mygymstatsapp.ui.theme.MyGymStatsAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = (application as GymApplication).container.userPreferencesRepository
        setContent {
            val profile by preferences.profile.collectAsStateWithLifecycle(initialValue = null)
            val darkTheme = when (profile?.themeMode ?: ThemeMode.DARK) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            DisposableEffect(darkTheme) {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }
            MyGymStatsAppTheme(darkTheme = darkTheme) {
                GymApp()
            }
        }
    }
}
