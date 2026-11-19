package com.example.mygymstatsapp.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.domain.model.PersonalRecord
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RecordsUiState(
    val isLoading: Boolean = true,
    val records: List<PersonalRecord> = emptyList(),
    val weightUnit: WeightUnit = WeightUnit.KG,
)

class RecordsViewModel(
    statsRepository: StatsRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<RecordsUiState> = combine(
        statsRepository.observePersonalRecords(),
        preferencesRepository.profile,
    ) { records, profile ->
        RecordsUiState(
            isLoading = false,
            // Strongest lifts first, then bodyweight exercises by max reps.
            records = records
                .filter { it.trackingType != TrackingType.DURATION }
                .sortedWith(
                    compareBy<PersonalRecord> { it.trackingType != TrackingType.WEIGHT_REPS }
                        .thenByDescending { it.bests.bestOneRmKg }
                        .thenByDescending { it.bests.maxReps }
                ),
            weightUnit = profile.weightUnit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordsUiState())
}
