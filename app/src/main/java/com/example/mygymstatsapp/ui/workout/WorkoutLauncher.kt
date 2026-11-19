package com.example.mygymstatsapp.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.domain.usecase.StartResult
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.ui.components.GymCard
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Shared "start a workout" behavior for every screen that can launch one
 * (dashboard, programs, workout detail). Handles the "a workout is already running" case.
 */
abstract class WorkoutLauncherViewModel(
    private val startWorkoutUseCase: StartWorkoutUseCase,
) : ViewModel() {

    private val _pendingRequest = MutableStateFlow<StartRequest?>(null)
    val pendingRequest: StateFlow<StartRequest?> = _pendingRequest.asStateFlow()

    private val _openActiveWorkout = Channel<Unit>(Channel.BUFFERED)
    val openActiveWorkout: Flow<Unit> = _openActiveWorkout.receiveAsFlow()

    fun startWorkout(request: StartRequest, replaceActive: Boolean = false) {
        viewModelScope.launch {
            _pendingRequest.value = null
            when (startWorkoutUseCase(request, replaceActive)) {
                is StartResult.Started -> _openActiveWorkout.send(Unit)
                StartResult.ActiveWorkoutExists -> _pendingRequest.value = request
                StartResult.NotFound -> Unit
            }
        }
    }

    fun resumeActiveWorkout() {
        _pendingRequest.value = null
        viewModelScope.launch { _openActiveWorkout.send(Unit) }
    }

    fun dismissPendingRequest() {
        _pendingRequest.value = null
    }
}

/** Navigation + conflict dialog for a [WorkoutLauncherViewModel]. */
@Composable
fun WorkoutLauncherEffects(launcher: WorkoutLauncherViewModel, onOpenActiveWorkout: () -> Unit) {
    val currentOnOpen by rememberUpdatedState(onOpenActiveWorkout)
    LaunchedEffect(launcher) { launcher.openActiveWorkout.collect { currentOnOpen() } }

    val pending by launcher.pendingRequest.collectAsStateWithLifecycle()
    pending?.let { request ->
        AlertDialog(
            onDismissRequest = launcher::dismissPendingRequest,
            title = { Text(stringResource(R.string.launcher_active_title)) },
            text = { Text(stringResource(R.string.launcher_active_message)) },
            confirmButton = { TextButton(onClick = launcher::resumeActiveWorkout) { Text(stringResource(R.string.action_resume)) } },
            dismissButton = {
                TextButton(
                    onClick = { launcher.startWorkout(request, replaceActive = true) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.launcher_replace)) }
            },
        )
    }
}

/** Bottom sheet: empty workout, a session of a program (next one first in mind), or a standalone session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartWorkoutSheet(
    programs: List<Program>,
    sessions: List<WorkoutTemplate>,
    onDismiss: () -> Unit,
    onStart: (StartRequest) -> Unit,
    onCreateTemplate: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                Text(stringResource(R.string.launcher_sheet_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
            }
            item {
                SheetOption(
                    title = stringResource(R.string.workouts_empty_workout),
                    subtitle = stringResource(R.string.launcher_empty_hint),
                    icon = { Icon(Icons.Rounded.Bolt, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onDismiss(); onStart(StartRequest.Empty) },
                )
            }
            programs.filter { it.sessions.isNotEmpty() }.forEach { program ->
                val next = program.nextSession
                item(key = "program_${program.id}") { SheetHeader(program.name) }
                items(program.sessions, key = { it.id }) { session ->
                    val summary = templateSummary(session)
                    SheetOption(
                        title = session.name,
                        subtitle = if (session.id == next?.id) stringResource(R.string.program_next) + " · " + summary else summary,
                        icon = { Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { onDismiss(); onStart(StartRequest.FromTemplate(session.id)) },
                    )
                }
            }
            if (sessions.isNotEmpty()) {
                item(key = "standalone") { SheetHeader(stringResource(R.string.programs_standalone)) }
            }
            items(sessions, key = { it.id }) { session ->
                SheetOption(
                    title = session.name,
                    subtitle = templateSummary(session),
                    icon = { Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { onDismiss(); onStart(StartRequest.FromTemplate(session.id)) },
                )
            }
            item {
                TextButton(onClick = { onDismiss(); onCreateTemplate() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.launcher_create_program), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SheetOption(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    GymCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            icon()
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
