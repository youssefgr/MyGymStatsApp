package com.example.mygymstatsapp.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val weightUnit: WeightUnit = WeightUnit.KG,
    val goal: Goal = Goal.MUSCLE_GAIN,
)

class OnboardingViewModel(private val preferencesRepository: UserPreferencesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onUnitChange(value: WeightUnit) = _uiState.update { it.copy(weightUnit = value) }
    fun onGoalChange(value: Goal) = _uiState.update { it.copy(goal = value) }

    /** Saves the answers (or only marks onboarding as done when [skip] is true). */
    fun complete(skip: Boolean = false) {
        val state = _uiState.value
        viewModelScope.launch {
            preferencesRepository.updateProfile { profile ->
                if (skip) {
                    profile.copy(onboardingCompleted = true)
                } else {
                    profile.copy(
                        name = state.name.trim(),
                        weightUnit = state.weightUnit,
                        goal = state.goal,
                        onboardingCompleted = true,
                    )
                }
            }
        }
    }
}
