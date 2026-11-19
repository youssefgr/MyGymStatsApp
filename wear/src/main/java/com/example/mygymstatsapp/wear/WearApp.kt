package com.example.mygymstatsapp.wear

import android.app.Application
import android.content.Context
import com.example.mygymstatsapp.wear.link.PhoneLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Watch app entry point. The watch keeps no database: everything comes from the phone through
 * [PhoneLink], so the container is small.
 */
class WearApp : Application() {

    lateinit var container: WatchContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = WatchContainer(this)
    }
}

class WatchContainer(context: Context) {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val link: PhoneLink = PhoneLink(context.applicationContext, scope)

    /** Latest heart rate measured during the workout (WorkoutService), null when not measuring. */
    val heartRate = MutableStateFlow<Int?>(null)
}

val Context.container: WatchContainer get() = (applicationContext as WearApp).container
