package com.example.mygymstatsapp.ui.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.SetType
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSet
import com.example.mygymstatsapp.ui.badge
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.theme.color
import com.example.mygymstatsapp.utils.formatDecimal
import com.example.mygymstatsapp.utils.fromKg

/** Column widths shared by the header and the rows. */
object SetColumns {
    val number = 36.dp
    val value = 68.dp
    val check = 44.dp
}

@Composable
fun SetHeaderRow(trackingType: TrackingType, unit: WeightUnit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeaderLabel(stringResource(R.string.set_header_set), Modifier.width(SetColumns.number))
        HeaderLabel(stringResource(R.string.set_header_previous), Modifier.weight(1f), TextAlign.Start)
        when (trackingType) {
            TrackingType.WEIGHT_REPS -> {
                HeaderLabel(unit.label.uppercase(), Modifier.width(SetColumns.value))
                HeaderLabel(stringResource(R.string.set_header_reps), Modifier.width(SetColumns.value))
            }
            TrackingType.REPS_ONLY -> HeaderLabel(stringResource(R.string.set_header_reps), Modifier.width(SetColumns.value))
            TrackingType.DURATION -> HeaderLabel(stringResource(R.string.set_header_min), Modifier.width(SetColumns.value))
        }
        Box(Modifier.width(SetColumns.check))
    }
}

@Composable
private fun HeaderLabel(text: String, modifier: Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
    )
}

