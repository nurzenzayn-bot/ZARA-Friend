package com.zara.friend.data.mapper

import com.zara.friend.domain.model.ContentType
import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessagePart
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SyncState
import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.data.local.entity.SettingsEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    // ---------- Conversation ----------

    @Test
    fun `conversation entity to domain maps all fields`() {
        val entity = ConversationEntity(
            id = "c1",
            title = "Ngobrol santai",
            personaMode = PersonaMode.GUE_LO.name,
            createdAt = 100L,
            updatedAt = 200L,
            isArchived = false,
            syncState = SyncState.PENDING.name,
            lastSyncedAt = null,
        )

        val domain = entity.toDomain()

        assertEquals("c1", domain.id)
        assertEquals("Ngobrol santai", domain.title)
        assertEquals(PersonaMode.GUE_LO, domain.personaMode)
        assertEquals(100L, domain.createdAt)
        assertEquals(200L, domain.updatedAt)
        assertEquals(false, domain.isArchived)
        assertEquals(SyncState.PENDING, domain.syncState)
        assertNull(domain.lastSyncedAt)
    }

    @Test
    fun `conversation domain to entity round trip is lossless`() {
        val domain = com.zara.friend.domain.model.Conversation(
            id = "c1",
            title = "T",
            personaMode = PersonaMode.AKU_KAMU,
            createdAt = 1L,
            updatedAt = 2L,
            isArchived = true,
            syncState = SyncState.SYNCED,
            lastSyncedAt = 3L,
        )

        assertEquals(domain, domain.toEntity().toDomain())
    }

    @Test
    fun `unknown persona mode falls back to AKU_KAMU`() {
        assertEquals(PersonaMode.AKU_KAMU, "BUKAN_MODE".toPersonaMode())
    }

    @Test
    fun `unknown sync state falls back to LOCAL_ONLY`() {
        assertEquals(SyncState.LOCAL_ONLY, "APAPUN".toSyncState())
    }

    // ---------- Message ----------

    @Test
    fun `text message entity maps to single text part`() {
        val entity = MessageEntity(
            id = "m1",
            conversationId = "c1",
            role = MessageRole.USER.name,
            contentText = "Hai Zara!",
            contentType = ContentType.TEXT.name,
            audioLocalPath = null,
            audioMimeType = null,
            audioDurationMs = null,
            createdAt = 10L,
            status = MessageStatus.SENT.name,
            errorMessage = null,
        )

        val message = entity.toDomain()

        assertEquals(listOf(MessagePart.TextPart("Hai Zara!")), message.parts)
        assertEquals("Hai Zara!", message.text)
        assertNull(message.audio)
        assertEquals(MessageRole.USER, message.role)
        assertEquals(MessageStatus.SENT, message.status)
    }

    @Test
    fun `audio message entity maps to audio part`() {
        val entity = MessageEntity(
            id = "m2",
            conversationId = "c1",
            role = MessageRole.MODEL.name,
            contentText = null,
            contentType = ContentType.AUDIO.name,
            audioLocalPath = "/data/audio/resp.wav",
            audioMimeType = "audio/wav",
            audioDurationMs = 1500L,
            createdAt = 20L,
            status = MessageStatus.SENT.name,
            errorMessage = null,
        )

        val message = entity.toDomain()

        assertNull(message.text)
        assertEquals(MessagePart.AudioPart("/data/audio/resp.wav", "audio/wav", 1500L), message.audio)
    }

    @Test
    fun `mixed message entity maps to text and audio parts`() {
        val entity = MessageEntity(
            id = "m3",
            conversationId = "c1",
            role = MessageRole.MODEL.name,
            contentText = "Transkrip balasan",
            contentType = ContentType.MIXED.name,
            audioLocalPath = "/data/audio/resp.wav",
            audioMimeType = "audio/wav",
            audioDurationMs = 900L,
            createdAt = 30L,
            status = MessageStatus.SENT.name,
            errorMessage = null,
        )

        val message = entity.toDomain()

        assertEquals(2, message.parts.size)
        assertEquals("Transkrip balasan", message.text)
        assertEquals("/data/audio/resp.wav", message.audio?.localPath)
    }

    @Test
    fun `mixed domain message to entity derives content type`() {
        val message = Message(
            id = "m4",
            conversationId = "c1",
            role = MessageRole.USER,
            parts = listOf(
                MessagePart.TextPart("Rekam suara"),
                MessagePart.AudioPart("/data/audio/in.wav", "audio/wav", 2000L),
            ),
            createdAt = 40L,
            status = MessageStatus.SENDING,
        )

        val entity = message.toEntity()

        assertEquals(ContentType.MIXED.name, entity.contentType)
        assertEquals("Rekam suara", entity.contentText)
        assertEquals("/data/audio/in.wav", entity.audioLocalPath)
        assertEquals(2000L, entity.audioDurationMs)
        assertEquals(MessageStatus.SENDING.name, entity.status)
    }

    @Test
    fun `audio only domain message to entity has null text`() {
        val message = Message(
            id = "m5",
            conversationId = "c1",
            role = MessageRole.MODEL,
            parts = listOf(MessagePart.AudioPart("/p.wav", "audio/wav", 500L)),
            createdAt = 50L,
            status = MessageStatus.SENT,
        )

        val entity = message.toEntity()

        assertNull(entity.contentText)
        assertEquals(ContentType.AUDIO.name, entity.contentType)
    }

    @Test
    fun `message round trip preserves parts`() {
        val message = Message(
            id = "m6",
            conversationId = "c1",
            role = MessageRole.USER,
            parts = listOf(MessagePart.TextPart("Halo apa kabar?")),
            createdAt = 60L,
            status = MessageStatus.FAILED,
            errorMessage = "network",
        )

        assertEquals(message, message.toEntity().toDomain())
    }

    // ---------- Settings ----------

    @Test
    fun `null settings entity maps to defaults`() {
        val settings = null.toDomainOrDefault()

        assertEquals(com.zara.friend.domain.model.UserSettingsDefaults.themeMode, settings.themeMode)
        assertEquals(com.zara.friend.domain.model.UserSettingsDefaults.personaMode, settings.personaMode)
        assertEquals(true, settings.voiceAutoPlay)
        assertEquals(true, settings.syncEnabled)
    }

    @Test
    fun `settings entity maps to domain`() {
        val entity = SettingsEntity(
            id = 1,
            themeMode = ThemeMode.DARK.name,
            personaMode = PersonaMode.GUE_LO.name,
            voiceAutoPlay = false,
            syncEnabled = false,
            anonymousUid = null,
        )

        val settings = entity.toDomain()

        assertEquals(ThemeMode.DARK, settings.themeMode)
        assertEquals(PersonaMode.GUE_LO, settings.personaMode)
        assertEquals(false, settings.voiceAutoPlay)
        assertEquals(false, settings.syncEnabled)
    }

    @Test
    fun `settings round trip preserves values`() {
        val settings = com.zara.friend.domain.model.UserSettings(
            themeMode = ThemeMode.LIGHT,
            personaMode = PersonaMode.GUE_LO,
            voiceAutoPlay = false,
            syncEnabled = true,
        )

        assertEquals(settings, settings.toEntity().toDomain())
    }
}
