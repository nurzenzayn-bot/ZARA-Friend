# ZARA Friend (Android Native)

Teman ngobrol dengan persona **Zara** — santai, hangat, bahasa Indonesia sehari-hari.
Dibangun dengan Kotlin + Jetpack Compose, Room, Firebase (Auth + Firestore), dan
Gemini Developer API (Google AI Studio key). Lihat `../PLAN.md` untuk rencana lengkap.

## Status pengerjaan

- [x] **Fase 1 — Fondasi & CI**: proyek Kotlin+Compose, Hilt, Navigation, theme,
      struktur paket, `libs.versions.toml`, `.gitignore`, template secrets,
      `android-ci.yml` (lint + unit test + assembleDebug).
- [ ] Fase 2 — Data Lokal & Domain Inti (Room + repository)
- [ ] Fase 3 — Firebase Auth & Sync Riwayat
- [ ] Fase 4 — Gemini Teks + Persona Zara
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
   dan meng-upload APK debug sebagai artifact. `GEMINI_API_KEY` baru dibutuhkan sejak Fase 4;
   `google-services.json` baru dibutuhkan sejak Fase 3 (di-restore dari secret base64).

## Struktur

Struktur paket mengikuti rencana (feature-first + Clean Architecture pragmatis):
`core/` (common, ui/theme, navigation, permissions, audio), `data/`, `domain/`,
`feature/` (home, chat, history, settings), `di/`. Direktori yang belum dipakai fase ini
ditandai `.gitkeep`.
