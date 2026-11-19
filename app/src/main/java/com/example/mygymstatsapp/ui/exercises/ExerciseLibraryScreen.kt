package com.example.mygymstatsapp.ui.exercises

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Exercise
import com.example.mygymstatsapp.domain.model.MuscleGroup
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.ConfirmDialog
import com.example.mygymstatsapp.ui.components.EmptyState
import com.example.mygymstatsapp.ui.components.MuscleGroupBadge
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.theme.color

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    onCreateExercise: () -> Unit,
    onEditExercise: (String) -> Unit,
    onExercisesPicked: (List<String>) -> Unit,
    onExerciseReplaced: (replaceKey: String, exerciseId: String) -> Unit,
    viewModel: ExerciseLibraryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var detailExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailExercise = state.exercises.firstOrNull { it.id == detailExerciseId }
    val replaceKey = state.replaceKey
    val onRowClick: (String) -> Unit = { exerciseId ->
        when {
            replaceKey != null -> onExerciseReplaced(replaceKey, exerciseId)
            state.pickMode -> viewModel.toggleSelection(exerciseId)
            else -> detailExerciseId = exerciseId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when {
                                replaceKey != null -> R.string.library_replace_title
                                state.pickMode -> R.string.action_add_exercises
                                else -> R.string.label_exercises
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (state.pickMode || replaceKey != null) {
                        IconButton(onClick = onCreateExercise) { Icon(Icons.Rounded.Add, stringResource(R.string.exercise_new)) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            if (!state.pickMode && replaceKey == null) {
                ExtendedFloatingActionButton(
                    onClick = onCreateExercise,
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text(stringResource(R.string.exercise_new)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.pickMode && state.selectedIds.isNotEmpty(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Button(
                        onClick = { onExercisesPicked(state.selectedIds) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        val count = state.selectedIds.size
                        Text(
                            pluralStringResource(R.plurals.library_add_count, count, count),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            FilterRow(selected = state.filter, onSelect = viewModel::onFilterChange)

            if (!state.isLoading && state.exercises.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = stringResource(R.string.template_empty_title),
                    message = stringResource(R.string.library_empty_message),
                    action = { OutlinedButton(shape = MaterialTheme.shapes.small, onClick = onCreateExercise) { Text(stringResource(R.string.library_create)) } },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                if (state.showGroupHeaders) {
                    state.exercises.groupBy { it.muscleGroup }.forEach { (group, exercises) ->
                        stickyHeader(key = "header_${group.name}") { GroupHeader(group, exercises.size) }
                        items(exercises, key = { it.id }) { exercise ->
                            ExerciseRow(
                                exercise = exercise,
                                pickMode = state.pickMode,
                                selectionIndex = state.selectedIds.indexOf(exercise.id),
                                onClick = { onRowClick(exercise.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(exercise.id) },
                            )
                        }
                    }
                } else {
                    items(state.exercises, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            pickMode = state.pickMode,
                            selectionIndex = state.selectedIds.indexOf(exercise.id),
                            onClick = { onRowClick(exercise.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(exercise.id) },
                        )
                    }
                }
            }
        }
    }

    detailExercise?.let { exercise ->
        ExerciseDetailSheet(
            exercise = exercise,
            unit = state.weightUnit,
            defaultIncrementKg = state.defaultIncrementKg,
            onDismiss = { detailExerciseId = null },
            onToggleFavorite = { viewModel.toggleFavorite(exercise.id) },
            onProgressionChange = { enabled, incrementKg -> viewModel.setProgression(exercise.id, enabled, incrementKg) },
            onEdit = {
                detailExerciseId = null
                onEditExercise(exercise.id)
            },
            onDelete = {
                detailExerciseId = null
                viewModel.deleteExercise(exercise.id)
            },
        )
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.library_search)) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Rounded.Close, stringResource(R.string.action_clear)) }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun FilterRow(selected: LibraryFilter, onSelect: (LibraryFilter) -> Unit) {
    val filters = remember {
        listOf(LibraryFilter.All, LibraryFilter.Favorites) + MuscleGroup.entries.map { LibraryFilter.Group(it) }
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(filters, key = { it.toString() }) { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        when (filter) {
                            LibraryFilter.All -> stringResource(R.string.library_filter_all)
                            LibraryFilter.Favorites -> stringResource(R.string.library_filter_favorites)
                            is LibraryFilter.Group -> stringResource(filter.group.labelRes)
                        }
                    )
                },
                leadingIcon = if (filter == LibraryFilter.Favorites) {
                    { Icon(Icons.Rounded.Star, null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun GroupHeader(group: MuscleGroup, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(group.color)
        )
        Text(
            stringResource(group.labelRes).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        )
        Text("$count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    pickMode: Boolean,
    selectionIndex: Int,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val selected = selectionIndex >= 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MuscleGroupBadge(exercise.muscleGroup, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(
                exercise.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(
                    stringResource(exercise.equipment.labelRes),
                    stringResource(exercise.muscleGroup.labelRes),
                    if (exercise.isCustom) stringResource(R.string.library_custom) else null,
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (pickMode) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Text(
                        "${selectionIndex + 1}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        } else {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (exercise.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = stringResource(R.string.library_favorite),
                    tint = if (exercise.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ExerciseDetailSheet(
    exercise: Exercise,
    unit: WeightUnit,
    defaultIncrementKg: Double,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onProgressionChange: (enabled: Boolean, incrementKg: Double?) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MuscleGroupBadge(exercise.muscleGroup, size = 56.dp)
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (exercise.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        stringResource(R.string.library_favorite),
                        tint = if (exercise.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(stringResource(exercise.muscleGroup.labelRes)) })
                AssistChip(onClick = {}, label = { Text(stringResource(exercise.equipment.labelRes)) })
                AssistChip(onClick = {}, label = { Text(stringResource(exercise.trackingType.labelRes)) })
            }
            if (exercise.description.isNotBlank()) {
                Text(exercise.description, style = MaterialTheme.typography.bodyLarge)
            }
            ProgressionSettings(
                exercise = exercise,
                defaultIncrementKg = defaultIncrementKg,
                unit = unit,
                range = null,
                onChange = onProgressionChange,
            )
            if (exercise.isCustom) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(shape = MaterialTheme.shapes.small, onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_edit))
                    }
                    OutlinedButton(shape = MaterialTheme.shapes.small, onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(R.string.library_builtin),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.library_delete_title),
            message = stringResource(R.string.library_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false },
        )
    }
}
