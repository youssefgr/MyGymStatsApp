package com.example.mygymstatsapp.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.Workout
import com.example.mygymstatsapp.domain.model.WorkoutExercise
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.domain.model.workingSetNumbers
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.badge
import com.example.mygymstatsapp.ui.components.ConfirmDialog
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.InlineStat
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.theme.color
import com.example.mygymstatsapp.ui.workout.RecordsCard
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherEffects
import com.example.mygymstatsapp.utils.formatDayMonthYear
import com.example.mygymstatsapp.utils.formatDecimal
import com.example.mygymstatsapp.utils.formatDuration
import com.example.mygymstatsapp.utils.formatFullDateTime
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeight
import com.example.mygymstatsapp.utils.formatWeightValue
import com.example.mygymstatsapp.utils.fromKg
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    viewModel: WorkoutDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    var showNotes by rememberSaveable { mutableStateOf(false) }
    var showTiming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editedSet by remember { mutableStateOf<Pair<WorkoutSet, TrackingType>?>(null) }

    WorkoutLauncherEffects(viewModel, onOpenActiveWorkout)
    LaunchedEffect(state.isDeleted, state.isLoading, state.workout) {
        if (state.isDeleted || (!state.isLoading && state.workout == null)) onBack()
    }
    val workout = state.workout

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(workout?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_options)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_rename)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = { menuOpen = false; showRename = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_timing)) },
                                leadingIcon = { Icon(Icons.Rounded.CalendarMonth, null) },
                                onClick = { menuOpen = false; showTiming = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.detail_repeat)) },
                                leadingIcon = { Icon(Icons.Rounded.Repeat, null) },
                                onClick = { menuOpen = false; viewModel.repeatWorkout() },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (workout == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") { DetailHeader(workout, state.weightUnit, onEditTiming = { showTiming = true }) }
            if (state.records.isNotEmpty()) {
                item(key = "records") { RecordsCard(state.records, state.weightUnit) }
            }
            item(key = "notes") {
                GymCard(onClick = { showNotes = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.detail_notes), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        workout.notes.ifBlank { stringResource(R.string.detail_add_note) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (workout.notes.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            items(workout.exercises, key = { it.id }) { exercise ->
                ExerciseDetailCard(
                    exercise = exercise,
                    unit = state.weightUnit,
                    onSetClick = { set -> editedSet = set to exercise.exercise.trackingType },
                )
            }
            item(key = "repeat") {
                Button(
                    onClick = viewModel::repeatWorkout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Repeat, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.detail_repeat).uppercase(), style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        if (showRename) {
            TextEditDialog(
                title = stringResource(R.string.dialog_rename_title),
                initial = workout.name,
                singleLine = true,
                onConfirm = viewModel::rename,
                onDismiss = { showRename = false },
            )
        }
        if (showNotes) {
            TextEditDialog(
                title = stringResource(R.string.detail_notes),
                initial = workout.notes,
                singleLine = false,
                onConfirm = viewModel::updateNotes,
                onDismiss = { showNotes = false },
            )
        }
        if (showTiming) {
            WorkoutTimingDialog(
                startedAt = workout.startedAt,
                durationSeconds = workout.durationSeconds,
                onSave = viewModel::updateTiming,
                onDismiss = { showTiming = false },
            )
        }
    }

    editedSet?.let { (set, trackingType) ->
        EditSetDialog(
            set = set,
            trackingType = trackingType,
            unit = state.weightUnit,
            onSave = { weight, reps, duration, type -> viewModel.editSet(set.id, weight, reps, duration, type) },
            onDelete = { viewModel.deleteSet(set.id) },
            onDismiss = { editedSet = null },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title),
            message = stringResource(R.string.detail_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = viewModel::deleteWorkout,
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun DetailHeader(workout: Workout, unit: WeightUnit, onEditTiming: () -> Unit) {
    GymCard(onClick = onEditTiming, modifier = Modifier.fillMaxWidth()) {
        Text(formatFullDateTime(workout.startedAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            InlineStat(stringResource(R.string.label_duration), formatDuration(workout.durationSeconds))
            InlineStat(stringResource(R.string.label_volume), formatVolume(workout.volumeKg, unit))
            InlineStat(stringResource(R.string.label_sets), "${workout.completedSetCount}")
            InlineStat(stringResource(R.string.label_exercises), "${workout.exercises.size}")
        }
        val avgHeartRate = workout.avgHeartRate
        val maxHeartRate = workout.maxHeartRate
        if (avgHeartRate != null && maxHeartRate != null) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                InlineStat(stringResource(R.string.label_heart_rate_avg), stringResource(R.string.value_bpm, avgHeartRate))
                InlineStat(stringResource(R.string.label_heart_rate_max), stringResource(R.string.value_bpm, maxHeartRate))
            }
        }
    }
}

@Composable
private fun ExerciseDetailCard(exercise: WorkoutExercise, unit: WeightUnit, onSetClick: (WorkoutSet) -> Unit) {
    val numbers = remember(exercise.sets) { workingSetNumbers(exercise.sets) }
    GymCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MuscleGroupBadge(exercise.exercise.muscleGroup, size = 36.dp)
            Text(exercise.exercise.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        exercise.sets.forEach { set ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSetClick(set) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val badgeColor = set.setType.color
                Text(
                    set.setType.badge.ifEmpty { "${numbers[set.id] ?: set.setNumber}" },
                    modifier = Modifier.width(28.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (badgeColor == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else badgeColor,
                )
                Text(setLabel(set, exercise.exercise.trackingType, unit), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (exercise.exercise.trackingType == TrackingType.WEIGHT_REPS && set.estimatedOneRepMaxKg > 0) {
                    Text(
                        stringResource(R.string.detail_one_rm, formatWeight(set.estimatedOneRepMaxKg, unit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (exercise.volumeKg > 0) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.detail_exercise_volume, formatVolume(exercise.volumeKg, unit)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** "60 kg × 10", "12 reps", "5 min", with " @8" when an RPE is set. */
@Composable
fun setLabel(set: WorkoutSet, trackingType: TrackingType, unit: WeightUnit): String = when (trackingType) {
    TrackingType.WEIGHT_REPS -> "${formatWeight(set.weightKg ?: 0.0, unit)} × ${set.reps ?: 0}"
    TrackingType.REPS_ONLY -> stringResource(R.string.value_reps, set.reps ?: 0)
    TrackingType.DURATION -> stringResource(R.string.value_minutes, formatDecimal((set.durationSeconds ?: 0) / 60.0, 1))
} + (set.rpe?.let { " @${formatDecimal(it.toDouble(), 1)}" } ?: "")

/** "60×10 · 60×10 · 65×8" for compact recaps (completed working sets). */
@Composable
fun setsSummary(exercise: WorkoutExercise, unit: WeightUnit): String {
    val sets = exercise.sets.filter { it.completed && it.setType != SetType.WARMUP }
    return when (exercise.exercise.trackingType) {
        TrackingType.WEIGHT_REPS -> sets.joinToString(" · ") { "${formatWeightValue(it.weightKg ?: 0.0, unit)}×${it.reps ?: 0}" }
        TrackingType.REPS_ONLY -> sets.map { stringResource(R.string.value_reps, it.reps ?: 0) }.joinToString(" · ")
        TrackingType.DURATION -> sets.map {
            stringResource(R.string.value_minutes, formatDecimal((it.durationSeconds ?: 0) / 60.0, 1))
        }.joinToString(" · ")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditSetDialog(
    set: WorkoutSet,
    trackingType: TrackingType,
    unit: WeightUnit,
    onSave: (weight: String, reps: String, duration: String, type: SetType) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var weight by remember { mutableStateOf(set.weightKg?.let { formatDecimal(unit.fromKg(it)) }.orEmpty()) }
    var reps by remember { mutableStateOf(set.reps?.toString().orEmpty()) }
    var duration by remember { mutableStateOf(set.durationSeconds?.let { formatDecimal(it / 60.0) }.orEmpty()) }
    var type by remember { mutableStateOf(set.setType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_edit_set_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (trackingType == TrackingType.WEIGHT_REPS) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text(stringResource(R.string.detail_weight_label, unit.label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                if (trackingType != TrackingType.DURATION) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { if (it.all(Char::isDigit)) reps = it },
                        label = { Text(stringResource(R.string.label_reps_range)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                } else {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text(stringResource(R.string.timing_duration_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SetType.entries.forEach { option ->
                        FilterChip(selected = option == type, onClick = { type = option }, label = { Text(stringResource(option.labelRes)) })
                    }
                }
                TextButton(onClick = { onDelete(); onDismiss() }) {
                    Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.set_delete), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(weight, reps, duration, type); onDismiss() }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/** Date, start time and duration of a past workout. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutTimingDialog(
    startedAt: Long,
    durationSeconds: Long,
    onSave: (startedAt: Long, durationSeconds: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val initial = remember(startedAt) { Instant.ofEpochMilli(startedAt).atZone(zone) }
    var date by remember { mutableStateOf(initial.toLocalDate()) }
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }
    var minutesText by remember { mutableStateOf((durationSeconds / 60).toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val durationMinutes = minutesText.toLongOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.detail_timing)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formatDayMonthYear(date))
                }
                OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Schedule, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("%02d:%02d".format(hour, minute))
                }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) minutesText = it },
                    label = { Text(stringResource(R.string.timing_duration_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = durationMinutes != null,
                onClick = {
                    val start = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
                    onSave(start, (durationMinutes ?: 0) * 60)
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )

    if (showDatePicker) {
        // DatePicker works with UTC midnight timestamps.
        val today = LocalDate.now()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.timing_start_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    hour = timeState.hour
                    minute = timeState.minute
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TextEditDialog(
    title: String,
    initial: String,
    singleLine: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(text); onDismiss() }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
