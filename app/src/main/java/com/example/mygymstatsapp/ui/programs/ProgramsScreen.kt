package com.example.mygymstatsapp.ui.programs

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Program
import com.example.mygymstatsapp.domain.model.WorkoutTemplate
import com.example.mygymstatsapp.domain.usecase.StartRequest
import com.example.mygymstatsapp.navigation.ProgramsSection
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.SectionHeader
import com.example.mygymstatsapp.ui.components.SegmentedTabs
import com.example.mygymstatsapp.ui.history.CalendarContent
import com.example.mygymstatsapp.ui.history.HistoryContent
import com.example.mygymstatsapp.ui.workout.WorkoutLauncherEffects

/** "Programmes" tab: programs (groups of sessions), standalone sessions, and the history. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramsScreen(
    initialSection: ProgramsSection,
    onOpenActiveWorkout: () -> Unit,
    onCreateSession: () -> Unit,
    onEditSession: (String) -> Unit,
    onOpenProgram: (String) -> Unit,
    onGenerateProgram: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    viewModel: ProgramsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialSection.ordinal) }
    var showNewProgramSheet by rememberSaveable { mutableStateOf(false) }
    var showProgramDialog by rememberSaveable { mutableStateOf(false) }
    val currentOnOpenProgram by rememberUpdatedState(onOpenProgram)

    WorkoutLauncherEffects(viewModel, onOpenActiveWorkout)
    LaunchedEffect(viewModel) { viewModel.createdProgram.collect { currentOnOpenProgram(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_programs), style = MaterialTheme.typography.headlineMedium) },
                actions = {
                    IconButton(onClick = onOpenLibrary) { Icon(Icons.Rounded.FitnessCenter, stringResource(R.string.workouts_library)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SegmentedTabs(
                options = ProgramsSection.entries.map { stringResource(it.labelRes) },
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (ProgramsSection.entries[selectedTab]) {
                ProgramsSection.PROGRAMS -> ProgramsContent(
                    state = state,
                    onStart = viewModel::startWorkout,
                    onNewProgram = { showNewProgramSheet = true },
                    onCreateProgramManually = { showProgramDialog = true },
                    onGenerateProgram = onGenerateProgram,
                    onOpenProgram = onOpenProgram,
                    onCreateSession = onCreateSession,
                    onEditSession = onEditSession,
                    onDeleteSession = viewModel::deleteSession,
                    onAddSessionToProgram = viewModel::addSessionToProgram,
                )
                ProgramsSection.CALENDAR -> CalendarContent(onOpenWorkout = onOpenWorkout)
                ProgramsSection.HISTORY -> HistoryContent(onOpenWorkout = onOpenWorkout)
            }
        }
    }

    if (showNewProgramSheet) {
        NewProgramSheet(
            onDismiss = { showNewProgramSheet = false },
            onCreateManually = { showNewProgramSheet = false; showProgramDialog = true },
            onGenerate = { showNewProgramSheet = false; onGenerateProgram() },
        )
    }
    if (showProgramDialog) {
        ProgramDialog(
            title = stringResource(R.string.program_new_title),
            confirmLabel = stringResource(R.string.action_create),
            onConfirm = viewModel::createProgram,
            onDismiss = { showProgramDialog = false },
        )
    }
}

@get:StringRes
private val ProgramsSection.labelRes: Int
    get() = when (this) {
        ProgramsSection.PROGRAMS -> R.string.workouts_tab_programs
        ProgramsSection.CALENDAR -> R.string.workouts_tab_calendar
        ProgramsSection.HISTORY -> R.string.workouts_tab_history
    }

@Composable
private fun ProgramsContent(
    state: ProgramsUiState,
    onStart: (StartRequest) -> Unit,
    onNewProgram: () -> Unit,
    onCreateProgramManually: () -> Unit,
    onGenerateProgram: () -> Unit,
    onOpenProgram: (String) -> Unit,
    onCreateSession: () -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onAddSessionToProgram: (sessionId: String, programId: String) -> Unit,
) {
    var pickProgramFor by rememberSaveable { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "quick") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickAction(
                    icon = Icons.Rounded.Bolt,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.workouts_empty_workout),
                    subtitle = stringResource(R.string.workouts_empty_workout_hint),
                    onClick = { onStart(StartRequest.Empty) },
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    icon = Icons.Rounded.Add,
                    tint = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.programs_new_session),
                    subtitle = stringResource(R.string.programs_new_session_hint),
                    onClick = onCreateSession,
                    modifier = Modifier.weight(1f),
                )
                QuickAction(
                    icon = Icons.Rounded.CalendarViewWeek,
                    tint = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.programs_new_program),
                    subtitle = stringResource(R.string.programs_new_program_hint),
                    onClick = onNewProgram,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "programs_header") { SectionHeader(stringResource(R.string.workouts_my_programs), Modifier.padding(top = 8.dp)) }
        if (!state.isLoading && state.programs.isEmpty()) {
            item(key = "no_program") { NoProgramCard(onCreateProgramManually, onGenerateProgram) }
        }
        items(state.programs, key = { it.id }) { program ->
            ProgramCard(
                program = program,
                onOpen = { onOpenProgram(program.id) },
                onStartSession = { onStart(StartRequest.FromTemplate(it.id)) },
            )
        }

        item(key = "standalone_header") {
            Column(Modifier.padding(top = 8.dp)) {
                SectionHeader(stringResource(R.string.programs_standalone))
                Text(
                    stringResource(R.string.programs_standalone_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(state.standaloneSessions, key = { it.id }) { session ->
            SessionCard(
                session = session,
                onStart = { onStart(StartRequest.FromTemplate(session.id)) },
                onEdit = { onEditSession(session.id) },
                onDelete = { onDeleteSession(session.id) },
                extraMenuItems = { closeMenu ->
                    if (state.programs.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.session_add_to_program)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                            onClick = { closeMenu(); pickProgramFor = session.id },
                        )
                    }
                },
            )
        }
    }

    pickProgramFor?.let { sessionId ->
        ProgramPickerDialog(
            programs = state.programs,
            onPick = { programId -> pickProgramFor = null; onAddSessionToProgram(sessionId, programId) },
            onDismiss = { pickProgramFor = null },
        )
    }
}

@Composable
private fun QuickAction(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GymCard(onClick = onClick, modifier = modifier, contentPadding = 12.dp) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Program summary with its next session ready to start. */
@Composable
private fun ProgramCard(program: Program, onOpen: () -> Unit, onStartSession: (WorkoutTemplate) -> Unit) {
    val next = program.nextSession
    GymCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(program.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    pluralStringResource(R.plurals.count_sessions, program.sessions.size, program.sessions.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (program.sessions.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                program.sessions.joinToString(" · ") { it.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (next != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.program_next).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(next.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Button(shape = MaterialTheme.shapes.small, onClick = { onStartSession(next) }, enabled = next.exercises.isNotEmpty()) {
                    Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_start), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun NoProgramCard(onCreateManually: () -> Unit, onGenerate: () -> Unit) {
    GymCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.workouts_no_program_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.workouts_no_program_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(shape = MaterialTheme.shapes.small, onClick = onCreateManually, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.program_create_manual), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(shape = MaterialTheme.shapes.small, onClick = onGenerate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_generate), maxLines = 1)
            }
        }
    }
}

/** "Nouveau programme": build it yourself or let the generator do it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewProgramSheet(onDismiss: () -> Unit, onCreateManually: () -> Unit, onGenerate: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(R.string.program_new_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )
            SheetChoice(
                icon = Icons.Rounded.Edit,
                title = stringResource(R.string.program_create_manual),
                subtitle = stringResource(R.string.program_create_manual_hint),
                onClick = onCreateManually,
            )
            SheetChoice(
                icon = Icons.Rounded.AutoAwesome,
                title = stringResource(R.string.program_create_generate),
                subtitle = stringResource(R.string.program_create_generate_hint),
                onClick = onGenerate,
            )
        }
    }
}

@Composable
private fun SheetChoice(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    GymCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProgramPickerDialog(programs: List<Program>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.session_add_to_program)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                programs.forEach { program ->
                    Text(
                        program.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(program.id) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
