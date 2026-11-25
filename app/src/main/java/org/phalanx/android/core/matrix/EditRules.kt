package org.phalanx.android.core.matrix

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * What a bridge accepts for edits in one room, as it publishes it in the room's
 * `com.beeper.room_features` state: mautrix bridges set it from the network's own rules
 * (WhatsApp and Meta 15 minutes, Signal 24 hours) and refuse an edit past [maxAgeMillis].
 */
internal data class EditRules(val supported: Boolean, val maxAgeMillis: Long?) {
    fun allow(sentAtMillis: Long, nowMillis: Long): Boolean =
        supported && (maxAgeMillis == null || nowMillis - sentAtMillis <= maxAgeMillis)
}

/**
 * The edit rules in a room's full state, given as the JSON array the client-server API returns;
 * null when no bridge published any, or the JSON can't be read.
 */
internal fun editRulesOf(stateJson: String): EditRules? =
    roomFeaturesIn(stateJson)?.let { content ->
        // mautrix's support levels: 1 partial, 2 full; left out (0) or below means edits are refused.
        val level = (content["edit"] as? JsonPrimitive)?.longOrNull ?: 0L
        val maxAgeSeconds = (content["edit_max_age"] as? JsonPrimitive)?.longOrNull?.takeIf { it > 0 }
        EditRules(level >= PARTIAL_SUPPORT, maxAgeSeconds?.let { it * MILLIS_PER_SECOND })
    }

private fun roomFeaturesIn(stateJson: String): JsonObject? {
    val events = try {
        Json.parseToJsonElement(stateJson) as? JsonArray
    } catch (_: SerializationException) {
        null
    }
    return events?.asSequence()
        ?.mapNotNull { it as? JsonObject }
        ?.firstOrNull { (it["type"] as? JsonPrimitive)?.content == ROOM_FEATURES }
        ?.get("content") as? JsonObject
}

private const val ROOM_FEATURES = "com.beeper.room_features"
private const val PARTIAL_SUPPORT = 1L
private const val MILLIS_PER_SECOND = 1_000L
