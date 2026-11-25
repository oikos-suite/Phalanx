package org.phalanx.android.core.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditRulesTest {
    @Test
    fun `mautrix-whatsapp's rules are read`() {
        // As mautrix-whatsapp v26.11 publishes them: edits fully supported for 15 minutes.
        val state = """
            [
              {"type": "m.room.create", "state_key": "", "content": {}},
              {"type": "com.beeper.room_features", "state_key": "fi.mau.whatsapp",
               "content": {"id": "fi.mau.whatsapp.capabilities", "edit": 2, "edit_max_age": 900, "delete": 2}}
            ]
        """.trimIndent()
        assertEquals(EditRules(supported = true, maxAgeMillis = 900_000), editRulesOf(state))
    }

    @Test
    fun `no published rules, or no JSON, gives none`() {
        assertNull(editRulesOf("""[{"type": "m.room.name", "content": {"name": "x"}}]"""))
        assertNull(editRulesOf("not json"))
    }

    @Test
    fun `a bridge that leaves edit out refuses edits`() =
        assertEquals(EditRules(supported = false, maxAgeMillis = null), editRulesOf(state("""{"delete": 2}""")))

    @Test
    fun `the window closes after its age`() {
        val rules = EditRules(supported = true, maxAgeMillis = 900_000)
        assertTrue(rules.allow(sentAtMillis = 0, nowMillis = 900_000))
        assertFalse(rules.allow(sentAtMillis = 0, nowMillis = 900_001))
        assertTrue(EditRules(supported = true, maxAgeMillis = null).allow(0, Long.MAX_VALUE))
        assertFalse(EditRules(supported = false, maxAgeMillis = null).allow(0, 0))
    }

    private fun state(content: String) = """[{"type": "com.beeper.room_features", "content": $content}]"""
}
