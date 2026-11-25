package org.phalanx.android.ui.screens.room

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.phalanx.android.data.model.Member
import org.phalanx.android.data.model.OutgoingMedia
import org.phalanx.android.data.model.TimelineEntry
import org.phalanx.android.data.repository.TimelineSession

/**
 * Sends [media] in order, with [text] as the first file's caption when it can be one, so a bridge
 * hands the network one captioned batch, which its apps group as an album, rather than a text and
 * loose files. False when any part failed.
 */
internal suspend fun TimelineSession.sendBatch(
    media: List<OutgoingMedia>,
    text: String,
    mentions: List<Member>,
    replyTo: TimelineEntry.Message?
): Boolean {
    val caption = text.takeIf { it.isNotEmpty() && fitsCaption(it, mentions) }
    val textSent = caption != null || text.isEmpty() || send(text, mentions, replyTo).isSuccess
    // Asked for in order, undispatched, so the session queues them in that order; it uploads
    // them on its own scope, which a screen left mid-way doesn't stop.
    val results = coroutineScope {
        media.mapIndexed { index, item ->
            val first = index == 0
            async(start = CoroutineStart.UNDISPATCHED) {
                sendMedia(item, caption.takeIf { first }, replyTo.takeIf { first && caption != null })
            }
        }.awaitAll()
    }
    return textSent && results.all { it.isSuccess }
}

/**
 * A caption carries no mention pills, and networks cap it below their text limit (Telegram at
 * 1,024 characters), so text with either goes as a message of its own before the files.
 */
internal fun fitsCaption(text: String, mentions: List<Member>): Boolean =
    mentions.isEmpty() && text.length <= CAPTION_LIMIT

private const val CAPTION_LIMIT = 1_024
