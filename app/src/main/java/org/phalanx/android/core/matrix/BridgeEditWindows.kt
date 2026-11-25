package org.phalanx.android.core.matrix

import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.phalanx.android.data.model.TimelineEntry
import org.phalanx.android.di.ApplicationScope

/**
 * The edit rules bridged rooms publish, read once per room while the app runs. The SDK only keeps
 * the state its sync asked for, which leaves this one out, so it is read with one request when a
 * bridged room opens. Large rooms are skipped, their whole state being too heavy for the battery
 * and data plan; edits there stay offered as before.
 */
@Singleton
internal class BridgeEditWindows @Inject constructor(
    private val api: ClientServerApi,
    @ApplicationScope private val scope: CoroutineScope
) {
    private val rules = ConcurrentHashMap<String, EditRules>()
    private val asked: MutableSet<String> = ConcurrentHashMap.newKeySet()

    /** Reads [roomId]'s rules in the background, unless they are known or being read. */
    fun prefetch(roomId: String, memberCount: Long) {
        if (memberCount > MAX_MEMBERS || !asked.add(roomId)) return
        scope.launch {
            val found = read(roomId)
            if (found == null) asked.remove(roomId) else rules[roomId] = found
        }
    }

    /** False once the bridge would refuse to edit [message]; true while its rules are unknown. */
    fun allow(roomId: String, message: TimelineEntry.Message, nowMillis: Long = System.currentTimeMillis()) =
        rules[roomId]?.allow(message.timestampMillis, nowMillis) ?: true

    // A failure (offline, no rights to read state) is forgotten, so the next opening asks again.
    private suspend fun read(roomId: String): EditRules? = try {
        val response = api.call("GET", "rooms/${URLEncoder.encode(roomId, "UTF-8")}/state")
        if (response.isSuccess) editRulesOf(response.body) ?: NO_RULES else null
    } catch (_: IOException) {
        null
    }

    private companion object {
        const val MAX_MEMBERS = 200L

        // A bridged room without published rules (an older bridge) refuses nothing it says.
        val NO_RULES = EditRules(supported = true, maxAgeMillis = null)
    }
}
