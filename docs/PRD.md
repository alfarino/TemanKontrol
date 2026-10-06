# Product Requirements Document — Teman Kontrol

| Field | Value |
|---|---|
| Produk | Teman Kontrol |
| Dokumen | `docs/PRD.md` (Single Source of Truth) |
| Versi | **1.0** |
| Status | **Approved** — OQ-01 s/d OQ-04 telah dijawab (lihat §2 Decision Log) |
| Tanggal | 6 Oktober 2026 |
| Platform | Android only (minSdk 30 / Android 11) |
| Stack | Kotlin + Jetpack Compose + Material 3 + Room + DataStore + Hilt |
| Application ID | `id.temankontrol.app` |
| Perangkat uji utama | Android Studio Virtual Device (Emulator API 30+) |
| Deadline frontend | 8 Oktober 2026 (hard milestone) |
| Deadline MVP | 6 November 2026 |
| Arsip versi lama | `docs/archive/PRD_v0.2.md` |

---

## 0. Cara Menggunakan Dokumen Ini

### 0.1 Aturan SSOT

1. Dokumen ini adalah **satu-satunya sumber kebenaran** untuk scope, perilaku, data, arsitektur, copy UI, dan standar kualitas Teman Kontrol.
2. Jika kode, desain, atau percakapan bertentangan dengan dokumen ini, **dokumen ini yang benar** sampai dokumen diperbarui melalui change control (§0.3).
3. Hal yang tidak tercantum di sini **tidak dikerjakan**. Ide baru dicatat di §29 (Backlog) dan tidak langsung diimplementasikan.
4. Versi library dikunci di `gradle/libs.versions.toml`. File tersebut adalah SSOT untuk angka versi; dokumen ini adalah SSOT untuk **pilihan** library.
5. Setiap commit/PR yang mengimplementasikan requirement menyebut ID-nya (misal `feat(question): enforce priority limit [FR-008]`).

### 0.2 Urutan Prioritas Jika Ada Konflik Internal

`§2 Decision Log` > `§20 Acceptance Criteria` > `§15–16 Data & Kontrak` > `§10 Functional Requirements` > `§9 Feature Spec` > bagian lain.

Konflik internal yang ditemukan wajib dilaporkan dan diperbaiki di dokumen, bukan diakali di kode.

### 0.3 Change Control

Setiap perubahan pada dokumen ini:

1. Diberi entri baru di §31 Changelog (versi, tanggal, ringkasan, ID yang terdampak).
2. Jika menyangkut keputusan, tambah entri `DEC-xxx` baru di §2 (keputusan lama ditandai *Superseded*, tidak dihapus).
3. Perubahan data model wajib memperbarui §15, §16, §26 (traceability), dan menaikkan versi skema Room bila sudah ada di Phase 2+.
4. Perubahan P0 setelah 8 Oktober wajib persetujuan project lead.

### 0.4 Konvensi ID

| Prefix | Arti | Contoh |
|---|---|---|
| P- | Problem | P-01 |
| US- | User story | US-03 |
| F- | Feature | F-05 |
| FR- | Functional requirement | FR-012 |
| NFR- | Non-functional requirement | NFR-024 |
| SCR- | Screen | SCR-04 |
| VR- | Validation rule | VR-07 |
| AC- | Acceptance criteria | AC-10 |
| TC- | Test case | TC-013 |
| DEC- | Decision | DEC-004 |
| R- | Risk | R-03 |
| OQ- | Open question | OQ-05 |
| A- | Assumption | A-008 |

---

## 1. Executive Summary

**Masalah.** Pasien sering datang ke konsultasi tanpa daftar pertanyaan yang terstruktur, kesulitan mencatat hasil pembicaraan, dan tidak punya satu tempat untuk melacak tindak lanjut. Panduan NHS menyarankan pasien menulis pertanyaan terpenting, mencatat hasil, dan memastikan langkah berikutnya; AHRQ QuestionBuilder membantu pasien menyiapkan pertanyaan sebelum kunjungan [REF-01][REF-02].

**Solusi.** Teman Kontrol adalah aplikasi Android **offline-first, single-user, single-device** untuk:
**sebelum kontrol** (menyiapkan & memprioritaskan pertanyaan) → **saat kontrol** (mencentang pertanyaan yang dibahas) → **setelah kontrol** (mencatat hasil & melacak tindak lanjut).

**Bukan:** alat diagnosis, triase, telemedicine, rekam medis resmi, atau pemberi rekomendasi klinis.

**Prinsip produk:**

1. **Ringan dan cepat** — tanpa jaringan, tanpa SDK pihak ketiga yang berat, target performa terukur (§11.1–11.2).
2. **Privat secara default** — data hanya di app-private storage, tanpa izin INTERNET, tanpa backup cloud (§11.4).
3. **Sederhana untuk pengguna awam** — maksimal dua level navigasi dari Beranda, bahasa Indonesia yang jelas.
4. **Konsisten** — satu design system, satu pola arsitektur, satu kamus istilah (§3).

**Urutan pengembangan:** kunci model data & kontrak repository → frontend lengkap dengan mock → persistensi Room → integrasi flow → QA & edge case → UAT → finalisasi.

---

## 2. Decision Log

| ID | Keputusan | Alasan | Status |
|---|---|---|---|
| DEC-001 | Framework: **Kotlin + Jetpack Compose + Material 3** (bukan Flutter) | Proyek sudah tersetup & berhasil compile; native, startup lebih ringan, tanpa runtime tambahan. Menjawab OQ-01. | Active |
| DEC-002 | **minSdk 30 (Android 11)**, targetSdk & compileSdk 37 | Baseline perangkat uji Redmi Note 10S. Menjawab OQ-02. | Active |
| DEC-003 | Persistensi: **Room** (data utama) + **Preferences DataStore** (flag notice) | Room resmi Android, dukungan Flow, migration, foreign key cascade, transaksi atomik. | Active |
| DEC-004 | Dependency Injection: **Hilt** (dengan KSP) | Standar Google, lifecycle-aware, memudahkan swap Mock ↔ Room. Wajib lolos Technical Spike TS-01 (§17.1). | Active |
| DEC-005 | Milestone 8 Okt = **UI lengkap + mock in-memory**; data boleh hilang saat restart. Persistensi di Phase 2. Menjawab OQ-03. | Menjaga milestone realistis; kontrak repository sama sehingga swap tanpa redesign. | Active |
| DEC-006 | **Backup dimatikan**: `android:allowBackup="false"`, data extraction rules mengecualikan semua domain | Catatan kesehatan tidak boleh keluar perangkat (NFR-014). | Active |
| DEC-007 | Application ID & namespace: **`id.temankontrol.app`**; tanpa identitas institusi. Menjawab OQ-04. | `com.example` tidak boleh dirilis; identitas netral. | Active |
| DEC-008 | Tema: **Light + Dark mengikuti sistem**, tanpa Dynamic Color | Warna brand & status konsisten di semua perangkat. | Active |
| DEC-009 | Satu file PRD lengkap (produk + spesifikasi teknis) | SSOT tunggal, tidak ada dua dokumen yang bisa saling bertentangan. | Active |
| DEC-010 | Transisi status kunjungan **bebas** antar Terjadwal/Selesai/Dibatalkan; konfirmasi saat ke Dibatalkan | Pengguna dapat mengoreksi kesalahan. | Active |
| DEC-011 | **Tidak mendeklarasikan izin INTERNET**; izin hasil merge dari library dihapus dengan `tools:node="remove"` | Jaminan teknis offline & privasi, bukan sekadar janji. | Active |
| DEC-012 | Saran pertanyaan disimpan sebagai **konstanta Kotlin**, bukan JSON | Nol biaya parsing, nol I/O, type-safe. | Active |
| DEC-013 | Font **sistem (Roboto)**; tanpa font unduhan | Nol ukuran tambahan, nol risiko gagal muat offline. | Active |
| DEC-014 | Ikon: `material-icons-core` + ikon tambahan sebagai **vector drawable XML** dari Material Symbols; `material-icons-extended` **dilarang** | Extended memperlambat build dan membengkakkan debug APK. | Active |
| DEC-015 | Enkripsi at-rest mengandalkan **File-Based Encryption Android** (wajib sejak Android 10); SQLCipher tidak dipakai di MVP | Mengurangi ukuran & overhead; app-private + FBE memadai untuk MVP. App lock di backlog. | Active |
| DEC-016 | Navigasi: **Navigation Compose type-safe routes** (kotlinx.serialization) | Argumen route ter-validasi compiler. | Active |
| DEC-017 | Semua penghapusan (kunjungan, pertanyaan, tindak lanjut) memakai **dialog konfirmasi**, bukan undo snackbar | Satu pola yang konsisten dan sederhana. | Active |
| DEC-018 | Timeline frontend: **Day 1 = 7 Okt, Day 2 = 8 Okt**; malam 6 Okt dipakai untuk finalisasi PRD & setup | 6 Okt telah terpakai untuk perencanaan. Deadline 8 Okt tidak berubah. | Active |
| DEC-019 | Lokasi PRD dipindah dari `.idea/` ke `docs/PRD.md` | `.idea/` adalah folder IDE dan sebagian di-ignore; dokumen proyek harus berada di folder yang di-versioning. | Active |
| DEC-020 | Waktu disimpan sebagai **epoch millis (UTC)**; tanggal tenggat sebagai **epoch day** (`LocalDate`); tampilan memakai zona waktu perangkat & locale `id-ID` | Menghindari bug zona waktu & DST; tanggal tenggat tidak bergeser. | Active |
| DEC-021 | Perangkat pengujian utama: **Android Studio Virtual Device (Emulator API 30+)** | Pengujian fungsional dan UI dilakukan via AVD Android Studio; acuan ukuran layar tetap mengacu pada rasio smartphone (baseline ~360×640 dp s/d 1080×2400). | Active |

---

## 3. Glosarium (Kamus Istilah UI ↔ Kode)

Istilah ini **wajib** dipakai konsisten di UI, kode, test, dan dokumentasi.

| Istilah UI (ID) | Nama di Kode | Definisi |
|---|---|---|
| Kunjungan | `Consultation` | Satu konsultasi/kontrol ke tenaga kesehatan. |
| Status kunjungan | `ConsultationStatus` | `SCHEDULED` (Terjadwal), `COMPLETED` (Selesai), `CANCELLED` (Dibatalkan). |
| Pertanyaan | `Question` | Pertanyaan yang disiapkan pengguna untuk satu kunjungan. |
| Prioritas | `isPriority` | Penanda pertanyaan terpenting; maksimum 3 per kunjungan. |
| Sudah dibahas | `isDiscussed` | Penanda pertanyaan sudah ditanyakan saat konsultasi. |
| Hasil Konsultasi | `ConsultationNote` | Catatan pengguna tentang hasil konsultasi (0 atau 1 per kunjungan). |
| Tindak Lanjut | `FollowUpAction` | Tindakan yang harus dilakukan setelah konsultasi. |
| Tenggat | `dueDate` | Tanggal target tindak lanjut (opsional). |
| Terlambat | `isOverdue` (derived) | Tindak lanjut belum selesai dan `dueDate` < hari ini. |
| Lewat jadwal | `isPastDue` (derived) | Kunjungan `SCHEDULED` dengan tanggal < hari ini. |
| Saran pertanyaan | `QuestionSuggestion` | Template pertanyaan generik non-klinis. |
| Pemberitahuan awal | `FirstLaunchNotice` | Layar batas penggunaan saat pertama kali dibuka. |

