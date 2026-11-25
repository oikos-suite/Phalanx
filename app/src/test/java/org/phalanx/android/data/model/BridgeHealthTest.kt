package org.phalanx.android.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BridgeHealthTest {
    @Test
    fun `a broken login is noticed`() = listOf(
        "You're not logged in",
        "Logged out from WhatsApp, please log in again",
        "Disconnected from Signal",
        "Your session expired (BAD_CREDENTIALS)"
    ).forEach { assertEquals(it, BridgeHealth.LoggedOut, healthOf(it)) }

    @Test
    fun `a working login is noticed`() = listOf(
        "Successfully logged in as +33612345678",
        "Logged in as @lea on Discord"
    ).forEach { assertEquals(it, BridgeHealth.Connected, healthOf(it)) }

    @Test
    fun `anything else tells nothing`() {
        assertEquals(BridgeHealth.Unknown, healthOf("Scan the QR code below"))
        assertEquals(BridgeHealth.Unknown, healthOf(null))
    }

    @Test
    fun `the bot chat's latest message counts, unless the user wrote it`() {
        val bot = chat(MessageBody.Text("Logged out from WhatsApp", null, MessageBody.TextStyle.Notice), own = false)
        assertEquals(BridgeHealth.LoggedOut, healthIn(bot))
        assertEquals(BridgeHealth.Unknown, healthIn(bot.copy(previewIsOwn = true)))
        assertEquals(BridgeHealth.Unknown, healthIn(null))
    }

    @Test
    fun `a bridge's bot is looked for under the names deployments use`() {
        assertEquals(listOf("instagrambot", "metabot"), Bridge.Instagram.botLocalparts)
        assertEquals(listOf("gmessagesbot"), Bridge.GoogleMessages.botLocalparts)
    }

    private fun chat(preview: MessageBody, own: Boolean) = RoomSummary(
        id = "!bot:x", name = "WhatsApp bridge bot", isDirect = true, avatar = null, preview = preview,
        previewIsOwn = own, previewSender = null, lastActivityMillis = null, unreadCount = 0,
        highlightCount = 0, isFavourite = false
    )
}
