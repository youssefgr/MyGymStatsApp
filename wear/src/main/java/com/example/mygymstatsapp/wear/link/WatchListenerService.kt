package com.example.mygymstatsapp.wear.link

import androidx.wear.tiles.TileService
import com.example.mygymstatsapp.wear.container
import com.example.mygymstatsapp.wear.tile.FonteTileService
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

/**
 * Receives what the phone publishes, also when the watch app is closed: the mirrored state stays
 * current and the tile is refreshed.
 */
class WatchListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        container.link.onDataChanged(dataEvents)
        TileService.getUpdater(this).requestUpdate(FonteTileService::class.java)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        container.link.onMessage(messageEvent)
    }
}
