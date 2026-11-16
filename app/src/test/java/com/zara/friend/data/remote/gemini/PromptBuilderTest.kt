package com.zara.friend.data.remote.gemini

import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessagePart
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    private fun msg(id: String, role: MessageRole, text: String) = Message(
        id = id,
        conversationId = "c1",
        role = role,
        parts = listOf(MessagePart.TextPart(text)),
        createdAt = 1L,
        status = MessageStatus.SENT,
    )

    @Test
    fun `systemInstruction uses aku-kamu pronouns for AKU_KAMU`() {
        val s = PromptBuilder.systemInstruction(PersonaMode.AKU_KAMU)
        assertTrue(s.contains("aku"))
        assertTrue(s.contains("kamu"))
        assertTrue(s.contains("Zara"))
    }

    @Test
    fun `systemInstruction uses gue-lo pronouns for GUE_LO`() {
        val s = PromptBuilder.systemInstruction(PersonaMode.GUE_LO)
        assertTrue(s.contains("gue"))
        assertTrue(s.contains("lo"))
    }

    @Test
    fun `buildHistory maps roles and filters blank text`() {
        val messages = listOf(
            msg("m1", MessageRole.USER, "Hai"),
            Message(
                id = "m2", conversationId = "c1", role = MessageRole.MODEL,
                parts = emptyList(), createdAt = 2L, status = MessageStatus.SENT,
            ), // no text → filtered
            msg("m3", MessageRole.MODEL, "Halo!"),
        )

        val history = PromptBuilder.buildHistory(messages)

        assertEquals(2, history.size)
        assertEquals(GeminiRole.USER, history[0].role)
        assertEquals("Hai", history[0].text)
        assertEquals(GeminiRole.MODEL, history[1].role)
        assertEquals("Halo!", history[1].text)
    }

    @Test
    fun `buildHistory applies sliding window (takeLast)`() {
        val messages = (1..25).map { i ->
            msg("m$i", if (i % 2 == 0) MessageRole.MODEL else MessageRole.USER, "pesan $i")
        }

        val history = PromptBuilder.buildHistory(messages, maxMessages = 5)

        assertEquals(5, history.size)
        assertEquals("pesan 21", history[0].text)
        assertEquals("pesan 25", history[4].text)
    }

    @Test
    fun `buildHistory trims text`() {
        val messages = listOf(msg("m1", MessageRole.USER, "  halo  "))
        assertEquals("halo", PromptBuilder.buildHistory(messages)[0].text)
    }
}
