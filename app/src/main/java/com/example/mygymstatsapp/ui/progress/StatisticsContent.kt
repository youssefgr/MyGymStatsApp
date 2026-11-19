package com.example.mygymstatsapp.ui.progress

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Scale
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.BarChart
import com.example.mygymstatsapp.ui.components.BarEntry
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.InlineStat
import com.example.mygymstatsapp.ui.components.StatTile
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.theme.color
import com.example.mygymstatsapp.utils.formatDuration
import com.example.mygymstatsapp.utils.formatShortDate
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.toEpochMillis

@Composable
fun StatisticsContent(
    viewModel: StatisticsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unit = state.weightUnit

    if (!state.isLoading && state.allTime.workoutCount == 0) {
        EmptyState(
            icon = Icons.Rounded.Insights,
            title = stringResource(R.string.stats_empty_title),
            message = stringResource(R.string.stats_empty_message),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "periods") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PeriodCard(stringResource(R.string.home_this_week), state.week.workoutCount, state.week.volumeKg, unit, Modifier.weight(1f))
                PeriodCard(stringResource(R.string.stats_this_month), state.month.workoutCount, state.month.volumeKg, unit, Modifier.weight(1f))
            }
        }
        item(key = "tiles") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(stringResource(R.string.stats_average_duration), formatDuration(state.allTime.averageDurationSeconds), Modifier.weight(1f), Icons.Rounded.Schedule)
                StatTile(stringResource(R.string.stats_records), pluralStringResource(R.plurals.count_exercises, state.recordCount, state.recordCount), Modifier.weight(1f), Icons.Rounded.EmojiEvents, MaterialTheme.colorScheme.tertiary)
            }
        }
        item(key = "weekly_volume") {
            val entries = remember(state.weeks) {
                state.weeks.map { BarEntry(formatShortDate(it.weekStart.toEpochMillis()), it.volumeKg) }
            }
            GymCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stats_weekly_volume), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.stats_last_12_weeks), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                BarChart(entries = entries, formatValue = { formatVolume(it, unit) }, color = MaterialTheme.colorScheme.secondary)
            }
        }
        item(key = "weekly_count") {
            val entries = remember(state.weeks) {
                state.weeks.map { BarEntry(formatShortDate(it.weekStart.toEpochMillis()), it.workoutCount.toDouble()) }
            }
            GymCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stats_weekly_workouts), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val resources = LocalResources.current
                BarChart(
                    entries = entries,
                    formatValue = { resources.getQuantityString(R.plurals.count_workouts, it.toInt(), it.toInt()) },
                    height = 160.dp,
                )
            }
        }
        if (state.topExercises.isNotEmpty()) {
            item(key = "top") {
                GymCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stats_top_exercises), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    val max = state.topExercises.maxOf { it.count }.coerceAtLeast(1)
                    state.topExercises.forEachIndexed { index, usage ->
                        RankRow(
                            label = "${index + 1}. ${usage.name}",
                            value = pluralStringResource(R.plurals.count_workouts, usage.count, usage.count),
                            fraction = usage.count.toFloat() / max,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (state.muscleShares.isNotEmpty()) {
            item(key = "muscles") {
                GymCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.stats_muscle_groups), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.stats_muscle_groups_hint, stringResource(state.muscleShares.first().muscleGroup.labelRes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    val max = state.muscleShares.maxOf { it.setCount }.coerceAtLeast(1)
                    state.muscleShares.forEach { share ->
                        RankRow(
                            label = stringResource(share.muscleGroup.labelRes),
                            value = pluralStringResource(R.plurals.count_sets, share.setCount, share.setCount),
                            fraction = share.setCount.toFloat() / max,
                            color = share.muscleGroup.color,
                        )
                    }
                }
            }
        }
        item(key = "all_time") {
            GymCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.stats_all_time), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    InlineStat(stringResource(R.string.label_workouts), "${state.allTime.workoutCount}")
                    InlineStat(stringResource(R.string.label_sets), "${state.allTime.setCount}")
                    InlineStat(stringResource(R.string.label_volume), formatVolume(state.allTime.volumeKg, unit))
                }
            }
        }
    }
}

@Composable
private fun PeriodCard(title: String, workouts: Int, volumeKg: Double, unit: WeightUnit, modifier: Modifier) {
    GymCard(modifier) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Text("$workouts", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(pluralStringResource(R.plurals.workouts_word, workouts), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Scale, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 6.dp))
            Text(formatVolume(volumeKg, unit), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RankRow(label: String, value: String, fraction: Float, color: Color) {
    val animated by animateFloatAsState(fraction, label = "rank")
    Column(Modifier.padding(vertical = 6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animated)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
