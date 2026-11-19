package com.example.mygymstatsapp.ui.profile

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.Goal
import com.example.mygymstatsapp.domain.model.ThemeMode
import com.example.mygymstatsapp.domain.model.WeightUnit
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.FonteWordmark
import com.example.mygymstatsapp.ui.components.GymCard
import com.example.mygymstatsapp.ui.components.InlineStat
import com.example.mygymstatsapp.ui.exercises.loadStepOptions
import com.example.mygymstatsapp.ui.labelRes
import com.example.mygymstatsapp.ui.workout.QUICK_REST_DURATIONS
import com.example.mygymstatsapp.utils.formatDecimal
import com.example.mygymstatsapp.utils.formatRest
import com.example.mygymstatsapp.utils.formatVolume
import com.example.mygymstatsapp.utils.fromKg
import com.example.mygymstatsapp.utils.toKg
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    val unit = profile.weightUnit

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.headlineMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (state.isLoading) return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "header") {
                GymCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                profile.name.take(1).uppercase().ifEmpty { "?" },
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(profile.name.ifBlank { stringResource(R.string.profile_athlete) }, style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(profile.goal.labelRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        InlineStat(stringResource(R.string.label_workouts), "${state.allTime.workoutCount}")
                        InlineStat(stringResource(R.string.label_sets), "${state.allTime.setCount}")
                        InlineStat(stringResource(R.string.profile_total_volume), formatVolume(state.allTime.volumeKg, unit))
                    }
                }
            }

            item(key = "info") {
                Section(stringResource(R.string.profile_information)) {
                    CommitTextField(
                        label = stringResource(R.string.onboarding_name_label),
                        value = profile.name,
                        onCommit = viewModel::setName,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CommitTextField(
                            label = stringResource(R.string.profile_body_weight, unit.label),
                            value = profile.bodyWeightKg?.let { formatDecimal(unit.fromKg(it), 1) }.orEmpty(),
                            onCommit = viewModel::setBodyWeight,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        CommitTextField(
                            label = stringResource(R.string.profile_height),
                            value = profile.heightCm?.toString().orEmpty(),
                            onCommit = viewModel::setHeight,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item(key = "goal") {
                Section(stringResource(R.string.profile_goal)) {
                    Goal.entries.forEach { goal ->
                        SelectableRow(stringResource(goal.labelRes), selected = goal == profile.goal, onClick = { viewModel.setGoal(goal) })
                    }
                }
            }

            item(key = "units") {
                Section(stringResource(R.string.profile_units)) {
                    SelectableRow(stringResource(R.string.unit_kilograms), unit == WeightUnit.KG) { viewModel.setWeightUnit(WeightUnit.KG) }
                    SelectableRow(stringResource(R.string.unit_pounds), unit == WeightUnit.LB) { viewModel.setWeightUnit(WeightUnit.LB) }
                }
            }

            item(key = "training") {
                Section(stringResource(R.string.profile_training)) {
                    Text(stringResource(R.string.profile_rest), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QUICK_REST_DURATIONS.forEach { seconds ->
                            Chip(formatRest(seconds), seconds == profile.defaultRestSeconds) { viewModel.setDefaultRest(seconds) }
                        }
                    }
                    Hint(stringResource(R.string.profile_rest_hint))

                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.profile_increment), style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        loadStepOptions(unit).forEach { value ->
                            val kg = unit.toKg(value)
                            Chip(
                                label = "+${formatDecimal(value)} ${unit.label}",
                                selected = abs(kg - profile.weightIncrementKg) < 0.01,
                                onClick = { viewModel.setWeightIncrement(kg) },
                            )
                        }
                    }
                    Hint(stringResource(R.string.profile_increment_hint))

                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.profile_auto_warmup), style = MaterialTheme.typography.titleSmall)
                            Hint(stringResource(R.string.profile_auto_warmup_hint))
                        }
                        Switch(checked = profile.autoWarmup, onCheckedChange = viewModel::setAutoWarmup)
                    }
                }
            }

            item(key = "timer") { RestTimerPermissions() }

            item(key = "theme") {
                Section(stringResource(R.string.profile_appearance)) {
                    ThemeMode.entries.forEach { mode ->
                        SelectableRow(stringResource(mode.labelRes), mode == profile.themeMode) { viewModel.setThemeMode(mode) }
                    }
                }
            }

            item(key = "about") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    FonteWordmark(markSize = 22.dp)
                    Text(
                        stringResource(R.string.brand_tagline).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.profile_about),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Notification and exact-alarm status for the rest timer, with shortcuts to the system settings. */
@Composable
private fun RestTimerPermissions() {
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(notificationsAllowed(context)) }
    var exactAlarmsAllowed by remember { mutableStateOf(exactAlarmsAllowed(context)) }
    // Re-check when coming back from the system settings.
    LifecycleResumeEffect(Unit) {
        notificationsAllowed = notificationsAllowed(context)
        exactAlarmsAllowed = exactAlarmsAllowed(context)
        onPauseOrDispose {}
    }

    Section(stringResource(R.string.cd_rest_timer)) {
        PermissionRow(
            title = stringResource(R.string.profile_notifications),
            status = stringResource(if (notificationsAllowed) R.string.profile_notifications_on else R.string.profile_notifications_off),
            granted = notificationsAllowed,
            onAllow = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            },
        )
        PermissionRow(
            title = stringResource(R.string.profile_exact_alarm),
            status = stringResource(if (exactAlarmsAllowed) R.string.profile_exact_on else R.string.profile_exact_off),
            granted = exactAlarmsAllowed,
            onAllow = {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                )
            },
        )
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

private fun exactAlarmsAllowed(context: Context): Boolean =
    context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

@Composable
private fun PermissionRow(title: String, status: String, granted: Boolean, onAllow: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Hint(status)
        }
        if (!granted) TextButton(onClick = onAllow) { Text(stringResource(R.string.action_allow)) }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    GymCard(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}

/**
 * Text field edited locally and saved when it loses focus (or on "Done"), so partial input
 * such as "72," is never reformatted while typing.
 */
@Composable
private fun CommitTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val currentOnCommit by rememberUpdatedState(onCommit)
    var text by remember { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!focused) text = value }
    DisposableEffect(Unit) {
        onDispose { if (focused) currentOnCommit(text) }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = keyboardOptions.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { state ->
                if (focused && !state.isFocused) currentOnCommit(text)
                focused = state.isFocused
            },
        shape = MaterialTheme.shapes.medium,
    )
}
