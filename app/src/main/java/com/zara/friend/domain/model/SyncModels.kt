package com.zara.friend.domain.model

/** Pemicu sinkronisasi (PLAN 3.5.4). */
enum class SyncTrigger { APP_START, AFTER_MESSAGE, MANUAL, WORKER }

/**
 * Ringkasan hasil satu siklus sync — dipakai untuk "UI status sync dasar".
 */
data class SyncSummary(
    val pushedConversations: Int = 0,
    val pulledConversations: Int = 0,
    val remoteDeletions: Int = 0,
    val syncedAt: Long = 0L,
    val isSkipped: Boolean = false,
)

/** Status sinkronisasi yang diamati UI (PLAN Fase 3: UI status sync dasar). */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Syncing(val trigger: SyncTrigger) : SyncStatus
    data class Success(val summary: SyncSummary) : SyncStatus
    data class Error(val message: String) : SyncStatus
}
