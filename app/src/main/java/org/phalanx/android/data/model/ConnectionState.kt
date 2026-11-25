package org.phalanx.android.data.model

/** How the account's link to its homeserver stands while the app is open. */
enum class ConnectionState {
    /** Syncing; messages go out as they are sent. */
    Online,

    /** Not syncing yet: the app is starting or was in the background. */
    Connecting,

    /** The server can't be reached. Messages wait in the send queue and go out once it can. */
    Offline
}
