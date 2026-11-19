package com.example.mygymstatsapp

import android.app.Application
import com.example.mygymstatsapp.di.AppContainer

/**
 * Application entry point. Owns the [AppContainer] (manual dependency injection),
 * so singletons such as the Room database, repositories and the rest timer
 * live for the whole process, independently of screens.
 */
class GymApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.restTimer.notifier.createChannels()
        // While the watch follows the workout, it gives the end-of-rest alert itself.
        container.restTimer.notifier.keepOnPhone = container.wearSync::isWatchFollowing
        container.wearSync.start()
    }
}
