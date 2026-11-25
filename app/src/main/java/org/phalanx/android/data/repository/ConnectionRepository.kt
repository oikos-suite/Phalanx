package org.phalanx.android.data.repository

import kotlinx.coroutines.flow.StateFlow
import org.phalanx.android.data.model.ConnectionState

interface ConnectionRepository {
    val state: StateFlow<ConnectionState>

    /** Tries to reach the server again now, rather than at the next scheduled attempt. */
    fun reconnect()
}
