package org.phalanx.android.ui.screens.room

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.phalanx.android.R
import org.phalanx.android.data.model.BridgeDelivery
import org.phalanx.android.data.model.SendState
import org.phalanx.android.data.model.TimelineEntry
import org.phalanx.android.ui.theme.Symbols

/**
 * The mark beside one of this account's messages: pending, waiting for the network, failed, or,
 * in a bridged chat, delivered or refused by the bridge. A sent message with nothing more to say
 * has none. [onTap] retries a failure, or explains one that retrying can't fix.
 */
@Composable
internal fun SendStatus(message: TimelineEntry.Message, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    when (message.sendState) {
        SendState.Sending -> Icon(
            Symbols.Schedule,
            contentDescription = stringResource(R.string.timeline_sending),
            tint = colors.onSurfaceVariant,
            modifier = modifier.size(14.dp)
        )
        SendState.Waiting -> Icon(
            Symbols.CloudOff,
            contentDescription = stringResource(R.string.timeline_waiting),
            tint = colors.onSurfaceVariant,
            modifier = modifier.size(14.dp)
        )
        SendState.Failed -> FailedMark(R.string.timeline_failed, onTap, modifier)
        SendState.Sent -> when (message.bridgeDelivery) {
            BridgeDelivery.Delivered -> Icon(
                Symbols.DoneAll,
                contentDescription = stringResource(R.string.timeline_bridged),
                tint = colors.primary,
                modifier = modifier.size(14.dp)
            )
            is BridgeDelivery.Failed -> FailedMark(R.string.timeline_not_bridged, onTap, modifier)
            null -> Unit
        }
    }
}

@Composable
private fun FailedMark(@StringRes description: Int, onTap: () -> Unit, modifier: Modifier) {
    Icon(
        Symbols.ErrorOutline,
        contentDescription = stringResource(description),
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier.size(16.dp).clickable(role = Role.Button, onClick = onTap)
    )
}
