package org.phalanx.android.ui.screens.room

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import org.phalanx.android.data.model.ConnectionState
import org.phalanx.android.data.repository.ConnectionRepository

/** Whether the server can be reached, for [OfflineBanner]. */
@HiltViewModel
class ConnectionViewModel @Inject constructor(private val connection: ConnectionRepository) : ViewModel() {
    val state: StateFlow<ConnectionState> = connection.state

    fun reconnect() = connection.reconnect()
}
