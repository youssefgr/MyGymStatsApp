package com.example.mygymstatsapp.wear.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonColors
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import androidx.wear.compose.material3.touchTargetAwareSize
import com.example.mygymstatsapp.shared.LiveExercise
import com.example.mygymstatsapp.shared.LiveRest
import com.example.mygymstatsapp.shared.LiveSet
import com.example.mygymstatsapp.shared.LiveWorkout
import com.example.mygymstatsapp.shared.WearSetType
import com.example.mygymstatsapp.shared.WearTracking
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.wear.R
import com.example.mygymstatsapp.wear.ui.theme.color
import com.example.mygymstatsapp.wear.workout.HeartRatePrompt
import com.example.mygymstatsapp.wear.workout.WorkoutService
import com.example.mygymstatsapp.wear.workout.hasHeartRatePermission
import com.example.mygymstatsapp.wear.workout.heartRatePermission
import kotlinx.coroutines.delay

/**
 * The workout in progress: one page per exercise (the current set, its values, ✓), then a last
 * page to finish. While resting, the rest timer covers the pages; "Série suivante" shows the
 * next set during the rest (to adjust the load) and the countdown stays at the top.
 */
@Composable
fun WorkoutScreen(
    state: WatchUiState,
    workout: LiveWorkout,
    viewModel: WatchViewModel,
    onEdit: (LiveSet, EditField) -> Unit,
) {
    val phone = state.phone ?: return
    val exercises = workout.exercises
    val currentIndex = exercises.indexOfFirst { e -> e.sets.any { !it.completed } }.let { if (it < 0) exercises.size else it }
    val pagerState = rememberPagerState(initialPage = currentIndex) { exercises.size + 1 }

    // Follow the workout: after the last set of an exercise, show the next one.
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) pagerState.animateScrollToPage(currentIndex)
    }

    // Heart rate: asked once, the first time a workout shows on the watch (as the phone does for
    // notifications); afterwards from the "Suivre le cœur" button on the last page.
    val context = LocalContext.current
    var heartRateGranted by remember { mutableStateOf(hasHeartRatePermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        heartRateGranted = hasHeartRatePermission(context)
        if (heartRateGranted) WorkoutService.start(context)
    }
    LaunchedEffect(Unit) {
        if (!heartRateGranted && !HeartRatePrompt.wasShown(context)) {
            HeartRatePrompt.markShown(context)
            permissionLauncher.launch(requestedPermissions())
        }
    }

    val rest = phone.rest
    var restHiddenFor by rememberSaveable { mutableStateOf<Long?>(null) }
    val restVisible = rest != null && restHiddenFor != rest.endsAt + rest.totalMs

    Box(Modifier.fillMaxSize()) {
        HorizontalPagerScaffold(pagerState = pagerState) {
            HorizontalPager(state = pagerState) { page ->
                run {
                    if (page < exercises.size) {
                        ExercisePage(
                            exercise = exercises[page],
                            unit = phone.unit,
                            heartRate = state.heartRate,
                            rest = if (restVisible) null else rest,
                            onShowRest = { restHiddenFor = null },
                            onValidate = { set -> viewModel.toggle(exercises[page], set) },
                            onAddSet = { viewModel.addSet(exercises[page]) },
                            onEdit = onEdit,
                        )
                    } else {
                        FinishPage(
                            workout = workout,
                            heartRate = state.heartRate,
                            canFollowHeartRate = !heartRateGranted,
                            onFollowHeartRate = { permissionLauncher.launch(requestedPermissions()) },
                            onFinish = { viewModel.finish(workout.id) },
                        )
                    }
                }
            }
        }
        if (restVisible) {
            RestPanel(
                rest = rest,
                onAdjust = viewModel::adjustRest,
                onPause = viewModel::pauseRest,
                onResume = viewModel::resumeRest,
                onSkip = viewModel::skipRest,
                onHide = { restHiddenFor = rest.endsAt + rest.totalMs },
            )
        }
    }
}

