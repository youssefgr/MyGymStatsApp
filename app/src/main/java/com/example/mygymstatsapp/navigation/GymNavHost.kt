package com.example.mygymstatsapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.mygymstatsapp.ui.exercises.ExerciseEditorScreen
import com.example.mygymstatsapp.ui.exercises.ExerciseLibraryScreen
import com.example.mygymstatsapp.ui.history.WorkoutDetailScreen
import com.example.mygymstatsapp.ui.home.HomeScreen
import com.example.mygymstatsapp.ui.profile.ProfileScreen
import com.example.mygymstatsapp.ui.programs.ProgramDetailScreen
import com.example.mygymstatsapp.ui.programs.ProgramGeneratorScreen
import com.example.mygymstatsapp.ui.programs.ProgramsScreen
import com.example.mygymstatsapp.ui.progress.ProgressScreen
import com.example.mygymstatsapp.ui.workout.ActiveWorkoutScreen
import com.example.mygymstatsapp.ui.workout.TemplateEditorScreen
import com.example.mygymstatsapp.ui.workout.WorkoutSummaryScreen

@Composable
fun GymNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val openActiveWorkout: () -> Unit = {
        navController.navigate(ActiveWorkoutRoute) { launchSingleTop = true }
    }
    val openWorkout: (String) -> Unit = { navController.navigate(WorkoutDetailRoute(it)) }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenActiveWorkout = openActiveWorkout,
                onOpenWorkout = openWorkout,
                onOpenHistory = { navController.navigateToProgramsSection(ProgramsSection.HISTORY) },
                onOpenCalendar = { navController.navigateToProgramsSection(ProgramsSection.CALENDAR) },
                onCreateTemplate = { navController.navigate(TemplateEditorRoute()) },
            )
        }

        composable<ProgramsRoute> { entry ->
            ProgramsScreen(
                initialSection = ProgramsSection.entries.getOrElse(entry.toRoute<ProgramsRoute>().section) { ProgramsSection.PROGRAMS },
                onOpenActiveWorkout = openActiveWorkout,
                onCreateSession = { navController.navigate(TemplateEditorRoute()) },
                onEditSession = { navController.navigate(TemplateEditorRoute(it)) },
                onOpenProgram = { navController.navigate(ProgramDetailRoute(it)) },
                onGenerateProgram = { navController.navigate(ProgramGeneratorRoute) },
                onOpenLibrary = { navController.navigate(ExerciseLibraryRoute()) },
                onOpenWorkout = openWorkout,
            )
        }

        composable<ProgramDetailRoute> {
            ProgramDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenActiveWorkout = openActiveWorkout,
                onAddSession = { programId -> navController.navigate(TemplateEditorRoute(programId = programId)) },
                onEditSession = { navController.navigate(TemplateEditorRoute(it)) },
            )
        }

        composable<ProgramGeneratorRoute> {
            ProgramGeneratorScreen(
                onClose = { navController.popBackStack() },
                onCreated = { programId ->
                    navController.navigate(ProgramDetailRoute(programId)) {
                        popUpTo<ProgramGeneratorRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<ProgressRoute> { ProgressScreen() }

        composable<ProfileRoute> { ProfileScreen() }

        composable<ExerciseLibraryRoute> {
            ExerciseLibraryScreen(
                onBack = { navController.popBackStack() },
                onCreateExercise = { navController.navigate(ExerciseEditorRoute()) },
                onEditExercise = { navController.navigate(ExerciseEditorRoute(it)) },
                onExercisesPicked = { ids ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISES_KEY, ArrayList(ids))
                    navController.popBackStack()
                },
                onExerciseReplaced = { key, exerciseId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(REPLACED_EXERCISE_KEY, arrayListOf(key, exerciseId))
                    navController.popBackStack()
                },
            )
        }

        composable<ExerciseEditorRoute> {
            ExerciseEditorScreen(onDone = { navController.popBackStack() })
        }

        composable<TemplateEditorRoute> { entry ->
            val picked by entry.pickedExercises()
            val replacement by entry.replacedExercise()
            TemplateEditorScreen(
                pickedExerciseIds = picked,
                onPickedHandled = { entry.clearPickedExercises() },
                replacement = replacement,
                onReplacementHandled = { entry.clearReplacedExercise() },
                onAddExercises = { navController.navigate(ExerciseLibraryRoute(pickMode = true)) },
                onReplaceExercise = { key -> navController.navigate(ExerciseLibraryRoute(replaceKey = key)) },
                onBack = { navController.popBackStack() },
                onStarted = {
                    navController.navigate(ActiveWorkoutRoute) {
                        popUpTo<TemplateEditorRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<ActiveWorkoutRoute> { entry ->
            val picked by entry.pickedExercises()
            val replacement by entry.replacedExercise()
            ActiveWorkoutScreen(
                pickedExerciseIds = picked,
                onPickedHandled = { entry.clearPickedExercises() },
                replacement = replacement,
                onReplacementHandled = { entry.clearReplacedExercise() },
                onAddExercises = { navController.navigate(ExerciseLibraryRoute(pickMode = true)) },
                onReplaceExercise = { workoutExerciseId -> navController.navigate(ExerciseLibraryRoute(replaceKey = workoutExerciseId)) },
                onClose = { navController.popBackStack() },
                onFinished = { workoutId ->
                    navController.navigate(WorkoutSummaryRoute(workoutId)) {
                        popUpTo<ActiveWorkoutRoute> { inclusive = true }
                    }
                },
            )
        }

        composable<WorkoutSummaryRoute> {
            WorkoutSummaryScreen(onDone = { navController.popBackStack() })
        }

        composable<WorkoutDetailRoute> {
            WorkoutDetailScreen(
                onBack = { navController.popBackStack() },
                onOpenActiveWorkout = openActiveWorkout,
            )
        }
    }
}

/** Opens a section of the Programmes tab from elsewhere (no restoreState, so the asked section shows). */
private fun NavHostController.navigateToProgramsSection(section: ProgramsSection) {
    navigate(ProgramsRoute(section.ordinal)) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
    }
}

fun NavHostController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun NavBackStackEntry.pickedExercises() =
    savedStateHandle.getStateFlow<ArrayList<String>?>(PICKED_EXERCISES_KEY, null).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearPickedExercises() {
    savedStateHandle[PICKED_EXERCISES_KEY] = null
}

@Composable
private fun NavBackStackEntry.replacedExercise() =
    savedStateHandle.getStateFlow<ArrayList<String>?>(REPLACED_EXERCISE_KEY, null).collectAsStateWithLifecycle()

private fun NavBackStackEntry.clearReplacedExercise() {
    savedStateHandle[REPLACED_EXERCISE_KEY] = null
}
