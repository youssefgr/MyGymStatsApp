package com.example.mygymstatsapp.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mygymstatsapp.R
import com.example.mygymstatsapp.domain.model.ActiveWorkoutHeader
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.timer.RestTimer
import com.example.mygymstatsapp.ui.AppViewModelProvider
import com.example.mygymstatsapp.ui.components.rememberElapsedSeconds
import com.example.mygymstatsapp.ui.components.rememberRestClock
import com.example.mygymstatsapp.ui.onboarding.OnboardingScreen
import com.example.mygymstatsapp.utils.formatClock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppViewModel(
    workoutRepository: WorkoutRepository,
    preferencesRepository: UserPreferencesRepository,
    restTimer: RestTimer,
) : ViewModel() {
    val activeWorkout: StateFlow<ActiveWorkoutHeader?> = workoutRepository.observeActiveWorkoutHeader()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val restTimerState: StateFlow<RestTimer.State> = restTimer.state

    /** null while loading, to avoid flashing the onboarding screen. */
    val onboardingCompleted: StateFlow<Boolean?> = preferencesRepository.profile
        .map { it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** Root scaffold: bottom navigation + "workout in progress" banner + navigation graph. */
@Composable
fun GymApp(viewModel: AppViewModel = viewModel(factory = AppViewModelProvider.Factory)) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val activeWorkout by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val restTimer by viewModel.restTimerState.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    when (onboardingCompleted) {
        null -> {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
            return
        }
        false -> {
            OnboardingScreen()
            return
        }
        true -> Unit
    }

    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination.isOn(it.route) }
    val showBottomBar = currentTopLevel != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                Column {
                    AnimatedVisibility(
                        visible = activeWorkout != null,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        activeWorkout?.let { workout ->
                            ActiveWorkoutBanner(
                                workout = workout,
                                restTimer = restTimer,
                                onClick = { navController.navigate(ActiveWorkoutRoute) { launchSingleTop = true } },
                            )
                        }
                    }
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        TopLevelDestination.entries.forEach { item ->
                            NavigationBarItem(
                                selected = item == currentTopLevel,
                                onClick = { navController.navigateToTopLevel(item.route) },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        GymNavHost(
            navController = navController,
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
        )
    }
}

private fun NavDestination?.isOn(route: Any): Boolean =
    this?.hierarchy?.any { it.hasRoute(route::class) } == true

@Composable
private fun ActiveWorkoutBanner(
    workout: ActiveWorkoutHeader,
    restTimer: RestTimer.State,
    onClick: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    workout.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.banner_workout_in_progress, formatClock(rememberElapsedSeconds(workout.startedAt))),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (restTimer.isActive) {
                val now = rememberRestClock(restTimer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Timer, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text(
                        formatClock((restTimer.remainingMs(now) + 999) / 1000),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.banner_open_workout))
        }
    }
}
