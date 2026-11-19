package com.example.mygymstatsapp.di

import android.content.Context
import com.example.mygymstatsapp.data.local.database.GymDatabase
import com.example.mygymstatsapp.data.preferences.UserPreferencesDataStore
import com.example.mygymstatsapp.data.repository.OfflineExerciseRepository
import com.example.mygymstatsapp.data.repository.OfflineProgramRepository
import com.example.mygymstatsapp.data.repository.OfflineStatsRepository
import com.example.mygymstatsapp.data.repository.OfflineTemplateRepository
import com.example.mygymstatsapp.data.repository.OfflineWorkoutRepository
import com.example.mygymstatsapp.domain.repository.ExerciseRepository
import com.example.mygymstatsapp.domain.repository.ProgramRepository
import com.example.mygymstatsapp.domain.repository.StatsRepository
import com.example.mygymstatsapp.domain.repository.TemplateRepository
import com.example.mygymstatsapp.domain.repository.UserPreferencesRepository
import com.example.mygymstatsapp.domain.repository.WorkoutRepository
import com.example.mygymstatsapp.domain.usecase.DetectPersonalRecordsUseCase
import com.example.mygymstatsapp.domain.usecase.PlanExercisesUseCase
import com.example.mygymstatsapp.domain.usecase.StartWorkoutUseCase
import com.example.mygymstatsapp.session.ActiveWorkoutController
import com.example.mygymstatsapp.timer.RestTimer
import com.example.mygymstatsapp.ui.LabelResolver
import com.example.mygymstatsapp.wear.PhoneWearSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container (no Hilt: fewer build plugins, explicit wiring).
 * Swapping a repository implementation (e.g. a cloud-synced one) happens here only.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Scope for work that must outlive a screen (rest timer, pending workout writes). */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: GymDatabase by lazy { GymDatabase.create(appContext) }

    val exerciseRepository: ExerciseRepository by lazy { OfflineExerciseRepository(database.exerciseDao()) }

    val templateRepository: TemplateRepository by lazy {
        OfflineTemplateRepository(database, database.templateDao())
    }

    val programRepository: ProgramRepository by lazy {
        OfflineProgramRepository(database, database.programDao(), database.templateDao())
    }

    val workoutRepository: WorkoutRepository by lazy {
        OfflineWorkoutRepository(database, database.workoutDao())
    }

    val statsRepository: StatsRepository by lazy { OfflineStatsRepository(database.statsDao()) }

    val userPreferencesRepository: UserPreferencesRepository by lazy { UserPreferencesDataStore(appContext) }

    val restTimer: RestTimer by lazy { RestTimer(appContext, applicationScope) }

    val labelResolver: LabelResolver by lazy { LabelResolver(appContext) }

    val startWorkoutUseCase: StartWorkoutUseCase by lazy {
        StartWorkoutUseCase(
            workoutRepository,
            templateRepository,
            userPreferencesRepository,
            planExercisesUseCase,
            labelResolver::defaultWorkoutName,
        )
    }

    val planExercisesUseCase: PlanExercisesUseCase by lazy {
        PlanExercisesUseCase(workoutRepository, exerciseRepository, userPreferencesRepository)
    }

    val detectPersonalRecordsUseCase: DetectPersonalRecordsUseCase by lazy {
        DetectPersonalRecordsUseCase(statsRepository)
    }

    /** The workout in progress, driven by the workout screen and by the watch. */
    val activeWorkoutController: ActiveWorkoutController by lazy {
        ActiveWorkoutController(
            workoutRepository,
            userPreferencesRepository,
            restTimer,
            detectPersonalRecordsUseCase,
            applicationScope,
        )
    }

    /** Galaxy Watch / Wear OS link: publishes the state to the watch and applies its commands. */
    val wearSync: PhoneWearSync by lazy {
        PhoneWearSync(
            appContext,
            applicationScope,
            workoutRepository,
            templateRepository,
            programRepository,
            statsRepository,
            userPreferencesRepository,
            restTimer,
            activeWorkoutController,
            startWorkoutUseCase,
            detectPersonalRecordsUseCase,
        )
    }
}
