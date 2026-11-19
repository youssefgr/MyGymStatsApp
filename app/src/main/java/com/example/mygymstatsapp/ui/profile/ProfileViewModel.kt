package com.example.mygymstatsapp.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.PeriodStats
import com.example.mygymstatsapp.domain.model.ThemeMode
import com.example.mygymstatsapp.domain.model.UserProfile
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.utils.parseDecimal
import com.example.mygymstatsapp.utils.toKg
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile = UserProfile(),
    val allTime: PeriodStats = PeriodStats.EMPTY,
)

class ProfileViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    statsRepository: StatsRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        preferencesRepository.profile,
        statsRepository.observePeriodStats(from = 0),
    ) { profile, allTime ->
        ProfileUiState(isLoading = false, profile = profile, allTime = allTime)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun setName(value: String) = update { it.copy(name = value.trim()) }

    /** Body weight typed in the display unit, stored in kg. */
    fun setBodyWeight(text: String) = update { profile ->
        profile.copy(bodyWeightKg = parseDecimal(text)?.takeIf { it > 0 }?.let { profile.weightUnit.toKg(it) })
    }

    fun setHeight(text: String) = update { it.copy(heightCm = text.toIntOrNull()?.takeIf { cm -> cm in 50..260 }) }
    fun setGoal(value: Goal) = update { it.copy(goal = value) }
    fun setWeightUnit(value: WeightUnit) = update { it.copy(weightUnit = value) }
    fun setDefaultRest(seconds: Int) = update { it.copy(defaultRestSeconds = seconds) }
    fun setThemeMode(value: ThemeMode) = update { it.copy(themeMode = value) }
    fun setWeightIncrement(kg: Double) = update { it.copy(weightIncrementKg = kg) }
    fun setAutoWarmup(enabled: Boolean) = update { it.copy(autoWarmup = enabled) }

    private fun update(transform: (UserProfile) -> UserProfile) {
        viewModelScope.launch { preferencesRepository.updateProfile(transform) }
    }
}
