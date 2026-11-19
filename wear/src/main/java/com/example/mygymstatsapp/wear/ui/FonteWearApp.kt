package com.example.mygymstatsapp.wear.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.example.mygymstatsapp.shared.FinishedSummary
import com.example.mygymstatsapp.shared.PhoneEvent
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.wear.R
import com.example.mygymstatsapp.wear.container
import com.example.mygymstatsapp.wear.ui.theme.FonteWearTheme
import com.example.mygymstatsapp.wear.workout.WorkoutService
import com.example.mygymstatsapp.wear.workout.hasHeartRatePermission
import kotlinx.coroutines.delay

private object Routes {
    const val MAIN = "main"
    const val EDIT = "edit/{setId}/{field}"
    const val SUMMARY = "summary"
    const val HISTORY = "history"
    const val HISTORY_DETAIL = "history/{workoutId}"
    const val RECORDS = "records"

    fun edit(setId: String, field: EditField) = "edit/$setId/${field.name}"
    fun historyDetail(workoutId: String) = "history/$workoutId"
}

@Composable
fun FonteWearApp() {
    val context = LocalContext.current
    val container = context.container
    val viewModel: WatchViewModel = viewModel(
        factory = viewModelFactory { initializer { WatchViewModel(container.link, container.heartRate) } },
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberSwipeDismissableNavController()
    var summary by remember { mutableStateOf<FinishedSummary?>(null) }
    var message by remember { mutableStateOf<WatchMessage?>(null) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { incoming ->
            if (incoming is WatchMessage.Finished) {
                summary = incoming.summary
                navController.navigate(Routes.SUMMARY) { popUpTo(Routes.MAIN) }
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                message = incoming
            }
        }
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(MESSAGE_DURATION_MS)
            message = null
        }
    }

    // Follow the workout on the watch (heart rate, rest alert) as soon as one is running.
    val workoutId = state.workout?.id
    LaunchedEffect(workoutId) {
        if (workoutId != null && hasHeartRatePermission(context)) WorkoutService.start(context)
    }

    FonteWearTheme {
        AppScaffold {
            SwipeDismissableNavHost(navController = navController, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) {
                    val workout = state.workout
                    if (workout != null) {
                        WorkoutScreen(
                            state = state,
                            workout = workout,
                            viewModel = viewModel,
                            onEdit = { set, field -> navController.navigate(Routes.edit(set.id, field)) },
                        )
                    } else {
                        HomeScreen(
                            state = state,
                            onStart = viewModel::start,
                            onHistory = { navController.navigate(Routes.HISTORY) },
                            onRecords = { navController.navigate(Routes.RECORDS) },
                        )
                    }
                }
                composable(Routes.EDIT) { entry ->
                    val setId = entry.arguments?.getString("setId")
                    val field = entry.arguments?.getString("field")?.let { name -> EditField.entries.firstOrNull { it.name == name } }
                    val workout = state.workout
                    val exercise = workout?.exercises?.firstOrNull { e -> e.sets.any { it.id == setId } }
                    val set = exercise?.sets?.firstOrNull { it.id == setId }
                    val phone = state.phone
                    if (exercise == null || set == null || field == null || phone == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        EditValueScreen(
                            set = set,
                            field = field,
                            unit = phone.unit,
                            weightStepKg = phone.weightStepKg,
                            onConfirm = { weightKg, reps, duration ->
                                viewModel.setValues(set, weightKg, reps, duration)
                                navController.popBackStack()
                            },
                        )
                    }
                }
                composable(Routes.SUMMARY) {
                    val finished = summary
                    if (finished == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        SummaryScreen(
                            summary = finished,
                            unit = state.phone?.unit ?: WearUnit.KG,
                            onDone = { navController.popBackStack(Routes.MAIN, inclusive = false) },
                        )
                    }
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(history = state.history, onOpen = { navController.navigate(Routes.historyDetail(it)) })
                }
                composable(Routes.HISTORY_DETAIL) { entry ->
                    val id = entry.arguments?.getString("workoutId")
                    val history = state.history
                    val workout = history?.workouts?.firstOrNull { it.id == id }
                    if (history == null || workout == null) {
                        LaunchedEffect(Unit) { navController.popBackStack() }
                    } else {
                        PastWorkoutScreen(workout = workout, unit = history.unit)
                    }
                }
                composable(Routes.RECORDS) {
                    RecordsScreen(history = state.history)
                }
            }
            MessageOverlay(message, state.phone?.unit ?: WearUnit.KG)
        }
    }
}

/** A short message over everything: a record from the phone, a refused action. */
@Composable
private fun MessageOverlay(message: WatchMessage?, unit: WearUnit) {
    AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (message) {
                is WatchMessage.Record -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(stringResource(R.string.record_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        message.record.exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                    Text(
                        recordValue(message.record, unit),
                        style = MaterialTheme.typography.numeralSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                is WatchMessage.Text -> Text(
                    stringResource(message.textRes),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                else -> Unit
            }
        }
    }
}

fun recordValue(record: PhoneEvent.Record, unit: WearUnit): String = when (record.recordType) {
    com.example.mygymstatsapp.shared.WearRecordType.MAX_REPS -> "${record.value.toInt()} reps"
    com.example.mygymstatsapp.shared.WearRecordType.VOLUME -> formatVolume(record.value, unit)
    else -> formatWeight(record.value, unit)
}

private const val MESSAGE_DURATION_MS = 3_000L
