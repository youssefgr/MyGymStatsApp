package com.example.mygymstatsapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.timer.RestTimer
import com.example.mygymstatsapp.ui.components.rememberRestClock
import com.example.mygymstatsapp.utils.formatClock
import com.example.mygymstatsapp.utils.formatRest

/** Sticky bar at the bottom of the workout screen while resting. */
@Composable
fun RestTimerBar(
    state: RestTimer.State,
    onAdjust: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
) {
    val now = rememberRestClock(state)
    val remainingSeconds = (state.remainingMs(now) + 999) / 1000
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
        ),
        shadowElevation = 8.dp,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            LinearProgressIndicator(
                progress = { state.progress(now) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (state.isPaused) R.string.rest_paused_label else R.string.rest_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatClock(remainingSeconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                TextButton(onClick = { onAdjust(-15) }) { Text("-15", style = MaterialTheme.typography.titleMedium) }
                TextButton(onClick = { onAdjust(15) }) { Text("+15", style = MaterialTheme.typography.titleMedium) }
                FilledTonalIconButton(onClick = if (state.isPaused) onResume else onPause) {
                    Icon(
                        if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = stringResource(if (state.isPaused) R.string.action_resume else R.string.action_pause),
                    )
                }
                FilledTonalIconButton(
                    onClick = onSkip,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                ) { Icon(Icons.Rounded.SkipNext, contentDescription = stringResource(R.string.action_skip)) }
            }
        }
    }
}

/** Manual timer start with quick durations. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RestTimerSheet(
    defaultRestSeconds: Int,
    onStart: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.cd_rest_timer), style = MaterialTheme.typography.headlineSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                QUICK_REST_DURATIONS.forEach { seconds ->
                    val isDefault = seconds == defaultRestSeconds
                    if (isDefault) {
                        FilledTonalButton(shape = MaterialTheme.shapes.small, onClick = { onStart(seconds); onDismiss() }, modifier = Modifier.size(width = 96.dp, height = 56.dp)) {
                            Text(formatRest(seconds), style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { onStart(seconds); onDismiss() }, modifier = Modifier.size(width = 96.dp, height = 56.dp)) {
                            Text(formatRest(seconds), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.rest_sheet_hint, formatRest(defaultRestSeconds)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

val QUICK_REST_DURATIONS = listOf(30, 60, 90, 120, 180)
