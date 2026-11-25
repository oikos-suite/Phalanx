package org.phalanx.android.ui.screens.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.phalanx.android.data.model.Bridge
import org.phalanx.android.data.model.BridgeHealth
import org.phalanx.android.data.model.healthIn
import org.phalanx.android.data.repository.BridgeRepository
import org.phalanx.android.data.repository.RoomListRepository

/**
 * The bridges whose bot last said they are signed out, each with the chat with its bot, kept live:
 * the moment the bot says so, or says it is back, the room's banner follows. Nothing is asked of
 * the bot or the server; its chat is read from the room list already in memory.
 */
@HiltViewModel
class BridgeLoginViewModel @Inject constructor(
    bridges: BridgeRepository,
    roomList: RoomListRepository
) : ViewModel() {
    val signedOut: StateFlow<Map<Bridge, String>> =
        combine(flow { emit(bridges.botChats()) }, roomList.rooms) { botChats, rooms ->
            botChats.filter { (_, roomId) ->
                healthIn(rooms?.firstOrNull { it.id == roomId }) == BridgeHealth.LoggedOut
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyMap())

    private companion object {
        const val STOP_MILLIS = 5_000L
    }
}
