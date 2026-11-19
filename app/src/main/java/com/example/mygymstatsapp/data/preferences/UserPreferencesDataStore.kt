package com.example.mygymstatsapp.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.mygymstatsapp.domain.model.UserProfile
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.userPreferences by preferencesDataStore(name = "user_preferences")

/** Profile and preferences (not workout data) stored with DataStore. */
class UserPreferencesDataStore(context: Context) : UserPreferencesRepository {

    private val dataStore = context.applicationContext.userPreferences

    override val profile: Flow<UserProfile> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { it.toProfile() }
        .distinctUntilChanged()

    override suspend fun updateProfile(transform: (UserProfile) -> UserProfile) {
        dataStore.edit { preferences ->
            val updated = transform(preferences.toProfile())
            preferences[Keys.NAME] = updated.name
            preferences.setOrRemove(Keys.BODY_WEIGHT_KG, updated.bodyWeightKg)
            preferences.setOrRemove(Keys.HEIGHT_CM, updated.heightCm)
            preferences[Keys.GOAL] = updated.goal.name
            preferences[Keys.WEIGHT_UNIT] = updated.weightUnit.name
            preferences[Keys.DEFAULT_REST_SECONDS] = updated.defaultRestSeconds
            preferences[Keys.THEME_MODE] = updated.themeMode.name
            preferences[Keys.WEIGHT_INCREMENT_KG] = updated.weightIncrementKg
            preferences[Keys.AUTO_WARMUP] = updated.autoWarmup
            preferences[Keys.ONBOARDING_COMPLETED] = updated.onboardingCompleted
            preferences[Keys.NOTIFICATION_PROMPT_SHOWN] = updated.notificationPromptShown
        }
    }

    private fun Preferences.toProfile(): UserProfile {
        val defaults = UserProfile()
        return UserProfile(
            name = this[Keys.NAME] ?: defaults.name,
            bodyWeightKg = this[Keys.BODY_WEIGHT_KG],
            heightCm = this[Keys.HEIGHT_CM],
            goal = enumOrDefault(this[Keys.GOAL], defaults.goal),
            weightUnit = enumOrDefault(this[Keys.WEIGHT_UNIT], defaults.weightUnit),
            defaultRestSeconds = this[Keys.DEFAULT_REST_SECONDS] ?: defaults.defaultRestSeconds,
            themeMode = enumOrDefault(this[Keys.THEME_MODE], defaults.themeMode),
            weightIncrementKg = this[Keys.WEIGHT_INCREMENT_KG] ?: defaults.weightIncrementKg,
            autoWarmup = this[Keys.AUTO_WARMUP] ?: defaults.autoWarmup,
            onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            notificationPromptShown = this[Keys.NOTIFICATION_PROMPT_SHOWN] ?: defaults.notificationPromptShown,
        )
    }

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
        if (value == null) remove(key) else this[key] = value
    }

    private object Keys {
        val NAME = stringPreferencesKey("name")
        val BODY_WEIGHT_KG = doublePreferencesKey("body_weight_kg")
        val HEIGHT_CM = intPreferencesKey("height_cm")
        val GOAL = stringPreferencesKey("goal")
        val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
        val DEFAULT_REST_SECONDS = intPreferencesKey("default_rest_seconds")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEIGHT_INCREMENT_KG = doublePreferencesKey("weight_increment_kg")
        val AUTO_WARMUP = booleanPreferencesKey("auto_warmup")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")
    }
}

