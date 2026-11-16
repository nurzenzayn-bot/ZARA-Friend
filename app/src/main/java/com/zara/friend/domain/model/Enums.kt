package com.zara.friend.domain.model

/**
 * Enum domain inti. Dipakai bersama oleh data layer (Room/Firestore) dan UI.
 * Penyimpanan lokal memakai representasi TEXT — konversi dilakukan di mapper.
 */

/** Gaya sapaan persona Zara. */
enum class PersonaMode {
    AKU_KAMU,
    GUE_LO,
}

/** Tema aplikasi. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/** Status sinkronisasi lokal (Fase 3 memakai ini untuk push/pull Firestore). */
enum class SyncState {
    LOCAL_ONLY,
    PENDING,
    SYNCED,
}

/** Peran pesan dalam percakapan. */
enum class MessageRole {
    USER,
    MODEL,
}

/** Jenis konten pesan (teks, audio, atau campuran). */
enum class ContentType {
    TEXT,
    AUDIO,
    MIXED,
}

/** Status pengiriman pesan di UI. */
enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
}
