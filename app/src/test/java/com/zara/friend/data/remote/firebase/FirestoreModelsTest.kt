package com.zara.friend.data.remote.firebase

import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.domain.model.ContentType
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreModelsTest {

    private fun conversationEntity() = ConversationEntity(
        id = "c1",
        title = "Ngobrol",
        personaMode = PersonaMode.GUE_LO.name,
        createdAt = 10L,
        updatedAt = 20L,
        isArchived = false,
        syncState = SyncState.SYNCED.name,
        lastSyncedAt = 20L,
    )

    private fun messageEntity(withAudio: Boolean = false) = MessageEntity(
        id = "m1",
        conversationId = "c1",
        role = MessageRole.MODEL.name,
        contentText = "Halo!",
        contentType = ContentType.TEXT.name,
        audioLocalPath = if (withAudio) "/cache/a.wav" else null,
        audioMimeType = if (withAudio) "audio/wav" else null,
        audioDurationMs = if (withAudio) 1_500L else null,
        createdAt = 30L,
        status = MessageStatus.SENT.name,
        errorMessage = null,
    )

    // ---------- Entity -> DTO -> map -> DTO round trip ----------

    @Test
    fun `conversation entity maps to firestore dto with audio-local fields omitted`() {
        val dto = conversationEntity().toFirestore()

        assertEquals("c1", dto.id)
        assertEquals("Ngobrol", dto.title)
        assertEquals(PersonaMode.GUE_LO.name, dto.personaMode)
        assertEquals(10L, dto.createdAt)
        assertEquals(20L, dto.updatedAt)
        assertEquals(false, dto.isArchived)

        val restored = dto.toMap().toFirestoreConversation()
        assertEquals(dto, restored)
    }

    @Test
    fun `message entity maps to dto and flags hasAudio`() {
        val withAudio = messageEntity(withAudio = true).toFirestore()
        assertTrue(withAudio.hasAudio)
        // updatedAt remote = createdAt lokal (pesan immutable).
        assertEquals(30L, withAudio.createdAt)

        val withoutAudio = messageEntity().toFirestore()
        assertEquals(false, withoutAudio.hasAudio)

        assertEquals(withAudio, withAudio.toMap().toFirestoreMessage())
    }

    @Test
    fun `conversation map with missing field returns null`() {
        val partial = mapOf(
            "id" to "c1",
            "title" to "Ngobrol",
            // personaMode hilang
            "createdAt" to 10L,
            "updatedAt" to 20L,
            "isArchived" to false,
        )

        assertNull(partial.toFirestoreConversation())
    }

    @Test
    fun `message map tolerates null contentText`() {
        val map = mapOf(
            "id" to "m1",
            "conversationId" to "c1",
            "role" to "USER",
            "contentText" to null,
            "contentType" to "TEXT",
            "createdAt" to 30L,
            "status" to "SENT",
            // hasAudio hilang → default false
        )

        val dto = map.toFirestoreMessage()

        assertEquals("m1", dto?.id)
        assertNull(dto?.contentText)
        assertEquals(false, dto?.hasAudio)
    }

    @Test
    fun `message map with missing required field returns null`() {
        val map = mapOf(
            "id" to "m1",
            "conversationId" to "c1",
            "role" to "USER",
            "contentText" to null,
            // contentType hilang
            "createdAt" to 30L,
            "status" to "SENT",
        )

        assertNull(map.toFirestoreMessage())
    }

    // ---------- Firestore -> Entity ----------

    @Test
    fun `conversation dto to entity keeps sync metadata out of remote payload`() {
        val entity = FirestoreConversation(
            id = "c1",
            title = "T",
            personaMode = "AKU_KAMU",
            createdAt = 1L,
            updatedAt = 2L,
            isArchived = true,
        ).toEntity(syncState = SyncState.SYNCED.name, lastSyncedAt = 99L)

        assertEquals(SyncState.SYNCED.name, entity.syncState)
        assertEquals(99L, entity.lastSyncedAt)
        assertEquals(true, entity.isArchived)
    }

    @Test
    fun `message dto to entity is audio-free and falls back on unknown enums`() {
        val entity = FirestoreMessage(
            id = "m1",
            conversationId = "c1",
            role = "ROLE_ANEH",
            contentText = "x",
            contentType = "TIPE_ANEH",
            createdAt = 5L,
            status = "STATUS_ANEH",
            hasAudio = true, // audio tidak di-sync walau flag true
        ).toEntity()

        assertEquals(MessageRole.USER.name, entity.role) // fallback USER
        assertEquals(ContentType.TEXT.name, entity.contentType) // fallback TEXT
        assertEquals(MessageStatus.SENT.name, entity.status) // fallback SENT
        assertNull(entity.audioLocalPath)
        assertNull(entity.audioMimeType)
        assertNull(entity.audioDurationMs)
    }
}
