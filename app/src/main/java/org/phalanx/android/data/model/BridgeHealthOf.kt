package org.phalanx.android.data.model

/**
 * How a bridge stands, from the last thing its bot said in the chat with it. mautrix bots say so
 * when a login succeeds, ends or breaks; nothing is sent to ask, so checking costs nothing.
 */
fun healthOf(lastBotMessage: String?): BridgeHealth {
    val text = lastBotMessage?.lowercase() ?: return BridgeHealth.Unknown
    return when {
        BROKEN.any { it in text } -> BridgeHealth.LoggedOut
        WORKING.any { it in text } -> BridgeHealth.Connected
        else -> BridgeHealth.Unknown
    }
}

/** How the bridge whose bot chat is [botChat] stands, from the bot's latest message there. */
fun healthIn(botChat: RoomSummary?): BridgeHealth =
    healthOf((botChat?.takeUnless { it.previewIsOwn }?.preview as? MessageBody.Text)?.text)

/**
 * The names a [Bridge]'s bot goes by on the homeserver, tried in order: those deployments
 * commonly use for the networks the app can sign in to, mautrix's default for the rest.
 */
val Bridge.botLocalparts: List<String>
    get() = BridgeNetwork.entries.firstOrNull { it.name == name }?.botLocalparts
        ?: localparts.map { it + "bot" }

// Checked first: "you're not logged in" contains "logged in".
private val BROKEN = listOf(
    "not logged in",
    "logged out",
    "log in again",
    "login again",
    "disconnected",
    "connection lost",
    "lost connection",
    "session expired",
    "bad_credentials",
    "bad credentials",
    "unknown_error",
    "please re-login",
    "relogin",
    "re-login"
)

private val WORKING = listOf(
    "successfully logged in",
    "logged in as",
    "login successful",
    "connected to",
    "reconnected"
)