@Composable
private fun ExercisePage(
    exercise: LiveExercise,
    unit: WearUnit,
    heartRate: Int?,
    rest: LiveRest?,
    onShowRest: () -> Unit,
    onValidate: (LiveSet) -> Unit,
    onAddSet: () -> Unit,
    onEdit: (LiveSet, EditField) -> Unit,
) {
    val set = exercise.sets.firstOrNull { !it.completed }
    val workingCount = exercise.sets.count { it.type != WearSetType.WARMUP }
    val muscleColor = exercise.muscle.color
    val heartColor = MaterialTheme.colorScheme.error

    ScreenScaffold(
        timeText = {
            // The heart rate rides along the clock (the pager dots already give the exercise position).
            TimeText { time ->
                timeTextCurvedText(time)
                if (heartRate != null) {
                    timeTextSeparator()
                    timeTextCurvedText("♥ $heartRate", CurvedTextStyle(color = heartColor))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Bottom room for the pager dots under the ✓.
                .padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            if (rest != null) RestChip(rest, onClick = onShowRest)
            OneLineTitle(exercise.name)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (set != null) setLabel(set, workingCount) else stringResource(R.string.workout_exercise_done),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        set == null -> MaterialTheme.colorScheme.primary
                        else -> set.type.color.takeOrElse(MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                )
                SetDots(exercise.sets)
                exercise.superset?.let { SupersetTag(it, muscleColor) }
            }
            if (set == null) {
                RoundAction(
                    onClick = onAddSet,
                    icon = Icons.Rounded.Add,
                    description = stringResource(R.string.workout_add_set),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                )
            } else {
                SetValues(exercise, set, unit, onEdit)
                val showsOwnValues = set.weightKg != null || set.reps != null || set.durationSeconds != null
                if (showsOwnValues) previousText(exercise, set, unit)?.let {
                    Text(it, style = MaterialTheme.typography.bodyExtraSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                RoundAction(
                    onClick = { onValidate(set) },
                    icon = Icons.Rounded.Check,
                    description = stringResource(R.string.workout_validate),
                )
            }
        }
    }
}

/** One line, shrinking from 16 to 12sp for long names before ellipsizing. */
@Composable
private fun OneLineTitle(text: String) {
    BasicText(
        text,
        style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 16.sp, stepSize = 1.sp),
    )
}

/** "🔗 A": this exercise is part of superset A (the phone shows « SUPERSET A »). */
@Composable
private fun SupersetTag(letter: String, color: Color) {
    val description = stringResource(R.string.workout_superset, letter)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(6.dp))
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 4.dp),
    ) {
        Icon(Icons.Rounded.Link, contentDescription = null, tint = color, modifier = Modifier.size(10.dp))
        Text(letter, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/**
 * The main action of a watch page: a round button the size of a fingertip (48dp), in the flow
 * under what it acts on, so it never covers the values.
 */
@Composable
internal fun RoundAction(
    onClick: () -> Unit,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.touchTargetAwareSize(IconButtonDefaults.SmallButtonSize),
        colors = colors,
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(IconButtonDefaults.SmallIconSize))
    }
}

/** The values of the current set, each one tappable to edit. Empty fields show last time's value, dimmed. */
@Composable
private fun SetValues(exercise: LiveExercise, set: LiveSet, unit: WearUnit, onEdit: (LiveSet, EditField) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        when (exercise.tracking) {
            WearTracking.WEIGHT_REPS -> {
                ValueBox(
                    value = (set.weightKg ?: set.previousWeightKg)?.let { formatDecimal(unit.fromKg(it)) },
                    suffix = unit.label,
                    isFallback = set.weightKg == null,
                    description = stringResource(R.string.cd_edit_weight),
                    onClick = { onEdit(set, EditField.WEIGHT) },
                )
                Text("×", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ValueBox(
                    value = (set.reps ?: set.previousReps)?.toString(),
                    suffix = null,
                    isFallback = set.reps == null,
                    description = stringResource(R.string.cd_edit_reps),
                    onClick = { onEdit(set, EditField.REPS) },
                )
            }
            WearTracking.REPS_ONLY -> ValueBox(
                value = (set.reps ?: set.previousReps)?.toString(),
                suffix = stringResource(R.string.unit_reps),
                isFallback = set.reps == null,
                description = stringResource(R.string.cd_edit_reps),
                onClick = { onEdit(set, EditField.REPS) },
            )
            WearTracking.DURATION -> ValueBox(
                value = (set.durationSeconds ?: set.previousDurationSeconds)?.let { formatRest(it) },
                suffix = null,
                isFallback = set.durationSeconds == null,
                description = stringResource(R.string.cd_edit_duration),
                onClick = { onEdit(set, EditField.DURATION) },
            )
        }
    }
}

