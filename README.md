# ZARA Friend (Android Native)

Teman ngobrol dengan persona **Zara** — santai, hangat, bahasa Indonesia sehari-hari.
Dibangun dengan Kotlin + Jetpack Compose, Room, Firebase (Auth + Firestore), dan
Gemini Developer API (Google AI Studio key). Lihat `../PLAN.md` untuk rencana lengkap.

## Status pengerjaan

- [x] **Fase 1 — Fondasi & CI**: proyek Kotlin+Compose, Hilt, Navigation, theme,
      struktur paket, `libs.versions.toml`, `.gitignore`, template secrets,
      `android-ci.yml` (lint + unit test + assembleDebug).
- [x] **Fase 2 — Data Lokal & Domain Inti**: Room (conversations/messages/settings),
      DAO, repository + mapper, model domain, unit test.
- [x] **Fase 3 — Firebase Auth & Sync Riwayat**: anonymous Auth, Firestore data source,
      `SyncManager` + `SyncWorker` (WorkManager, push/pull merge last-write-wins),
      tombstone penghapusan, UI status sync di layar Pengaturan,
      aturan keamanan di `firebase/firestore.rules`.
- [x] **Fase 4 — Gemini Teks + Persona Zara**: `GeminiApiService` (API key), `PromptBuilder`
      (persona Zara, sliding window konteks), `SendTextMessageUseCase` + `ObserveConversationUseCase`,
      penyimpanan riwayat end-to-end, error/fallback. Unit test PromptBuilder & use case.
- [ ] Fase 5 — UI Chat & Navigasi MVP
- [ ] Fase 6 — Suara Gemini Native (turn-based)
- [ ] Fase 7 — Hardening, Keamanan & Rilis

## Menyiapkan lingkungan

1. Butuh JDK 17 dan Android SDK (SDK 35).
2. Salin `local.properties.example` → `local.properties`, isi `sdk.dir` dan `GEMINI_API_KEY`.
   File ini **tidak** di-commit.
3. Build lokal:
   ```bash
   ./gradlew assembleDebug
   ```
4. CI (GitHub Actions) otomatis menjalankan `lintDebug testDebugUnitTest assembleDebug`
   dan meng-upload APK debug sebagai artifact. `GEMINI_API_KEY` dibutuhkan sejak Fase 4;
   `google-services.json` di-restore dari secret `GOOGLE_SERVICES_JSON_B64` (base64).
5. **Build tanpa `google-services.json`** tetap lulus — plugin google-services hanya
   diterapkan bila file tersebut ada. Saat dijalankan di device, fitur sync akan
   gagal terkontrol (status error di layar Pengaturan) sampai file dipasang.
6. Aturan keamanan Firestore ada di `firebase/firestore.rules` (deploy via Firebase CLI
   atau Console): `users/{uid}` hanya bisa diakses `request.auth.uid == uid`.

## Struktur

Struktur paket mengikuti rencana (feature-first + Clean Architecture pragmatis):
`core/` (common, ui/theme, navigation, permissions, audio), `data/`, `domain/`,
`feature/` (home, chat, history, settings), `di/`. Direktori yang belum dipakai fase ini
ditandai `.gitkeep`.
