package com.example.mygymstatsapp.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.mygymstatsapp.R
import kotlinx.serialization.Serializable

// Type-safe Navigation Compose routes.

@Serializable data object HomeRoute

/** Sections of the Programmes tab, in display order. */
enum class ProgramsSection { PROGRAMS, CALENDAR, HISTORY }

/** [section] is a [ProgramsSection] ordinal: a plain Int keeps the route trivially serializable. */
@Serializable data class ProgramsRoute(val section: Int = 0)

@Serializable data object ProgressRoute

@Serializable data object ProfileRoute

/**
 * [pickMode]: pick exercises to add. [replaceKey]: pick one exercise to replace the item with this
 * key (template editor key or workout exercise id); the result comes back under REPLACED_EXERCISE_KEY.
 */
@Serializable data class ExerciseLibraryRoute(val pickMode: Boolean = false, val replaceKey: String? = null)

@Serializable data class ExerciseEditorRoute(val exerciseId: String? = null)

/** A session to edit ([templateId]) or to create, inside [programId] when given. */
@Serializable data class TemplateEditorRoute(val templateId: String? = null, val programId: String? = null)

@Serializable data class ProgramDetailRoute(val programId: String)

@Serializable data object ProgramGeneratorRoute

@Serializable data object ActiveWorkoutRoute

@Serializable data class WorkoutSummaryRoute(val workoutId: String)

@Serializable data class WorkoutDetailRoute(val workoutId: String)

/** Key used to return the exercises picked in the library to the previous screen. */
const val PICKED_EXERCISES_KEY = "picked_exercise_ids"

/** [replaceKey, new exercise id] returned by the library in replace mode. */
const val REPLACED_EXERCISE_KEY = "replaced_exercise"

enum class TopLevelDestination(val route: Any, @get:StringRes val labelRes: Int, val icon: ImageVector) {
    HOME(HomeRoute, R.string.nav_home, Icons.Rounded.Home),
    PROGRAMS(ProgramsRoute(), R.string.nav_programs, Icons.Rounded.CalendarViewWeek),
    PROGRESS(ProgressRoute, R.string.nav_progress, Icons.Rounded.Insights),
    PROFILE(ProfileRoute, R.string.nav_profile, Icons.Rounded.Person),
}
