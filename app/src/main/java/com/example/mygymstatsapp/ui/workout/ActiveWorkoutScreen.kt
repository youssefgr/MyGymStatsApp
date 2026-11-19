package com.example.mygymstatsapp.ui.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.NewPersonalRecord
import com.example.mygymstatsapp.domain.model.ProgressionPlanner
import com.example.mygymstatsapp.domain.model.RepRange
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.SupersetGrouping
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WarmupPlanner
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.fallbackSet
import com.example.mygymstatsapp.domain.model.matchPreviousSets
import com.example.mygymstatsapp.domain.model.progressionPlan
import com.example.mygymstatsapp.domain.model.workingSetNumbers
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.ConfirmDialog
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.components.rememberElapsedSeconds
import com.example.mygymstatsapp.ui.exercises.ProgressionSettings
import com.example.mygymstatsapp.utils.formatClock
import com.example.mygymstatsapp.utils.formatRest
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeight
import kotlinx.coroutines.delay

/** Position of an exercise inside a superset, if any. */
private data class SupersetInfo(val letter: Char, val chainsIntoNext: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    pickedExerciseIds: List<String>?,
    onPickedHandled: () -> Unit,
    /** [workout exercise id, new exercise id] coming back from the library. */
    replacement: List<String>?,
    onReplacementHandled: () -> Unit,
    onAddExercises: () -> Unit,
    onReplaceExercise: (workoutExerciseId: String) -> Unit,
    onClose: () -> Unit,
    onFinished: (String) -> Unit,
    viewModel: ActiveWorkoutViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimerState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    var celebratedRecord by remember { mutableStateOf<NewPersonalRecord?>(null) }
    var showTimerSheet by rememberSaveable { mutableStateOf(false) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    val currentOnClose by rememberUpdatedState(onClose)
    val currentOnFinished by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ActiveWorkoutEvent.RecordAchieved -> celebratedRecord = event.record
                is ActiveWorkoutEvent.Message -> snackbarHostState.showSnackbar(resources.getString(event.textRes))
                is ActiveWorkoutEvent.Finished -> currentOnFinished(event.workoutId)
                ActiveWorkoutEvent.Discarded -> currentOnClose()
            }
        }
    }
    LaunchedEffect(celebratedRecord) {
        if (celebratedRecord != null) {
            delay(3_500)
            celebratedRecord = null
        }
    }
    LaunchedEffect(replacement, state.workout?.id) {
        if (replacement != null && replacement.size == 2 && state.workout != null) {
            viewModel.replaceExercise(workoutExerciseId = replacement[0], exerciseId = replacement[1])
            onReplacementHandled()
        }
    }
    LaunchedEffect(pickedExerciseIds, state.workout?.id) {
        if (!pickedExerciseIds.isNullOrEmpty() && state.workout != null) {
            viewModel.addExercises(pickedExerciseIds)
            onPickedHandled()
        }
    }

    // Rest notifications need the runtime permission on Android 13+: asked once, at the first workout.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(state.isLoading, state.notificationPromptShown) {
        if (!state.isLoading && !state.notificationPromptShown) {
            viewModel.onNotificationPromptShown()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // The workout was finished or discarded elsewhere: nothing to show.
    if (!state.isLoading && state.workout == null && !state.isClosing) {
        LaunchedEffect(Unit) { currentOnClose() }
        return
    }
    val workout = state.workout ?: return
    val supersets = remember(workout.exercises) { supersetInfo(workout.exercises) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.cd_minimize))
                    }
                },
                title = {
                    Column(Modifier.clickable { showRenameDialog = true }) {
                        Text(workout.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        ElapsedText(workout.startedAt, state.heartRate)
                    }
                },
                actions = {
                    IconButton(onClick = { showTimerSheet = true }) {
                        Icon(Icons.Rounded.Timer, stringResource(R.string.cd_rest_timer))
                    }
                    Button(
                        shape = MaterialTheme.shapes.small,
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.padding(end = 8.dp),
                        enabled = !state.isClosing,
                    ) { Text(stringResource(R.string.action_finish), fontWeight = FontWeight.Bold) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = restTimer.isActive,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                RestTimerBar(
                    state = restTimer,
                    onAdjust = viewModel::adjustRestTimer,
                    onPause = viewModel::pauseRestTimer,
                    onResume = viewModel::resumeRestTimer,
                    onSkip = viewModel::skipRestTimer,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "summary") { WorkoutProgressHeader(workout, state.weightUnit) }

                itemsIndexed(workout.exercises, key = { _, item -> item.id }) { index, exercise ->
                    WorkoutExerciseCard(
                        workoutExercise = exercise,
                        index = index,
                        previous = state.previous[exercise.exercise.id].orEmpty(),
                        superset = supersets[exercise.id],
                        unit = state.weightUnit,
                        defaultRestSeconds = state.defaultRestSeconds,
                        weightIncrementKg = state.weightIncrementKg,
                        isLast = index == workout.exercises.lastIndex,
                        onAction = viewModel::onAction,
                        onReplace = { onReplaceExercise(exercise.id) },
                    )
                }

                item(key = "add") {
                    OutlinedButton(
                        onClick = onAddExercises,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_add_exercises), style = MaterialTheme.typography.titleSmall)
                    }
                }

                item(key = "notes") { WorkoutNotesField(workout.notes, viewModel::updateNotes) }

                item(key = "discard") {
                    TextButton(
                        onClick = { showDiscardDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(stringResource(R.string.action_discard_workout)) }
                }
            }

            AnimatedVisibility(
                visible = celebratedRecord != null,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
            ) {
                celebratedRecord?.let { RecordBanner(it, state.weightUnit) }
            }
        }
    }

    if (showTimerSheet) {
        RestTimerSheet(
            defaultRestSeconds = state.defaultRestSeconds,
            onStart = viewModel::startRestTimer,
            onDismiss = { showTimerSheet = false },
        )
    }
    if (showFinishDialog) {
        val pending = workout.totalSetCount - workout.completedSetCount
        ConfirmDialog(
            title = stringResource(R.string.dialog_finish_title),
            message = when {
                workout.completedSetCount == 0 -> stringResource(R.string.dialog_finish_no_sets)
                pending > 0 -> pluralStringResource(R.plurals.dialog_finish_pending, pending, pending)
                else -> stringResource(R.string.dialog_finish_ok)
            },
            confirmLabel = stringResource(R.string.action_finish),
            onConfirm = viewModel::finishWorkout,
            onDismiss = { showFinishDialog = false },
        )
    }
    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_discard_title),
            message = stringResource(R.string.dialog_discard_message),
            confirmLabel = stringResource(R.string.action_discard),
            destructive = true,
            onConfirm = viewModel::discardWorkout,
            onDismiss = { showDiscardDialog = false },
        )
    }
    if (showRenameDialog) {
        RenameDialog(
            initialName = workout.name,
            onConfirm = viewModel::rename,
            onDismiss = { showRenameDialog = false },
        )
    }
}

