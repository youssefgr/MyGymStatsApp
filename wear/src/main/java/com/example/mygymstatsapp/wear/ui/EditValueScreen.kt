package com.example.mygymstatsapp.wear.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.touchTargetAwareSize
import com.example.mygymstatsapp.shared.LiveSet
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.wear.R
import kotlin.math.roundToInt

enum class EditField { WEIGHT, REPS, DURATION }

/**
 * One value of a set, changed with the crown or the − / + buttons. Weight moves by the profile's
 * load step (5 lb in pounds), reps by 1, durations by 15 s.
 */
@Composable
fun EditValueScreen(
    set: LiveSet,
    field: EditField,
    unit: WearUnit,
    weightStepKg: Double,
    onConfirm: (weightKg: Double?, reps: Int?, durationSeconds: Int?) -> Unit,
) {
    val step = when (field) {
        EditField.WEIGHT -> if (unit == WearUnit.KG) weightStepKg else LB_STEP
        EditField.REPS -> 1.0
        EditField.DURATION -> DURATION_STEP_SECONDS
    }
    // Weight is edited in the display unit, then converted back to kg once.
    var value by rememberSaveable {
        mutableDoubleStateOf(
            when (field) {
                EditField.WEIGHT -> unit.fromKg(set.weightKg ?: set.previousWeightKg ?: 0.0)
                EditField.REPS -> (set.reps ?: set.previousReps ?: 0).toDouble()
                EditField.DURATION -> (set.durationSeconds ?: set.previousDurationSeconds ?: 0).toDouble()
            }
        )
    }
    val haptics = LocalHapticFeedback.current
    fun move(direction: Int) {
        // Snap to the step grid first (80,3 kg -> 80 / 82,5), never below zero.
        value = ((Math.round(value / step) + direction) * step).coerceAtLeast(0.0)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val focusRequester = remember { FocusRequester() }
    var rotaryAccumulator by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onRotaryScrollEvent { event ->
                    rotaryAccumulator += event.verticalScrollPixels
                    while (rotaryAccumulator >= ROTARY_PIXELS_PER_STEP) {
                        move(1)
                        rotaryAccumulator -= ROTARY_PIXELS_PER_STEP
                    }
                    while (rotaryAccumulator <= -ROTARY_PIXELS_PER_STEP) {
                        move(-1)
                        rotaryAccumulator += ROTARY_PIXELS_PER_STEP
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = 30.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Text(
                    stringResource(
                        when (field) {
                            EditField.WEIGHT -> R.string.edit_weight
                            EditField.REPS -> R.string.edit_reps
                            EditField.DURATION -> R.string.edit_duration
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(onClick = { move(-1) }, modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.SmallButtonSize)) {
                        Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.cd_decrease))
                    }
                    Text(
                        when (field) {
                            EditField.WEIGHT -> formatDecimal(value)
                            EditField.REPS -> value.roundToInt().toString()
                            EditField.DURATION -> formatRest(value.roundToInt())
                        },
                        style = MaterialTheme.typography.numeralMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 64.dp),
                        maxLines = 1,
                    )
                    FilledTonalIconButton(onClick = { move(1) }, modifier = Modifier.touchTargetAwareSize(IconButtonDefaults.SmallButtonSize)) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_increase))
                    }
                }
                Text(
                    when (field) {
                        EditField.WEIGHT -> unit.label
                        EditField.REPS -> stringResource(R.string.unit_reps)
                        EditField.DURATION -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                RoundAction(
                    onClick = {
                        when (field) {
                            EditField.WEIGHT -> onConfirm((unit.toKg(value) * 100).roundToInt() / 100.0, set.reps, set.durationSeconds)
                            EditField.REPS -> onConfirm(set.weightKg, value.roundToInt(), set.durationSeconds)
                            EditField.DURATION -> onConfirm(set.weightKg, set.reps, value.roundToInt())
                        }
                    },
                    icon = Icons.Rounded.Check,
                    description = stringResource(R.string.edit_ok),
                )
            }
        }
    }
}

private const val LB_STEP = 5.0
private const val DURATION_STEP_SECONDS = 15.0
private const val ROTARY_PIXELS_PER_STEP = 48f
