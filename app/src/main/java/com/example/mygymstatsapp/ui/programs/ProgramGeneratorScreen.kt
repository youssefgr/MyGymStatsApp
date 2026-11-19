package com.example.mygymstatsapp.ui.programs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.ProgramGenerator
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.components.SectionHeader
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.theme.color
import com.example.mygymstatsapp.ui.workout.targetLabel

/** Builds a program from priorities (muscles, sessions per week, goal) with a live preview. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProgramGeneratorScreen(
    onClose: () -> Unit,
    onCreated: (programId: String) -> Unit,
    viewModel: ProgramGeneratorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnCreated by rememberUpdatedState(onCreated)
    LaunchedEffect(viewModel) { viewModel.created.collect { currentOnCreated(it) } }
    val options = state.options

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.generator_title)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = viewModel::create,
                    enabled = !state.isLoading && !state.isSaving && state.sessions.isNotEmpty(),
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(56.dp),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.generator_create), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "intro") {
                Text(
                    stringResource(R.string.generator_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item(key = "focus") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel(stringResource(R.string.generator_focus))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MuscleGroup.entries.forEach { group ->
                            FilterChip(
                                selected = group in options.focus,
                                onClick = { viewModel.toggleFocus(group) },
                                label = { Text(stringResource(group.labelRes)) },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(group.color)
                                    )
                                },
                                colors = selectedColors(),
                            )
                        }
                    }
                    Hint(stringResource(R.string.generator_focus_hint))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.generator_focus_only), style = MaterialTheme.typography.titleSmall)
                            Hint(stringResource(R.string.generator_focus_only_hint))
                        }
                        Switch(
                            checked = options.focusOnly,
                            onCheckedChange = viewModel::setFocusOnly,
                            enabled = options.focus.isNotEmpty(),
                        )
                    }
                }
            }

            item(key = "sessions") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel(stringResource(R.string.generator_sessions))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (ProgramGenerator.MIN_SESSIONS..ProgramGenerator.MAX_SESSIONS).forEach { count ->
                            FilterChip(
                                selected = count == options.sessionsPerWeek,
                                onClick = { viewModel.setSessionsPerWeek(count) },
                                label = {
                                    Text(
                                        "$count",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = MaterialTheme.typography.labelLarge,
                                        textAlign = TextAlign.Center,
                                    )
                                },
                                colors = selectedColors(),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            item(key = "goal") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldLabel(stringResource(R.string.generator_goal))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Goal.entries.forEach { goal ->
                            FilterChip(
                                selected = goal == options.goal,
                                onClick = { viewModel.setGoal(goal) },
                                label = { Text(stringResource(goal.labelRes)) },
                                colors = selectedColors(),
                            )
                        }
                    }
                }
            }

            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(stringResource(R.string.program_name_label)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item(key = "preview_header") { SectionHeader(stringResource(R.string.generator_preview)) }
            itemsIndexed(state.sessions) { index, session -> SessionPreviewCard(index, session) }
        }
    }
}

@Composable
private fun SessionPreviewCard(index: Int, session: SessionPreview) {
    GymCard(Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Text(
            stringResource(R.string.program_day, index + 1).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(session.name, style = MaterialTheme.typography.titleLarge)
        Hint(session.notes)
        Spacer(Modifier.height(10.dp))
        session.exercises.forEach { (exercise, planned) ->
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MuscleGroupBadge(exercise.muscleGroup, size = 24.dp)
                Text(
                    exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    targetLabel(planned.targetSets, planned.targetRepsMin, planned.targetRepsMax),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun selectedColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
)
