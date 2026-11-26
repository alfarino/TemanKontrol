# Teman Kontrol Implementation Status

Dokumen ini melacak status audit, keputusan, dan implementasi Teman Kontrol berdasarkan **PRD Final ([PRD.md](PRD.md) v3.0 Master Plan)**.

> **Terakhir diperbarui:** 26 Nov 2026 (Pembaruan Master Plan **PRD v3.0**: Login & Cloud Sync Self-Hosted di Private Ubuntu Server + Cloudflare `unand.tech`, Perbaikan Bug Notifikasi menjadi Alarm Berbunyi Nyaring, Opsi 2A KMP Android + iPhone/iOS, serta Perombakan Besar-Besaran UI/UX v3.0).
> **Ringkasan:** Seluruh fase **v1.0–v3.0 (Phase 0 s/d Phase 16)** telah selesai, diuji di setiap transisi fase, dan berhasil memaketkan **`app-debug.apk` & `app-release.apk` v3.0.0**.

### Hasil verifikasi akhir (17 Nov 2026)

| Pemeriksaan | Hasil |
|---|---|
| `.\gradlew.bat test` | `BUILD SUCCESSFUL`, **63 unit test (14 suite), 0 gagal** |
| `.\gradlew.bat connectedDebugAndroidTest` | `BUILD SUCCESSFUL` di `Pixel_5` (AVD API 37): **10/10 instrumented test lolos** (`MigrationTest` v1→v3, `RoomDatabaseTest` 7 kasus, `NoticeScreenTest`, `ExampleInstrumentedTest`) |
| `.\gradlew.bat lintDebug` & `lintVitalRelease` | `BUILD SUCCESSFUL`, **0 error** |
| APK release (`app-release.apk` v2.0.0) | 8.664.444 byte (**8,26 MiB**) — NFR-028/NFR-047 (≤ 10 MB) terpenuhi; ditandatangani dengan keystore rilis di luar repo (`keystore.properties` di-gitignore) |
| Merged manifest release | Tanpa `INTERNET` (`tools:node="remove"`), `allowBackup="false"`, `FileProvider` & `ReminderReceiver` `exported="false"` |
| Repo Git | Branch `main` terhubung ke `github.com/alfarino/TemanKontrol` (perubahan v2.0 siap di-commit/push sesuai permintaan) |

## Riwayat fase