**Kata terlarang di UI:** "diagnosis", "resep", "rekam medis", "catatan dokter", "rekomendasi pengobatan", "darurat" (kecuali dalam kalimat disclaimer "bukan layanan darurat").

---

## 4. Product Overview

- **One-liner:** Pendamping offline untuk mempersiapkan, mencatat, dan menindaklanjuti konsultasi kesehatan.
- **Vision:** Pasien datang ke konsultasi lebih siap dan pulang dengan langkah tindak lanjut yang jelas.
- **Mission:** Menyatukan pertanyaan, catatan hasil, dan tindak lanjut dalam alur mobile yang ringan, aman, dan mudah digunakan.

**Nilai pengguna:**

- Satu tempat untuk seluruh catatan satu kunjungan.
- Pertanyaan prioritas tampil paling atas saat waktu konsultasi terbatas.
- Hasil konsultasi terpisah jelas dari rencana tindak lanjut.
- Berfungsi penuh tanpa internet dan tanpa akun.

---

## 5. Background & Problem Statement

| ID | Problem | Root cause | Dampak |
|---|---|---|---|
| P-01 | Lupa pertanyaan penting sebelum/saat konsultasi | Tidak ada struktur prioritas; catatan tercecer | Pertanyaan penting tidak dibahas |
| P-02 | Hasil pembicaraan tidak tersimpan terstruktur | Catatan bebas di media berbeda | Sulit meninjau hasil konsultasi |
| P-03 | Sulit tahu tindakan mana yang belum selesai | Catatan tidak menjadi daftar tindakan | Tindak lanjut terlewat |
| P-04 | Aplikasi berbasis akun & internet terlalu kompleks | Ketergantungan server | Hambatan pemakaian, risiko privasi |

**Problem statement:** Pasien umum dan pendamping membutuhkan cara sederhana untuk mengorganisasi persiapan, hasil, dan tindak lanjut konsultasi pada satu perangkat, karena catatan yang tersebar membuat pertanyaan penting dan tindakan lanjutan terlewat.

---

## 6. Goals & Success Metrics

### 6.1 Goals

| Jenis | Goal |
|---|---|
| Produk | Satu record terstruktur per konsultasi; core journey berjalan tanpa internet; data kesehatan minimal. |
| Pengguna | Menyiapkan pertanyaan; melihat ≤3 prioritas dengan cepat; mencentang yang sudah dibahas; menyimpan hasil & tindak lanjut; meninjau riwayat. |
| Proyek | Frontend demo 8 Okt; MVP end-to-end 6 Nov; berjalan di Redmi Note 10S; tanpa fitur klinis. |

### 6.2 Success Metrics

| Metric | Cara ukur | Target MVP |
|---|---|---|
| Core journey task success | UAT: buat kunjungan → selesaikan 1 tindak lanjut | ≥ 4 dari 5 peserta tanpa bantuan |
| Data persistence | Force-stop lalu buka ulang setelah setiap jenis simpan | 100% skenario lulus |
| Critical defect | Bug yang memblokir core journey | 0 saat rilis |
| Validation coverage | VR-01 s/d VR-12 memiliki pesan error & unit test | 100% |
| Performa | Budget §11.1 diukur di Redmi Note 10S (release build) | 100% budget terpenuhi |
| Crash-free | Eksekusi regression test & UAT | 0 crash |

---

## 7. Scope

| Scope | Fitur | Priority | Deadline |
|---|---|---:|---|
| Day-2 Frontend | Seluruh SCR-01 s/d SCR-09, navigasi, design system | P0 | 8 Okt |
| Day-2 Frontend | Mock repository in-memory (full CRUD + skenario debug) | P0 | 8 Okt |
| Day-2 Frontend | Validasi form (VR-01 s/d VR-12) & unsaved-change dialog | P0 | 8 Okt |
| Day-2 Frontend | Loading, empty, error, success states | P0 | 8 Okt |
| MVP | Persistensi Room + DataStore | P0 | 6 Nov |
| MVP | CRUD & status kunjungan | P0 | 6 Nov |
| MVP | Pertanyaan, maks. 3 prioritas, checklist dibahas, cek duplikat | P0 | 6 Nov |
| MVP | Hasil konsultasi | P0 | 6 Nov |
| MVP | CRUD tindak lanjut & status selesai | P0 | 6 Nov |
| MVP | Riwayat + filter status | P1 | 6 Nov |
| MVP | Saran pertanyaan statis | P1 | 6 Nov |
| MVP | Standar performa, privasi, aksesibilitas (§11) | P0 | 6 Nov |
| Post-MVP | Pengingat lokal, ekspor/share, app lock, pencarian riwayat | P2 | — |
| Post-MVP | Sinkronisasi cloud | P3 | — |
| Out of scope | Diagnosis/triase/terapi, chat dokter, booking, login/admin, rekam medis resmi, integrasi RS, iOS | — | Tidak dikerjakan |

---

## 8. Users & User Stories

### 8.1 Persona

| | Primary: Pasien dewasa | Secondary: Pendamping |
|---|---|---|
| Konteks | Kontrol rutin/sesekali di puskesmas, klinik, RS | Keluarga yang mendampingi, memakai HP yang sama atas izin pasien |
| Goal | Tidak lupa pertanyaan, paham tindak lanjut | Membantu bertanya & mencatat |
| Kebutuhan | Input cepat, tulisan jelas, offline | Struktur ringkas, status jelas |
| Literasi teknis | Dasar–menengah | Dasar–menengah |

### 8.2 User Stories

| ID | Story | Pri |
|---|---|---:|
| US-01 | Sebagai pasien, saya ingin membuat catatan kunjungan agar semua info terkait punya satu konteks. | P0 |
| US-02 | Sebagai pasien, saya ingin menambah pertanyaan dan menandai maks. 3 sebagai prioritas agar yang terpenting tampil lebih dulu. | P0 |
| US-03 | Sebagai pasien, saya ingin menandai pertanyaan yang sudah dibahas agar tahu sisanya saat konsultasi. | P0 |
| US-04 | Sebagai pasien, saya ingin menyimpan ringkasan hasil konsultasi agar bisa meninjaunya kembali. | P0 |
| US-05 | Sebagai pasien, saya ingin menambah tindak lanjut dengan tenggat opsional agar tahu langkah berikutnya. | P0 |
| US-06 | Sebagai pasien, saya ingin menandai tindak lanjut selesai agar progres terlihat. | P0 |
| US-07 | Sebagai pasien, saya ingin mengedit atau menghapus data agar kesalahan bisa dikoreksi. | P0 |
| US-08 | Sebagai pasien, saya ingin melihat riwayat kunjungan agar bisa bersiap untuk kunjungan berikutnya. | P1 |
| US-09 | Sebagai pasien, saya ingin saran pertanyaan agar tidak mulai dari halaman kosong. | P1 |

---

## 9. Feature Specification

### F-01 — Pemberitahuan Awal (First-Launch Notice)

- **Screen:** SCR-01. **Pri:** P0.
- **Flow:** App dibuka pertama kali → splash tetap tampil selama flag dibaca dari DataStore → SCR-01 → "Saya Mengerti" → Beranda (back stack dibersihkan).
- **Alternatif:** Dibuka ulang dari Tentang → tombol "Tutup" (bukan "Saya Mengerti"), kembali ke Tentang.
- **Error:** Gagal menyimpan flag → tetap masuk Beranda; notice muncul lagi di peluncuran berikutnya; tidak ada pesan error ke pengguna.
- **Aturan:** Isi notice memakai copy C-01 (§14.3). Tidak ada checkbox persetujuan medis. Tombol Android back di SCR-01 pertama kali = keluar aplikasi.
- **Data:** `PreferencesRepository.noticeAccepted`.

### F-02 — Manajemen Kunjungan

- **Screen:** SCR-03, SCR-04. **Pri:** P0.
- **Flow buat:** Beranda → FAB "Tambah Kunjungan" → isi form → Simpan → snackbar C-20 → Detail kunjungan baru (form dihapus dari back stack).
- **Flow edit:** Detail → menu ⋮ → Edit → form terisi → Simpan → kembali ke Detail + snackbar C-21.
- **Flow status:** Detail → kontrol status (segmented button) → Selesai langsung tersimpan; Dibatalkan memerlukan dialog C-31. Transisi bebas (DEC-010).
- **Flow hapus:** Detail → ⋮ → Hapus → dialog C-30 (menyebut jumlah pertanyaan & tindak lanjut yang ikut terhapus) → hapus atomik → kembali ke layar sebelumnya + snackbar C-22.
- **Input & validasi:** VR-01 s/d VR-05.
- **Proses:** trim, kolaps whitespace berturut-turut di field satu baris, UUID v4, status awal `SCHEDULED`, `createdAt`/`updatedAt`.
- **Aturan:** UUID tidak berubah. Tanggal lampau **diizinkan** (mencatat kunjungan yang sudah terjadi) dengan info "Tanggal ini sudah lewat" (bukan error).

### F-03 — Pertanyaan & Checklist

- **Screen:** SCR-04 (daftar & checklist), SCR-05 (form). **Pri:** P0.
- **Flow:** Detail → "Tambah Pertanyaan" → ketik atau pilih saran → toggle Prioritas → Simpan → kembali ke Detail.
- **Checklist:** di Detail, setiap pertanyaan memiliki checkbox berlabel "Sudah dibahas"; tap mengubah status langsung.
- **Urutan tampil:** prioritas dulu, lalu non-prioritas; masing-masing berdasarkan `createdAt` naik. Item yang sudah dibahas **tidak** pindah posisi (mencegah item melompat saat konsultasi), hanya diberi gaya coret + ikon centang.
- **Ringkasan:** header section menampilkan "x dari y sudah dibahas".
- **Validasi:** VR-06, VR-07, VR-08.
- **Aturan:** Total pertanyaan tidak dibatasi. Jika > 15, tampil info C-40. Toggle prioritas di form dinonaktifkan dengan teks penjelasan jika sudah ada 3 prioritas lain (pencegahan di UI) **dan** repository tetap menolak (pertahanan kedua).
- **Status checkbox:** ditulis ke repository; jika gagal, state dikembalikan dan snackbar C-50 muncul.

### F-04 — Hasil Konsultasi

- **Screen:** SCR-06. **Pri:** P0.
- **Field:** Ringkasan pembicaraan (`summary`), Arahan dari tenaga kesehatan (`providerInstruction`), Catatan tambahan (`additionalNotes`).
- **Label section:** "Catatan pengguna" (selalu). Tidak boleh "Catatan dokter".
- **Proses:** upsert — satu `ConsultationNote` per kunjungan.
- **Validasi:** VR-09, VR-10. Jika ketiga field dikosongkan saat edit catatan yang ada → simpan ditolak dengan C-12; untuk menghapus catatan gunakan menu "Hapus catatan" dengan konfirmasi C-33.
- **Detail menampilkan:** preview ringkasan maks. 3 baris + "Diperbarui {tanggal waktu}".

