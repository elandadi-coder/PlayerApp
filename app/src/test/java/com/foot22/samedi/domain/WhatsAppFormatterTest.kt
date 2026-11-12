package com.foot22.samedi.domain

import com.foot22.samedi.data.local.PlayerEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsAppFormatterTest {

    @Test
    fun `parses copied whatsapp attendance list ignoring case and accents`() {
        val players = listOf(
            PlayerEntity(id = 1, name = "Karim"),
            PlayerEntity(id = 2, name = "Mehdi", nickname = "Méhdi"),
            PlayerEntity(id = 3, name = "Badr"),
            PlayerEntity(id = 4, name = "Sofiane")
        )

        val whatsappMessage = """
            Match Samedi 22h :
            1. karim ✅
            2. mehdi présent
            3. BADR
        """.trimIndent()

        val matched = WhatsAppFormatter.parseAttendanceNames(whatsappMessage, players)
        assertEquals(listOf(1L, 2L, 3L), matched.map { it.id })
    }
}
