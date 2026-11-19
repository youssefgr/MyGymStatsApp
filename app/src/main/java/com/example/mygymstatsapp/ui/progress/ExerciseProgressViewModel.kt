package com.example.mygymstatsapp.ui.progress

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.ExerciseProgressPoint
import com.example.mygymstatsapp.domain.model.PersonalRecord
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.utils.monthsAgoMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class ProgressRange(@get:StringRes val labelRes: Int, val months: Long?) {
    ONE_MONTH(R.string.range_1m, 1),
    THREE_MONTHS(R.string.range_3m, 3),
    SIX_MONTHS(R.string.range_6m, 6),
    ONE_YEAR(R.string.range_1y, 12),
    ALL(R.string.range_all, null),
    ;

    fun since(): Long = months?.let { monthsAgoMillis(it) } ?: 0L
}

enum class ProgressMetric(@get:StringRes val labelRes: Int) {
    MAX_WEIGHT(R.string.record_max_weight),
    ONE_RM(R.string.record_one_rm),
    VOLUME(R.string.label_volume),
    REPS(R.string.metric_reps),
    DURATION(R.string.label_duration),
    FREQUENCY(R.string.metric_frequency),
    ;

    companion object {
        fun availableFor(trackingType: TrackingType): List<ProgressMetric> = when (trackingType) {
            TrackingType.WEIGHT_REPS -> listOf(MAX_WEIGHT, ONE_RM, VOLUME, REPS, FREQUENCY)
            TrackingType.REPS_ONLY -> listOf(REPS, FREQUENCY)
            TrackingType.DURATION -> listOf(DURATION, FREQUENCY)
        }
    }
}

data class ExerciseProgressUiState(
    val isLoading: Boolean = true,
    val performedExercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val range: ProgressRange = ProgressRange.THREE_MONTHS,
    val metric: ProgressMetric = ProgressMetric.MAX_WEIGHT,
    val metrics: List<ProgressMetric> = emptyList(),
    val points: List<ExerciseProgressPoint> = emptyList(),
    val record: PersonalRecord? = null,
    val weightUnit: WeightUnit = WeightUnit.KG,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseProgressViewModel(
    private val savedStateHandle: SavedStateHandle,
    statsRepository: StatsRepository,
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val selectedId = savedStateHandle.getStateFlow<String?>(KEY_EXERCISE, null)
    private val range = MutableStateFlow(ProgressRange.THREE_MONTHS)
    private val metric = MutableStateFlow<ProgressMetric?>(null)

    private val performed = statsRepository.observePerformedExercises()

    private val selectedExercise = combine(performed, selectedId) { exercises, id ->
        exercises.firstOrNull { it.id == id } ?: exercises.firstOrNull()
    }.distinctUntilChanged()

    private val points = combine(selectedExercise.map { it?.id }.distinctUntilChanged(), range) { id, range -> id to range }
        .flatMapLatest { (id, range) ->
            if (id == null) flowOf(emptyList()) else statsRepository.observeExerciseProgress(id, range.since())
        }

    private val selection = combine(range, metric) { range, metric -> range to metric }
    private val context = combine(preferencesRepository.profile, statsRepository.observePersonalRecords()) { profile, records ->
        profile.weightUnit to records
    }

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        performed,
        selectedExercise,
        points,
        selection,
        context,
    ) { performed, exercise, points, (range, metric), (unit, records) ->
        val metrics = exercise?.let { ProgressMetric.availableFor(it.trackingType) }.orEmpty()
        ExerciseProgressUiState(
            isLoading = false,
            performedExercises = performed,
            selectedExercise = exercise,
            range = range,
            metric = metric?.takeIf { it in metrics } ?: metrics.firstOrNull() ?: ProgressMetric.MAX_WEIGHT,
            metrics = metrics,
            points = points,
            record = records.firstOrNull { it.exerciseId == exercise?.id },
            weightUnit = unit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    fun selectExercise(id: String) {
        savedStateHandle[KEY_EXERCISE] = id
        metric.value = null
    }

    fun selectRange(value: ProgressRange) {
        range.value = value
    }

    fun selectMetric(value: ProgressMetric) {
        metric.value = value
    }

    private companion object {
        const val KEY_EXERCISE = "selected_exercise_id"
    }
}