### F-05 — Tindak Lanjut

- **Screen:** SCR-04 (daftar), SCR-07 (form). **Pri:** P0.
- **Flow:** Detail → "Tambah Tindak Lanjut" → isi tindakan & tenggat opsional (date picker) → Simpan.
- **Status:** checkbox satu tap. Selesai → `completedAt = now`. Dibuka kembali → `completedAt = null`.
- **Urutan tampil:** grup "Belum selesai" (tenggat naik, tanpa tenggat di akhir, lalu `createdAt` naik), kemudian grup "Selesai" (`completedAt` turun).
- **Terlambat:** belum selesai & `dueDate` < hari ini → label teks "Terlambat" + ikon, bukan warna saja.
- **Validasi:** VR-11, VR-12. Tenggat lampau diizinkan dengan peringatan C-41.

### F-06 — Riwayat & Filter

- **Screen:** SCR-08. **Pri:** P1.
- **Isi:** **semua** kunjungan, urut `appointmentAt` turun.
- **Filter chip:** Semua (default) / Terjadwal / Selesai / Dibatalkan. Filter terpilih bertahan selama proses aplikasi hidup (disimpan di `SavedStateHandle`).
- **Kartu:** judul, tanggal, fasilitas (bila ada), status chip, label "Lewat jadwal" bila relevan, ringkasan "x/y pertanyaan · z tindak lanjut terbuka".
- **Empty:** tidak ada data sama sekali → C-60 + CTA "Tambah Kunjungan"; filter tanpa hasil → C-61 + tombol "Tampilkan semua".

### F-07 — Saran Pertanyaan Statis

- **Screen:** SCR-05. **Pri:** P1.
- **Kategori:** Gejala, Pemeriksaan, Perawatan, Langkah Berikutnya (masing-masing 4–6 template; isi final di §14.4).
- **Perilaku:** tap saran → teks disalin ke field (menimpa isi kosong; jika field berisi, minta konfirmasi "Ganti teks?") → tetap bisa diedit → hanya tersimpan saat Simpan.
- **Data:** konstanta Kotlin (DEC-012). Tidak pernah gagal dimuat.

### F-08 — Beranda

- **Screen:** SCR-02. **Pri:** P0.
- **Section 1 "Kunjungan Mendatang":** status `SCHEDULED` dengan `appointmentAt` ≥ awal hari ini, urut naik, maks. 3, tombol "Lihat semua" → Riwayat (filter Terjadwal).
- **Section 2 "Tindak Lanjut Terbuka":** tindak lanjut belum selesai dari semua kunjungan, urut sama seperti F-05, maks. 5, setiap item menampilkan judul kunjungan; checkbox selesai dapat di-tap langsung.
- **Section 3 (kondisional) "Perlu diperbarui":** jumlah kunjungan "Lewat jadwal" → tap membuka Riwayat (filter Terjadwal).
- **FAB:** "Tambah Kunjungan" (extended FAB dengan teks).
- **Empty total:** ilustrasi vektor ringan + C-62 + CTA.

### F-09 — Tentang

- **Screen:** SCR-09. **Pri:** P1.
- **Isi:** deskripsi singkat, batas penggunaan (buka ulang SCR-01), penjelasan data (C-70), versi aplikasi (`BuildConfig.VERSION_NAME`).
- **Debug only:** section "Developer" (hanya `BuildConfig.DEBUG`) untuk memilih skenario mock: Normal, Kosong, Error, Lambat (delay 1500 ms). Dipakai untuk demo state Day-2 dan UI test.

---

## 10. Functional Requirements

| ID | Requirement | Pri | Stage | Feature |
|---|---|---:|---|---|
| FR-001 | Menampilkan pemberitahuan awal saat first launch dan menyimpan acknowledgement. | P0 | Day-2 (mock) / MVP (persisten) | F-01 |
| FR-002 | Beranda menampilkan maks. 3 kunjungan mendatang dan maks. 5 tindak lanjut terbuka sesuai F-08. | P0 | Day-2/MVP | F-08 |
| FR-003 | Membuat kunjungan. | P0 | Day-2/MVP | F-02 |
| FR-004 | Mengedit kunjungan. | P0 | Day-2/MVP | F-02 |
| FR-005 | Menghapus kunjungan beserta seluruh child data secara atomik setelah konfirmasi. | P0 | Day-2 (mock) / MVP | F-02 |
| FR-006 | Mengubah status kunjungan secara bebas antar 3 status. | P0 | Day-2/MVP | F-02 |
| FR-007 | CRUD pertanyaan. | P0 | Day-2/MVP | F-03 |
| FR-008 | Membatasi maks. 3 pertanyaan prioritas per kunjungan (UI + repository). | P0 | Day-2/MVP | F-03 |
| FR-009 | Menolak pertanyaan duplikat (setelah normalisasi) dalam kunjungan yang sama. | P1 | Day-2/MVP | F-03 |
| FR-010 | Menandai pertanyaan sudah/belum dibahas. | P0 | Day-2/MVP | F-03 |
| FR-011 | Upsert & hapus catatan hasil konsultasi. | P0 | Day-2/MVP | F-04 |
| FR-012 | CRUD tindak lanjut. | P0 | Day-2/MVP | F-05 |
| FR-013 | Mengisi/mengosongkan `completedAt` sesuai status tindak lanjut. | P0 | Day-2/MVP | F-05 |
| FR-014 | Riwayat urut tanggal kunjungan terbaru dulu. | P1 | Day-2/MVP | F-06 |
| FR-015 | Filter riwayat berdasarkan status. | P1 | Day-2/MVP | F-06 |
| FR-016 | Saran pertanyaan statis yang dapat diedit sebelum disimpan. | P1 | MVP | F-07 |
| FR-017 | Data bertahan setelah aplikasi ditutup, force-stop, dan reboot. | P0 | MVP | Semua |
| FR-018 | Mencegah aksi ganda akibat multiple tap. | P0 | Day-2/MVP | Semua form |
| FR-019 | Dialog unsaved-change (Tetap di sini / Buang) saat keluar dari form yang berubah, termasuk via gesture back. | P1 | Day-2/MVP | Semua form |
| FR-020 | Loading, empty, error + retry state di semua layar daftar/detail. | P0 | Day-2/MVP | Semua |
| FR-021 | Terpasang & berjalan di Redmi Note 10S. | P0 | Day-2/MVP | — |
| FR-022 | Data hanya disimpan lokal; aplikasi tidak memiliki izin INTERNET. | P0 | Day-2/MVP | — |
| FR-023 | Draft form bertahan saat rotasi, pindah aplikasi, dan process death. | P1 | MVP | Semua form |
| FR-024 | Detail menampilkan "Kunjungan tidak ditemukan" + tombol kembali bila ID tidak ada (mis. terhapus). | P0 | Day-2/MVP | F-02 |
| FR-025 | Menampilkan label "Lewat jadwal" untuk kunjungan Terjadwal bertanggal lampau dan "Terlambat" untuk tindak lanjut lewat tenggat. | P1 | Day-2/MVP | F-02, F-05 |

---

## 11. Non-Functional Requirements

### 11.1 Performa (Budget Terukur)

Semua angka diukur pada **Redmi Note 10S, release build (R8 aktif), dataset uji D-100** (§21.4), kecuali disebut lain.

| ID | Requirement | Target | Cara ukur |
|---|---|---|---|
| NFR-001 | Cold start sampai frame pertama Beranda | ≤ 1.500 ms (median 5 kali) | `adb shell am start -W -n id.temankontrol.app/.MainActivity` → `TotalTime` |
| NFR-002 | Feedback operasi tulis lokal (simpan/centang/hapus) | ≤ 300 ms; bila > 300 ms tampilkan indikator progres | Log durasi debug + observasi |
| NFR-003 | Tombol submit nonaktif selama tulis berlangsung | Wajib | AC-02 |
| NFR-004 | Hapus kunjungan & child data atomik | Wajib (transaksi + FK CASCADE) | TC-013 |
| NFR-005 | Kegagalan storage tidak menutup aplikasi | Pesan + retry | TC-014 |
| NFR-024 | **Tidak ada I/O di main thread.** Room & DataStore hanya diakses via `suspend`/`Flow`; Room tidak boleh `allowMainThreadQueries()` | 0 pelanggaran | StrictMode (debug) `detectDiskReads/Writes().penaltyLog()` tanpa log pelanggaran dari kode aplikasi |
| NFR-025 | Scroll Beranda/Riwayat/Detail bebas jank | Janky frames < 5% saat scroll riwayat D-100 | Android Studio Profiler / `adb shell dumpsys gfxinfo id.temankontrol.app` |
| NFR-026 | Query daftar & detail | ≤ 50 ms per query pada D-100 | Log durasi di debug build |
| NFR-027 | Memori | PSS ≤ 150 MB setelah navigasi seluruh layar | `adb shell dumpsys meminfo id.temankontrol.app` |
| NFR-028 | Ukuran APK release | ≤ 10 MB | Ukuran file APK release |
| NFR-029 | Tidak ada memory leak Activity/ViewModel | 0 leak | LeakCanary (debugImplementation saja) setelah skenario TC-015 |
| NFR-030 | Baseline Profile | Disertakan di release untuk mempercepat startup & scroll | Modul `:baselineprofile` (Phase 6; boleh di-skip bila NFR-001 & NFR-025 sudah lulus tanpa itu) |

### 11.2 Aturan Implementasi Performa (wajib diikuti di kode)