@Composable
private fun ValueBox(value: String?, suffix: String?, isFallback: Boolean, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .widthIn(min = 48.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = description }
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            value ?: "–",
            style = MaterialTheme.typography.numeralSmall,
            color = if (isFallback) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.alignByBaseline(),
        )
        if (suffix != null) {
            Text(
                " $suffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}

/** One dot per set: filled when done, ringed for the current one. */
@Composable
private fun SetDots(sets: List<LiveSet>) {
    val current = sets.firstOrNull { !it.completed }?.id
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 2.dp)) {
        sets.forEach { set ->
            val tint = set.type.color.takeOrElse(MaterialTheme.colorScheme.primary)
            Box(
                Modifier
                    .size(6.dp)
                    .then(
                        when {
                            set.completed -> Modifier.background(tint, CircleShape)
                            set.id == current -> Modifier.border(1.5.dp, tint, CircleShape)
                            else -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        }
                    ),
            )
        }
    }
}

@Composable
private fun setLabel(set: LiveSet, workingCount: Int): String = when (set.type) {
    WearSetType.WARMUP -> stringResource(R.string.workout_warmup)
    WearSetType.DROP -> stringResource(R.string.workout_drop)
    WearSetType.FAILURE -> stringResource(R.string.workout_failure)
    WearSetType.NORMAL -> stringResource(R.string.workout_set, set.displayNumber ?: set.setNumber) + "/" + workingCount
}

@Composable
private fun previousText(exercise: LiveExercise, set: LiveSet, unit: WearUnit): String? {
    if (set.previousWeightKg == null && set.previousReps == null && set.previousDurationSeconds == null) return null
    return stringResource(
        R.string.workout_previous,
        setText(set.previousWeightKg, set.previousReps, set.previousDurationSeconds, exercise.tracking, unit),
    )
}

@Composable
private fun HeartRateText(bpm: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = stringResource(R.string.cd_heart_rate),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(12.dp),
        )
        Text(" $bpm", style = MaterialTheme.typography.labelSmall)
    }
}

// ---- Rest ---------------------------------------------------------------------------------------

/** Remaining rest, refreshed while visible (the end time comes from the phone, in wall-clock time). */
@Composable
private fun rememberRemainingMs(rest: LiveRest): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(rest) {
        while (!rest.isPaused) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }
    return rest.remainingMs(now)
}

