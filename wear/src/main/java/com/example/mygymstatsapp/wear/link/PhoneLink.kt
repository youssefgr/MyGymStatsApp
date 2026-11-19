package com.example.mygymstatsapp.wear.link

import android.content.Context
import com.example.mygymstatsapp.shared.PhoneEvent
import com.example.mygymstatsapp.shared.PhoneState
import com.example.mygymstatsapp.shared.WatchCommand
import com.example.mygymstatsapp.shared.WatchHistory
import com.example.mygymstatsapp.shared.WearPaths
import com.example.mygymstatsapp.shared.decodePhoneEvent
import com.example.mygymstatsapp.shared.decodePhoneState
import com.example.mygymstatsapp.shared.decodeWatchHistory
import com.example.mygymstatsapp.shared.encode
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Watch side of the Data Layer link. The phone owns the data: this class only mirrors what it
 * publishes ([state], [history]) and sends commands back. The DataItems are cached by the system
 * on the watch, so the last known state is available instantly, even with the phone away.
 */
class PhoneLink(context: Context, private val scope: CoroutineScope) {

    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)

    private val _state = MutableStateFlow<PhoneState?>(null)
    val state: StateFlow<PhoneState?> = _state.asStateFlow()

    private val _history = MutableStateFlow<WatchHistory?>(null)
    val history: StateFlow<WatchHistory?> = _history.asStateFlow()

    /** Null until known. */
    private val _phoneReachable = MutableStateFlow<Boolean?>(null)
    val phoneReachable: StateFlow<Boolean?> = _phoneReachable.asStateFlow()

    private val _events = MutableSharedFlow<PhoneEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PhoneEvent> = _events.asSharedFlow()

    private val loadMutex = Mutex()
    private var loaded = false

    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener { info -> onCapability(info) }

    init {
        scope.launch {
            ensureLoaded()
            refreshReachability()
        }
        capabilityClient.addListener(capabilityListener, WearPaths.CAPABILITY_PHONE)
    }

    /** Reads the DataItems the system already holds (last state published by the phone). */
    suspend fun ensureLoaded() = loadMutex.withLock {
        if (loaded) return@withLock
        runCatching {
            val items = dataClient.dataItems.await()
            try {
                items.forEach { onDataItem(it) }
            } finally {
                items.release()
            }
            loaded = true
        }
    }

    suspend fun refreshReachability() {
        runCatching {
            onCapability(capabilityClient.getCapability(WearPaths.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE).await())
        }.onFailure { _phoneReachable.value = false }
    }

    /** Called by WatchListenerService. */
    fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) onDataItem(event.dataItem)
        }
    }

    /** Called by WatchListenerService. */
    fun onMessage(event: MessageEvent) {
        if (event.path != WearPaths.EVENT) return
        decodePhoneEvent(event.data)?.let { _events.tryEmit(it) }
    }

    /** Sends a command to the phone; false if no phone with Fonte is reachable. */
    suspend fun send(command: WatchCommand): Boolean {
        val node = phoneNode() ?: run {
            _phoneReachable.value = false
            return false
        }
        return runCatching { messageClient.sendMessage(node.id, WearPaths.COMMAND, command.encode()).await() }
            .onSuccess { _phoneReachable.value = true }
            .isSuccess
    }

    fun sendAsync(command: WatchCommand, onFailure: () -> Unit = {}) {
        scope.launch { if (!send(command)) onFailure() }
    }

    private suspend fun phoneNode(): Node? = runCatching {
        val nodes = capabilityClient.getCapability(WearPaths.CAPABILITY_PHONE, CapabilityClient.FILTER_REACHABLE).await().nodes
        nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
    }.getOrNull()

    private fun onCapability(info: CapabilityInfo) {
        _phoneReachable.value = info.nodes.isNotEmpty()
    }

    private fun onDataItem(item: DataItem) {
        val path = item.uri.path ?: return
        val json = runCatching { DataMapItem.fromDataItem(item).dataMap.getString(WearPaths.KEY_JSON) }.getOrNull() ?: return
        when (path) {
            WearPaths.STATE -> decodePhoneState(json)?.let { incoming ->
                // Items can arrive out of order (cache read vs. live update): keep the newest.
                val current = _state.value
                if (current == null || incoming.publishedAt >= current.publishedAt) _state.value = incoming
            }
            WearPaths.HISTORY -> decodeWatchHistory(json)?.let { incoming ->
                val current = _history.value
                if (current == null || incoming.publishedAt >= current.publishedAt) _history.value = incoming
            }
        }
    }
}