1. **Threading:** Room DAO memakai `suspend fun` & `Flow`. Logika non-trivial (normalisasi teks, sorting besar) di `Dispatchers.Default`; I/O di `Dispatchers.IO` (di-inject via Hilt qualifier agar bisa diganti di test).
2. **State:** setiap ViewModel mengekspos **satu** `StateFlow<XxxUiState>` dibuat dengan `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. Composable mengumpulkan dengan `collectAsStateWithLifecycle()`.
3. **Stabilitas Compose:** model UI adalah `data class` dengan `val` saja; koleksi memakai `kotlinx.collections.immutable` (`ImmutableList`) atau kelas ditandai `@Immutable`. Lambda event diteruskan sebagai referensi yang di-`remember` bila perlu.
4. **Lazy list:** semua `LazyColumn` memakai `key = { it.id }` dan `contentType`. Dilarang menaruh `LazyColumn` di dalam `Column` yang `verticalScroll`.
5. **Derived state:** hitungan turunan (jumlah dibahas, overdue) dihitung di ViewModel/mapper, bukan di composable setiap recomposition.
6. **Query:** hanya ambil kolom yang dibutuhkan untuk kartu (projection `ConsultationSummary`), bukan entitas penuh + semua relasi. Agregat (jumlah pertanyaan, tindak lanjut terbuka) dihitung di SQL.
7. **Index:** sesuai §15.3.
8. **Flow:** gunakan `distinctUntilChanged()` pada flow yang di-combine; jangan membuat flow baru di dalam composable.
9. **Tanpa reflection-heavy library**, tanpa Gson; serialisasi (hanya untuk route) memakai kotlinx.serialization.
10. **Gambar:** hanya vector drawable; tanpa bitmap besar; tanpa library image loading.
11. **Splash:** `androidx.core:core-splashscreen` dengan `setKeepOnScreenCondition` sampai start destination diketahui — mencegah kedipan layar notice → beranda.
12. **Release:** `isMinifyEnabled = true`, `isShrinkResources = true`, R8 full mode (default AGP).
13. **Logging:** `android.util.Log` dibungkus `AppLogger`; panggilan `Log.v/d/i` dihapus di release dengan aturan R8 `-assumenosideeffects`.

### 11.3 Usability & Aksesibilitas

| ID | Requirement |
|---|---|
| NFR-006 | Core journey dicapai maks. 2 level navigasi dari Beranda. |
| NFR-007 | Setiap input memiliki label persisten (`OutlinedTextField` dengan `label`), bukan placeholder saja. |
| NFR-008 | Pesan error menyebut field & cara memperbaiki (copy §14.3). |
| NFR-009 | Status tidak dibedakan hanya dengan warna (teks + ikon). |
| NFR-010 | Touch target ≥ 48×48 dp. |
| NFR-011 | Ikon tanpa teks memiliki `contentDescription`; ikon dekoratif `contentDescription = null`. |
| NFR-012 | Tanpa teks/CTA terpotong pada 360×640 dp dengan font scale 1.3; tanpa crash & tetap dapat dipakai (scroll) pada font scale 2.0. |
| NFR-031 | Kontras teks ≥ 4.5:1 (normal) dan ≥ 3:1 (teks besar/ikon) di tema terang & gelap. |
| NFR-032 | Checkbox & toggle memakai `Modifier.toggleable`/`semantics` sehingga TalkBack membaca label + status. |
| NFR-033 | Form tetap dapat discroll dan tombol Simpan terjangkau saat keyboard terbuka (`imePadding()`, edge-to-edge). |

### 11.4 Keamanan & Privasi

| ID | Requirement |
|---|---|
| NFR-013 | Tidak ada field wajib berisi nama lengkap, NIK, nomor BPJS, diagnosis, atau foto dokumen. |
| NFR-014 | Data di app-private storage saja; `allowBackup="false"`; `data_extraction_rules.xml` mengecualikan semua domain untuk cloud-backup dan device-transfer (DEC-006). |
| NFR-015 | Tidak ada secret/credential di kode; keystore release tidak di-commit. |
| NFR-016 | Hapus menghapus record dan relasinya dari database (bukan soft delete). |
| NFR-017 | Log hanya berisi ID teknis & kategori error, **tidak pernah** isi teks pengguna. |
| NFR-018 | Mengikuti prinsip MASVS-STORAGE & MASVS-PRIVACY [REF-05]. |
| NFR-034 | Manifest final tidak mengandung izin `INTERNET` / `ACCESS_NETWORK_STATE` (dicek di merged manifest) (DEC-011). |
| NFR-035 | Tidak ada analytics, crash reporter, atau SDK pihak ketiga yang mengirim data. |
| NFR-036 | Hanya `MainActivity` yang `exported`; tidak ada ContentProvider publik. |

### 11.5 Kompatibilitas & Ketahanan

| ID | Requirement |
|---|---|
| NFR-019 | Android 11 (API 30) ke atas. |
| NFR-020 | Portrait sebagai target desain; landscape & rotasi tidak crash dan tidak kehilangan draft. |
| NFR-021 | Berfungsi penuh dalam airplane mode. |
| NFR-022 | Lulus smoke test di Redmi Note 10S (MIUI). |
| NFR-023 | iOS bukan target. |
| NFR-037 | Edge-to-edge (wajib untuk targetSdk ≥ 35) dengan insets ditangani; konten tidak tertutup status/navigation bar. |
| NFR-038 | Predictive back didukung; dialog unsaved-change memakai `BackHandler` sehingga tetap bekerja dengan gesture back. |
| NFR-039 | Mendukung tema terang & gelap mengikuti sistem (DEC-008). |

---

## 12. Information Architecture & Navigation

```text
Splash (sistem)
└── [noticeAccepted == false] → SCR-01 Pemberitahuan Awal
└── Main Shell (Bottom Navigation)
    ├── SCR-02 Beranda
    │   ├── SCR-03 Form Kunjungan (buat)
    │   └── SCR-04 Detail Kunjungan
    │       ├── SCR-03 Form Kunjungan (edit)
    │       ├── SCR-05 Form Pertanyaan (tambah/edit)
    │       ├── SCR-06 Hasil Konsultasi
    │       └── SCR-07 Form Tindak Lanjut (tambah/edit)
    ├── SCR-08 Riwayat → SCR-04
    └── SCR-09 Tentang → SCR-01 (mode baca)
```

### 12.1 Route (type-safe, DEC-016)

| Route (Kotlin) | Argumen | Screen | Bottom bar tampil? |
|---|---|---|---|
| `NoticeRoute` | `readOnly: Boolean = false` | SCR-01 | Tidak |
| `HomeRoute` | — | SCR-02 | Ya |
| `HistoryRoute` | `initialFilter: StatusFilter = ALL` | SCR-08 | Ya |
| `AboutRoute` | — | SCR-09 | Ya |
| `ConsultationFormRoute` | `consultationId: String? = null` | SCR-03 | Tidak |
| `ConsultationDetailRoute` | `consultationId: String` | SCR-04 | Tidak |
| `QuestionFormRoute` | `consultationId: String, questionId: String? = null` | SCR-05 | Tidak |
| `NoteFormRoute` | `consultationId: String` | SCR-06 | Tidak |
| `FollowUpFormRoute` | `consultationId: String, actionId: String? = null` | SCR-07 | Tidak |

**Aturan navigasi:**

- Bottom nav: Beranda, Riwayat, Tentang; pindah tab memakai `launchSingleTop` + `restoreState` + `popUpTo(start){ saveState = true }`.
- Setelah buat kunjungan: navigasi ke Detail dengan `popUpTo<ConsultationFormRoute>{ inclusive = true }`.
- Setelah hapus kunjungan: `popBackStack()` ke layar asal.
- Guard double-navigation: abaikan klik navigasi bila lifecycle entry saat ini bukan `RESUMED`.

---

## 13. Screen Specification

Setiap layar list/detail memiliki state: **Loading** (indikator muncul hanya jika > 300 ms untuk mencegah kedip), **Content**, **Empty**, **Error** (pesan C-51 + tombol "Coba lagi").

| ID | Screen | Komponen utama | Aksi | State khusus |
|---|---|---|---|---|
| SCR-01 | Pemberitahuan Awal | Ikon app, judul, 3 poin batas (C-01), tombol utama | Saya Mengerti / Tutup | — |
| SCR-02 | Beranda | TopAppBar "Teman Kontrol", section F-08, ExtendedFAB | Buka detail, centang tindak lanjut, tambah | Empty total (C-62) |
| SCR-03 | Form Kunjungan | Field: Judul*, Tanggal*, Jam*, Alasan kunjungan*, Fasilitas, Nama tenaga kesehatan; tombol Simpan di bawah (sticky di atas keyboard) | Simpan, Back | Info tanggal lampau; unsaved dialog |
| SCR-04 | Detail Kunjungan | Header (judul, tanggal, fasilitas, nakes, alasan), kontrol status, section **Persiapan Pertanyaan** → **Hasil Konsultasi** → **Tindak Lanjut**, menu ⋮ (Edit, Hapus) | Semua aksi child | Not found (FR-024); loading per section tidak mengganti seluruh layar |
| SCR-05 | Form Pertanyaan | Field pertanyaan (multiline, counter), switch Prioritas + teks bantuan, daftar saran per kategori (chips) | Simpan, Hapus (mode edit), Back | Limit prioritas; duplikat |
| SCR-06 | Hasil Konsultasi | 3 field multiline + counter, label "Catatan pengguna" | Simpan, Hapus catatan (bila ada), Back | Semua kosong |
| SCR-07 | Form Tindak Lanjut | Field tindakan, pemilih tanggal tenggat (opsional, bisa dihapus) | Simpan, Hapus (mode edit), Back | Peringatan tenggat lampau |
| SCR-08 | Riwayat | Filter chips, LazyColumn kartu | Buka detail | Empty total vs empty filter |
| SCR-09 | Tentang | Deskripsi, batas penggunaan, data & privasi, versi, [Developer – debug only] | Buka SCR-01 | — |

**Catatan perangkat (Redmi Note 10S, 6.43", 1080×2400):** uji keyboard terbuka di semua form, font besar, mode gelap, dan airplane mode di perangkat fisik — bukan emulator saja.

---

## 14. UI/UX & Design System

### 14.1 Prinsip

- Satu primary CTA per layar. Aksi destruktif selalu dialog konfirmasi (DEC-017).
- Detail mengikuti urutan waktu: Persiapan → Hasil → Tindak Lanjut.
- Feedback eksplisit untuk setiap create/update/delete (snackbar) — kecuali toggle checkbox (perubahan visual sudah menjadi feedback).
- Error tidak memakai kode teknis. Empty state selalu menunjukkan langkah berikutnya.
- UI polish tidak boleh mendahului kelengkapan fungsi & state.

### 14.2 Design Tokens

**Warna** — skema dibuat dengan Material Theme Builder dari seed **`#0F7C80`** (teal tenang), hasil di-commit ke `core/designsystem/theme/Color.kt` dan tidak diubah manual per layar.

| Token | Light | Dark | Pemakaian |
|---|---|---|---|
| primary | dari seed | dari seed | CTA, FAB, elemen aktif |
| status.scheduled | `#1E5AA8` | `#A8C8FF` | Chip Terjadwal + ikon jadwal |
| status.completed | `#2E7D32` | `#94D69A` | Chip Selesai + ikon centang |
| status.cancelled | `#5F6368` | `#C4C7C5` | Chip Dibatalkan + ikon silang |
| status.warning | `#B26A00` | `#FFB95C` | Terlambat / Lewat jadwal + ikon peringatan |
| priority | `#8A4FFF` | `#CDB4FF` | Badge "Prioritas" + ikon bintang |

Semua pasangan warna wajib memenuhi NFR-031.

**Tipografi:** Material 3 type scale dengan font sistem (DEC-013). Body minimum `bodyLarge` (16 sp) untuk isi catatan & pertanyaan.

**Spacing (dp):** 4, 8, 12, 16, 24, 32. Padding layar horizontal 16; jarak antar section 24.

**Shape:** kartu 16 dp; tombol & chip mengikuti default M3; dialog 28 dp.

**Komponen reusable (wajib dipakai, dilarang duplikasi ad-hoc):**
`AppScaffold`, `AppTopBar`, `StateView` (loading/empty/error), `ConsultationCard`, `SectionCard`, `LabeledTextField` (dengan counter & error), `DateField`, `TimeField`, `StatusChip`, `PriorityBadge`, `ChecklistItem`, `PrimaryButton` (dengan state loading), `ConfirmDialog`, `UnsavedChangesDialog`.

### 14.3 Microcopy (SSOT teks UI)

Semua string berada di `res/values/strings.xml` (tidak di-hardcode) dengan key sesuai ID berikut.

