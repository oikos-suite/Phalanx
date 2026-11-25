package org.phalanx.android.ui.screens.room

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.phalanx.android.R
import org.phalanx.android.data.model.BridgeDelivery
import org.phalanx.android.data.model.MessageBody
import org.phalanx.android.data.model.SendFailure
import org.phalanx.android.data.model.SendState
import org.phalanx.android.data.model.TimelineEntry

class SendProblemsTest {
    @Test
    fun `a failure that retrying fixes is retried`() {
        assertNull(sendProblemOf(failed(SendFailure.Rejected)))
        assertNull(sendProblemOf(failed(null)))
    }

    @Test
    fun `a failure that retrying can't fix is explained`() =
        assertEquals(R.string.send_problem_unverified_devices, sendProblemOf(failed(SendFailure.UnverifiedDevices)))

    @Test
    fun `a message the bridge refused is explained`() =
        assertEquals(
            R.string.send_problem_not_bridged,
            sendProblemOf(failed(null).copy(sendState = SendState.Sent, bridgeDelivery = BridgeDelivery.Failed("x")))
        )

    private fun failed(failure: SendFailure?) = TimelineEntry.Message(
        id = "1", eventId = null, transactionId = "t", senderId = "@me:x", senderName = "Me",
        senderAvatar = null, isOwn = true, body = MessageBody.Text("x", null, MessageBody.TextStyle.Normal),
        replyTo = null, reactions = emptyList(), timestampMillis = 0, isEdited = false,
        sendState = SendState.Failed, canReply = false, canEdit = false, sendFailure = failure
    )
}
