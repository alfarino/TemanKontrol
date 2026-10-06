# Teman Kontrol - Implementation Status

Audit baseline: 6 Oktober 2026

## Phase 0 checkpoint scope

Dokumen ini adalah hasil audit awal terhadap repository dan `docs/PRD.md` v1.0. Phase 0 tidak mengubah implementasi aplikasi.

## Kondisi awal

- Repository berisi satu modul Gradle, `:app`.
- Source aplikasi masih berupa template Compose dari Android Studio.
- Paket dan identitas aplikasi masih `com.example.temankontrol`, bukan `id.temankontrol.app`.
- Hanya `MainActivity`, theme Compose default, satu unit test contoh, dan satu instrumented test contoh yang tersedia.
- Tidak ada implementasi domain, repository, datasource, ViewModel, navigation, Room, DataStore, Hilt, KSP, atau layar PRD.
- Build output lama tersedia di `app/build`, tetapi bukan bukti bahwa baseline saat ini dapat dibangun ulang.
- `docs/PRD.md` v1.0 tersedia sebagai SSOT; arsip v0.2 tersedia di `docs/archive/`.
- Tidak ada folder `.git` pada workspace yang diaudit, sehingga checkpoint commit tidak dapat dibuat pada Phase 0.

## File penting yang diaudit

| Area | File | Kondisi |
|---|---|---|
| Gradle root | `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties` | Satu modul `:app`; plugin hanya Android Application dan Kotlin Compose |
| Version catalog | `gradle/libs.versions.toml` | AGP 9.4.1, Kotlin 2.2.10, Compose BOM; dependency masih minimal |
| Modul Android | `app/build.gradle.kts` | Namespace/applicationId template; minSdk 30; Java 11; Compose aktif; belum ada Hilt/KSP/Room/DataStore/navigation |
| Manifest | `app/src/main/AndroidManifest.xml` | Activity launcher tersedia; `allowBackup=true`; belum ada splash/app class; tidak ada permission eksplisit |
| Entry point | `app/src/main/java/com/example/temankontrol/MainActivity.kt` | Menampilkan `Hello Android!`; edge-to-edge sudah dipanggil |
| Theme | `app/src/main/java/com/example/temankontrol/ui/theme/*` | Theme template dengan purple/pink dan dynamic color default |
| Resource | `app/src/main/res/values/strings.xml` | Hanya `app_name`; resource backup masih template |
| Test | `app/src/test/.../ExampleUnitTest.kt`, `app/src/androidTest/.../ExampleInstrumentedTest.kt` | Hanya smoke test template; instrumented test mengharapkan package lama |

## Requirement yang sudah ada

- Android module dengan Compose dan Material 3 dasar.
- `minSdk = 30`, `targetSdk = 37`, dan `compileSdk = 37` sesuai DEC-002.
- `enableEdgeToEdge()` sudah digunakan pada activity.
- Wrapper Gradle 9.6 tersedia.
- Test source set unit dan instrumented tersedia.
- Tidak ditemukan permission jaringan atau dependency backend pada source/config yang diaudit.

Status di atas hanya menunjukkan fondasi/template, bukan pemenuhan acceptance criteria.

## Requirement yang belum ada

### Setup dan arsitektur

- Namespace/applicationId `id.temankontrol.app` (DEC-007).
- `TemanKontrolApp` dengan `@HiltAndroidApp`, Hilt + KSP, dan module binding.
- Technical spike TS-01: Hilt + Room + KSP + kotlinx.serialization.
- `core`, `domain`, `data`, `feature`, dan `navigation` sesuai struktur paket PRD.
- StrictMode debug, splash screen, type-safe Navigation Compose, dan `kotlinx.serialization`.
- Java 17 compile options sesuai PRD; konfigurasi saat ini masih Java 11.
- BuildConfig, release R8/shrink resources, dan konfigurasi release yang ditentukan PRD.

### Domain dan data