@Composable
private fun RestChip(rest: LiveRest, onClick: () -> Unit) {
    val remaining = rememberRemainingMs(rest)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
    ) {
        Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
        Text(
            " " + formatClock((remaining + 999) / 1000),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * Recovery is steel, never orange (docs/BRAND.md): the ring, the countdown and every control.
 * A round screen narrows at the bottom, so the last row holds a single button.
 */
@Composable
private fun RestPanel(
    rest: LiveRest,
    onAdjust: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onHide: () -> Unit,
) {
    val remaining = rememberRemainingMs(rest)
    val progress = if (rest.totalMs <= 0) 0f else (remaining.toFloat() / rest.totalMs).coerceIn(0f, 1f)
    val steel = MaterialTheme.colorScheme.secondary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tonalSteel = IconButtonDefaults.filledTonalIconButtonColors(contentColor = steel)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            colors = ProgressIndicatorDefaults.colors(
                indicatorColor = steel,
                trackColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            if (remaining <= 0) {
                Text(stringResource(R.string.rest_over), style = MaterialTheme.typography.titleMedium, color = steel)
            } else {
                // The countdown is the pause button: the small icon says what a tap does.
                val toggleDescription = stringResource(if (rest.isPaused) R.string.cd_rest_resume else R.string.cd_rest_pause)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = if (rest.isPaused) onResume else onPause)
                        .semantics(mergeDescendants = true) { contentDescription = toggleDescription }
                        .padding(horizontal = 6.dp),
                ) {
                    Icon(
                        if (rest.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = muted,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        formatClock((remaining + 999) / 1000),
                        style = MaterialTheme.typography.numeralLarge,
                        // Dimmed while paused.
                        color = if (rest.isPaused) muted else steel,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val minusDescription = stringResource(R.string.cd_rest_minus)
                FilledTonalIconButton(
                    onClick = { onAdjust(-15) },
                    modifier = Modifier
                        .touchTargetAwareSize(IconButtonDefaults.SmallButtonSize)
                        .semantics { contentDescription = minusDescription },
                    colors = tonalSteel,
                ) { Text("-15", style = MaterialTheme.typography.labelMedium) }
                RoundAction(
                    onClick = onSkip,
                    icon = Icons.Rounded.SkipNext,
                    description = stringResource(R.string.rest_skip),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = steel, contentColor = MaterialTheme.colorScheme.onSecondary),
                )
                val plusDescription = stringResource(R.string.cd_rest_plus)
                FilledTonalIconButton(
                    onClick = { onAdjust(15) },
                    modifier = Modifier
                        .touchTargetAwareSize(IconButtonDefaults.SmallButtonSize)
                        .semantics { contentDescription = plusDescription },
                    colors = tonalSteel,
                ) { Text("+15", style = MaterialTheme.typography.labelMedium) }
            }
            // Shows the next set during the rest (to change the load); the countdown stays at the top.
            RoundAction(
                onClick = onHide,
                icon = Icons.Rounded.FitnessCenter,
                description = stringResource(R.string.rest_next_set),
                colors = tonalSteel,
            )
        }
    }
}

// ---- Finish -------------------------------------------------------------------------------------

@Composable
private fun FinishPage(
    workout: LiveWorkout,
    heartRate: Int?,
    canFollowHeartRate: Boolean,
    onFollowHeartRate: () -> Unit,
    onFinish: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    val elapsed = rememberElapsedSeconds(workout.startedAt)
    val setsText = stringResource(R.string.workout_sets_done, workout.completedSets, workout.totalSets)

    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 18.dp, top = 30.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            OneLineTitle(workout.name)
            Text(formatClock(elapsed), style = MaterialTheme.typography.numeralSmall, color = MaterialTheme.colorScheme.primary)
            if (canFollowHeartRate && heartRate == null) {
                CompactButton(
                    onClick = onFollowHeartRate,
                    icon = { Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) {
                    Text(stringResource(R.string.workout_heart_rate), style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    setsText + (heartRate?.let { " · ♥ $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            CompactButton(onClick = { confirm = true }) {
                Text(stringResource(R.string.workout_finish), style = MaterialTheme.typography.labelMedium)
            }
        }
    }

    AlertDialog(
        visible = confirm,
        onDismissRequest = { confirm = false },
        title = { Text(stringResource(R.string.workout_finish_title)) },
        text = { Text(setsText) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(onClick = {
                confirm = false
                onFinish()
            })
        },
    )
}

private fun requestedPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(heartRatePermission(), Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(heartRatePermission())
    }

@Composable
private fun rememberElapsedSeconds(startedAt: Long): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000 - now % 1_000)
        }
    }
    return ((now - startedAt) / 1_000).coerceAtLeast(0)
}

private fun Color.takeOrElse(fallback: Color): Color = if (this == Color.Unspecified) fallback else this
