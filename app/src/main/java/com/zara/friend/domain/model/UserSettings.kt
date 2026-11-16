package com.zara.friend.domain.model

/** Nilai default pengaturan (baris tunggal user_settings). */
object UserSettingsDefaults {
    val themeMode: ThemeMode = ThemeMode.SYSTEM
    val personaMode: PersonaMode = PersonaMode.AKU_KAMU
    const val voiceAutoPlay: Boolean = true
    const val syncEnabled: Boolean = true
}

/**
 * Pengaturan pengguna (lokal, disimpan di Room table user_settings).
 */
data class UserSettings(
    val themeMode: ThemeMode = UserSettingsDefaults.themeMode,
    val personaMode: PersonaMode = UserSettingsDefaults.personaMode,
    val voiceAutoPlay: Boolean = UserSettingsDefaults.voiceAutoPlay,
    val syncEnabled: Boolean = UserSettingsDefaults.syncEnabled,
)
