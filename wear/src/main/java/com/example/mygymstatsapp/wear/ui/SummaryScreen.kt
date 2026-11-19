package com.example.mygymstatsapp.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.mygymstatsapp.shared.FinishedSummary
import com.example.mygymstatsapp.shared.WearUnit
import com.example.mygymstatsapp.wear.R

/** "Séance bouclée": the figures of the workout just finished, brass when a record fell. */
@Composable
fun SummaryScreen(summary: FinishedSummary, unit: WearUnit, onDone: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val hasRecord = summary.records.isNotEmpty()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onDone) { Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.edit_ok)) }
        },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        painterResource(R.drawable.ic_fonte_mark),
                        contentDescription = null,
                        tint = if (hasRecord) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(stringResource(R.string.summary_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(summary.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        stringResource(if (hasRecord) R.string.summary_cheer_record else R.string.summary_cheer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    StatLine(stringResource(R.string.label_duration), formatDuration(summary.durationSeconds))
                    StatLine(stringResource(R.string.label_volume), formatVolume(summary.volumeKg, unit))
                    StatLine(stringResource(R.string.label_sets), "${summary.setCount}")
                    summary.avgHeartRate?.let { StatLine(stringResource(R.string.label_heart_rate_avg), stringResource(R.string.value_bpm, it)) }
                    summary.maxHeartRate?.let { StatLine(stringResource(R.string.label_heart_rate_max), stringResource(R.string.value_bpm, it)) }
                }
            }
            items(summary.records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                        Column {
                            Text(record.exerciseName, style = MaterialTheme.typography.labelMedium)
                            Text(recordValue(record, unit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelMedium)
    }
}
