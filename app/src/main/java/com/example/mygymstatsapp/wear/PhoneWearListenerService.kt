package com.example.mygymstatsapp.wear

import com.example.mygymstatsapp.GymApplication
import com.example.mygymstatsapp.shared.WearPaths
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.launch

/**
 * Receives the watch's commands, even when the phone app is not running: the system starts the
 * process, GymApplication builds the container (which also starts publishing the state back).
 */
class PhoneWearListenerService : WearableListenerService() {

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != WearPaths.COMMAND) return
        val container = (application as GymApplication).container
        // On the application scope: the command may outlive this short-lived service.
        container.applicationScope.launch { container.wearSync.handle(event.data) }
    }
}