@Composable
fun SetRow(
    set: WorkoutSet,
    /** Working set number (warm-ups are not counted), null for warm-ups. */
    displayNumber: Int?,
    workoutExerciseId: String,
    previous: WorkoutSet?,
    trackingType: TrackingType,
    unit: WeightUnit,
    repsPlaceholder: String,
    onAction: (WorkoutAction) -> Unit,
    /** What empty fields stand for (dimmed): last time, else the set before (see fallbackSet). */
    fallback: WorkoutSet? = previous,
) {
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val background by animateColorAsState(
        if (set.completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
        label = "setBackground",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SetBadge(set, displayNumber, onAction)

        Text(
            text = previous?.let { previousLabel(it, trackingType, unit) } ?: "—",
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = previous != null && !set.completed) {
                    focusManager.clearFocus()
                    previous?.let { onAction(WorkoutAction.CopyPrevious(set.id, it)) }
                }
                .padding(vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )

        when (trackingType) {
            TrackingType.WEIGHT_REPS -> {
                SetValueField(
                    value = set.weightKg?.let { unit.fromKg(it) },
                    placeholder = fallback?.weightKg?.let { formatDecimal(unit.fromKg(it)) } ?: "0",
                    keyboardType = KeyboardType.Decimal,
                    completed = set.completed,
                    onValueChange = { onAction(WorkoutAction.WeightChanged(set.id, it)) },
                )
                SetValueField(
                    value = set.reps?.toDouble(),
                    placeholder = fallback?.reps?.toString() ?: repsPlaceholder,
                    keyboardType = KeyboardType.Number,
                    completed = set.completed,
                    onValueChange = { onAction(WorkoutAction.RepsChanged(set.id, it)) },
                )
            }
            TrackingType.REPS_ONLY -> SetValueField(
                value = set.reps?.toDouble(),
                placeholder = fallback?.reps?.toString() ?: repsPlaceholder,
                keyboardType = KeyboardType.Number,
                completed = set.completed,
                onValueChange = { onAction(WorkoutAction.RepsChanged(set.id, it)) },
            )
            TrackingType.DURATION -> SetValueField(
                value = set.durationSeconds?.let { it / 60.0 },
                placeholder = fallback?.durationSeconds?.let { formatDecimal(it / 60.0) } ?: "0",
                keyboardType = KeyboardType.Decimal,
                completed = set.completed,
                onValueChange = { onAction(WorkoutAction.DurationChanged(set.id, it)) },
            )
        }

        Box(
            modifier = Modifier
                .size(SetColumns.check)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                )
                .clickable {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    focusManager.clearFocus()
                    onAction(WorkoutAction.ToggleCompleted(workoutExerciseId, set.id))
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = stringResource(if (set.completed) R.string.cd_set_done else R.string.cd_set_validate),
                tint = if (set.completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun previousLabel(previous: WorkoutSet, trackingType: TrackingType, unit: WeightUnit): String =
    when (trackingType) {
        TrackingType.WEIGHT_REPS -> "${formatDecimal(unit.fromKg(previous.weightKg ?: 0.0))} × ${previous.reps ?: 0}"
        TrackingType.REPS_ONLY -> stringResource(R.string.value_reps, previous.reps ?: 0)
        TrackingType.DURATION -> stringResource(R.string.value_minutes, formatDecimal((previous.durationSeconds ?: 0) / 60.0))
    }

/** Set number or set type letter; opens the set menu (type, RPE, delete). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetBadge(set: WorkoutSet, displayNumber: Int?, onAction: (WorkoutAction) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    var rpeDialog by remember { mutableStateOf(false) }
    val typeColor = set.setType.color
    Box {
        Box(
            modifier = Modifier
                .size(SetColumns.number)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable { menuOpen = true },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = set.setType.badge.ifEmpty { (displayNumber ?: set.setNumber).toString() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (typeColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else typeColor,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SetType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelRes)) },
                    leadingIcon = {
                        Text(
                            type.badge.ifEmpty { "${displayNumber ?: set.setNumber}" },
                            fontWeight = FontWeight.Bold,
                            color = if (type.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else type.color,
                            modifier = Modifier.width(20.dp),
                            textAlign = TextAlign.Center,
                        )
                    },
                    trailingIcon = if (type == set.setType) {
                        { Icon(Icons.Rounded.Check, null) }
                    } else {
                        null
                    },
                    onClick = {
                        menuOpen = false
                        onAction(WorkoutAction.ChangeSetType(set.id, type))
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        set.rpe?.let { stringResource(R.string.set_rpe, formatDecimal(it.toDouble(), 1)) }
                            ?: stringResource(R.string.set_add_rpe)
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Speed, null) },
                onClick = { menuOpen = false; rpeDialog = true },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.set_delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuOpen = false
                    onAction(WorkoutAction.DeleteSet(set.id))
                },
            )
        }
    }
    if (rpeDialog) {
        AlertDialog(
            onDismissRequest = { rpeDialog = false },
            title = { Text(stringResource(R.string.dialog_rpe_title)) },
            text = {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RPE_VALUES.forEach { value ->
                        FilterChip(
                            selected = set.rpe == value,
                            onClick = {
                                rpeDialog = false
                                onAction(WorkoutAction.ChangeRpe(set.id, value))
                            },
                            label = { Text(formatDecimal(value.toDouble(), 1)) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    rpeDialog = false
                    onAction(WorkoutAction.ChangeRpe(set.id, null))
                }) { Text(stringResource(R.string.rpe_none)) }
            },
        )
    }
}

private val RPE_VALUES = listOf(6f, 6.5f, 7f, 7.5f, 8f, 8.5f, 9f, 9.5f, 10f)

/**
 * Numeric field that keeps the user's text locally while focused (so "62," is not reformatted
 * while typing) and shows the stored value, formatted, the rest of the time.
 */
@Composable
private fun SetValueField(
    value: Double?,
    placeholder: String,
    keyboardType: KeyboardType,
    completed: Boolean,
    onValueChange: (String) -> Unit,
) {
    var text by remember { mutableStateOf(value?.let(::formatDecimal).orEmpty()) }
    var isFocused by remember { mutableStateOf(false) }
    // While focused, the typed text is the source of truth: database echoes of earlier
    // keystrokes arrive late and must not overwrite what is being typed.
    LaunchedEffect(value, isFocused) {
        if (!isFocused) text = value?.let(::formatDecimal).orEmpty()
    }
    val textColor = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    BasicTextField(
        value = text,
        onValueChange = { input ->
            val allowed = if (keyboardType == KeyboardType.Number) input.all(Char::isDigit) else input.all { it.isDigit() || it == '.' || it == ',' }
            if (input.length <= 6 && allowed) {
                text = input
                onValueChange(input)
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(
            color = textColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = Modifier
            .width(SetColumns.value)
            .height(44.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                if (text.isEmpty()) {
                    Text(
                        placeholder,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                    )
                }
                inner()
            }
        },
    )
}
