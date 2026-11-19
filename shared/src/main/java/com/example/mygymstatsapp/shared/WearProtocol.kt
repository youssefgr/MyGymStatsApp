package com.example.mygymstatsapp.shared

import kotlinx.serialization.json.Json

/*
 * Phone <-> watch sync over the Wearable Data Layer.
 *
 * The phone owns the data (Room). It publishes two DataItems that the system keeps in sync on
 * every connected watch: the live state (workout in progress, rest timer, sessions to start)
 * and a history snapshot. The watch never writes data itself: it sends commands as messages,
 * the phone applies them, and the new state comes back through the DataItem.
 */
object WearPaths {
    /** DataItem, phone -> watch: [PhoneState]. */
    const val STATE = "/fonte/state"

    /** DataItem, phone -> watch: [WatchHistory]. */
    const val HISTORY = "/fonte/history"

    /** Message, watch -> phone: [WatchCommand]. */
    const val COMMAND = "/fonte/command"

    /** Message, phone -> watch: [PhoneEvent]. */
    const val EVENT = "/fonte/event"

    /** Prefix shared by every path, used by the listener services' intent filters. */
    const val PREFIX = "/fonte"

    /** DataMap key holding the JSON payload of a DataItem. */
    const val KEY_JSON = "json"

    /** Capabilities declared in res/values/wear.xml of each app. */
    const val CAPABILITY_PHONE = "fonte_phone"
    const val CAPABILITY_WATCH = "fonte_watch"
}

/** One JSON configuration for both sides: unknown fields are ignored so either app can be updated first. */
val WearJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    classDiscriminator = "type"
}

fun WatchCommand.encode(): ByteArray = WearJson.encodeToString(WatchCommand.serializer(), this).encodeToByteArray()

fun decodeWatchCommand(bytes: ByteArray): WatchCommand? =
    runCatching { WearJson.decodeFromString(WatchCommand.serializer(), bytes.decodeToString()) }.getOrNull()

fun PhoneEvent.encode(): ByteArray = WearJson.encodeToString(PhoneEvent.serializer(), this).encodeToByteArray()

fun decodePhoneEvent(bytes: ByteArray): PhoneEvent? =
    runCatching { WearJson.decodeFromString(PhoneEvent.serializer(), bytes.decodeToString()) }.getOrNull()

fun PhoneState.toJson(): String = WearJson.encodeToString(PhoneState.serializer(), this)

fun decodePhoneState(json: String): PhoneState? =
    runCatching { WearJson.decodeFromString(PhoneState.serializer(), json) }.getOrNull()

fun WatchHistory.toJson(): String = WearJson.encodeToString(WatchHistory.serializer(), this)

fun decodeWatchHistory(json: String): WatchHistory? =
    runCatching { WearJson.decodeFromString(WatchHistory.serializer(), json) }.getOrNull()
