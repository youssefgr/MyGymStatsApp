package com.example.mygymstatsapp.ui.programs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.ConfirmDialog
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.SectionHeader
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherEffects
import com.example.mygymstatsapp.ui.workout.templateSummary

/** A program: its sessions in day order, the next one highlighted. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramDetailScreen(
    onBack: () -> Unit,
    onOpenActiveWorkout: () -> Unit,
    onAddSession: (programId: String) -> Unit,
    onEditSession: (String) -> Unit,
    viewModel: ProgramDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    var menuOpen by remember { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var showAddExisting by rememberSaveable { mutableStateOf(false) }

    WorkoutLauncherEffects(viewModel, onOpenActiveWorkout)
    LaunchedEffect(state.isMissing) { if (state.isMissing) currentOnBack() }

    val program = state.program
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (program != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_options)) }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.program_edit_title)) },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                    onClick = { menuOpen = false; showEditDialog = true },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; confirmDelete = true },
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (program == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        val next = program.nextSession
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (program.notes.isNotBlank()) {
                item(key = "notes") {
                    Text(program.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "header") { SectionHeader(stringResource(R.string.program_sessions)) }
            if (program.sessions.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.CalendarViewWeek,
                        title = stringResource(R.string.program_empty_title),
                        message = stringResource(R.string.program_empty_message),
                    )
                }
            }
            itemsIndexed(program.sessions, key = { _, session -> session.id }) { index, session ->
                val isNext = session.id == next?.id && program.sessions.size > 1
                SessionCard(
                    session = session,
                    label = stringResource(if (isNext) R.string.program_day_next else R.string.program_day, index + 1),
                    highlighted = isNext,
                    onStart = { viewModel.startWorkout(StartRequest.FromTemplate(session.id)) },
                    onEdit = { onEditSession(session.id) },
                    onDelete = { viewModel.deleteSession(session.id) },
                    extraMenuItems = { closeMenu ->
                        if (index > 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_move_up)) },
                                leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) },
                                onClick = { closeMenu(); viewModel.moveSession(session.id, -1) },
                            )
                        }
                        if (index < program.sessions.lastIndex) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_move_down)) },
                                leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) },
                                onClick = { closeMenu(); viewModel.moveSession(session.id, 1) },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.session_remove_from_program)) },
                            leadingIcon = { Icon(Icons.Rounded.PlaylistRemove, null) },
                            onClick = { closeMenu(); viewModel.removeSession(session.id) },
                        )
                    },
                )
            }
            item(key = "add") {
                Column {
                    OutlinedButton(
                        onClick = { onAddSession(program.id) },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.program_add_session), style = MaterialTheme.typography.titleSmall)
                    }
                    if (state.standaloneSessions.isNotEmpty()) {
                        TextButton(onClick = { showAddExisting = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.program_add_existing))
                        }
                    }
                }
            }
        }

        if (showEditDialog) {
            ProgramDialog(
                title = stringResource(R.string.program_edit_title),
                confirmLabel = stringResource(R.string.action_save),
                initialName = program.name,
                initialNotes = program.notes,
                onConfirm = viewModel::updateProgram,
                onDismiss = { showEditDialog = false },
            )
        }
        if (confirmDelete) {
            ConfirmDialog(
                title = stringResource(R.string.program_delete_title, program.name),
                message = stringResource(R.string.program_delete_message),
                confirmLabel = stringResource(R.string.action_delete),
                destructive = true,
                onConfirm = viewModel::deleteProgram,
                onDismiss = { confirmDelete = false },
            )
        }
        if (showAddExisting) {
            ExistingSessionDialog(
                sessions = state.standaloneSessions,
                onPick = { showAddExisting = false; viewModel.addExistingSession(it) },
                onDismiss = { showAddExisting = false },
            )
        }
    }
}

@Composable
private fun ExistingSessionDialog(sessions: List<WorkoutTemplate>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.program_add_existing_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                sessions.forEach { session ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onPick(session.id) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(session.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            templateSummary(session),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
