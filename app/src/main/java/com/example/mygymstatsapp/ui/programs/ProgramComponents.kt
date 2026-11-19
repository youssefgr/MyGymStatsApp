package com.example.mygymstatsapp.ui.programs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.ui.components.ConfirmDialog
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.workout.targetLabel
import com.example.mygymstatsapp.ui.workout.templateSummary

private const val PREVIEW_EXERCISES = 4

/**
 * A session with its first exercises and a start button. [label] sits above the name
 * ("Jour 2 · prochaine"); [highlighted] outlines the program's next session.
 */
@Composable
fun SessionCard(
    session: WorkoutTemplate,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    highlighted: Boolean = false,
    extraMenuItems: @Composable (closeMenu: () -> Unit) -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    GymCard(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        border = if (highlighted) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (label != null) {
                    Text(
                        label.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(session.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (session.notes.isNotBlank()) {
                    Text(
                        session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_options)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_edit)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    extraMenuItems { menuOpen = false }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        session.exercises.take(PREVIEW_EXERCISES).forEach { item ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MuscleGroupBadge(item.exercise.muscleGroup, size = 24.dp)
                Text(
                    item.exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    targetLabel(item.targetSets, item.targetRepsMin, item.targetRepsMax),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val hidden = session.exercises.size - PREVIEW_EXERCISES
        if (hidden > 0) {
            Text(
                pluralStringResource(R.plurals.workouts_more_exercises, hidden, hidden),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                templateSummary(session),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Button(shape = MaterialTheme.shapes.small, onClick = onStart, enabled = session.exercises.isNotEmpty()) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_start), fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.session_delete_title, session.name),
            message = stringResource(R.string.session_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Name + description of a program, to create or edit it. */
@Composable
fun ProgramDialog(
    title: String,
    confirmLabel: String,
    onConfirm: (name: String, notes: String) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialNotes: String = "",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }
    var showError by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text(stringResource(R.string.program_name_label)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.program_name_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.program_notes_label)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    showError = true
                } else {
                    onDismiss()
                    onConfirm(name, notes)
                }
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
