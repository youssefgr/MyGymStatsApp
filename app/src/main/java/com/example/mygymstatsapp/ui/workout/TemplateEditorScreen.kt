package com.example.mygymstatsapp.ui.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.SupersetGrouping
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.utils.formatRest

val REST_OPTIONS: List<Int?> = listOf(null, 30, 45, 60, 90, 120, 150, 180, 240, 300)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    pickedExerciseIds: List<String>?,
    onPickedHandled: () -> Unit,
    /** [item key, new exercise id] coming back from the library. */
    replacement: List<String>?,
    onReplacementHandled: () -> Unit,
    onAddExercises: () -> Unit,
    onReplaceExercise: (key: String) -> Unit,
    onBack: () -> Unit,
    onStarted: () -> Unit,
    viewModel: TemplateEditorViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val letters = remember(state.exercises) { SupersetGrouping.letters(state.exercises.map { it.supersetGroup }) }

    LaunchedEffect(pickedExerciseIds) {
        if (!pickedExerciseIds.isNullOrEmpty()) {
            viewModel.addExercises(pickedExerciseIds)
            onPickedHandled()
        }
    }
    LaunchedEffect(replacement) {
        if (replacement != null && replacement.size == 2) {
            viewModel.replaceExercise(key = replacement[0], exerciseId = replacement[1])
            onReplacementHandled()
        }
    }
    LaunchedEffect(state.isSaved) { if (state.isSaved) onBack() }
    WorkoutLauncherEffects(viewModel, onStarted)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEditing) R.string.template_edit_title else R.string.template_new_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) } },
                actions = {
                    TextButton(onClick = { viewModel.save() }) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Button(
                    onClick = { viewModel.save(startAfterSave = true) },
                    enabled = state.exercises.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.template_start), style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(R.string.template_name_label)) },
                    placeholder = { Text(stringResource(R.string.template_name_hint)) },
                    isError = state.showNameError,
                    supportingText = if (state.showNameError) {
                        { Text(stringResource(R.string.template_name_error)) }
                    } else {
                        null
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            item(key = "notes") {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text(stringResource(R.string.template_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            item(key = "header") {
                Text(
                    stringResource(R.string.template_exercises_header, state.exercises.size),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (!state.isLoading && state.exercises.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = Icons.Rounded.Add,
                        title = stringResource(R.string.template_empty_title),
                        message = stringResource(R.string.template_empty_message),
                    )
                }
            }
            itemsIndexed(state.exercises, key = { _, item -> item.key }) { index, item ->
                val nextGroup = state.exercises.getOrNull(index + 1)?.supersetGroup
                TemplateExerciseCard(
                    item = item,
                    supersetLetter = item.supersetGroup?.let { letters[it] },
                    chainsIntoNext = item.supersetGroup != null && nextGroup == item.supersetGroup,
                    canMoveUp = index > 0,
                    canMoveDown = index < state.exercises.lastIndex,
                    onUpdate = { transform -> viewModel.updateExercise(item.key, transform) },
                    onMove = { offset -> viewModel.moveExercise(item.key, offset) },
                    onLinkNext = { viewModel.linkSupersetWithNext(item.key) },
                    onUnlink = { viewModel.unlinkSuperset(item.key) },
                    onReplace = { onReplaceExercise(item.key) },
                    onRemove = { viewModel.removeExercise(item.key) },
                )
            }
            item(key = "add") {
                OutlinedButton(
                    onClick = onAddExercises,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.template_add_exercise))
                }
            }
        }
    }
}

@Composable
private fun TemplateExerciseCard(
    item: EditableTemplateExercise,
    supersetLetter: Char?,
    chainsIntoNext: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onUpdate: ((EditableTemplateExercise) -> EditableTemplateExercise) -> Unit,
    onMove: (Int) -> Unit,
    onLinkNext: () -> Unit,
    onUnlink: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var restMenuOpen by remember { mutableStateOf(false) }
    val supersetColor = MaterialTheme.colorScheme.secondary

    GymCard(
        modifier = Modifier.fillMaxWidth(),
        border = supersetLetter?.let { BorderStroke(1.5.dp, supersetColor.copy(alpha = 0.6f)) },
    ) {
        if (supersetLetter != null) {
            Text(
                stringResource(
                    if (chainsIntoNext) R.string.superset_chain else R.string.superset_rest_after,
                    supersetLetter.toString(),
                ),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = supersetColor,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MuscleGroupBadge(item.exercise.muscleGroup, size = 36.dp)
            Text(
                item.exercise.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Box {
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, stringResource(R.string.action_options)) }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_replace_exercise)) },
                        leadingIcon = { Icon(Icons.Rounded.SwapHoriz, null) },
                        onClick = { menuOpen = false; onReplace() },
                    )
                    if (!chainsIntoNext && canMoveDown) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_superset_link)) },
                            leadingIcon = { Icon(Icons.Rounded.Link, null) },
                            onClick = { menuOpen = false; onLinkNext() },
                        )
                    }
                    if (supersetLetter != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_superset_unlink)) },
                            leadingIcon = { Icon(Icons.Rounded.LinkOff, null) },
                            onClick = { menuOpen = false; onUnlink() },
                        )
                    }
                    if (canMoveUp) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_move_up)) },
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) },
                            onClick = { menuOpen = false; onMove(-1) },
                        )
                    }
                    if (canMoveDown) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_move_down)) },
                            leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) },
                            onClick = { menuOpen = false; onMove(1) },
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_remove), color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Sets stepper
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.label_sets), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(
                        onClick = { onUpdate { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) } },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Rounded.Remove, stringResource(R.string.cd_less), modifier = Modifier.size(16.dp)) }
                    Text(
                        "${item.sets}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center,
                    )
                    FilledTonalIconButton(
                        onClick = { onUpdate { it.copy(sets = (it.sets + 1).coerceAtMost(20)) } },
                        modifier = Modifier.size(32.dp),
                    ) { Icon(Icons.Rounded.Add, stringResource(R.string.cd_more), modifier = Modifier.size(16.dp)) }
                }
            }
            // Reps range
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.label_reps_range), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RepsField(item.repsMin, { value -> onUpdate { it.copy(repsMin = value) } }, Modifier.weight(1f))
                    Text("–", modifier = Modifier.padding(horizontal = 4.dp))
                    RepsField(item.repsMax, { value -> onUpdate { it.copy(repsMax = value) } }, Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Box {
            AssistChip(
                onClick = { restMenuOpen = true },
                label = {
                    Text(stringResource(R.string.exercise_rest, item.restSeconds?.let(::formatRest) ?: stringResource(R.string.rest_none)))
                },
                leadingIcon = { Icon(Icons.Rounded.Timer, null, modifier = Modifier.size(18.dp)) },
            )
            DropdownMenu(expanded = restMenuOpen, onDismissRequest = { restMenuOpen = false }) {
                REST_OPTIONS.forEach { seconds ->
                    DropdownMenuItem(
                        text = { Text(seconds?.let(::formatRest) ?: stringResource(R.string.rpe_none)) },
                        onClick = { restMenuOpen = false; onUpdate { it.copy(restSeconds = seconds) } },
                    )
                }
            }
        }
        if (item.notes.isNotBlank()) {
            Text(
                item.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun RepsField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 3 && new.all(Char::isDigit)) onValueChange(new) },
        modifier = modifier,
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.small,
    )
}
