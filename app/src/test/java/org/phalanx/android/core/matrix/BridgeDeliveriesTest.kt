package org.phalanx.android.core.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.phalanx.android.data.model.BridgeDelivery
import org.phalanx.android.data.model.MessageBody
import org.phalanx.android.data.model.ReplyPreview
import org.phalanx.android.data.model.SendState
import org.phalanx.android.data.model.TimelineEntry

class BridgeDeliveriesTest {
    @Test
    fun `a bridged receipt marks the message it sits on and those before it`() {
        val marked = listOf(
            own("1"),
            own("2", readBy = setOf("@whatsapp_123:x")),
            own("3")
        ).withBridgeDelivery().deliveries()
        assertEquals(listOf(BridgeDelivery.Delivered, BridgeDelivery.Delivered, null), marked)
    }

    @Test
    fun `the bridge bot's receipt counts`() =
        assertEquals(
            listOf(BridgeDelivery.Delivered),
            listOf(own("1", readBy = setOf("@signalbot:x"))).withBridgeDelivery().deliveries()
        )

    @Test
    fun `a Matrix user's receipt says nothing about the bridge`() =
        assertNull(listOf(own("1", readBy = setOf("@ana:x"))).withBridgeDelivery().deliveries().single())

    @Test
    fun `the bot's error notice marks the message it answers, over a later receipt`() {
        val notice = message(
            "n", senderId = "@whatsappbot:x", own = false,
            body = MessageBody.Text("⚠️ Your message was not bridged: too large", null, MessageBody.TextStyle.Notice),
            replyTo = ReplyPreview(null, null, eventId = "\$1"),
            readBy = setOf("@whatsapp_123:x")
        )
        val marked = listOf(own("1"), notice).withBridgeDelivery()
        assertEquals(
            BridgeDelivery.Failed("Your message was not bridged: too large"),
            (marked[0] as TimelineEntry.Message).bridgeDelivery
        )
    }

    @Test
    fun `a notice from someone else is not a bridge error`() {
        val notice = message(
            "n", senderId = "@ana:x", own = false,
            body = MessageBody.Text("⚠ careful", null, MessageBody.TextStyle.Notice),
            replyTo = ReplyPreview(null, null, eventId = "\$1")
        )
        assertNull((listOf(own("1"), notice).withBridgeDelivery()[0] as TimelineEntry.Message).bridgeDelivery)
    }

    @Test
    fun `messages still sending and others' messages get no mark`() {
        val marked = listOf(
            message("1", own = false),
            own("2", state = SendState.Waiting),
            own("3", readBy = setOf("@telegram_9:x"))
        ).withBridgeDelivery().deliveries()
        assertEquals(listOf(null, null, BridgeDelivery.Delivered), marked)
    }

    private fun List<TimelineEntry>.deliveries() = map { (it as TimelineEntry.Message).bridgeDelivery }

    private fun own(id: String, readBy: Set<String> = emptySet(), state: SendState = SendState.Sent) =
        message(id, own = true, readBy = readBy, state = state)

    private fun message(
        id: String,
        own: Boolean,
        senderId: String = "@me:x",
        body: MessageBody = MessageBody.Text("x", null, MessageBody.TextStyle.Normal),
        replyTo: ReplyPreview? = null,
        readBy: Set<String> = emptySet(),
        state: SendState = SendState.Sent
    ) = TimelineEntry.Message(
        id = id, eventId = "\$$id", transactionId = null, senderId = senderId, senderName = "A",
        senderAvatar = null, isOwn = own, body = body, replyTo = replyTo, reactions = emptyList(),
        timestampMillis = 0, isEdited = false, sendState = state, canReply = true, canEdit = true,
        readBy = readBy
    )
}
