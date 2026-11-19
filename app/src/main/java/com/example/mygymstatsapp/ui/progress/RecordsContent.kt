package com.example.mygymstatsapp.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.PersonalRecord
import com.example.mygymstatsapp.domain.model.TrackingType
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.InlineStat
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.utils.formatRelativeDate
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.formatWeight

@Composable
fun RecordsContent(
    viewModel: RecordsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (!state.isLoading && state.records.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.EmojiEvents,
            title = stringResource(R.string.records_empty_title),
            message = stringResource(R.string.records_empty_message),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Text(stringResource(R.string.records_title) + " 🏆", style = MaterialTheme.typography.titleLarge)
        }
        items(state.records, key = { it.exerciseId }) { record -> RecordCard(record, state.weightUnit) }
    }
}

@Composable
private fun RecordCard(record: PersonalRecord, unit: WeightUnit) {
    GymCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MuscleGroupBadge(record.muscleGroup, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(record.exerciseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.records_last_time, formatRelativeDate(record.lastPerformedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (record.trackingType == TrackingType.WEIGHT_REPS) {
                    Text("1RM", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text(formatWeight(record.bests.bestOneRmKg, unit), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                } else {
                    Text(stringResource(R.string.set_header_reps), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                    Text("${record.bests.maxReps}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
        if (record.trackingType == TrackingType.WEIGHT_REPS) {
            HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InlineStat(stringResource(R.string.record_max_weight), formatWeight(record.bests.maxWeightKg, unit))
                InlineStat(stringResource(R.string.metric_reps), "${record.bests.maxReps}")
                InlineStat(stringResource(R.string.record_volume), formatVolume(record.bests.bestVolumeKg, unit))
            }
        }
    }
}
