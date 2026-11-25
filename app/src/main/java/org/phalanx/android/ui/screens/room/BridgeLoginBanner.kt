package org.phalanx.android.ui.screens.room

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.phalanx.android.R
import org.phalanx.android.data.model.Bridge
import org.phalanx.android.ui.screens.roomlist.label
import org.phalanx.android.ui.theme.Symbols

/**
 * Warns, in a chat bridged to [bridge], that the bridge is signed out, before a message is sent
 * that would never reach the network. "Sign in" opens the chat with its bot.
 */
@Composable
internal fun BridgeLoginBanner(
    bridge: Bridge?,
    onOpenBotChat: (roomId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BridgeLoginViewModel = hiltViewModel()
) {
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()
    val botChat = bridge?.let(signedOut::get)
    val motion = MaterialTheme.motionScheme
    AnimatedVisibility(
        visible = botChat != null,
        enter = expandVertically(motion.defaultSpatialSpec()) + fadeIn(motion.defaultEffectsSpec()),
        exit = shrinkVertically(motion.fastSpatialSpec()) + fadeOut(motion.fastEffectsSpec()),
        modifier = modifier
    ) {
        val colors = MaterialTheme.colorScheme
        val network = bridge?.let { stringResource(it.label) }.orEmpty()
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.errorContainer,
            contentColor = colors.onErrorContainer,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = 560.dp)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Symbols.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.bridge_signed_out, network),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                TextButton(onClick = { botChat?.let(onOpenBotChat) }) {
                    Text(stringResource(R.string.bridge_sign_in), color = colors.error)
                }
            }
        }
    }
}