| ID | Konteks | Teks |
|---|---|---|
| C-01 | Notice | Judul: "Sebelum mulai". Poin: (1) "Teman Kontrol membantu Anda menyiapkan pertanyaan, mencatat hasil, dan melacak tindak lanjut konsultasi." (2) "Aplikasi ini bukan alat diagnosis, tidak memberi saran pengobatan, dan bukan layanan darurat. Untuk keadaan darurat, hubungi 119 atau fasilitas kesehatan terdekat." (3) "Semua catatan Anda tulis sendiri dan hanya tersimpan di perangkat ini." Tombol: "Saya Mengerti" |
| C-10 | Field wajib kosong | "{Nama field} wajib diisi." |
| C-11 | Terlalu panjang | "{Nama field} maksimal {n} karakter." |
| C-12 | Catatan kosong | "Isi minimal satu bagian catatan sebelum menyimpan." |
| C-13 | Prioritas penuh | "Maksimal 3 pertanyaan prioritas. Hapus tanda prioritas pada pertanyaan lain terlebih dahulu." |
| C-14 | Duplikat | "Pertanyaan ini sudah ada di kunjungan ini." |
| C-15 | Tanggal tidak valid | "Pilih tanggal yang valid." |
| C-20 | Kunjungan dibuat | "Kunjungan disimpan." |
| C-21 | Diperbarui | "Perubahan disimpan." |
| C-22 | Dihapus | "Kunjungan dihapus." |
| C-23 | Pertanyaan/tindak lanjut/catatan disimpan | "Tersimpan." |
| C-30 | Hapus kunjungan | Judul "Hapus kunjungan?" Isi "{x} pertanyaan, catatan hasil, dan {y} tindak lanjut juga akan terhapus. Tindakan ini tidak dapat dibatalkan." Tombol "Hapus" / "Batal" |
| C-31 | Batalkan kunjungan | "Tandai kunjungan ini sebagai dibatalkan? Anda dapat mengubahnya lagi nanti." Tombol "Tandai Dibatalkan" / "Kembali" |
| C-32 | Hapus pertanyaan/tindak lanjut | "Hapus {pertanyaan/tindak lanjut} ini?" Tombol "Hapus" / "Batal" |
| C-33 | Hapus catatan | "Hapus catatan hasil konsultasi?" Tombol "Hapus" / "Batal" |
| C-34 | Unsaved | Judul "Buang perubahan?" Isi "Perubahan yang belum disimpan akan hilang." Tombol "Buang" / "Tetap di sini" |
| C-40 | Daftar panjang | "Daftar pertanyaan cukup panjang. Pastikan 3 pertanyaan terpenting ditandai prioritas." |
| C-41 | Tenggat lampau | "Tanggal ini sudah lewat. Tetap simpan jika memang benar." |
| C-42 | Tanggal kunjungan lampau | "Tanggal ini sudah lewat — cocok untuk mencatat kunjungan yang sudah terjadi." |
| C-50 | Gagal simpan | "Gagal menyimpan. Data Anda masih ada di formulir — coba lagi." Aksi snackbar: "Coba lagi" |
| C-51 | Gagal memuat | "Data tidak dapat dimuat." Tombol "Coba lagi" |
| C-52 | Tidak ditemukan | "Kunjungan tidak ditemukan. Mungkin sudah dihapus." Tombol "Kembali" |
| C-60 | Riwayat kosong | "Belum ada kunjungan tercatat." CTA "Tambah Kunjungan" |
| C-61 | Filter kosong | "Tidak ada kunjungan dengan status ini." Tombol "Tampilkan semua" |
| C-62 | Beranda kosong | "Siapkan kunjungan berikutnya. Tulis pertanyaan Anda sebelum bertemu tenaga kesehatan." CTA "Tambah Kunjungan" |
| C-63 | Section pertanyaan kosong | "Belum ada pertanyaan. Tulis hal yang ingin Anda tanyakan." |
| C-64 | Section hasil kosong | "Belum ada catatan hasil konsultasi." |
| C-65 | Section tindak lanjut kosong | "Belum ada tindak lanjut." |
| C-70 | Tentang – data | "Data hanya tersimpan di perangkat ini, tidak dikirim ke server, dan tidak ikut dicadangkan. Menghapus aplikasi akan menghapus semua catatan." |

### 14.4 Katalog Saran Pertanyaan (F-07)

Disusun generik, non-klinis, terinspirasi pola AHRQ QuestionBuilder [REF-02].

| Kategori | Template |
|---|---|
| Gejala | "Apa kemungkinan penyebab keluhan yang saya alami?" · "Keluhan apa yang perlu saya perhatikan atau segera laporkan?" · "Berapa lama biasanya keluhan ini membaik?" · "Apakah ada aktivitas yang sebaiknya saya hindari?" |
| Pemeriksaan | "Pemeriksaan apa yang perlu saya jalani dan untuk apa?" · "Apa yang perlu saya siapkan sebelum pemeriksaan?" · "Kapan dan bagaimana saya mendapatkan hasilnya?" · "Apa arti hasil pemeriksaan saya?" |
| Perawatan | "Apa saja pilihan perawatan yang tersedia?" · "Bagaimana cara menggunakan obat ini dengan benar?" · "Efek samping apa yang perlu saya waspadai?" · "Apa yang harus saya lakukan jika lupa minum obat?" |
| Langkah Berikutnya | "Kapan saya perlu kontrol lagi?" · "Siapa yang dapat saya hubungi jika ada pertanyaan?" · "Tanda apa yang berarti saya harus segera kembali?" · "Apa yang perlu saya bawa pada kunjungan berikutnya?" |

### 14.5 Format Tanggal & Waktu

- Locale `id-ID`, zona waktu perangkat, jam 24 jam.
- Tanggal panjang: `Senin, 12 Okt 2026`. Waktu: `09.30`. Gabungan: `Senin, 12 Okt 2026 · 09.30`.
- Relatif di Beranda: "Hari ini", "Besok", selain itu tanggal panjang.
- Date/Time picker: Material 3 `DatePicker` & `TimePicker`.

---

## 15. Data Specification

### 15.1 Domain Model (Kotlin, `domain/model`)

```kotlin
@JvmInline value class ConsultationId(val value: String)

enum class ConsultationStatus { SCHEDULED, COMPLETED, CANCELLED }
enum class StatusFilter { ALL, SCHEDULED, COMPLETED, CANCELLED }
enum class SuggestionCategory { SYMPTOM, EXAM, TREATMENT, NEXT_STEP }

data class Consultation(
    val id: ConsultationId,
    val title: String,
    val appointmentAt: Instant,
    val reason: String,
    val facilityName: String?,
    val providerName: String?,
    val status: ConsultationStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ConsultationSummary(        // projection untuk kartu Beranda/Riwayat
    val id: ConsultationId,
    val title: String,
    val appointmentAt: Instant,
    val facilityName: String?,
    val status: ConsultationStatus,
    val questionCount: Int,
    val discussedCount: Int,
    val openActionCount: Int,
)

data class Question(
    val id: String,
    val consultationId: ConsultationId,
    val text: String,
    val isPriority: Boolean,
    val isDiscussed: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class ConsultationNote(
    val consultationId: ConsultationId,
    val summary: String?,
    val providerInstruction: String?,
    val additionalNotes: String?,
    val updatedAt: Instant,
)

data class FollowUpAction(
    val id: String,
    val consultationId: ConsultationId,
    val text: String,
    val dueDate: LocalDate?,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class OpenFollowUp(val action: FollowUpAction, val consultationTitle: String)

data class QuestionSuggestion(val id: String, val category: SuggestionCategory, val text: String)
```

Input untuk create/update memakai kelas terpisah (`ConsultationInput`, `QuestionInput`, `NoteInput`, `FollowUpInput`) yang berisi field yang dapat diedit pengguna saja. Waktu (`Instant`, `LocalDate`) dari `java.time` (tersedia native di API 26+). Waktu "sekarang" diambil dari `Clock` yang di-inject (agar dapat diuji).

### 15.2 Skema Room (versi skema 1)

**`consultations`**

| Kolom | Tipe | Constraint |
|---|---|---|
| id | TEXT | PK (UUID) |
| title | TEXT | NOT NULL |
| appointment_at | INTEGER | NOT NULL (epoch millis UTC) |
| reason | TEXT | NOT NULL |
| facility_name | TEXT | NULL |
| provider_name | TEXT | NULL |
| status | TEXT | NOT NULL (`SCHEDULED`/`COMPLETED`/`CANCELLED`) |
| created_at | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |

**`questions`**

| Kolom | Tipe | Constraint |
|---|---|---|
| id | TEXT | PK |
| consultation_id | TEXT | NOT NULL, FK → consultations.id ON DELETE CASCADE |
| text | TEXT | NOT NULL |
| text_normalized | TEXT | NOT NULL (lowercase, trim, whitespace dikolaps) |
| is_priority | INTEGER | NOT NULL (0/1) |
| is_discussed | INTEGER | NOT NULL (0/1) |
| created_at | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |

**`consultation_notes`**

| Kolom | Tipe | Constraint |
|---|---|---|
| consultation_id | TEXT | PK, FK → consultations.id ON DELETE CASCADE |
| summary | TEXT | NULL |
| provider_instruction | TEXT | NULL |
| additional_notes | TEXT | NULL |
| updated_at | INTEGER | NOT NULL |

**`follow_up_actions`**

| Kolom | Tipe | Constraint |
|---|---|---|
| id | TEXT | PK |
| consultation_id | TEXT | NOT NULL, FK → consultations.id ON DELETE CASCADE |
| text | TEXT | NOT NULL |
| due_date | INTEGER | NULL (epoch day) |
| is_completed | INTEGER | NOT NULL (0/1) |
| completed_at | INTEGER | NULL |
| created_at | INTEGER | NOT NULL |
| updated_at | INTEGER | NOT NULL |

**DataStore (Preferences):** `notice_accepted: Boolean` (default `false`).

### 15.3 Index

| Tabel | Index | Tujuan |
|---|---|---|
| consultations | `(status, appointment_at)` | Beranda & filter riwayat |
| consultations | `(appointment_at)` | Riwayat "Semua" |
| questions | `UNIQUE (consultation_id, text_normalized)` | Cek duplikat (FR-009) di level DB |
| questions | `(consultation_id, is_priority)` | Hitung prioritas & daftar per kunjungan |
| follow_up_actions | `(consultation_id)` | Daftar per kunjungan, FK |
| follow_up_actions | `(is_completed, due_date)` | Tindak lanjut terbuka di Beranda |

### 15.4 Integritas & Transaksi

- Hapus kunjungan: satu `DELETE` pada `consultations` dalam `@Transaction`; child terhapus via FK CASCADE (foreign keys aktif default di Room).
- Tambah/edit pertanyaan dengan prioritas: dalam `withTransaction { count prioritas lain; if ≥ 3 → LimitExceeded; else write }`.
- Duplikat: ditangkap dari `SQLiteConstraintException` pada unique index → `AppError.Duplicate`.
- Setiap update kunjungan atau child memperbarui `updated_at` milik record itu sendiri.
- Room `exportSchema = true`; JSON skema di-commit (`app/schemas/`). Perubahan skema setelah rilis demo wajib migration + test migration (`MigrationTestHelper`). `fallbackToDestructiveMigration` **dilarang**.

