package com.example.mygymstatsapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.domain.model.WorkoutSummary
import com.example.mygymstatsapp.utils.formatDuration
import com.example.mygymstatsapp.utils.formatRelativeDate
import com.example.mygymstatsapp.utils.formatVolume

/** Compact card for a completed workout (dashboard, history). */
@Composable
fun WorkoutSummaryCard(
    workout: WorkoutSummary,
    unit: WeightUnit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GymCard(onClick = onClick, modifier = modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            val mainGroup = workout.muscleGroups.firstOrNull()
            if (mainGroup != null) {
                MuscleGroupBadge(mainGroup, size = 44.dp)
            } else {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.LocalFireDepartment, null) }
            }
            Column(Modifier.weight(1f)) {
                Text(workout.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatRelativeDate(workout.startedAt) + " · " + formatDuration(workout.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatVolume(workout.volumeKg, unit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    pluralStringResource(R.plurals.count_exercises, workout.exerciseCount, workout.exerciseCount) + " · " +
                        pluralStringResource(R.plurals.count_sets, workout.setCount, workout.setCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
