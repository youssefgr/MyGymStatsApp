package com.example.mygymstatsapp.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.mygymstatsapp.GymApplication
import com.example.mygymstatsapp.di.AppContainer
import com.example.mygymstatsapp.navigation.AppViewModel
import com.example.mygymstatsapp.ui.exercises.ExerciseEditorViewModel
import com.example.mygymstatsapp.ui.exercises.ExerciseLibraryViewModel
import com.example.mygymstatsapp.ui.history.CalendarViewModel
import com.example.mygymstatsapp.ui.history.HistoryViewModel
import com.example.mygymstatsapp.ui.history.WorkoutDetailViewModel
import com.example.mygymstatsapp.ui.home.HomeViewModel
import com.example.mygymstatsapp.ui.onboarding.OnboardingViewModel
import com.example.mygymstatsapp.ui.profile.ProfileViewModel
import com.example.mygymstatsapp.ui.programs.ProgramDetailViewModel
import com.example.mygymstatsapp.ui.programs.ProgramGeneratorViewModel
import com.example.mygymstatsapp.ui.programs.ProgramsViewModel
import com.example.mygymstatsapp.ui.progress.ExerciseProgressViewModel
import com.example.mygymstatsapp.ui.progress.RecordsViewModel
import com.example.mygymstatsapp.ui.progress.StatisticsViewModel
import com.example.mygymstatsapp.ui.workout.ActiveWorkoutViewModel
import com.example.mygymstatsapp.ui.workout.TemplateEditorViewModel
import com.example.mygymstatsapp.ui.workout.WorkoutSummaryViewModel

/** Builds every ViewModel from the [AppContainer] (manual DI). */
object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { with(container()) { AppViewModel(workoutRepository, userPreferencesRepository, restTimer) } }
        initializer { OnboardingViewModel(container().userPreferencesRepository) }
        initializer {
            with(container()) {
                HomeViewModel(
                    userPreferencesRepository,
                    workoutRepository,
                    statsRepository,
                    templateRepository,
                    programRepository,
                    startWorkoutUseCase,
                )
            }
        }
        initializer { with(container()) { ProgramsViewModel(templateRepository, programRepository, startWorkoutUseCase) } }
        initializer { with(container()) { CalendarViewModel(workoutRepository, userPreferencesRepository) } }
        initializer {
            with(container()) {
                ProgramDetailViewModel(createSavedStateHandle(), programRepository, templateRepository, startWorkoutUseCase)
            }
        }
        initializer {
            with(container()) {
                ProgramGeneratorViewModel(exerciseRepository, programRepository, userPreferencesRepository, labelResolver)
            }
        }
        initializer {
            with(container()) {
                TemplateEditorViewModel(createSavedStateHandle(), templateRepository, exerciseRepository, startWorkoutUseCase)
            }
        }
        initializer {
            with(container()) {
                ExerciseLibraryViewModel(createSavedStateHandle(), exerciseRepository, userPreferencesRepository, labelResolver)
            }
        }
        initializer { ExerciseEditorViewModel(createSavedStateHandle(), container().exerciseRepository) }
        initializer {
            with(container()) {
                ActiveWorkoutViewModel(
                    workoutRepository,
                    exerciseRepository,
                    userPreferencesRepository,
                    planExercisesUseCase,
                    restTimer,
                    activeWorkoutController,
                    wearSync.liveHeartRate,
                )
            }
        }
        initializer {
            with(container()) {
                WorkoutSummaryViewModel(createSavedStateHandle(), workoutRepository, userPreferencesRepository, detectPersonalRecordsUseCase)
            }
        }
        initializer { with(container()) { HistoryViewModel(workoutRepository, userPreferencesRepository) } }
        initializer {
            with(container()) {
                WorkoutDetailViewModel(
                    createSavedStateHandle(),
                    workoutRepository,
                    userPreferencesRepository,
                    detectPersonalRecordsUseCase,
                    startWorkoutUseCase,
                )
            }
        }
        initializer {
            with(container()) { ExerciseProgressViewModel(createSavedStateHandle(), statsRepository, userPreferencesRepository) }
        }
        initializer { with(container()) { StatisticsViewModel(statsRepository, workoutRepository, userPreferencesRepository) } }
        initializer { with(container()) { RecordsViewModel(statsRepository, userPreferencesRepository) } }
        initializer { with(container()) { ProfileViewModel(userPreferencesRepository, statsRepository) } }
    }

    private fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as GymApplication).container
}