### 15.5 Validation Rules

Validasi dijalankan di domain layer (`domain/validation`, fungsi murni, di-unit-test). UI menampilkan error inline **setelah** field pernah disentuh atau saat Simpan ditekan.

| ID | Field | Aturan | Copy |
|---|---|---|---|
| VR-01 | Judul kunjungan | Wajib; 1–80 karakter setelah trim | C-10, C-11 |
| VR-02 | Tanggal & jam kunjungan | Wajib; valid | C-10, C-15; info C-42 bila lampau |
| VR-03 | Alasan kunjungan | Wajib; 1–500 | C-10, C-11 |
| VR-04 | Fasilitas | Opsional; ≤ 100 | C-11 |
| VR-05 | Nama tenaga kesehatan | Opsional; ≤ 100 | C-11 |
| VR-06 | Teks pertanyaan | Wajib; 1–250 | C-10, C-11 |
| VR-07 | Prioritas | Maks. 3 prioritas per kunjungan (tidak termasuk dirinya saat edit) | C-13 |
| VR-08 | Duplikat pertanyaan | `text_normalized` unik per kunjungan (saat edit, dirinya dikecualikan) | C-14 |
| VR-09 | Field catatan | Masing-masing ≤ 2.000 | C-11 |
| VR-10 | Catatan | Minimal satu field terisi | C-12 |
| VR-11 | Teks tindak lanjut | Wajib; 1–250 | C-10, C-11 |
| VR-12 | Tenggat | Opsional; valid; lampau → peringatan C-41 (bukan error) | C-15, C-41 |

**Character counter** tampil bila panjang ≥ 80% batas. Input yang melebihi batas tetap diterima di field (tidak dipotong diam-diam) tetapi Simpan menampilkan error.

**Normalisasi:** `trim()`; field satu baris: whitespace berturut-turut dikolaps menjadi satu spasi; field multiline: hanya trim. String kosong setelah trim pada field opsional disimpan sebagai `null`.

---

## 16. Architecture & Repository Contract

### 16.1 Arsitektur

- **Pola:** MVVM + Unidirectional Data Flow, mengikuti Android Guide to App Architecture [REF-03]: UI → ViewModel → Repository (interface di `domain`) → DataSource (Mock / Room / DataStore).
- **Source of truth:** database lokal (offline-first) [REF-04].
- **Single module `:app`** (+ opsional `:baselineprofile`). Modularisasi tidak diperlukan untuk skala ini.
- UI **tidak pernah** mengakses DAO/DataStore secara langsung.

### 16.2 Struktur Paket

```text
id.temankontrol.app
├── TemanKontrolApp.kt          (@HiltAndroidApp)
├── MainActivity.kt             (@AndroidEntryPoint, splash, edge-to-edge)
├── core/
│   ├── common/                 (AppResult, AppError, AppLogger, DispatcherQualifiers, Clock)
│   ├── designsystem/           (theme/, component/)
│   └── ui/                     (DateTimeFormatter id-ID, UiText, extensions)
├── domain/
│   ├── model/
│   ├── repository/             (interface)
│   └── validation/
├── data/
│   ├── local/                  (AppDatabase, dao/, entity/, mapper/)
│   ├── preferences/            (DataStore)
│   ├── repository/             (Room*Repository implementasi)
│   ├── mock/                   (InMemory*Repository, MockScenario, SampleData)
│   ├── suggestion/             (StaticSuggestionCatalog)
│   └── di/                     (Hilt modules: DatabaseModule, RepositoryModule)
├── feature/
│   ├── notice/  home/  history/  about/
│   ├── consultation/ (form/, detail/)
│   ├── question/  note/  followup/
│   (tiap feature: XxxScreen.kt, XxxViewModel.kt, XxxUiState.kt)
└── navigation/                 (Routes.kt, AppNavHost.kt, BottomBar.kt)
```

### 16.3 Error & Result

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data class Validation(val field: String, val rule: String) : AppError
    data object PriorityLimitExceeded : AppError
    data object Duplicate : AppError
    data object NotFound : AppError
    data object Storage : AppError        // semua exception I/O / SQLite lain
}
```

Repository **tidak melempar exception** ke ViewModel untuk operasi tulis; selalu `AppResult`. Flow baca boleh error; ViewModel menangkap dengan `.catch {}` → state Error; tombol "Coba lagi" memicu ulang pengumpulan flow (retry trigger `MutableSharedFlow`).

### 16.4 Kontrak Repository (`domain/repository`)

```kotlin
interface ConsultationRepository {
    fun observeUpcoming(limit: Int = 3): Flow<List<ConsultationSummary>>
    fun observePastDueCount(): Flow<Int>
    fun observeHistory(filter: StatusFilter): Flow<List<ConsultationSummary>>
    fun observe(id: ConsultationId): Flow<Consultation?>
    suspend fun create(input: ConsultationInput): AppResult<ConsultationId>
    suspend fun update(id: ConsultationId, input: ConsultationInput): AppResult<Unit>
    suspend fun setStatus(id: ConsultationId, status: ConsultationStatus): AppResult<Unit>
    suspend fun delete(id: ConsultationId): AppResult<Unit>
    suspend fun childCounts(id: ConsultationId): AppResult<ChildCounts> // untuk dialog C-30
}

interface QuestionRepository {
    fun observeByConsultation(id: ConsultationId): Flow<List<Question>>   // sudah terurut sesuai F-03
    suspend fun get(questionId: String): AppResult<Question>
    suspend fun create(id: ConsultationId, input: QuestionInput): AppResult<String>
    suspend fun update(questionId: String, input: QuestionInput): AppResult<Unit>
    suspend fun setDiscussed(questionId: String, discussed: Boolean): AppResult<Unit>
    suspend fun delete(questionId: String): AppResult<Unit>
    suspend fun priorityCount(id: ConsultationId, excludeQuestionId: String? = null): AppResult<Int>
}

interface NoteRepository {
    fun observe(id: ConsultationId): Flow<ConsultationNote?>
    suspend fun upsert(id: ConsultationId, input: NoteInput): AppResult<Unit>
    suspend fun delete(id: ConsultationId): AppResult<Unit>
}

interface FollowUpRepository {
    fun observeByConsultation(id: ConsultationId): Flow<List<FollowUpAction>> // terurut sesuai F-05
    fun observeOpen(limit: Int = 5): Flow<List<OpenFollowUp>>
    suspend fun get(actionId: String): AppResult<FollowUpAction>
    suspend fun create(id: ConsultationId, input: FollowUpInput): AppResult<String>
    suspend fun update(actionId: String, input: FollowUpInput): AppResult<Unit>
    suspend fun setCompleted(actionId: String, completed: Boolean): AppResult<Unit>
    suspend fun delete(actionId: String): AppResult<Unit>
}

interface PreferencesRepository {
    val noticeAccepted: Flow<Boolean>
    suspend fun setNoticeAccepted(): AppResult<Unit>
}

interface SuggestionCatalog {
    fun byCategory(category: SuggestionCategory): List<QuestionSuggestion>
}
```

**Aturan kontrak:**

- Mock (`data/mock`) dan Room (`data/repository`) mengimplementasikan interface yang **sama persis**, termasuk validasi bisnis (limit prioritas, duplikat, not found). Perilaku mock = perilaku Room.
- Mock memakai `MutableStateFlow` in-memory dan mendukung `MockScenario` (Normal/Kosong/Error/Lambat).
- Swap Mock → Room hanya mengubah binding di `RepositoryModule` (Hilt). Tidak ada perubahan di `feature/`.
- Tidak ada HTTP/network call di mana pun.

### 16.5 Pola ViewModel Form (standar untuk SCR-03, 05, 06, 07)

- State: field values, field errors, `isDirty`, `isSubmitting`, `canSubmit`.
- Draft disimpan di `SavedStateHandle` (FR-023).
- `submit()`: return segera jika `isSubmitting`; set `isSubmitting = true`; validasi; panggil repository; hasil sukses → one-shot event `NavigateBack`/`NavigateToDetail` + snackbar via `Channel`; gagal → map `AppError` ke copy, `isSubmitting = false`, draft tetap.
- Back ditekan & `isDirty` → dialog C-34.

---

## 17. Technology Stack & Project Configuration

| Area | Pilihan |
|---|---|
| Bahasa | Kotlin (versi di `libs.versions.toml`, saat ini 2.2.10) |
| Build | Gradle wrapper 9.6 + AGP 9.4.1, Kotlin DSL, version catalog |
| JDK | JBR bawaan Android Studio (Java 21+); `compileOptions` Java 17 |
| UI | Jetpack Compose (BOM), Material 3, `material-icons-core` |
| Navigasi | `androidx.navigation:navigation-compose` + `kotlinx-serialization-json` plugin |
| Lifecycle | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` |
| DI | Hilt + `hilt-navigation-compose`, via **KSP** |
| Database | Room (`room-runtime`, `room-ktx`, `room-compiler` via KSP, `room-testing`) |
| Preferences | `datastore-preferences` |
| Koleksi | `kotlinx-collections-immutable` |
| Splash | `core-splashscreen` |
| Test | JUnit4, `kotlinx-coroutines-test`, Turbine, Compose UI Test, Room in-memory, Hilt testing |
| Debug only | LeakCanary |
| Dilarang | Gson, RxJava, Retrofit/OkHttp, Firebase/analytics, `material-icons-extended`, library image loading |

**Konfigurasi wajib (dilakukan Day 1):**

1. `namespace` & `applicationId` → `id.temankontrol.app`; pindahkan paket dari `com.example.temankontrol`.
2. `allowBackup="false"`; perbarui `data_extraction_rules.xml` & `backup_rules.xml` agar mengecualikan semua (DEC-006).
3. Release: `isMinifyEnabled = true`, `isShrinkResources = true`.
4. Perbarui library lama di catalog (`coreKtx 1.10.1`, `lifecycle 2.6.1`, `activityCompose 1.8.0`) ke versi stabil terbaru yang kompatibel.
5. `buildConfig = true` (untuk `DEBUG`, `VERSION_NAME`).
6. Aktifkan StrictMode di `TemanKontrolApp` saat debug.
7. Room `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`.

### 17.1 Technical Spike TS-01 (gate Day 1, maks. 2 jam)

Tujuan: memastikan **Hilt + Room + KSP + kotlinx.serialization** bekerja dengan AGP 9.4.1 & Kotlin bawaan AGP.

- **Lulus:** `./gradlew assembleDebug` sukses dengan satu `@HiltViewModel`, satu entity Room dummy, dan satu route serializable; app terbuka di Redmi Note 10S/emulator.
- **Gagal:** berhenti, catat error, eskalasi ke project lead dengan opsi: (a) sesuaikan versi AGP/Kotlin/KSP ke kombinasi kompatibel terbaru, (b) ganti DI ke manual DI dengan interface yang sama. Keputusan dicatat sebagai DEC baru.

---

## 18. Delivery Plan

### 18.1 Day 0 — 6 Okt (malam) ✅