- Semua domain model, `AppResult`, `AppError`, clock/dispatcher abstraction, dan validation VR-01 sampai VR-12.
- Interface repository pada domain layer dan kontrak Mock/Room yang sama.
- InMemory repositories, `MockScenario`, dan `SampleData`.
- Room entities, DAO, converters, indexes, foreign keys/cascade, transactions, schema export, migration test.
- Preferences DataStore untuk `notice_accepted`.
- Mapper, error mapping, sorting, overdue/past-due, dan projection summary.
- SavedStateHandle draft persistence untuk form.

### UI dan perilaku

- SCR-01 sampai SCR-09, seluruh route, bottom navigation, notice gate, dan state loading/content/empty/error/retry.
- Design system PRD dan reusable components pada §14.2.
- Microcopy C-01 sampai C-70 di `strings.xml`.
- Form state `isDirty`, `isSubmitting`, `canSubmit`, inline validation, duplicate protection, priority limit, snackbar, confirm dialog, dan unsaved-changes dialog.
- Status kunjungan, checklist pertanyaan/tindak lanjut, history filter, static suggestions, serta not-found state.
- Theme seed teal PRD, light/dark non-dynamic color, accessibility, keyboard/insets, stable lazy-list keys, dan lifecycle collection.

### Testing dan verification

- Unit tests validation/normalization/sorting/derived state/mapper/ViewModel.
- Repository and contract tests untuk InMemory dan Room.
- Compose UI tests TC-001 sampai TC-013.
- Manual device verification TC-014 sampai TC-024 pada Redmi Note 10S.
- Measurement untuk NFR-001, NFR-025 sampai NFR-028, memory leak, merged manifest, backup, dan privacy.

## Baseline verification

Command yang dijalankan:

```powershell
.\gradlew.bat test --no-daemon --console=plain
```

Hasil: **BLOCKED oleh environment/toolchain**, sebelum task test berjalan.

Error utama:

```text
Unable to download toolchain matching the requirements
({languageVersion=25, ...}) from api.foojay.io
Received status code 400: Bad Request
```

Temuan terkait:

- `gradle/gradle-daemon-jvm.properties` memaksa `toolchainVersion=25`.
- `JAVA_HOME` saat audit menunjuk `C:\Program Files\Java\jdk-1.8`.
- `java -version` menghasilkan Java 8.
- PRD meminta JBR bawaan Android Studio Java 21+ dan `compileOptions` Java 17.
- Karena Gradle gagal memilih/menyediakan JVM yang sesuai, `test`, `assembleDebug`, dan `lint` belum dapat dinilai ulang dari source saat ini.
- Instrumented test juga belum dapat dijalankan; tidak ada bukti perangkat Redmi Note 10S terhubung pada audit ini.

Klasifikasi: **A. Environment failure**, bukan regression dari perubahan Phase 0. Perbaikan toolchain harus dilakukan sebelum TS-01 dan validasi build berikutnya.

## Potensi konflik atau ketidaksesuaian

1. PRD menyebut toolchain Day 0 telah diverifikasi, tetapi environment aktif hanya memiliki Java 8 dan Gradle mencoba mengunduh Java 25. Status verifikasi Day 0 tidak dapat direproduksi pada environment ini.
2. PRD meminta compile Java 17, sedangkan `app/build.gradle.kts` memakai Java 11.
3. PRD menetapkan dynamic color tidak digunakan (DEC-008), sedangkan theme template mengaktifkan `dynamicColor = true` secara default.
4. PRD menetapkan backup dimatikan dan semua domain dikecualikan (DEC-006), sedangkan manifest masih `allowBackup="true"` dan aturan backup masih template. Ini adalah gap implementasi, bukan perubahan PRD.
5. Acceptance/functional behavior belum dapat diverifikasi karena aplikasi belum memiliki screen atau feature layer.

Tidak ditemukan konflik internal PRD yang mengharuskan keputusan baru selama audit ini. Konflik/ketidaksesuaian di atas tidak diimplementasikan pada Phase 0.

## Risiko implementasi

