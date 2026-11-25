package org.phalanx.android.ui.screens.room

import androidx.annotation.StringRes
import org.phalanx.android.R
import org.phalanx.android.data.model.BridgeDelivery
import org.phalanx.android.data.model.SendFailure
import org.phalanx.android.data.model.TimelineEntry

/**
 * What to tell the user when they tap a message's send status, instead of sending it again:
 * null when sending again is the answer.
 */
@StringRes
internal fun sendProblemOf(message: TimelineEntry.Message): Int? = when {
    message.bridgeDelivery is BridgeDelivery.Failed -> R.string.send_problem_not_bridged
    else -> when (message.sendFailure?.takeUnless { it.retryHelps }) {
        SendFailure.UnverifiedDevices -> R.string.send_problem_unverified_devices
        SendFailure.IdentityChanged -> R.string.send_problem_identity_changed
        SendFailure.VerificationRequired -> R.string.send_problem_verification_required
        SendFailure.MediaMissing -> R.string.send_problem_media_missing
        SendFailure.SessionExpired, SendFailure.Rejected, null -> null
    }
}
