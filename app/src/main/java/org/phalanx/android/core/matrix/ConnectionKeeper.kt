package org.phalanx.android.core.matrix

import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.matrix.rustcomponents.sdk.Client
import org.matrix.rustcomponents.sdk.ClientException
import org.matrix.rustcomponents.sdk.SendQueueRoomErrorListener
import org.matrix.rustcomponents.sdk.SyncService
import org.matrix.rustcomponents.sdk.SyncServiceState
import org.matrix.rustcomponents.sdk.SyncServiceStateObserver
import org.matrix.rustcomponents.sdk.use
import org.phalanx.android.data.model.ConnectionState
import org.phalanx.android.data.repository.ConnectionRepository

/**
 * Follows the sync service while the app is open and gets things moving again after a drop:
 *
 * - The SDK disables a room's send queue on a network error and never enables it again by itself,
 *   so every message after a drop would sit there failed. Queues are enabled again as soon as sync
 *   is back, and, a few times with growing waits, when a room's queue stops while still online.
 * - A sync that stopped on an error is started again, a few times with growing waits, then only
 *   when the user asks with [reconnect]. Losing the network itself is the SDK's offline mode's job.
 */
@Singleton
internal class ConnectionKeeper @Inject constructor(private val active: ActiveClient) : ConnectionRepository {
    private val mutableState = MutableStateFlow(ConnectionState.Connecting)
    override val state: StateFlow<ConnectionState> = mutableState.asStateFlow()

    private val retryNow = Channel<Unit>(Channel.CONFLATED)

    // How often sync was started again since it last ran, and each room's queue enabled again.
    @Volatile
    private var syncRestarts = 0
    private val queueRestarts = ConcurrentHashMap<String, Int>()

    override fun reconnect() {
        retryNow.trySend(Unit)
    }

    /** Runs for as long as sync does; [SyncController] owns the lifetime. */
    suspend fun run() {
        val client = active.client() ?: return
        val service = active.syncService.value ?: return
        try {
            watch(client, service)
        } finally {
            mutableState.value = ConnectionState.Connecting
        }
    }

    private suspend fun watch(client: Client, service: SyncService) {
        coroutineScope {
            launch { restartStoppedQueues(client) }
            // Each state's work runs until the next state comes in, which cancels it.
            service.states().collectLatest { sdkState ->
                mutableState.value = sdkState.toConnection()
                when (sdkState) {
                    SyncServiceState.RUNNING -> {
                        syncRestarts = 0
                        queueRestarts.clear()
                        client.enableAllSendQueues(true)
                    }
                    SyncServiceState.ERROR -> restartSyncWithBackoff()
                    SyncServiceState.OFFLINE -> restartSyncWhenAsked()
                    SyncServiceState.IDLE, SyncServiceState.TERMINATED -> Unit
                }
            }
        }
    }

    /** Once the retries are spent, only the user's [reconnect] tries again. */
    private suspend fun restartSyncWithBackoff() {
        while (true) {
            val wait = SYNC_RETRY_MILLIS.getOrNull(syncRestarts++)
            val asked = if (wait == null) {
                retryNow.receive()
                true
            } else {
                withTimeoutOrNull(wait) { retryNow.receive() } != null
            }
            if (asked) syncRestarts = 0
            active.startSync()
        }
    }

    // Offline mode already tries to reach the server again by itself; this only lets the user hurry it.
    private suspend fun restartSyncWhenAsked() {
        while (true) {
            retryNow.receive()
            active.startSync()
        }
    }

    private suspend fun restartStoppedQueues(client: Client) = coroutineScope {
        client.stoppedQueues().collect { roomId ->
            val attempt = queueRestarts.merge(roomId, 1) { old, one -> old + one } ?: 1
            val wait = QUEUE_RETRY_MILLIS.getOrNull(attempt - 1) ?: return@collect
            launch {
                delay(wait)
                // Offline, the whole set is enabled again once sync is back.
                if (mutableState.value == ConnectionState.Online) {
                    active.room(roomId)?.use { it.enableSendQueue(true) }
                }
            }
        }
    }

    private fun SyncService.states() = callbackFlow {
        val handle = state(
            object : SyncServiceStateObserver {
                override fun onUpdate(state: SyncServiceState) {
                    trySend(state)
                }
            }
        )
        awaitClose {
            handle.cancel()
            handle.destroy()
        }
    }

    private fun Client.stoppedQueues() = callbackFlow {
        val handle = subscribeToSendQueueStatus(
            object : SendQueueRoomErrorListener {
                override fun onError(roomId: String, error: ClientException) {
                    trySend(roomId)
                }
            }
        )
        awaitClose {
            handle.cancel()
            handle.destroy()
        }
    }

    private companion object {
        val SYNC_RETRY_MILLIS = listOf(2_000L, 5_000L, 15_000L, 30_000L, 60_000L)
        val QUEUE_RETRY_MILLIS = listOf(3_000L, 10_000L, 30_000L)
    }
}

private fun SyncServiceState.toConnection(): ConnectionState = when (this) {
    SyncServiceState.RUNNING -> ConnectionState.Online
    SyncServiceState.OFFLINE, SyncServiceState.ERROR -> ConnectionState.Offline
    SyncServiceState.IDLE, SyncServiceState.TERMINATED -> ConnectionState.Connecting
}