| Risiko | Dampak | Mitigasi pada fase berikutnya |
|---|---|---|
| Java/Gradle toolchain tidak siap | Semua build/test terblokir; TS-01 tidak dapat dimulai | Pilih JBR yang kompatibel, set `JAVA_HOME`, lalu ulangi baseline tanpa mengubah stack PRD |
| AGP 9.4.1, Kotlin 2.2.10, KSP, Hilt, dan Room belum diuji bersama | Risiko dependency incompatibility saat setup | Jalankan TS-01 sebagai gate pertama Phase 1 |
| Scope sembilan screen dan kontrak penuh dalam milestone 8 Okt | Risiko milestone frontend terlewat | Implementasikan kontrak/domain dan design system reusable lebih dulu; tetap mock-only pada Phase 1 |
| Package rename memengaruhi test, manifest, dan source path | Risiko build/test reference lama | Lakukan rename terencana sebelum feature implementation |
| Backup/privacy belum dikonfigurasi | Risiko melanggar NFR-014/NFR-034 | Perbaiki pada setup Phase 1 dan verifikasi merged manifest |
| Tidak ada repository abstraction saat ini | Risiko feature layer langsung terikat datasource | Bangun interface domain sebelum screen dan ViewModel |

## Phase 0 outcome

**Status: PASS — Blocker Resolved & Baseline Verified.**

- Blocker environment telah diperbaiki: `JAVA_HOME` diarahkan permanen ke JBR Android Studio 25 (`C:\Program Files\Android\Android Studio1\jbr`).
- Repository Git telah diinisialisasi (`git init -b main`).
- Perangkat pengujian ditetapkan: **Android Studio Virtual Device (Emulator API 30+)** sesuai DEC-021.
- Eksekusi baseline:
  ```powershell
  .\gradlew.bat test lint --console=plain
  ```
  Hasil: **BUILD SUCCESSFUL** (33 tasks executed/up-to-date, unit test template passed, lint report generated tanpa error fatal).

## Phase 1 Progress — Setup Foundation & TS-01

**Status TS-01: PASS (Lolos Sempurna).**

1. ✅ **Package Rename & Namespace (DEC-007):** Diperbarui ke `id.temankontrol.app` di `app/build.gradle.kts`, `AndroidManifest.xml`, source tree, dan test.
2. ✅ **Konfigurasi Privasi & Backup (DEC-006, NFR-014):** `android:allowBackup="false"`, data extraction rules & backup rules mengecualikan semua domain.
3. ✅ **Java 17 & Build Environment:** `compileOptions` diatur ke Java 17, `buildConfig = true`, build layout dioptimalkan di drive `D:/.gradle-builds` untuk mencegah file-locking OneDrive.
4. ✅ **Technical Spike TS-01 Verified:**
   - Dagger Hilt 2.60 terkonfigurasi via KSP dengan `@HiltAndroidApp` dan `@AndroidEntryPoint`.
   - Room 2.6.1 + KSP dengan `room.generateKotlin = true` dan `room.schemaLocation` berhasil meng-generate entity & DAO tanpa signature clash.
   - Kotlinx Serialization + Navigation Compose terintegrasi.
   - Pustaka pendukung terpasang: DataStore Preferences, Splash Screen (`core-splashscreen`), Immutable Collections, Turbine, Coroutines Test.
   - Eksekusi verifikasi:
     ```powershell
     .\gradlew.bat assembleDebug test --console=plain
     ```
     Hasil: **BUILD SUCCESSFUL** (51 tasks executed/up-to-date, APK debug ter-generate, unit tests hijau).

## Langkah Selanjutnya (Phase 1 — Core Domain & Mock Layer):
1. **Domain Models & Validation Rules (VR-01 s/d VR-12):** Implementasi domain model di `domain/model` dan fungsi validasi murni + unit tests di `domain/validation`.
2. **Repository Interfaces (PRD §16.4):** Interface `ConsultationRepository`, `QuestionRepository`, `NoteRepository`, `FollowUpRepository`, `PreferencesRepository`, dan `SuggestionCatalog`.
3. **In-Memory Mock Repositories & Scenarios (PRD §16.4):** Implementasi mock dengan state management, sample data, dan mock scenario switcher (Normal, Kosong, Error, Lambat).
4. **Hilt Modules:** Bind mock repositories untuk pengembangan frontend Day-2.


