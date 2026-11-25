package org.phalanx.android.core.matrix

import org.phalanx.android.data.model.BridgeDelivery
import org.phalanx.android.data.model.MessageBody
import org.phalanx.android.data.model.SendState
import org.phalanx.android.data.model.TimelineEntry

/**
 * Marks this account's sent messages with what their bridge said of them, from what the timeline
 * already holds; Matrix has no stable event for it.
 *
 * - Failed: the bridge's bot answered the message with a "⚠ Your message was not bridged" notice,
 *   which mautrix bridges send by default (`message_error_notices`).
 * - Delivered: the bot (`delivery_receipts`, off by default) or someone on the network (their read
 *   receipt, which only exists once the network has the message) read it or anything after it.
 *
 * Neither is known for most messages of a bridge left on its defaults, so they keep no mark.
 */
internal fun List<TimelineEntry>.withBridgeDelivery(): List<TimelineEntry> {
    val failures = bridgeFailures()
    var deliveredFromHere = false
    val marked = ArrayList<TimelineEntry>(size)
    for (entry in asReversed()) {
        if (entry !is TimelineEntry.Message) {
            marked.add(entry)
            continue
        }
        if (entry.readBy.any(::isBridgedAccount)) deliveredFromHere = true
        val eventId = entry.eventId
        val delivery = when {
            !entry.isOwn || entry.sendState != SendState.Sent || eventId == null -> null
            eventId in failures -> BridgeDelivery.Failed(failures.getValue(eventId))
            deliveredFromHere -> BridgeDelivery.Delivered
            else -> null
        }
        marked.add(if (delivery == entry.bridgeDelivery) entry else entry.copy(bridgeDelivery = delivery))
    }
    marked.reverse()
    return marked
}

/** The bot's error notices, by the event they answer; the newest wins. */
private fun List<TimelineEntry>.bridgeFailures(): Map<String, String> =
    filterIsInstance<TimelineEntry.Message>().mapNotNull { it.bridgeFailure() }.toMap()

private fun TimelineEntry.Message.bridgeFailure(): Pair<String, String>? {
    val target = replyTo?.eventId
    val text = (body as? MessageBody.Text)?.takeIf { it.style == MessageBody.TextStyle.Notice }?.text?.trim()
        ?.takeIf { it.startsWith(WARNING_SIGN) }
    if (target == null || text == null || !isBridgeBot(senderId)) return null
    return target to text.removePrefix(WARNING_SIGN).trimStart('\uFE0F', ' ')
}

/** Someone a bridge brings over, or its bot: "@whatsapp_…", "@whatsappbot". */
private fun isBridgedAccount(userId: String): Boolean = bridgeOf(listOf(userId)) != null

private const val WARNING_SIGN = "⚠"