PRD v1.0 difinalisasi; toolchain diverifikasi (Android SDK, JBR, Gradle, `compileDebugKotlin` sukses).

### 18.2 Day 1 — 7 Okt

| Urutan | Workstream | Deliverable | Exit criteria |
|---|---|---|---|
| 1 | Setup | Konfigurasi wajib §17 (1–7), TS-01 | `assembleDebug` sukses; spike lulus |
| 2 | Device | USB debugging Redmi Note 10S (MIUI: aktifkan *USB debugging* dan *Install via USB*) + emulator API 30 cadangan | `adb devices` menampilkan perangkat; app terpasang |
| 3 | Contract | Domain model, `AppResult`, interface repository, validasi + unit test VR | `./gradlew test` hijau |
| 4 | Mock | InMemory repositories + SampleData + MockScenario | Data tampil di UI |
| 5 | Design system | Theme (light/dark), token, komponen §14.2 | Dipakai ≥ 2 layar |
| 6 | Navigation | Routes, NavHost, bottom bar, splash + notice gate | Semua route dapat dibuka |
| 7 | Screens | SCR-01, 02, 04, 08, 09 | State loading/empty/error/content dapat dipicu via Developer menu |

### 18.3 Day 2 — 8 Okt

| Workstream | Deliverable | Exit criteria |
|---|---|---|
| Forms | SCR-03, 05, 06, 07 dengan validasi, counter, date/time picker | VR-01–12 tampil benar |
| Aksi | Status, checklist, hapus + dialog, prioritas, duplikat, saran | Semua aksi bekerja dalam satu sesi |
| UX | Unsaved dialog, snackbar, keyboard/insets, dark mode | Tidak ada overflow di Redmi Note 10S |
| QA | Smoke test TC-001–013 versi mock | Tidak ada blocker |
| Handoff | Changelog PRD bila ada temuan | Room dapat dimulai tanpa redesign |

### 18.4 Day-2 Definition of Done

- [ ] SCR-01 s/d SCR-09 tersedia dan dapat dinavigasi.
- [ ] Beranda, Detail, Riwayat memiliki state loading/empty/error/content yang dapat dipicu.
- [ ] Semua form memvalidasi VR terkait dan mencegah double submit.
- [ ] Mock mendukung seluruh kontrak §16.4 dalam satu sesi.
- [ ] Tema terang & gelap; copy sesuai §14.3 (di `strings.xml`).
- [ ] Tidak ada backend, login, notifikasi, ekspor, izin INTERNET.
- [ ] Berjalan di Redmi Note 10S.

### 18.5 One-Month Plan

| Phase | Tanggal | Fokus | Exit criteria |
|---|---|---|---|
| 1 — Frontend | 6–8 Okt | §18.2–18.4 | Day-2 DoD |
| 2 — Data layer | 9–14 Okt | Room entity/DAO/index, DataStore, Room repositories, repository test | Repository test hijau; FR-017 lulus |
| 3 — Integrasi | 15–21 Okt | Swap binding ke Room, process-death draft, happy path end-to-end | Happy path §19.1 lulus di perangkat |
| 4 — Edge case & QA | 22–28 Okt | Error/duplikat/cascade/aksesibilitas/performa §11.1 | Regression suite hijau; budget performa terukur |
| 5 — UAT | 29 Okt–2 Nov | UAT 5 peserta, perbaikan | 0 critical defect |
| 6 — Finalisasi | 3–6 Nov | Release build tersign, Baseline Profile (opsional), data demo fiktif, dokumentasi | MVP checklist §19.3 lengkap |

---

## 19. User Flows & MVP Definition

### 19.1 Happy Path

```text
Buka app → Pemberitahuan Awal → Saya Mengerti → Beranda
→ Tambah Kunjungan → isi valid → Simpan → Detail
→ Tambah 4 pertanyaan (3 prioritas) → saat kontrol centang "Sudah dibahas"
→ Isi Hasil Konsultasi → Tambah Tindak Lanjut (tenggat besok)
→ Ubah status ke Selesai → Riwayat (filter Selesai) → buka Detail
→ Centang tindak lanjut selesai → tutup paksa app → buka lagi → semua data ada
```

### 19.2 Error Path

```text
Submit → Validasi
  ├── Invalid → error inline → koreksi → submit ulang
  ├── Tap berulang → diabaikan selama isSubmitting
  └── Storage gagal → draft tetap → snackbar C-50 + Coba lagi
```

### 19.3 MVP Checklist

**MVP selesai bila pengguna dapat menjalankan §19.1 dalam airplane mode dan data tetap ada setelah aplikasi ditutup paksa.**

- [ ] FR-001 s/d FR-025 terpenuhi sesuai stage MVP.
- [ ] AC-01 s/d AC-22 lulus.
- [ ] Budget performa NFR-001, 025–028 terpenuhi di Redmi Note 10S.
- [ ] NFR-014 & NFR-034 diverifikasi (merged manifest, backup off).
- [ ] 0 critical defect, 0 crash pada regression & UAT.
- [ ] Disclaimer & istilah konsisten dengan §3 dan §14.3.

---

## 20. Acceptance Criteria

| AC | Given | When | Then |
|---|---|---|---|
| AC-01 | First launch | Pengguna tap "Saya Mengerti" | Beranda terbuka; notice tidak muncul di peluncuran berikutnya |
| AC-02 | Form kunjungan valid | Tap Simpan berulang cepat | Tepat satu record dibuat; Detail terbuka |
| AC-03 | Field wajib kosong | Tap Simpan | Tidak ada tulis; error inline C-10 |
| AC-04 | Sudah ada 3 prioritas | Pertanyaan ke-4 ditandai prioritas | Toggle nonaktif dengan teks C-13; repository juga menolak |
| AC-05 | Pertanyaan "Kapan kontrol lagi?" ada | Simpan "  kapan KONTROL   lagi? " | Ditolak dengan C-14 |
| AC-06 | Pertanyaan belum dibahas | Dicentang | Status bertahan setelah relaunch; posisi item tidak berubah |
| AC-07 | Minimal satu field catatan valid | Simpan | Preview & waktu diperbarui tampil di Detail, bertahan setelah relaunch |
| AC-08 | Semua field catatan kosong | Simpan | Ditolak dengan C-12 |
| AC-09 | Tindak lanjut terbuka | Dicentang selesai | `completedAt` terisi; item pindah ke grup Selesai; bertahan setelah relaunch |
| AC-10 | Kunjungan punya pertanyaan, catatan, tindak lanjut | Hapus dikonfirmasi | Parent & seluruh child terhapus atomik; tidak muncul di Beranda/Riwayat |
| AC-11 | Penulisan storage gagal (skenario Error) | Submit | Draft tetap; snackbar C-50 dengan "Coba lagi" |
| AC-12 | Filter Selesai | Buka Riwayat | Hanya kunjungan Selesai tampil |
| AC-13 | Tidak ada data | Buka Beranda/Riwayat | Empty state C-62/C-60 dengan CTA |
| AC-14 | Airplane mode | Jalankan §19.1 | Tidak ada error jaringan; semua fitur P0 bekerja |
| AC-15 | Form berubah | Back (tombol/gesture) | Dialog C-34 tampil |
| AC-16 | Redmi Note 10S via USB | Developer menjalankan app | Terpasang & terbuka |
| AC-17 | Form setengah terisi | Rotasi layar / app dibunuh di background lalu dibuka kembali | Draft masih ada |
| AC-18 | Detail kunjungan terbuka di Riwayat | Kunjungan tidak ada (ID invalid) | C-52 tampil dengan tombol Kembali |
| AC-19 | Release build terpasang | Cek merged manifest & `adb shell dumpsys package` | Tidak ada izin INTERNET; `allowBackup=false` |
| AC-20 | Release build, dataset D-100 | Cold start 5 kali | Median `TotalTime` ≤ 1.500 ms |
| AC-21 | Kunjungan Terjadwal kemarin | Buka Riwayat/Beranda | Label "Lewat jadwal" tampil; Beranda menampilkan section "Perlu diperbarui" |
| AC-22 | Tema gelap sistem | Buka semua layar | Teks terbaca (NFR-031), tidak ada warna hardcode terang |

---

## 21. Testing Strategy

### 21.1 Level Pengujian

| Level | Lokasi | Cakupan | Target |
|---|---|---|---|
| Unit | `src/test` | Validasi VR, normalisasi, sorting, overdue/past-due, mapper, ViewModel (dengan fake repository) | Domain & validation ≥ 80% line coverage |
| Repository | `src/androidTest` (Room in-memory) | CRUD, cascade, unique index, limit prioritas dalam transaksi, error mapping | Semua method kontrak §16.4 |
| Contract | `src/androidTest` | Test suite abstrak yang sama dijalankan terhadap InMemory **dan** Room repository | Perilaku identik |
| UI | `src/androidTest` (Compose) | Validasi form, dialog, navigasi, state view | TC-001–013 |
| Manual device | Redmi Note 10S | Keyboard, font besar, dark mode, airplane, performa | TC-014–024 |
| UAT | 5 peserta | Skenario §19.1 | Metric §6.2 |

### 21.2 Test Cases

| TC | Skenario | Linked |
|---|---|---|
| TC-001 | First launch acknowledgement | AC-01 |
| TC-002 | Buat kunjungan valid | FR-003 |
| TC-003 | Tolak kunjungan invalid | AC-03 |
| TC-004 | Cegah duplikat karena tap cepat | AC-02 |
| TC-005 | Tambah/edit/hapus pertanyaan | FR-007 |
| TC-006 | Batas 3 prioritas | AC-04 |
| TC-007 | Tolak pertanyaan duplikat | AC-05 |
| TC-008 | Status dibahas bertahan | AC-06 |
| TC-009 | Upsert & hapus catatan | AC-07, AC-08 |
| TC-010 | Buat & selesaikan tindak lanjut | AC-09 |
| TC-011 | Tenggat lampau diizinkan dengan peringatan | VR-12 |
| TC-012 | Filter riwayat | AC-12 |
| TC-013 | Cascade delete | AC-10 |
| TC-014 | Pulih dari kegagalan storage | AC-11 |
| TC-015 | Journey penuh di airplane mode | AC-14 |
| TC-016 | Relaunch & force-stop, verifikasi data | FR-017 |
| TC-017 | Merged manifest & backup off; log tanpa isi pengguna | AC-19, NFR-017 |
| TC-018 | 360×640 dp, font scale 1.3 & 2.0 | NFR-012 |
| TC-019 | Install & run di Redmi Note 10S | AC-16 |
| TC-020 | Smoke test emulator API 30 | NFR-019 |
| TC-021 | Rotasi & process death (opsi Developer "Don't keep activities") | AC-17 |
| TC-022 | Budget performa (NFR-001, 025–028) | AC-20 |
| TC-023 | Dark mode semua layar | AC-22 |
| TC-024 | TalkBack membaca checkbox & status | NFR-032 |