private fun supersetInfo(exercises: List<WorkoutExercise>): Map<String, SupersetInfo> {
    val letters = SupersetGrouping.letters(exercises.map { it.supersetGroup })
    return exercises.mapIndexedNotNull { index, exercise ->
        val group = exercise.supersetGroup ?: return@mapIndexedNotNull null
        val letter = letters[group] ?: return@mapIndexedNotNull null
        exercise.id to SupersetInfo(letter, chainsIntoNext = exercises.getOrNull(index + 1)?.supersetGroup == group)
    }.toMap()
}

@Composable
private fun ElapsedText(startedAt: Long, heartRate: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            formatClock(rememberElapsedSeconds(startedAt)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        // Measured by the watch while it follows the workout.
        if (heartRate != null) {
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = stringResource(R.string.cd_heart_rate),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
            Text(
                " $heartRate",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WorkoutProgressHeader(workout: Workout, unit: WeightUnit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        HeaderStat(stringResource(R.string.label_volume), formatVolume(workout.volumeKg, unit))
        HeaderStat(stringResource(R.string.label_sets), "${workout.completedSetCount}/${workout.totalSetCount}")
        HeaderStat(stringResource(R.string.label_exercises), "${workout.exercises.size}")
    }
}

@Composable
private fun HeaderStat(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutExerciseCard(
    workoutExercise: WorkoutExercise,
    index: Int,
    previous: List<WorkoutSet>,
    superset: SupersetInfo?,
    unit: WeightUnit,
    defaultRestSeconds: Int,
    weightIncrementKg: Double,
    isLast: Boolean,
    onAction: (WorkoutAction) -> Unit,
    onReplace: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var restDialog by remember { mutableStateOf(false) }
    var progressionSheet by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    val exercise = workoutExercise.exercise
    val target = repsRangeLabel(workoutExercise.targetRepsMin, workoutExercise.targetRepsMax)
    val rest = workoutExercise.restSeconds ?: defaultRestSeconds
    val previousBySet = remember(workoutExercise.sets, previous) { matchPreviousSets(workoutExercise.sets, previous) }
    val numbers = remember(workoutExercise.sets) { workingSetNumbers(workoutExercise.sets) }
    val supersetColor = MaterialTheme.colorScheme.secondary

    GymCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = 12.dp,
        border = superset?.let { BorderStroke(1.5.dp, supersetColor.copy(alpha = 0.6f)) },
    ) {
        if (superset != null) {
            Text(
                stringResource(
                    if (superset.chainsIntoNext) R.string.superset_chain else R.string.superset_rest_after,
                    superset.letter.toString(),
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = supersetColor,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MuscleGroupBadge(exercise.muscleGroup, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val restLabel = when {
                    superset?.chainsIntoNext == true -> stringResource(R.string.exercise_no_rest_superset)
                    rest > 0 -> stringResource(R.string.exercise_rest, formatRest(rest))
                    else -> stringResource(R.string.exercise_rest, stringResource(R.string.rest_none))
                }
                Text(
                    if (target != null) stringResource(R.string.exercise_target, target) + " · " + restLabel else restLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, stringResource(R.string.cd_exercise_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_rest_time)) },
                        leadingIcon = { Icon(Icons.Rounded.Timer, null) },
                        onClick = { menuOpen = false; restDialog = true },
                    )
                    if (exercise.trackingType != TrackingType.DURATION) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_progression)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, null) },
                            onClick = { menuOpen = false; progressionSheet = true },
                        )
                    }
                    if (exercise.trackingType == TrackingType.WEIGHT_REPS) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_add_warmups)) },
                            leadingIcon = { Icon(Icons.Rounded.LocalFireDepartment, null) },
                            onClick = {
                                menuOpen = false
                                val count = if (index == 0) WarmupPlanner.FIRST_EXERCISE_WARMUPS else WarmupPlanner.OTHER_EXERCISE_WARMUPS
                                onAction(WorkoutAction.AddWarmups(workoutExercise.id, count))
                            },
                        )
                    }
                    if (superset?.chainsIntoNext != true && !isLast) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_superset_link)) },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            onClick = { menuOpen = false; onAction(WorkoutAction.LinkSuperset(workoutExercise.id)) },
                        )
                    }
                    if (superset != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_superset_unlink)) },
                            leadingIcon = { Icon(Icons.Rounded.LinkOff, null) },
                            onClick = { menuOpen = false; onAction(WorkoutAction.UnlinkSuperset(workoutExercise.id)) },
                        )
                    }
                    if (index > 0) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_move_up)) },
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) },
                            onClick = { menuOpen = false; onAction(WorkoutAction.MoveExercise(workoutExercise.id, -1)) },
                        )
                    }
                    if (!isLast) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_move_down)) },
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) },
                            onClick = { menuOpen = false; onAction(WorkoutAction.MoveExercise(workoutExercise.id, 1)) },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_replace_exercise)) },
                        leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) },
                        onClick = { menuOpen = false; onReplace() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_remove_exercise), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; confirmRemove = true },
                    )
                }
            }
        }
        if (workoutExercise.notes.isNotBlank()) {
            Text(
                workoutExercise.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }
        ProgressionHint(workoutExercise, previous, unit, weightIncrementKg, onAction)
        Spacer(Modifier.height(10.dp))
        SetHeaderRow(exercise.trackingType, unit)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            workoutExercise.sets.forEach { set ->
                key(set.id) {
                    SetRow(
                        set = set,
                        displayNumber = numbers[set.id],
                        workoutExerciseId = workoutExercise.id,
                        previous = previousBySet[set.id],
                        trackingType = exercise.trackingType,
                        unit = unit,
                        repsPlaceholder = target ?: "0",
                        onAction = onAction,
                        fallback = fallbackSet(workoutExercise.sets, set, previousBySet[set.id]),
                    )
                }
            }
        }
        TextButton(
            onClick = { onAction(WorkoutAction.AddSet(workoutExercise.id)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.action_add_set))
        }
    }

    if (restDialog) {
        AlertDialog(
            onDismissRequest = { restDialog = false },
            title = { Text(stringResource(R.string.dialog_rest_title, exercise.name)) },
            text = {
                Column {
                    REST_OPTIONS.forEach { seconds ->
                        Text(
                            text = seconds?.let(::formatRest) ?: stringResource(R.string.rest_default, formatRest(defaultRestSeconds)),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (seconds == workoutExercise.restSeconds) FontWeight.Bold else FontWeight.Normal,
                            color = if (seconds == workoutExercise.restSeconds) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    restDialog = false
                                    onAction(WorkoutAction.ChangeRest(workoutExercise.id, seconds))
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { restDialog = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
    if (progressionSheet) {
        ModalBottomSheet(
            onDismissRequest = { progressionSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall)
                ProgressionSettings(
                    exercise = exercise,
                    defaultIncrementKg = weightIncrementKg,
                    unit = unit,
                    range = workoutExercise.targetRepsMax?.let { RepRange(workoutExercise.targetRepsMin, it) },
                    onChange = { enabled, incrementKg ->
                        onAction(WorkoutAction.UpdateProgression(exercise.id, enabled, incrementKg))
                    },
                    showMissingRange = true,
                )
            }
        }
    }
    if (confirmRemove) {
        ConfirmDialog(
            title = stringResource(R.string.dialog_remove_exercise_title, exercise.name),
            message = stringResource(R.string.dialog_remove_exercise_message),
            confirmLabel = stringResource(R.string.action_remove),
            destructive = true,
            onConfirm = { onAction(WorkoutAction.RemoveExercise(workoutExercise.id)) },
            onDismiss = { confirmRemove = false },
        )
    }
}

/**
 * What progressive overload did to this exercise ("+2.5 kg", "+1 rep per set"), with a way back
 * to last time's values, or a way to apply it when the sets don't carry it.
 */
@Composable
private fun ProgressionHint(
    workoutExercise: WorkoutExercise,
    previous: List<WorkoutSet>,
    unit: WeightUnit,
    weightIncrementKg: Double,
    onAction: (WorkoutAction) -> Unit,
) {
    val exercise = workoutExercise.exercise
    val plan = remember(previous, exercise, workoutExercise.targetRepsMin, workoutExercise.targetRepsMax, weightIncrementKg) {
        exercise.progressionPlan(previous, workoutExercise.targetRepsMin, workoutExercise.targetRepsMax, weightIncrementKg)
    } ?: return
    if (workoutExercise.sets.none { !it.completed && it.setType != SetType.WARMUP }) return
    val applied = plan.isAppliedTo(workoutExercise.sets)
    val first = plan.targets.first()
    val message = when (plan.step) {
        ProgressionPlanner.Step.ADD_WEIGHT -> if (applied) {
            stringResource(R.string.progression_weight_applied, formatWeight(plan.incrementKg, unit), plan.repsMax)
        } else {
            stringResource(R.string.progression_weight_available, formatWeight(first.weightKg ?: 0.0, unit), first.reps ?: 0)
        }
        ProgressionPlanner.Step.ADD_REPS -> if (applied) {
            stringResource(R.string.progression_reps_applied, plan.targets.joinToString(" · ") { "${it.reps}" })
        } else {
            stringResource(R.string.progression_reps_available)
        }
    }

    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        TextButton(
            onClick = {
                onAction(
                    if (applied) {
                        WorkoutAction.RevertProgression(workoutExercise.id)
                    } else {
                        WorkoutAction.ApplyProgression(workoutExercise.id)
                    }
                )
            },
        ) {
            Text(stringResource(if (applied) R.string.action_undo else R.string.action_apply), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WorkoutNotesField(notes: String, onNotesChange: (String) -> Unit) {
    var text by remember { mutableStateOf(notes) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            onNotesChange(it)
        },
        label = { Text(stringResource(R.string.workout_notes_label)) },
        placeholder = { Text(stringResource(R.string.workout_notes_hint)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun RecordBanner(record: NewPersonalRecord, unit: WeightUnit) {
    GymCard(containerColor = MaterialTheme.colorScheme.tertiary, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiary, modifier = Modifier.size(18.dp))
            Text(
                stringResource(R.string.record_new),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiary,
            )
        }
        Text(record.exerciseName, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onTertiary)
        Text(recordDescription(record, unit), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiary)
    }
}

@Composable
private fun RenameDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Edit, null) },
        title = { Text(stringResource(R.string.dialog_rename_title)) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name); onDismiss() }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
