package org.phalanx.android.data.model

import androidx.compose.runtime.Immutable

/** One row of a room's timeline. [id] is stable for the life of the row, including while it is still sending. */
@Immutable
sealed interface TimelineEntry {
    val id: String

    data class DayDivider(override val id: String, val millis: Long) : TimelineEntry

    data class Message(
        override val id: String,
        /** Null while a message of this device's is still being sent, which then has [transactionId]. */
        val eventId: String?,
        val transactionId: String?,
        val senderId: String,
        /** Disambiguated with the Matrix ID when another member uses the same name. */
        val senderName: String,
        val senderAvatar: MediaRef?,
        val isOwn: Boolean,
        val body: MessageBody,
        val replyTo: ReplyPreview?,
        val reactions: List<ReactionSummary>,
        val timestampMillis: Long,
        val isEdited: Boolean,
        val sendState: SendState,
        val canReply: Boolean,
        val canEdit: Boolean,
        /** The other members whose public read receipt sits on this message: it is where they stopped. */
        val readBy: Set<String> = emptySet(),
        /** Why [sendState] is [SendState.Failed]; null otherwise. */
        val sendFailure: SendFailure? = null,
        /** For this account's sent messages in a bridged room: what the bridge said of it, if anything. */
        val bridgeDelivery: BridgeDelivery? = null
    ) : TimelineEntry
}

/**
 * The message being answered. [senderName] and [body] are null until the SDK has fetched it, and
 * stay null with [isUnavailable] when it could not (deleted, or from before this account joined).
 */
@Immutable
data class ReplyPreview(
    val senderName: String?,
    val body: MessageBody?,
    val isUnavailable: Boolean = false,
    /** The quoted event, to jump to; null for the composer's own preview. */
    val eventId: String? = null
)

/** [senderIds] in the order they reacted. [image] when the key is a custom emote (an `mxc://` URL). */
@Immutable
data class ReactionSummary(
    val key: String,
    val count: Int,
    val byMe: Boolean,
    val senderIds: List<String>,
    val image: MediaRef? = null
)

enum class SendState {
    Sent,
    Sending,

    /** The connection dropped: the SDK holds it and sends it once the server is reachable again. */
    Waiting,
    Failed
}

/** Why a message could not be sent; [retryHelps] is false when sending it again would fail the same way. */
enum class SendFailure(val retryHelps: Boolean) {
    /** The room has devices that are not verified, which the encryption settings refuse to share with. */
    UnverifiedDevices(false),

    /** Someone verified earlier is no longer. */
    IdentityChanged(false),

    /** This session must be verified before it may send. */
    VerificationRequired(false),

    /** The sign-in expired; the SDK sends it once it has a working one again. */
    SessionExpired(true),

    /** The file to upload is gone from the cache. */
    MediaMissing(false),

    /** The server refused it. */
    Rejected(true)
}

/**
 * What a bridge said of a message it was handed. [Delivered] once the bridge, or someone on the
 * network, has a read receipt at or after it; [Failed] when its bot answered it with an error notice.
 */
@Immutable
sealed interface BridgeDelivery {
    data object Delivered : BridgeDelivery

    /** [reason] is the bot's notice, as it wrote it. */
    data class Failed(val reason: String) : BridgeDelivery
}