### 21.3 Perintah Standar

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio1\jbr"
.\gradlew.bat test                    # unit test
.\gradlew.bat connectedDebugAndroidTest  # instrumented test (perangkat terhubung)
.\gradlew.bat lint                    # 0 error
.\gradlew.bat assembleRelease         # release build (R8)
```

### 21.4 Dataset Uji

- **D-0:** kosong.
- **D-SAMPLE:** 6 kunjungan fiktif (variasi status, lewat jadwal, dengan/tanpa catatan).
- **D-100:** 100 kunjungan × 8 pertanyaan × 4 tindak lanjut, teks panjang mendekati batas — untuk uji performa. Dibangkitkan oleh seeder debug-only.
- Dilarang memakai data pasien nyata (A-009).

---

## 22. Engineering Conventions

- **Gaya kode:** Kotlin official code style (formatter Android Studio). `./gradlew lint` tanpa error sebelum merge.
- **Penamaan:** sesuai §3 dan §16.2. Composable layar: `XxxScreen` (stateful, mengambil ViewModel) + `XxxContent` (stateless, untuk preview & test).
- **Preview:** setiap `XxxContent` memiliki `@Preview` light & dark minimal untuk state content dan empty.
- **Git:** branch `main` selalu bisa di-build; fitur di branch `feat/<scope>`; commit Conventional Commits menyertakan ID requirement.
- **Definition of Done per task:** sesuai requirement & AC terkait; test terkait ditulis/diperbarui; lint bersih; dicoba di perangkat/emulator; tidak ada string hardcode; PRD diperbarui bila ada perubahan perilaku.
- **Versi aplikasi:** `versionName` SemVer (`1.0.0` untuk MVP), `versionCode` naik setiap build yang dibagikan.
- **Signing:** keystore release disimpan di luar repo; kredensial via `keystore.properties` (di-gitignore).

---

## 23. Risks & Mitigations

| ID | Risk | Impact | Prob. | Mitigasi | Owner |
|---|---|---|---|---|---|
| R-01 | Scope creep sebelum Day-2 | High | High | Scope freeze §7; ide ke §29 | PM |
| R-02 | Hilt/Room/KSP tidak kompatibel dengan AGP 9.4.1 | High | Medium | TS-01 di awal Day 1 dengan jalur eskalasi | Tech lead |
| R-03 | Mock berperilaku beda dari Room | High | Medium | Kontrak §16.4 + contract test §21.1 | Developer |
| R-04 | Terlalu banyak layar untuk dua hari | High | Medium | Komponen reusable; pola form tunggal §16.5; tanpa eksperimen visual | FE lead |
| R-05 | Data kesehatan bocor (log/backup) | High | Medium | DEC-006, DEC-011, NFR-017, TC-017 | Developer/QA |
| R-06 | Wording terkesan nasihat medis | High | Medium | Glosarium & kata terlarang §3; copy §14.3 | PM |
| R-07 | Kehilangan data saat perubahan skema | High | Low | Schema export + migration test; destructive migration dilarang | Developer |
| R-08 | Redmi Note 10S gagal terdeteksi (MIUI) | Medium | Medium | Aktifkan *Install via USB*; emulator API 30 cadangan; kabel data cadangan | Tech lead |
| R-09 | Performa turun di data besar | Medium | Low | Index §15.3, projection, D-100, budget §11.1 | Developer |
| R-10 | UAT tertunda | Medium | Medium | Rekrut 5 peserta paling lambat 24 Okt | PM/QA |
| R-11 | Pengguna kehilangan data saat ganti HP (konsekuensi DEC-006) | Medium | Medium | Dijelaskan di C-70; ekspor di backlog | PM |

---

## 24. Dependencies

| Dependency | Stage | Bottleneck | Status |
|---|---|---|---|
| Keputusan framework, minSdk, Day-2, identitas | Day 0 | High | ✅ DEC-001, 002, 005, 007 |
| TS-01 kompatibilitas toolchain | Day 1 | High | Pending |
| Redmi Note 10S + USB debugging | Day 1 | High | Pending (`adb devices` belum mendeteksi perangkat per 6 Okt) |
| Kontrak repository | Day 1 | High | Terdefinisi §16.4 |
| Copy saran pertanyaan | Day 2 | Low | Terdefinisi §14.4 |
| Peserta UAT | Phase 5 | Medium | Pending |

---

## 25. Open Questions

Semua pertanyaan blocking telah dijawab. Sisa pertanyaan non-blocking:

| ID | Pertanyaan | Default bila tidak dijawab | Batas |
|---|---|---|---|
| OQ-05 | Apakah pengingat lokal masuk MVP? | Tidak (backlog) | 21 Okt |
| OQ-06 | Perlu pencarian teks riwayat? | Tidak (backlog) | Setelah MVP |
| OQ-07 | Perlu ekspor PDF/share? | Tidak (backlog) | Setelah MVP |
| OQ-08 | Perlu app lock untuk demo? | Tidak | Setelah MVP |
| OQ-09 | Pendamping perlu profil pasien terpisah? | Tidak (single profile) | Setelah MVP |
| OQ-10 | Perlu perangkat Android cadangan selain Redmi Note 10S? | Emulator API 30 sebagai cadangan | Sebelum UAT |
| OQ-11 | Desain ikon aplikasi (launcher icon)? | Ikon vektor sederhana berbasis warna seed | 3 Nov |

---

## 26. Assumptions

| ID | Asumsi | Dampak bila salah |
|---|---|---|
| A-001 | Deadline final 6 Nov 2026 | Roadmap bergeser |
| A-003 | Single-user, single-device | Model akun/sync berubah besar |
| A-004 | Pengguna utama pasien umum dewasa | Copy & skenario berubah |
| A-005 | Core flow: prepare → record → follow up | Prioritas fitur berubah |
| A-006 | Tanpa backend di MVP | Auth/API menjadi scope baru |
| A-007 | Batas 3 prioritas diterima sebagai aturan UX | Validasi & UI berubah |
| A-008 | 5 peserta UAT tersedia | Target metric disesuaikan |
| A-009 | Data demo fiktif | Risiko privasi |
| A-010 | Bahasa Indonesia satu-satunya bahasa | Perlu localization |
| A-011 | Redmi Note 10S tersedia sebagai perangkat demo | Strategi demo berubah |

(A-002 tentang framework telah menjadi DEC-001.)

---

## 27. Requirement Traceability

| Problem | Story | Feature / FR | Screen | Kontrak / Data | AC / TC |
|---|---|---|---|---|---|
| P-01 | US-02, US-03, US-09 | F-03, F-07; FR-007–010, FR-016 | SCR-04, SCR-05 | QuestionRepository, SuggestionCatalog / `questions` | AC-04–06; TC-005–008 |
| P-02 | US-04 | F-04; FR-011 | SCR-04, SCR-06 | NoteRepository / `consultation_notes` | AC-07–08; TC-009 |
| P-03 | US-05, US-06 | F-05, F-08; FR-002, FR-012–013, FR-025 | SCR-02, SCR-04, SCR-07 | FollowUpRepository / `follow_up_actions` | AC-09, AC-21; TC-010–011 |
| P-01–03 | US-01, US-07 | F-02; FR-003–006, FR-024 | SCR-03, SCR-04 | ConsultationRepository / `consultations` | AC-02–03, AC-10, AC-18; TC-002–004, TC-013 |
| P-03 | US-08 | F-06; FR-014–015 | SCR-08 | ConsultationRepository | AC-12–13; TC-012 |
| P-04 | Semua | F-01, F-09; FR-001, FR-017–023; NFR-001–039 | Semua | Room, DataStore | AC-01, AC-11, AC-14–17, AC-19–20, AC-22; TC-014–024 |

---

## 28. Scope Control Rules

1. P0 tidak boleh dihapus tanpa keputusan project lead + entri DEC.
2. P2/P3 otomatis ditunda bila milestone terancam.
3. Change request memuat: alasan, nilai bagi pengguna, ID terdampak, estimasi, risiko, keputusan.
4. Login, backend, cloud, notifikasi, ekspor tidak boleh masuk P0 tanpa pengurangan scope sebanding.
5. Deadline 8 Okt adalah hard milestone; 6 Nov adalah target delivery (A-001).
6. Fungsi & state handling lebih dulu dari polish visual.
7. Data pasien nyata tidak dipakai untuk demo/testing.
8. iOS tidak masuk scope tanpa perubahan timeline & resource.

---

## 29. Backlog (Post-MVP, tidak dikerjakan sekarang)

| Item | Pri | Catatan |
|---|---|---|
| Pengingat lokal kunjungan & tenggat | P2 | Butuh izin `POST_NOTIFICATIONS` (API 33+) & exact alarm policy |
| Ekspor/share ringkasan | P2 | Menjadi solusi R-11 |
| App lock (PIN/biometrik) & `FLAG_SECURE` | P2 | |
| Pencarian riwayat | P2 | Room FTS4 |
| Sinkronisasi cloud | P3 | Butuh auth, conflict resolution, privacy notice — PRD change request |

---

## 30. References

| ID | Sumber |
|---|---|
| REF-01 | NHS — *Making the most of your GP appointment / Questions to ask your doctor*. https://www.nhs.uk |
| REF-02 | AHRQ — *Question Builder* (Questions Are the Answer). https://www.ahrq.gov/questions/question-builder/index.html |
| REF-03 | Android Developers — *Guide to app architecture*. https://developer.android.com/topic/architecture |
| REF-04 | Android Developers — *Build an offline-first app*. https://developer.android.com/topic/architecture/data-layer/offline-first |
| REF-05 | OWASP — *Mobile Application Security Verification Standard (MASVS)*. https://mas.owasp.org/MASVS/ |
| REF-06 | Android Developers — *Save data in a local database using Room*. https://developer.android.com/training/data-storage/room |
| REF-07 | Android Developers — *Jetpack Compose performance best practices*. https://developer.android.com/develop/ui/compose/performance/bestpractices |
| REF-08 | Android Developers — *Accessibility: touch target size*. https://developer.android.com/guide/topics/ui/accessibility/apps |
| REF-09 | Android Developers — *Back up user data with Auto Backup*. https://developer.android.com/identity/data/autobackup |

---

## 31. Changelog

| Versi | Tanggal | Perubahan |
|---|---|---|
| 0.2 | 6 Okt 2026 | Draft: Android-only, offline, Redmi Note 10S (arsip: `docs/archive/PRD_v0.2.md`) |
| 1.0 | 6 Okt 2026 | **Approved.** OQ-01–04 dijawab (DEC-001, 002, 005, 007). Ditambahkan: Decision Log, Glosarium, kontrak repository Kotlin, skema Room + index, validation rules (VR), microcopy SSOT, design tokens, katalog saran, standar performa terukur (NFR-024–030) & aturan implementasi, privasi teknis (backup off, tanpa INTERNET), aksesibilitas (NFR-031–033), kompatibilitas (edge-to-edge, predictive back, dark mode), F-08 Beranda & F-09 Tentang, FR-023–025, AC-17–22, TC-021–024, technical spike TS-01, engineering conventions, references. Diperbaiki: timeline Day 1/Day 2, definisi Beranda & Riwayat, urutan daftar, transisi status, inkonsistensi nama layar, tag referensi yang tidak dapat ditelusuri, risiko "PRD technology-neutral" yang bertentangan dengan keputusan stack. Lokasi dipindah ke `docs/PRD.md`. |
