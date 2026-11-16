package com.zara.friend.data.mapper

import com.zara.friend.data.local.entity.ConversationEntity
import com.zara.friend.data.local.entity.MessageEntity
import com.zara.friend.data.local.entity.SettingsEntity
import com.zara.friend.domain.model.Conversation
import com.zara.friend.domain.model.ContentType
import com.zara.friend.domain.model.Message
import com.zara.friend.domain.model.MessagePart
import com.zara.friend.domain.model.MessageRole
import com.zara.friend.domain.model.MessageStatus
import com.zara.friend.domain.model.PersonaMode
import com.zara.friend.domain.model.SyncState
import com.zara.friend.domain.model.ThemeMode
import com.zara.friend.domain.model.UserSettings

/**
 * Mapper Entity <-> Domain. Semua konversi enum dari TEXT dilakukan di sini
 * dengan fallback aman (data lama/rusak tidak melempar exception).
 */

// ---------- Conversation ----------

fun ConversationEntity.toDomain(): Conversation = Conversation(
    id = id,
    title = title,
    personaMode = personaMode.toPersonaMode(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
    syncState = syncState.toSyncState(),
    lastSyncedAt = lastSyncedAt,
)

fun Conversation.toEntity(): ConversationEntity = ConversationEntity(
    id = id,
    title = title,
    personaMode = personaMode.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isArchived = isArchived,
    syncState = syncState.name,
    lastSyncedAt = lastSyncedAt,
)

// ---------- Message ----------

fun MessageEntity.toDomain(): Message = Message(
    id = id,
    conversationId = conversationId,
    role = role.toMessageRole(),
    parts = buildParts(this),
    createdAt = createdAt,
    status = status.toMessageStatus(),
    errorMessage = errorMessage,
)

/**
 * Domain -> Entity. contentText / audio fields / contentType diturunkan
 * dari daftar [MessagePart] agar satu sumber kebenaran.
 */
fun Message.toEntity(): MessageEntity {
    val textPart = parts.filterIsInstance<MessagePart.TextPart>()
        .joinToString(separator = "\n") { it.text }
        .takeIf { it.isNotBlank() }
    val audioPart = parts.filterIsInstance<MessagePart.AudioPart>().firstOrNull()

    val contentType = when {
        textPart != null && audioPart != null -> ContentType.MIXED
        audioPart != null -> ContentType.AUDIO
        else -> ContentType.TEXT
    }

    return MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        contentText = textPart,
        contentType = contentType.name,
        audioLocalPath = audioPart?.localPath,
        audioMimeType = audioPart?.mime,
        audioDurationMs = audioPart?.durationMs,
        createdAt = createdAt,
        status = status.name,
        errorMessage = errorMessage,
    )
}

private fun buildParts(entity: MessageEntity): List<MessagePart> {
    val parts = mutableListOf<MessagePart>()

    // content_text bisa berupa isi teks atau transkrip — tetap tampilkan.
    if (!entity.contentText.isNullOrBlank()) {
        parts += MessagePart.TextPart(entity.contentText)
    }

    entity.audioLocalPath?.let { path ->
        parts += MessagePart.AudioPart(
            localPath = path,
            mime = entity.audioMimeType ?: "audio/wav",
            durationMs = entity.audioDurationMs,
        )
    }

    return parts
}

// ---------- Settings ----------

fun SettingsEntity.toDomain(): UserSettings = UserSettings(
    themeMode = themeMode.toThemeMode(),
    personaMode = personaMode.toPersonaMode(),
    voiceAutoPlay = voiceAutoPlay,
    syncEnabled = syncEnabled,
)

fun UserSettings.toEntity(): SettingsEntity = SettingsEntity(
    id = com.zara.friend.data.local.entity.SettingsEntity.SINGLE_ROW_ID,
    themeMode = themeMode.name,
    personaMode = personaMode.name,
    voiceAutoPlay = voiceAutoPlay,
    syncEnabled = syncEnabled,
    anonymousUid = null, // di-manage via SettingsDao.updateAnonymousUid
)

/** Default yang aman bila belum ada baris settings. */
fun SettingsEntity?.toDomainOrDefault(): UserSettings =
    this?.toDomain() ?: UserSettings()

// ---------- Enum helpers (aman terhadap nilai tak dikenal) ----------

fun String.toPersonaMode(): PersonaMode =
    runCatching { PersonaMode.valueOf(this) }.getOrDefault(PersonaMode.AKU_KAMU)

fun String.toThemeMode(): ThemeMode =
    runCatching { ThemeMode.valueOf(this) }.getOrDefault(ThemeMode.SYSTEM)

fun String.toSyncState(): SyncState =
    runCatching { SyncState.valueOf(this) }.getOrDefault(SyncState.LOCAL_ONLY)

fun String.toMessageRole(): MessageRole =
    runCatching { MessageRole.valueOf(this) }.getOrDefault(MessageRole.USER)

fun String.toContentType(): ContentType =
    runCatching { ContentType.valueOf(this) }.getOrDefault(ContentType.TEXT)

fun String.toMessageStatus(): MessageStatus =
    runCatching { MessageStatus.valueOf(this) }.getOrDefault(MessageStatus.SENT)