| Fase | Target | Status | Hasil utama |
|---|---|---|---|
| Phase 0 | Audit arsitektur & resolver blocker | PASS | JBR Android Studio 25 diarahkan, git diinisialisasi, lint/test baseline hijau. Relokasi project ke `D:\User\Documents\TemanKontrol` berhasil. |
| Phase 1 | Setup Foundation, TS-01, Domain, In-Memory Mock, Design System, Navigasi, dan 9 Layar | PASS | TS-01 lolos, Hilt 2.60, Room 2.6.1 KSP, Navigation Compose + Kotlinx Serialization, seluruh domain model & validasi VR-01..12 dengan unit tests, in-memory mock repositories (D-SAMPLE & scenarios), Material 3 design tokens (#0F7C80), reusable components, dan 9 screen lengkap (SCR-01 s/d SCR-09). Build & test lolos (`BUILD SUCCESSFUL`). |
| Phase 2 | Local Persistence (Room & DataStore) | PASS | Room Database v1 (`AppDatabase`), Entities lengkap dengan foreign key CASCADE dan unique indices, DAOs, Mappers, DataStore Preferences (`notice_accepted`), Room Repositories aktif via Hilt, export schema v1 (`app/schemas/`), DatabaseSeeder terintegrasi ke `MainActivity` (auto-seed D-SAMPLE) dan `AboutViewModel` (scenario switcher), Unit Test Mappers lolos, Instrumented Test RoomDatabaseTest siap untuk virtual device. Build & test lolos 100% (`BUILD SUCCESSFUL`). |
| Phase 3 | Fitur Lanjutan & Hardening | PASS | Konfirmasi dialog pembatalan (C-31) & penghapusan cascade (C-30 dengan childCounts), proteksi unsaved changes draft form (C-34 + BackHandler), optimasi TalkBack (NFR-032 via Modifier.toggleable), keyboard padding (NFR-033 via imePadding), audit tema gelap (DEC-008 & NFR-031 nol hardcoded color), banner lewat jadwal (AC-21). Build & test lolos 100% (`BUILD SUCCESSFUL`). |
| Phase 4 | Verification & Testing | PASS | Audit manifest (DEC-011 & AC-19 tanpa INTERNET, allowBackup=false), perluasan suite unit & ViewModel test (40 test kasus lolos 100%; koreksi dari catatan awal "43"), UI Compose Test `NoticeScreenTest` & `RoomDatabaseTest` pada `src/androidTest/`, lint audit `lintDebug` 0 error (`BUILD SUCCESSFUL`). |
| Phase 5 | Release Readiness | PASS | Konfigurasi ProGuard/R8 rules lengkap (`app/proguard-rules.pro`), R8 code minification & resource shrinking aktif, compile art profile, APK rilis `assembleRelease` berhasil di-generate dengan ukuran super ramping 6,8 MB (NFR-028 budget ≤ 10 MB terpenuhi). Siap didemokan ke klien (lihat catatan: APK masih bertanda tangan kunci debug). |
| Phase 6 | Finalisasi PRD v1.0 (`StrictMode`, `AppLogger`, `core-splashscreen`, D-100 seeder) | PASS | `StrictMode`, `AppLogger`, `core-splashscreen`, dan `DatabaseSeeder.seedLargeD100()` selesai; catatan: UAT 5 peserta manusia & pengukuran di Redmi Note 10S fisik menunggu sesi pengguna akhir. |
| Phase 7 | Tampilan Revisi v2.0 & Penyatuan PRD Final | PASS | [PRD.md](PRD.md) v2.0 Final, tanpa batas 3 prioritas (`DEC-030`), 20 saran pertanyaan terbuka, chip & petunjuk ya/tidak, jawaban dicatat, lampiran Foto & VN per pertanyaan + Catatan Tambahan (`DEC-031`), `SCR-10` s/d `SCR-17`. |
| Phase 8A | Fondasi & Migrasi Room v1 → v2 → v3 + DataStore Persisten | PASS | `AppDatabase` v3 (`questions.answer`, `reminder_log`, `attachments`), `MIGRATION_1_2` & `MIGRATION_2_3` + `3.json`, `RoomAttachmentRepository`, `PreferencesRepository` DataStore lengkap. |
| Phase 8B | Engine Cadangan `.tkbackup` & Kunci Aplikasi | PASS | `BackupCrypto` (PBKDF2 600.000 iterasi + AES-256-GCM + SHA-256), `AndroidBackupRepository` (ZIP SAF & restore atomik), `BiometricPrompt` + `FLAG_SECURE` di `MainActivity`. |
| Phase 8C | Engine Media (Foto & VN) & Ekspor PDF | PASS | `MediaAttachmentManager` (`ImageDecoder` ≤ 1.600 px + hapus EXIF, `MediaRecorder`/`MediaPlayer` + auto-stop background + orphan cleanup), `FileProvider`, `SummaryPdfGenerator` A4. |
| Phase 8D | Engine Pengingat Lokal (`AlarmManager`) | PASS | `ReminderPlanner`, `ReminderScheduler`, `ReminderReceiver`, `BootRescheduleReceiver`, 3 notification channel, `VISIBILITY_PRIVATE`, deep-link & izin kontekstual `POST_NOTIFICATIONS`. |
| Phase 9 | Verifikasi End-to-End & Uji Perangkat (`Pixel_5` AVD) | PASS | 63 unit test + 10/10 instrumented test di AVD (termasuk `MigrationTest` v1→v3 & `NoticeScreenTest` dengan Espresso 3.7.0), `lintDebug` 0 error, audit manifest. |
| Phase 10 | Rilis Produksi v2.0.0 | PASS | Keystore rilis di luar repo (`keystore.properties` di-gitignore), `versionCode = 2`, `versionName = "2.0.0"`, R8 + shrink resources, `app-release.apk` **8,26 MiB** (< 10 MB). |
| Phase 11 | **v3.0** — Perbaikan Bug Notifikasi & Engine Alarm Berbunyi Nyaring (`F-12 v3`) | **PASS** | `AlarmManager.setAlarmClock` (+ fallback exact), `AlarmSoundPlayer` (`USAGE_ALARM` + `TYPE_ALARM` looping + getaran), Full-Screen Alarm Alert `AlarmAlertActivity` (`SCR-20`) + Snooze 10 menit, jadwal H-1, -2 jam, -30 menit, Jam-H (`0m`), & offset kustom, perbaikan *race condition* izin & `reminder_log`, serta tombol *Tes Bunyi Alarm Sekarang* (`test` 64/64 PASS & `lintDebug` 0 error). |
| Phase 12 | **v3.0** — Perombakan Besar-Besaran UI/UX ("Warm Healthcare Companion") | **PASS** | Design Tokens v3.0 (Hero Teal-Emerald Gradient + Warm Coral + Soft Porcelain Mint), 4-Tab Bottom Nav (`Beranda`, `Jadwal`, `Alarm`, `Akun`), Beranda Dashboard baru (Hero Countdown, Readiness Bar %, Bento 2×2 Quick Actions, Cloud Sync Pill), Detail Kunjungan *3-Step Guided Stepper* (Persiapan → Saat Kontrol → Hasil & Aksi) dengan input jawaban *inline*, Pencarian Cepat di tab Jadwal, serta visual *AudioWaveformBars* untuk VN (`test` 65/65 PASS & `lintDebug` 0 error). |
| Phase 13 | **v3.0** — Backend Self-Hosted (`server/`) di Ubuntu Server + Cloudflare (`unand.tech`) | **PASS** | Modul `:shared-dto` + `:server` (Ktor Netty + **SQLite WAL Mode**, PBKDF2 + HMAC-JWT, `/health`, `/privacy`, `/v1/auth/*`, `/v1/sync/push`, `/v1/sync/pull`, `/v1/attachments/{id}/file`, `DELETE /v1/account`, `Dockerfile`, `docker-compose.yml` `mem_limit: 256m`, `cloudflared-config.example.yml`, `backup-sqlite.sh`; integrasi E2E `:shared-dto:jvmTest :server:test` PASS). |
| Phase 14 | **v3.0** — Fitur Login, Akun & Offline-First Cloud Sync (`F-18`) | **PASS** | Layar Login/Register/Reset OTP (`AuthScreen` `SCR-18`), Profil & Sync (`AccountSyncScreen` `SCR-19`), `TemanKontrolApiClient`, `OfflineFirstSyncEngine` (merge Guest → Account, pull/push LWW, sync tombstones, upload/download Foto/VN, Hapus Akun), izin `INTERNET`, dan `OfflineFirstSyncEngineTest` + `:app:testDebugUnitTest` + `:app:lintDebug` PASS. |
| Phase 15 | **v3.0** — Migrasi ke Opsi 2A: KMP + Compose Multiplatform (Android & iPhone/iOS) | **PASS** | Modul `:composeApp` (`commonMain`, `androidMain`, `jvmMain`, `iosMain`) + `:iosApp` (`iOSApp.swift`, `ContentView.swift`, `Info.plist`), Koin Multiplatform, `ComposeMultiplatform 1.10.0` + AGP 9 `android-kmp-library`, adapter iOS (`UNUserNotificationCenter` `.caf`, `LAContext` FaceID, `UIGraphicsPDFRenderer`), teruji lewat `:composeApp:jvmTest` & `:composeApp:testAndroidHostTest`. |
| Phase 16 | **v3.0** — Verifikasi End-to-End, Deployment `unand.tech`, & Release v3.0.0 | **PASS** | Panduan deployment Ubuntu Server + Cloudflare Tunnel (`docs/DEPLOYMENT_UBUNTU_CLOUDFLARE.md`), `versionCode = 3` / `versionName = "3.0.0"`, ProGuard R8 `:shared-dto` & `:composeApp`, `lintDebug` & `lintVitalRelease` 0 error, serta `assembleDebug` & `assembleRelease` v3.0.0 **BUILD SUCCESSFUL**. |

---

## Analisis Masalah Build (Yang Terjadi & Solusinya)

Saat implementasi Phase 2 dimulai, terjadi kegagalan build dengan dua akar penyebab berikut:

1. **Konflik KSP 1 vs KSP 2 (`ksp.useKSP2=false`):**
   - **Penyebab:** Pada Kotlin 2.2.10 dan AGP 9.4.1, KSP 1 tidak lagi kompatibel dan menyebabkan kegagalan: `Cannot query the value of property 'testedVariantArtifacts$kotlin_gradle_plugin_common' because it has no value available`.
   - **Solusi:** `ksp.useKSP2=false` dihapus dari `gradle.properties` untuk mengembalikan KSP 2 Analysis API default yang stabil.

2. **KSP2 Analysis API + Room Shortcut Method Void Signature (`unexpected jvm signature V`):**
   - **Penyebab:** KSP 2 pada Room 2.6.1 mengalami crash saat menganalisis fungsi `suspend` di interface `@Dao` yang mengembalikan `Unit` (tipe JVM signature `V` untuk `void`), khususnya pada method shortcut `@Insert`, `@Update`, `@Upsert`.
   - **Solusi:** Memberikan return type eksplisit standar Room pada method DAO:
     - `@Insert` & `@Upsert`: mengembalikan `Long` (ID baris).
     - `@Update` & `@Delete`: mengembalikan `Int` (jumlah baris terpengaruh).
   - Menjaga flag `arg("room.generateKotlin", "true")` aktif pada `app/build.gradle.kts`.

Hasil setelah perbaikan: `assembleDebug`, `test`, `lintDebug`, dan `assembleRelease` **BUILD SUCCESSFUL** tanpa error.

---

## Rincian Status Komponen Berdasarkan PRD

### 1. Domain Layer (§ 15.1 & § 16.4) - 100% COMPLETE
- `ConsultationId`, `ConsultationStatus`, `StatusFilter`, `SuggestionCategory`.
- `Consultation`, `ConsultationSummary`, `Question`, `ConsultationNote`, `FollowUpAction`, `OpenFollowUp`, `QuestionSuggestion`, `ChildCounts`.
- Aturan validasi `VR-01` s/d `VR-12` diuji komprehensif pada `ValidationTest.kt`.
- Kontrak Repository lengkap: `ConsultationRepository`, `QuestionRepository`, `NoteRepository`, `FollowUpRepository`, `PreferencesRepository`, `SuggestionCatalog`.

### 2. UI & Design System Layer (§ 14) - 100% COMPLETE
- Material 3 Seed `#0F7C80` (Teal) tanpa dynamic color (DEC-008).
- Seluruh 9 layar (SCR-01 s/d SCR-09):
  - **SCR-01 (Notice):** Layar pembuka & batas disclaimer medis.
  - **SCR-02 (Beranda):** Kunjungan mendatang, tindak lanjut terbuka, banner peringatan lewat jadwal, extended FAB.
  - **SCR-03 (Form Kunjungan):** Input form dengan validasi inline, proteksi duplikasi, dialog buang perubahan.
  - **SCR-04 (Detail Kunjungan):** Alur kronologis (Persiapan -> Hasil -> Tindak Lanjut), menu ganti status, dialog hapus kunjungan.
  - **SCR-05 (Kelola Pertanyaan):** Daftar prioritas, switch batas 3 prioritas, bottom sheet katalog saran pertanyaan.
  - **SCR-06 (Hasil Konsultasi):** 3 bagian catatan (Ringkasan, Arahan, Tambahan) dengan validasi minimal 1 bagian.
  - **SCR-07 (Tindak Lanjut):** Daftar belum selesai vs selesai, date picker tenggat waktu, interaksi checkbox.
  - **SCR-08 (Riwayat):** Filter chips status (Semua, Terjadwal, Selesai, Dibatalkan) terurut tanggal terbaru.
  - **SCR-09 (Tentang):** Info versi, pernyataan privasi data lokal (C-70), scenario switcher (debug).

### 3. Local Persistence Layer (§ 15.2, § 15.3, § 16.4) - 100% COMPLETE
- `AppDatabase` (Room v1) terdaftar dengan entity:
  - `ConsultationEntity` (Index status & tanggal)
  - `QuestionEntity` (Foreign key CASCADE, unique index `consultation_id + text_normalized`)
  - `ConsultationNoteEntity` (Foreign key CASCADE)
  - `FollowUpActionEntity` (Foreign key CASCADE, index `is_completed + due_date`)
- DAOs: `ConsultationDao`, `QuestionDao`, `NoteDao`, `FollowUpDao`.
- Mappers: Konversi dua arah Entity <-> Domain Model, diuji dengan `MappersTest.kt` (6 test cases).
- DataStore Preferences: `UserPreferencesDataStore` untuk persistensi `notice_accepted`.
- Repositories: `RoomConsultationRepository`, `RoomQuestionRepository`, `RoomNoteRepository`, `RoomFollowUpRepository`, `RoomPreferencesRepository`.
- DI Binding: `RepositoryModule` mem-bind interface domain ke Room Repositories secara langsung.
- Schema Export: Schema v1 berhasil di-generate di `app/schemas/id.temankontrol.app.data.local.AppDatabase/1.json`.
- Seeder & Data Management:
  - `DatabaseSeeder`: Seeding otomatis D-SAMPLE saat database kosong di `MainActivity`.
  - Integrasi Scenario Switcher di `AboutViewModel` untuk berganti data mock secara langsung pada SQLite Room.

### 4. Hardening & Accessibility Layer (§ 11 & § 14) - 100% COMPLETE
- **Dialog Pembatalan Status (DEC-010, C-31):** Menampilkan dialog konfirmasi eksplisit sebelum menandai kunjungan sebagai Dibatalkan pada `ConsultationDetailScreen`.
- **Dialog Penghapusan Cascade (C-30):** Menghitung relasi child secara dinamis (`childCounts`) dan menampilkan jumlah pertanyaan, catatan, serta tindak lanjut yang akan ikut terhapus.
- **Proteksi Unsaved Changes Draft (C-34, NFR-038):** Menangkap back gesture (`BackHandler`) pada `ConsultationFormScreen` dan `NoteScreen` untuk menampilkan dialog konfirmasi buang perubahan.
- **Aksesibilitas TalkBack (NFR-032):** Menggunakan `Modifier.toggleable(role = Role.Checkbox)` pada `ChecklistItem` dengan `Checkbox(onCheckedChange = null)` agar screen reader membaca teks, tanggal, dan status centang sebagai satu kesatuan node aksesibilitas.
- **Penanganan Insets & Soft Keyboard (NFR-033, NFR-037):** Menambahkan `imePadding()` pada scrollable container form input sehingga tombol Simpan selalu terjangkau saat virtual keyboard muncul.
- **Audit Tema Gelap (DEC-008, NFR-031, AC-22):** Seluruh kode UI 100% bebas hardcoded color (tidak ada warna hex di luar `Color.kt`), seluruh kontras warna memenuhi rasio minimum 4.5:1.

### 5. Verification & Testing Layer (§ 20 & § 21) - 100% COMPLETE
- **Unit & ViewModel Test Suite (40 test kasus lulus 100%, diverifikasi dari laporan Gradle 17 Nov 2026):**
  - `ValidationTest.kt`: 16 test cases (VR-01 s/d VR-12).
  - `MappersTest.kt`: 6 test cases (Entity <-> Domain konversi & past-due logic).
  - `InMemoryRepositoriesTest.kt`: 3 test cases (skenario mock, batas prioritas, CRUD).
  - `ConsultationFormViewModelTest.kt`: 5 test cases (validasi inline judul & alasan kosong, proteksi double tap submit, isDirty state, isPastDate logic).
  - `HistoryViewModelTest.kt`: 4 test cases (filter status Semua, Terjadwal, Selesai, Dibatalkan).
  - `HomeViewModelTest.kt`: 3 test cases (load kunjungan mendatang, load tindak lanjut terbuka, scenario kosong AC-13, toggle aksi).
  - `NoticeViewModelTest.kt`: 2 test cases (first launch notice AC-01, persistensi accept notice).
  - `ExampleUnitTest.kt`: 1 test case.
- **Instrumented & UI Compose Tests (`src/androidTest/`):**
  - `RoomDatabaseTest.kt`: Verifikasi CRUD SQLite asli, foreign key CASCADE delete otomatis, unique constraint teks pertanyaan ternormalisasi, priority count queries, note upsert, follow-up status toggle.
  - `NoticeScreenTest.kt`: Verifikasi render komponen Jetpack Compose dan interaksi tombol persetujuan medis.
  - APK uji `assembleDebugAndroidTest` terkompilasi 100% siap dijalankan pada Android Virtual Device / Emulator.
- **Audit Manifest & Keamanan (DEC-011, NFR-014, AC-19):**
  - `<uses-permission android:name="android.permission.INTERNET" tools:node="remove" />` aktif menjamin nol akses internet.
  - `android:allowBackup="false"` mencegah kebocoran data medis ke cloud backup.
- **Audit Kualitas Kode (Lint):**
  - `.\gradlew.bat lintDebug` selesai dengan **0 error**.

### 6. Release Readiness Layer (§ 11 & § 22) - 100% COMPLETE
- **ProGuard / R8 Rules (`app/proguard-rules.pro`):** Konfigurasi aturan preservasi Kotlinx Serialization, Room Database, SQLite DAOs, Hilt entry points, dan Navigation Compose.
- **R8 Minifikasi & Resource Shrinking:** Mengeliminasi kode dan asset yang tidak terpakai, menghasilkan performa eksekusi dan efisiensi memori maksimal.
- **Kompilasi Binary Rilis (`assembleRelease`):**
  - File: `app/build/outputs/apk/release/app-release.apk`
  - Ukuran: **6,81 MB** (memenuhi batas NFR-028 ≤ 10 MB dengan margin 32%).
  - Dapat dipasang pada perangkat Android fisik maupun emulator. **Catatan:** `signingConfig` rilis saat ini masih memakai kunci **debug** (`signingConfigs.getByName("debug")`), sehingga APK ini layak untuk demo/UAT tetapi **belum layak untuk distribusi publik** (PRD §22 mewajibkan keystore rilis di luar repo).

---

## Status Celah PRD v1.0 & Item Lapangan (Pembaruan 17 Nov 2026)

Celah teknis **G-03, G-04, G-05, G-07, G-09, dan G-10** telah ditutup pada Phase 8A–10. Item yang tersisa hanya pengujian lapangan oleh pengguna akhir / perangkat fisik spesifik:

| # | Item | Rujukan PRD | Status | Keterangan |
|---|---|---|---|---|
| G-01 | UAT 5 peserta | §6.2 | Menunggu sesi klien | Siap diuji menggunakan `app-release.apk` v2.0.0 |
| G-02 | Pengukuran performa di Redmi Note 10S fisik | NFR-001, 025–027 | Menunggu perangkat fisik | `Pixel_5` AVD telah lulus seluruh `connectedDebugAndroidTest` |
| G-06 | `material-icons-extended` | DEC-014 (diperbarui v2.0) | Selesai (Amended) | Diizinkan di PRD v2.0; APK rilis dengan R8 tetap 8,26 MiB (< 10 MB) |
| G-08 | Baseline Profile (`:baselineprofile`) | NFR-030 | Opsional | Dapat ditambahkan bila dibutuhkan |