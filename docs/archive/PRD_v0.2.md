# Product Requirements Document

**Produk:** Teman Kontrol  
**Dokumen:** PRD.md  
**Versi:** 0.2  
**Status:** Draft direvisi untuk validasi akhir

## Executive Summary

**Core Problem:** Pasien sering datang ke konsultasi tanpa daftar pertanyaan yang terstruktur, kesulitan mencatat hasil pembicaraan, dan tidak memiliki satu tempat untuk melacak tindak lanjut. Panduan NHS menyarankan pasien menulis 2–3 pertanyaan terpenting, membawa catatan, mencatat hasil konsultasi, dan memastikan langkah berikutnya; AHRQ juga menyediakan QuestionBuilder untuk membantu pasien dan pendamping mempersiapkan kunjungan medis.[web:3][web:4]

**Core Solution:** Teman Kontrol adalah aplikasi mobile Android offline-first untuk menyiapkan pertanyaan sebelum konsultasi, mencentang pertanyaan saat konsultasi, mencatat hasil, serta memantau tindak lanjut setelah konsultasi. Aplikasi adalah alat pencatatan pribadi, bukan alat diagnosis, konsultasi jarak jauh, rekam medis resmi, atau pemberi rekomendasi klinis.[file:34]

**Platform Decision Update:** Produk secara resmi ditetapkan **Android-only untuk fase proyek ini** karena Android dapat dibangun, diuji, dan dipasang lebih sederhana daripada iOS; Flutter mendukung pengembangan dan pemasangan aplikasi Android pada perangkat fisik maupun emulator, termasuk dari lingkungan Windows.[web:50][web:51][web:62]

**Storage Decision Update:** MVP tidak memerlukan backend. Data disimpan secara lokal di perangkat menggunakan app-private storage/local database; Flutter merekomendasikan SQLite untuk data lokal yang perlu disimpan dan di-query pada perangkat.[web:36][file:34]

**Primary Test Device Update:** Perangkat uji utama ditetapkan **Redmi Note 10S**, yang memiliki RAM 6/8 GB dan penyimpanan 64/128 GB, sehingga memadai untuk aplikasi form-based offline yang ringan seperti Teman Kontrol.[web:66][web:69]

**Day-2 Frontend Scope:** Navigasi dan antarmuka untuk Beranda, Riwayat, Form Kunjungan, Detail Kunjungan, Persiapan Pertanyaan, Hasil Konsultasi, dan Tindak Lanjut; menggunakan mock repository, form validation, serta loading, empty, error, and success states.[file:34]

**MVP Scope:** Seluruh core journey berjalan secara offline dan persisten: membuat kunjungan, menyiapkan pertanyaan, mencatat hasil, membuat tindak lanjut, mengubah status tindak lanjut, melihat riwayat, serta mengedit dan menghapus data.[file:34]

**Post-MVP Scope:** Pengingat lokal, ekspor ringkasan, kunci aplikasi, sinkronisasi opsional, dan dukungan pendamping lintas perangkat.[file:34]

**Critical Risks:** Scope creep, frontend dua hari berubah menjadi implementasi backend, keputusan teknologi terlambat, data mock tidak sesuai model final, dan penanganan data kesehatan yang kurang aman.[file:34]

**Recommended Development Sequence:** Kunci model data dan kontrak repository → selesaikan frontend dengan mock data → implementasikan penyimpanan lokal → integrasikan seluruh flow → lakukan pengujian dan perbaikan → finalisasi dokumentasi dan demo.[file:34]

## 1. Document Information

| Field | Value |
|---|---|
| Nama produk | Teman Kontrol |
| Versi PRD | 0.2 |
| Status | Draft direvisi untuk validasi akhir |
| Tanggal | 6 Oktober 2026 |
| Target platform | **Android mobile only** |
| Target release | MVP v1.0 |
| Timeline proyek | 6 Oktober–6 November 2026 |
| Deadline frontend | 8 Oktober 2026 |
| Deadline keseluruhan | 6 November 2026 |
| Model operasi MVP | Offline-first, single-device, single-user |
| Primary test device | Redmi Note 10S |
| Target OS awal | Android 11 atau lebih tinggi |
| Pemilik keputusan produk | Project lead / tim mahasiswa |

> **ASSUMPTION A-001:** “Sekitar satu bulan” diterjemahkan menjadi target 6 November 2026.  
> **ASSUMPTION A-002:** Implementasi awal menggunakan Flutter untuk Android dan penyimpanan lokal. Keputusan ini sekarang menjadi **default working assumption yang dipakai tim**, tetapi masih dapat diganti jika seluruh tim sepakat sebelum implementasi dimulai.  
> **ASSUMPTION A-003:** Redmi Note 10S menjadi perangkat uji utama dan baseline kompatibilitas minimum proyek.

## 2. Product Overview

### 2.1 Identitas Produk

- **Nama:** Teman Kontrol
- **One-line description:** Pendamping offline untuk mempersiapkan, mencatat, dan menindaklanjuti konsultasi kesehatan.
- **Vision:** Membantu pasien datang ke konsultasi dengan lebih siap dan pulang dengan langkah tindak lanjut yang lebih jelas.
- **Mission:** Menyatukan pertanyaan, catatan hasil, dan tindak lanjut konsultasi dalam alur mobile yang ringan, aman, dan mudah digunakan.

### 2.2 Ringkasan

Teman Kontrol berfokus pada tiga tahap: **sebelum kontrol**, **saat kontrol**, dan **setelah kontrol**. Pengguna membuat entri kunjungan, menulis serta memprioritaskan pertanyaan, mencentang pertanyaan yang sudah dibahas, menulis ringkasan hasil, lalu membuat tugas tindak lanjut.[file:34]

Produk tidak menilai gejala, menafsirkan hasil pemeriksaan, menentukan diagnosis, atau menyarankan terapi. Nilai utama produk adalah mengurangi catatan yang tercecer dan menyediakan kontinuitas informasi pribadi dari persiapan sampai tindak lanjut.[file:34]

### 2.3 Nilai Pengguna

- Satu tempat untuk seluruh catatan terkait satu kunjungan.[file:34]
- Pertanyaan prioritas tampil lebih dahulu ketika waktu konsultasi terbatas.[file:34]
- Hasil konsultasi tidak tercampur dengan rencana tindak lanjut.[file:34]
- Data inti tetap dapat digunakan tanpa internet; pendekatan offline-first menjaga fungsi kritis tersedia saat jaringan tidak andal.[web:19][file:34]
- Pengguna tidak perlu membuat akun atau bergantung pada server untuk menjalankan fungsi inti.[file:34]

## 3. Background & Problem Statement

### 3.1 Current Situation

Pengguna dapat menyimpan jadwal di kalender, pertanyaan di aplikasi catatan, dan tindak lanjut di tempat lain. Pemisahan ini membuat konteks satu kunjungan sulit ditinjau sebagai rangkaian yang utuh.[file:34]

### 3.2 Problems

| ID | Problem |
|---|---|
| P-01 | Pengguna dapat lupa pertanyaan penting sebelum atau saat konsultasi. |
| P-02 | Hasil pembicaraan dan instruksi tindak lanjut tidak tersimpan secara terstruktur. |
| P-03 | Pengguna sulit mengetahui tindakan mana yang belum selesai setelah konsultasi. |
| P-04 | Aplikasi yang bergantung pada akun dan internet menambah kompleksitas yang tidak diperlukan untuk proyek sederhana. |

### 3.3 Root Causes

- Persiapan, catatan hasil, dan tindak lanjut berada di media berbeda.[file:34]
- Tidak ada struktur yang membedakan pertanyaan prioritas dari pertanyaan biasa.[file:34]
- Catatan bebas tidak otomatis menjadi daftar tindakan.[file:34]
- Scope produk kesehatan sering melebar ke diagnosis, konsultasi daring, atau integrasi fasilitas kesehatan.[file:34]

### 3.4 Impact

- Pertanyaan yang dianggap penting tidak sempat dibahas.[file:34]
- Pengguna dapat lupa siapa yang perlu dihubungi, kapan kembali, atau tindakan apa yang harus dilakukan.[file:34]
- Riwayat konsultasi pribadi sulit ditinjau sebelum kunjungan berikutnya.[file:34]

### 3.5 Opportunity

Panduan pasien menyarankan persiapan pertanyaan, membawa daftar informasi, mengambil catatan, memastikan pemahaman, dan mengetahui langkah berikutnya.[web:4] Teman Kontrol menerjemahkan kebiasaan tersebut menjadi workflow sederhana tanpa mencoba menggantikan tenaga kesehatan.[file:34]

### 3.6 Problem Statement

> Pasien umum dan pendamping membutuhkan cara sederhana untuk mengorganisasi persiapan, hasil, dan tindak lanjut konsultasi pada satu perangkat, karena catatan yang tersebar dapat membuat pertanyaan penting dan tindakan lanjutan terlewat.[file:34]

## 4. Goals & Objectives

### 4.1 Product Goals

- Menyediakan satu record terstruktur untuk setiap konsultasi.[file:34]
- Mendukung core journey dari persiapan sampai tindak lanjut tanpa internet.[file:34]
- Menyediakan UI yang dapat diselesaikan dalam milestone frontend dua hari tanpa menunggu backend.[file:34]
- Meminimalkan data kesehatan yang dikumpulkan.[file:34]

### 4.2 User Goals

- Menyiapkan topik dan pertanyaan sebelum bertemu tenaga kesehatan.[file:34]
- Melihat tiga pertanyaan prioritas dengan cepat.[file:34]
- Menandai pertanyaan yang sudah dibahas.[file:34]
- Menyimpan ringkasan hasil dan tindakan berikutnya.[file:34]
- Meninjau kunjungan sebelumnya.[file:34]

### 4.3 Project Goals

- Frontend demonstrable pada 8 Oktober 2026.[file:34]
- MVP end-to-end demonstrable pada 6 November 2026.[file:34]
- Dokumen, demo, dan implementasi menggunakan istilah dan scope yang konsisten.[file:34]
- Tidak ada fitur klinis yang memerlukan validasi medis pada MVP.[file:34]
- Build Android dapat dijalankan pada Redmi Note 10S sebagai perangkat demo utama.[web:66][file:34]

### 4.4 Success Metrics

| Metric | Cara Ukur | Target MVP |
|---|---|---|
| Core journey task success | UAT membuat kunjungan sampai menyelesaikan satu tindak lanjut | Target awal 4 dari 5 peserta tanpa bantuan; **ASSUMPTION** |
| Data persistence | Tutup dan buka ulang aplikasi setelah menyimpan | 100% skenario persistence lulus |
| Critical defect | Bug yang memblokir core journey | 0 saat rilis |
| Validation coverage | Field wajib yang memiliki pesan error teruji | 100% |
| Local response | Waktu membuka daftar/detail pada perangkat uji | Maksimal 500 ms pada Redmi Note 10S; **ASSUMPTION** |
| Crash-free scripted test | Eksekusi regression test utama | 100% selama pengujian rilis |

## 5. Scope

| Scope | Fitur | Priority | Deadline | Status |
|---|---|---:|---|---|
| Day-2 Frontend | Navigasi utama dan seluruh screen core | P0 | 8 Okt 2026 | Must Have |
| Day-2 Frontend | Mock repository dan sample data | P0 | 8 Okt 2026 | Must Have |
| Day-2 Frontend | Form create/edit dengan validation UI | P0 | 8 Okt 2026 | Must Have |
| Day-2 Frontend | Loading, empty, error, success states | P0 | 8 Okt 2026 | Must Have |
| MVP | Penyimpanan lokal persisten | P0 | 6 Nov 2026 | Must Have |
| MVP | CRUD kunjungan | P0 | 6 Nov 2026 | Must Have |
| MVP | Pertanyaan dan maksimum 3 prioritas | P0 | 6 Nov 2026 | Must Have |
| MVP | Checklist pertanyaan saat konsultasi | P0 | 6 Nov 2026 | Must Have |
| MVP | Catatan hasil konsultasi | P0 | 6 Nov 2026 | Must Have |
| MVP | CRUD tindak lanjut dan status selesai | P0 | 6 Nov 2026 | Must Have |
| MVP | Riwayat dan filter status sederhana | P1 | 6 Nov 2026 | Should Have |
| MVP | Saran pertanyaan statis dan disclaimer | P1 | 6 Nov 2026 | Should Have |
| Post-MVP | Pengingat lokal | P2 | Setelah MVP | Nice to Have |
| Post-MVP | Ekspor/share ringkasan | P2 | Setelah MVP | Nice to Have |
| Post-MVP | App lock/PIN/biometric | P2 | Setelah MVP | Nice to Have |
| Post-MVP | Sinkronisasi cloud opsional | P3 | Setelah MVP | Optional |
| Out of Scope | Diagnosis, triase, atau rekomendasi terapi | P3 | Tidak dikerjakan | Excluded |
| Out of Scope | Chat dokter, telemedicine, dan booking fasilitas | P3 | Tidak dikerjakan | Excluded |
| Out of Scope | Login, admin, dashboard tenaga kesehatan | P3 | Tidak dikerjakan | Excluded |
| Out of Scope | Rekam medis resmi dan integrasi rumah sakit | P3 | Tidak dikerjakan | Excluded |
| Out of Scope | iOS release dalam fase proyek ini | P3 | Tidak dikerjakan | Excluded |

## 6. Target Users

### 6.1 Primary Persona

| Attribute | Description |
|---|---|
| Role | Pasien dewasa yang datang ke puskesmas, klinik, atau rumah sakit |
| Characteristics | Menggunakan Android; dapat memiliki kontrol rutin atau kunjungan sesekali |
| Goals | Tidak melupakan pertanyaan dan memahami tindak lanjut |
| Needs | Input cepat, tulisan jelas, akses offline, riwayat terstruktur |
| Pain points | Catatan tersebar, lupa hal yang ingin ditanyakan, instruksi sulit dilacak |
| Technical familiarity | Dasar–menengah |
| Typical scenario | Menyiapkan pertanyaan malam sebelumnya, membuka checklist saat konsultasi, mencatat hasil setelahnya |

### 6.2 Secondary Persona

| Attribute | Description |
|---|---|
| Role | Anggota keluarga atau pendamping pasien |
| Characteristics | Mencatat atas persetujuan pasien pada perangkat Android yang sama |
| Goals | Membantu percakapan dan memastikan tindak lanjut tidak terlewat |
| Needs | Struktur ringkas dan status tindakan yang jelas |
| Pain points | Harus mendengar, bertanya, dan mencatat dalam waktu bersamaan |
| Technical familiarity | Dasar–menengah |
| Typical scenario | Menyusun pertanyaan bersama pasien dan mencatat arahan tenaga kesehatan |

## 7. User Stories

| ID | User Story | Priority | Acceptance Criteria Ringkas |
|---|---|---:|---|
| US-01 | As a patient, I want to create a consultation record, so that all related information has one context. | P0 | Record valid tersimpan dan tampil di daftar. |
| US-02 | As a patient, I want to add questions and mark up to three as priority, so that the most important topics appear first. | P0 | Prioritas diurutkan di atas; prioritas keempat ditolak. |
| US-03 | As a patient, I want to mark questions as discussed, so that I can see what remains during the visit. | P0 | Status berubah dan persisten. |
| US-04 | As a patient, I want to save a consultation summary, so that I can review what was discussed. | P0 | Catatan tersimpan pada record yang benar. |
| US-05 | As a patient, I want to add follow-up actions with optional due dates, so that I know what to do next. | P0 | Action tampil dengan status Belum Selesai. |
| US-06 | As a patient, I want to mark a follow-up action complete, so that progress is visible. | P0 | Status dan completion timestamp tersimpan. |
| US-07 | As a patient, I want to edit or delete my record, so that incorrect or unnecessary data can be corrected. | P0 | Edit terpantul; delete memerlukan konfirmasi. |
| US-08 | As a patient, I want to view past consultations, so that I can prepare for a future visit. | P1 | Kunjungan selesai tampil di riwayat. |
| US-09 | As a patient, I want question suggestions, so that I can start without a blank page. | P1 | Memilih saran membuat pertanyaan editable. |

## 8. Feature Specification

### 8.1 Feature F-01 — First-Launch Notice

- **Purpose:** Menjelaskan fungsi dan batas produk.
- **User:** Primary dan secondary user.
- **Priority/Timeline:** P0; Day-2 Frontend dan MVP.
- **Preconditions:** Aplikasi dibuka pertama kali.
- **Main Flow:** Sistem menampilkan notice → pengguna membaca → memilih “Saya Mengerti” → Beranda terbuka.
- **Alternative Flow:** Pengguna membuka kembali notice dari Tentang Aplikasi.
- **Error Flow:** Jika status persetujuan gagal disimpan, notice tampil kembali pada pembukaan berikutnya tanpa memblokir sesi saat ini.
- **Input:** Acknowledgement boolean.
- **Processing:** Simpan flag lokal.
- **Output:** Akses ke Beranda.
- **Validation Rules:** CTA aktif; tidak ada checkbox persetujuan medis.
- **Business Rules:** Notice wajib menyatakan bukan diagnosis, bukan layanan darurat, dan data dimasukkan oleh pengguna.[file:34]
- **UI Behavior:** Modal/full screen ringkas; tombol utama tetap terlihat tanpa horizontal scroll.
- **API Dependency:** Tidak ada.
- **Data Dependency:** AppPreference.noticeAccepted.
- **Acceptance Criteria:** Given first launch, when user taps “Saya Mengerti”, then Home opens and notice does not reappear on the next launch.

### 8.2 Feature F-02 — Consultation Record Management

- **Purpose:** Membuat wadah untuk seluruh informasi satu kunjungan.
- **User:** Primary/secondary user.
- **Priority/Timeline:** P0; Day-2 Frontend dan MVP.
- **Preconditions:** Notice telah diterima.
- **Main Flow:** Beranda → Tambah Kunjungan → isi judul, fasilitas/tenaga kesehatan opsional, tanggal, alasan → Simpan → Detail.
- **Alternative Flow:** Pengguna menyimpan tanpa nama fasilitas atau tenaga kesehatan.
- **Exception Flow:** Field wajib kosong, tanggal tidak valid, double tap Simpan, atau storage gagal.
- **Input:** Judul, tanggal/waktu, alasan, fasilitas opsional, nama tenaga kesehatan opsional.
- **Processing:** Trim teks, validasi, buat UUID, status Terjadwal, timestamps.
- **Output:** Record baru pada daftar dan halaman detail.
- **Validation Rules:** Judul 1–80 karakter; alasan 1–500; field opsional maksimum 100; tanggal wajib valid.
- **Business Rules:** Record baru berstatus Terjadwal; status dapat menjadi Selesai atau Dibatalkan; UUID tidak dapat diubah.[file:34]
- **UI Behavior:** Inline error; Simpan disabled ketika submit berlangsung; snackbar sukses; unsaved-change dialog saat keluar.
- **API Dependency:** Tidak ada pada MVP; gunakan ConsultationRepository.
- **Data Dependency:** Consultation.
- **Acceptance Criteria:** Given valid values, when Save is tapped once or repeatedly, then exactly one record is created and Detail opens.

### 8.3 Feature F-03 — Question Preparation & Checklist

- **Purpose:** Membantu pengguna menyiapkan dan melacak pertanyaan.
- **User:** Primary/secondary user.
- **Priority/Timeline:** P0; Day-2 Frontend dan MVP.
- **Preconditions:** Consultation record tersedia.
- **Main Flow:** Detail → Tambah Pertanyaan → ketik/pilih saran → tandai prioritas opsional → Simpan → pertanyaan tampil; saat konsultasi pengguna mencentang “Sudah dibahas”.
- **Alternative Flow:** Edit, ubah urutan secara otomatis berdasarkan prioritas, atau hapus pertanyaan.
- **Exception Flow:** Teks kosong, duplikat identik, prioritas keempat, atau persistence gagal.
- **Input:** Question text, priority flag, discussed flag.
- **Processing:** Normalisasi whitespace; cek duplicate case-insensitive dalam satu consultation; hitung prioritas.
- **Output:** Daftar prioritas lebih dahulu, lalu non-prioritas; masing-masing memiliki status.
- **Validation Rules:** 1–250 karakter; maksimum 3 pertanyaan prioritas; duplicate identik ditolak.
- **Business Rules:** Jumlah pertanyaan total tidak dibatasi pada level produk; UI memberikan peringatan bila daftar panjang. Prioritas dibatasi tiga, selaras dengan praktik memusatkan 2–3 pertanyaan penting.[web:4][file:34]
- **UI Behavior:** Checkbox memiliki label teks; perubahan status optimistis hanya jika local write berhasil; kegagalan mengembalikan state sebelumnya.
- **API Dependency:** Tidak ada pada MVP.
- **Data Dependency:** ConsultationQuestion, QuestionSuggestion.
- **Acceptance Criteria:** Given three priority questions, when a fourth is marked priority, then save is blocked and a clear message is shown.

### 8.4 Feature F-04 — Consultation Result Notes

- **Purpose:** Menyimpan ringkasan yang ditulis pengguna setelah atau selama konsultasi.
- **User:** Primary/secondary user.
- **Priority/Timeline:** P0; Day-2 Frontend dan MVP.
- **Preconditions:** Consultation tersedia.
- **Main Flow:** Detail → Hasil Konsultasi → isi ringkasan, arahan, dan catatan tambahan → Simpan → kembali ke Detail.
- **Alternative Flow:** Simpan sebagian field; edit kemudian.
- **Exception Flow:** Semua field kosong, storage gagal, atau pengguna keluar dengan perubahan belum disimpan.
- **Input:** Summary, providerInstruction, additionalNotes.
- **Processing:** Trim, validate length, upsert satu ConsultationNote per consultation.
- **Output:** Preview ringkas dan waktu pembaruan.
- **Validation Rules:** Minimal satu field terisi; setiap field maksimum 2.000 karakter.
- **Business Rules:** Label selalu “Catatan pengguna”, bukan “Catatan dokter” atau “rekam medis”.[file:34]
- **UI Behavior:** Multiline fields; character counter mendekati batas; success feedback setelah write selesai.
- **API Dependency:** Tidak ada pada MVP.
- **Data Dependency:** ConsultationNote.
- **Acceptance Criteria:** Given at least one valid note, when Save is tapped, then the note persists and is shown on Consultation Detail after relaunch.

### 8.5 Feature F-05 — Follow-Up Actions

- **Purpose:** Mengubah langkah berikutnya menjadi daftar tindakan yang dapat dilacak.
- **User:** Primary/secondary user.
- **Priority/Timeline:** P0; Day-2 Frontend dan MVP.
- **Preconditions:** Consultation tersedia.
- **Main Flow:** Detail → Tambah Tindak Lanjut → isi tindakan dan due date opsional → Simpan → item Belum Selesai → tandai selesai.
- **Alternative Flow:** Edit, hapus, atau tandai kembali belum selesai.
- **Exception Flow:** Teks kosong, date invalid, duplicate submit, atau persistence gagal.
- **Input:** Action text, optional due date, status.
- **Processing:** Validate; create UUID; set completion timestamp saat selesai.
- **Output:** Daftar open actions lebih dahulu, completed actions setelahnya.
- **Validation Rules:** Tindakan 1–250 karakter; due date harus tanggal valid; tanggal lampau diizinkan dengan warning, bukan error.
- **Business Rules:** Satu action terkait tepat satu consultation; menghapus consultation menghapus child data setelah konfirmasi.[file:34]
- **UI Behavior:** Status dapat diubah satu tap; delete membutuhkan konfirmasi; item terlambat memakai teks dan ikon, bukan warna saja.
- **API Dependency:** Tidak ada pada MVP.
- **Data Dependency:** FollowUpAction.
- **Acceptance Criteria:** Given an open action, when user marks it complete, then completedAt is recorded and the item moves to the completed group.

### 8.6 Feature F-06 — History & Status Filter

- **Purpose:** Menemukan kembali konsultasi sebelumnya.
- **User:** Primary/secondary user.
- **Priority/Timeline:** P1; Day-2 UI dan MVP.
- **Preconditions:** Minimal nol record; empty state tetap valid.
- **Main Flow:** Bottom navigation → Riwayat → lihat record terbaru → pilih filter Semua/Terjadwal/Selesai/Dibatalkan → buka Detail.
- **Alternative Flow:** Tidak ada hasil pada filter.
- **Exception Flow:** Read database gagal.
- **Input:** Selected filter.
- **Processing:** Query/sort descending by appointment date; filter by status.
- **Output:** List card atau empty/error state.
- **Validation Rules:** Hanya enum status yang didukung.
- **Business Rules:** Pengguna dapat mengubah status dari Detail.[file:34]
- **UI Behavior:** Filter chips; empty state menyediakan CTA Tambah Kunjungan bila seluruh data kosong.
- **API Dependency:** Tidak ada pada MVP.
- **Data Dependency:** Consultation.
- **Acceptance Criteria:** Given records with different statuses, when a filter is selected, then only matching records are displayed.

### 8.7 Feature F-07 — Static Question Suggestions

- **Purpose:** Mengurangi blank-page problem tanpa memberi nasihat klinis.
- **User:** Primary/secondary user.
- **Priority/Timeline:** P1; MVP.
- **Preconditions:** Pengguna membuka form pertanyaan.
- **Main Flow:** Pilih kategori Gejala/Pemeriksaan/Perawatan/Langkah Berikutnya → pilih template → edit → simpan.
- **Alternative Flow:** Tulis pertanyaan sendiri.
- **Exception Flow:** Katalog tidak tersedia; form tetap dapat dipakai secara manual.
- **Input:** Suggestion selection.
- **Processing:** Copy template menjadi draft editable.
- **Output:** Draft question.
- **Validation Rules:** Sama dengan pertanyaan manual.
- **Business Rules:** Saran berupa prompt komunikasi, bukan diagnosis atau rekomendasi pengobatan. AHRQ mengelompokkan persiapan pertanyaan berdasarkan jenis pertemuan medis dan memungkinkan pertanyaan disesuaikan.[web:3][file:34]
- **UI Behavior:** Chips/list; tidak menyimpan selection sebelum pengguna menekan Simpan.
- **API/Data:** Local JSON/static constants.
- **Acceptance Criteria:** Given the suggestion catalog is unavailable, when Add Question opens, then manual input remains usable.

## 9. Functional Requirements

| ID | Requirement | Priority | Stage | Acceptance Criteria |
|---|---|---:|---|---|
| FR-001 | Sistem menampilkan notice pada first launch. | P0 | Day-2/MVP | Acknowledgement membuka Home dan tersimpan. |
| FR-002 | Sistem menampilkan daftar kunjungan terdekat di Beranda. | P0 | Day-2/MVP | Maksimum tiga record aktif diurutkan tanggal. |
| FR-003 | Pengguna dapat membuat consultation record. | P0 | Day-2/MVP | Valid input membuat satu record. |
| FR-004 | Pengguna dapat mengedit consultation record. | P0 | Day-2/MVP | Perubahan tampil setelah save/relaunch. |
| FR-005 | Pengguna dapat menghapus record setelah konfirmasi. | P0 | MVP | Parent dan child data terhapus atomik. |
| FR-006 | Pengguna dapat mengubah status Terjadwal/Selesai/Dibatalkan. | P0 | MVP | Status terbarui dan terfilter benar. |
| FR-007 | Pengguna dapat menambah, mengedit, dan menghapus pertanyaan. | P0 | Day-2/MVP | CRUD question berhasil. |
| FR-008 | Sistem membatasi maksimal tiga pertanyaan prioritas. | P0 | MVP | Prioritas keempat ditolak. |
| FR-009 | Sistem menolak pertanyaan identik dalam consultation yang sama. | P1 | MVP | Pesan duplicate tampil. |
| FR-010 | Pengguna dapat menandai pertanyaan sudah/belum dibahas. | P0 | Day-2/MVP | Status persisten. |
| FR-011 | Pengguna dapat menyimpan satu set catatan hasil per consultation. | P0 | Day-2/MVP | Upsert note berhasil. |
| FR-012 | Pengguna dapat mengelola follow-up actions. | P0 | Day-2/MVP | CRUD action berhasil. |
| FR-013 | Sistem mencatat waktu penyelesaian action. | P0 | MVP | completedAt terisi/dikosongkan sesuai status. |
| FR-014 | Sistem menampilkan riwayat terbaru lebih dahulu. | P1 | Day-2/MVP | Urutan descending benar. |
| FR-015 | Pengguna dapat memfilter riwayat berdasarkan status. | P1 | MVP | Hanya status terpilih tampil. |
| FR-016 | Sistem menyediakan saran pertanyaan statis yang editable. | P1 | MVP | Template menjadi draft, bukan autosave. |
| FR-017 | Sistem mempertahankan data setelah app restart. | P0 | MVP | Persistence test lulus. |
| FR-018 | Sistem mencegah duplicate create karena multiple tap. | P0 | MVP | Satu aksi menghasilkan satu record. |
| FR-019 | Sistem memperingatkan unsaved changes saat pengguna keluar. | P1 | MVP | Dialog Stay/Discard tampil. |
| FR-020 | Sistem menyediakan loading, empty, error, dan retry state. | P0 | Day-2/MVP | Seluruh state dapat dipicu dalam UI test. |
| FR-021 | Sistem dapat dipasang dan dijalankan pada perangkat Android fisik target proyek. | P0 | MVP | Build release atau debug berhasil dipasang di Redmi Note 10S. |
| FR-022 | Sistem menyimpan data hanya di perangkat lokal pada MVP. | P0 | MVP | Tidak ada network call yang dibutuhkan untuk P0 features. |

## 10. Non-Functional Requirements

### 10.1 Performance & Reliability

- NFR-001: Initial Home render maksimal 2 detik pada perangkat uji setelah cold start, di luar instalasi pertama.[file:34]
- NFR-002: Read/write lokal biasa memberikan feedback maksimal 500 ms pada Redmi Note 10S; bila lebih lama, tampilkan loading state.[file:34]
- NFR-003: Tombol submit dinonaktifkan selama write untuk mencegah duplicate action.[file:34]
- NFR-004: Operasi penghapusan consultation dan child records harus atomik.[file:34]
- NFR-005: Kegagalan storage tidak boleh menutup aplikasi; tampilkan pesan dan opsi retry.[file:34]

### 10.2 Usability & Accessibility

- NFR-006: Core journey dapat dicapai melalui maksimal dua level navigasi dari Beranda.[file:34]
- NFR-007: Setiap input memiliki persistent label, bukan placeholder saja.[file:34]
- NFR-008: Error menjelaskan field dan tindakan koreksi.[file:34]
- NFR-009: Status tidak dibedakan hanya melalui warna; gunakan teks/ikon.[file:34]
- NFR-010: Touch target Android minimal 48 x 48 dp dan elemen berdekatan memiliki ruang yang memadai.[web:18][web:29]
- NFR-011: Ikon tanpa teks memiliki semantic/content description.[file:34]
- NFR-012: Teks mendukung pembesaran font sistem tanpa CTA utama terpotong pada ukuran uji 360 × 640 dp atau lebih besar.[file:34]

### 10.3 Security & Privacy

- NFR-013: MVP tidak meminta nama lengkap, NIK, nomor BPJS, diagnosis, atau foto dokumen sebagai field wajib.[file:34]
- NFR-014: Data disimpan pada app-private storage; tidak ditulis ke public folder, analytics, crash log, atau debug log.[file:34]
- NFR-015: Secret dan credential tidak boleh hard-coded. MVP tidak memiliki token karena tidak memakai login/API.[file:34]
- NFR-016: Delete menghapus record dan relasinya dari local store.[file:34]
- NFR-017: Debug logging berisi ID teknis dan error category saja, bukan isi catatan kesehatan.[file:34]
- NFR-018: Perlindungan data lokal mengikuti prinsip secure storage dan prevention of sensitive-data leakage dari OWASP MASVS.[web:21][web:28]

### 10.4 Compatibility

- NFR-019: Target utama Android dengan baseline awal **Android 11 atau lebih tinggi** berdasarkan perangkat uji Redmi Note 10S.[web:66]
- NFR-020: Orientasi utama portrait; landscape tidak menjadi target desain MVP tetapi tidak boleh crash.[file:34]
- NFR-021: Aplikasi berfungsi penuh dalam airplane mode setelah instalasi.[file:34]
- NFR-022: Aplikasi harus lolos smoke test pada Redmi Note 10S sebagai primary device.[web:66]
- NFR-023: iOS tidak menjadi target kompatibilitas untuk fase proyek ini.[file:34]

## 11. Information Architecture

```text
First Launch Notice
└── Main Shell
    ├── Beranda
    │   ├── Tambah Kunjungan
    │   └── Detail Kunjungan
    │       ├── Persiapan Pertanyaan
    │       ├── Hasil Konsultasi
    │       ├── Tindak Lanjut
    │       └── Edit / Status / Hapus
    ├── Riwayat
    │   └── Detail Kunjungan
    └── Tentang
        └── Batas Penggunaan & Privasi
```

- **Entry point normal:** Beranda.[file:34]
- **Primary navigation:** Bottom navigation: Beranda, Riwayat, Tentang.[file:34]
- **Contextual navigation:** Detail dan form memakai top app bar/back.[file:34]
- **Exit point:** Android system back; form kotor memicu unsaved-change dialog.[file:34]

## 12. Screen / Page Specification

### 12.1 Screen Inventory

| ID | Screen Name | Purpose | Access | Priority | Day-2? |
|---|---|---|---|---:|---|
| SCR-01 | First Launch Notice | Menjelaskan batas penggunaan | First launch/Tentang | P0 | Ya |
| SCR-02 | Beranda | Ringkasan dan entry point | Bottom nav | P0 | Ya |
| SCR-03 | Form Kunjungan | Create/edit record | Beranda/Detail | P0 | Ya |
| SCR-04 | Detail Kunjungan | Hub seluruh data kunjungan | List/Home | P0 | Ya |
| SCR-05 | Form Pertanyaan | Add/edit/select suggestion | Detail | P0 | Ya |
| SCR-06 | Hasil Konsultasi | Edit note | Detail | P0 | Ya |
| SCR-07 | Form Tindak Lanjut | Add/edit action | Detail | P0 | Ya |
| SCR-08 | Riwayat | List dan filter record | Bottom nav | P1 | Ya |
| SCR-09 | Tentang | Disclaimer dan info data | Bottom nav | P1 | Ya |

### 12.2 Device-Specific UI Notes

- Layout minimum harus nyaman pada layar sekitar 6.43 inci seperti Redmi Note 10S.[web:66]
- Uji form ketika keyboard aktif untuk memastikan tombol simpan tetap dapat diakses.[file:34]
- Uji ukuran teks besar dan panjang input pada perangkat fisik utama, bukan emulator saja.[file:34]
- Uji dalam mode airplane untuk memverifikasi tidak ada ketergantungan jaringan pada P0 features.[file:34]

## 13. User Flow

### 13.1 Happy Path

```text
Open App
  ↓
Accept First-Launch Notice
  ↓
Home → Add Consultation
  ↓
Complete Valid Form → Save
  ↓
Consultation Detail
  ↓
Add Questions → Mark up to 3 Priority
  ↓
During Visit: Mark Questions Discussed
  ↓
Write Consultation Result
  ↓
Add Follow-Up Action
  ↓
Mark Consultation Completed
  ↓
History → Reopen Detail
  ↓
Mark Follow-Up Completed
```

### 13.2 Alternative Path

```text
Home → Existing Consultation → Detail
  ├── Edit appointment data
  ├── Add manual question
  ├── Select and edit suggested question
  ├── Add result later
  └── Cancel consultation → remains in History
```

### 13.3 Error Path

```text
Submit Form
  ↓
Validate
  ├── Invalid → Inline error → Correct → Resubmit
  ├── Duplicate tap → Ignore while submitting
  └── Storage failure → Preserve draft → Error + Retry
```

## 14. UI/UX Requirements

- Gunakan bottom navigation tiga tujuan; hindari drawer untuk aplikasi kecil.[file:34]
- Satu primary CTA per screen; tempatkan di area mudah dijangkau.[file:34]
- Struktur Detail mengikuti urutan waktu: Persiapan → Hasil → Tindak Lanjut.[file:34]
- Gunakan komponen reusable: AppScaffold, StateView, ConsultationCard, SectionCard, LabeledField, ConfirmDialog, StatusChip, PrimaryButton.[file:34]
- Seluruh aksi create/update/delete memberi feedback eksplisit.[file:34]
- Loading tidak mengganti seluruh layar jika hanya satu section yang diproses.[file:34]
- Empty state selalu menjelaskan tindakan berikutnya.[file:34]
- Error message tidak menggunakan kode teknis.[file:34]
- Destructive action menggunakan confirmation dialog.[file:34]
- Form draft dipertahankan selama screen masih hidup setelah save error.[file:34]
- Prioritas dan status menggunakan kombinasi label, icon, dan warna.[file:34]
- UI harus diuji langsung pada Redmi Note 10S untuk memastikan tidak ada overflow saat keyboard terbuka.[web:66][file:34]

## 15. Data Requirements

### 15.1 Data Inventory

| Data | Description | Source | Required? | Used By |
|---|---|---|---|---|
| AppPreference | Notice flag dan preference sederhana | Sistem/user | Ya | First launch |
| Consultation | Identitas dan konteks kunjungan | User | Ya | Home, Detail, History |
| ConsultationQuestion | Pertanyaan, prioritas, status dibahas | User/template | Ya untuk fitur pertanyaan | Detail/Form |
| ConsultationNote | Ringkasan hasil konsultasi versi pengguna | User | Opsional | Detail/Hasil |
| FollowUpAction | Tindakan, due date, completion state | User | Opsional | Home/Detail |
| QuestionSuggestion | Prompt komunikasi generik | Bundled local data | Opsional | Form Pertanyaan |
| UI State | Loading/error/filter/draft | Sistem | Sementara | Semua screen |

### 15.2 Relationships & Storage Notes

- Consultation 1:N ConsultationQuestion.[file:34]
- Consultation 1:0..1 ConsultationNote.[file:34]
- Consultation 1:N FollowUpAction.[file:34]
- Delete Consultation menggunakan cascade transaction.[file:34]
- Local database menjadi source of truth pada MVP.[web:19][file:34]
- Data tidak dikirim ke backend pada MVP.[file:34]

## 16. API / Backend Dependency

MVP tidak memerlukan backend. Frontend wajib menggunakan repository abstraction agar mock repository pada Day-2 dapat diganti local repository tanpa mengubah screen logic. Android merekomendasikan local source sebagai source of truth untuk aplikasi offline-first.[web:19][file:34]

| API/Service | Method | Purpose | Request | Response | Frontend Usage | Priority |
|---|---|---|---|---|---|---:|
| ConsultationRepository | list/get/create/update/delete | Kelola kunjungan lokal | Domain object/filter | Result/domain data | Semua consultation screen | P0 |
| QuestionRepository | list/create/update/delete | Kelola pertanyaan | Question object | Result/question | Detail/Form | P0 |
| NoteRepository | get/upsert | Kelola hasil konsultasi | Note object | Result/note | Detail/Hasil | P0 |
| FollowUpRepository | list/create/update/delete | Kelola action | Action object | Result/action | Home/Detail | P0 |
| SuggestionCatalog | listByCategory | Baca template bundled | Category | List suggestion | Form Pertanyaan | P1 |
| Cloud Sync API | TBD | Future multi-device sync | API CONTRACT TO BE DEFINED | TBD | Tidak dipakai MVP | P3 |

### Integration Rules

- UI tidak mengakses database secara langsung.[file:34]
- Repository mengembalikan success/error type yang konsisten.[file:34]
- Day-2 memakai in-memory/mock implementation dengan interface yang sama.[file:34]
- Tidak ada HTTP/network call pada core journey MVP.[file:34]
- Jika backend kelak ditambahkan, conflict resolution, authentication, authorization, timeout, retry, privacy notice, dan migration wajib menjadi PRD change request.[file:34]

## 17. Frontend 2-Day Delivery Plan

### Day 1 — 6 Oktober 2026

| Workstream | Deliverable | Exit Criteria |
|---|---|---|
| Foundation | Project setup, theme tokens, folder structure | App builds on target emulator/device |
| Contract | Domain models dan repository interfaces | UI dapat membaca mock data |
| Navigation | Bottom nav dan routes | Semua Day-2 screen dapat dibuka |
| Core UI | Home, History, Detail | Mock states tampil konsisten |
| Components | Button, field, card, chip, state view, dialog | Dipakai minimal pada dua screen |
| Mock data | Empty, populated, error fixtures | State dapat dipilih saat development |
| Device setup | Flutter doctor, USB debugging, device detection | Redmi Note 10S terdeteksi untuk run/debug |

### Day 2 — 7–8 Oktober 2026

| Workstream | Deliverable | Exit Criteria |
|---|---|---|
| Forms | Consultation, question, note, follow-up forms | Validation UI bekerja |
| State | Local UI state + mock repository actions | Create/update tampak dalam session |
| States | Loading, empty, error, success | Semua state dapat didemokan |
| UX polish | Unsaved dialog, confirmation, keyboard handling | Tidak ada overflow pada perangkat uji |
| QA | Smoke test seluruh routes dan main interactions | Tidak ada blocker UI |
| Handoff | Data contract dan integration notes | Local persistence dapat dimulai tanpa redesign |

### Day-2 Definition of Done

- Seluruh SCR-01 sampai SCR-09 tersedia dan dapat dinavigasi.[file:34]
- Home, Detail, dan History memiliki populated, empty, loading, serta error preview/state.[file:34]
- Form memvalidasi required fields dan panjang input.[file:34]
- Mock repository mendukung create/update untuk demo dalam satu session.[file:34]
- Tidak ada backend, login, notification, export, atau cloud sync.[file:34]
- Build dapat dijalankan pada minimal satu perangkat/emulator Android.[file:34]
- Build berhasil dijalankan pada Redmi Note 10S sebagai perangkat fisik utama proyek.[web:66]

## 18. One-Month Development Plan

| Phase | Date | Focus | Exit Criteria |
|---|---|---|---|
| 1 — Frontend Foundation | 6–8 Okt | UI, navigation, mock repository | Day-2 DoD terpenuhi |
| 2 — Local Data Layer | 9–14 Okt | Database, repository, migration baseline | CRUD persisten lulus |
| 3 — Core Integration | 15–21 Okt | Questions, notes, follow-ups, status | Happy path end-to-end lulus |
| 4 — Edge Cases & QA | 22–28 Okt | Error, duplicate, cascade delete, accessibility | Regression suite hijau |
| 5 — UAT & Refinement | 29 Okt–2 Nov | Uji pengguna, fix usability/blocker | 0 critical defect |
| 6 — Finalization | 3–6 Nov | Release build, demo data, dokumentasi | MVP checklist lengkap |

## 19. MVP Definition

**MVP dianggap selesai apabila seorang pengguna dapat menyelesaikan core user journey dari membuat kunjungan hingga menyelesaikan tindak lanjut tanpa internet dan data tetap tersedia setelah aplikasi ditutup.**[file:34]

### Objective Checklist

- [ ] First-launch notice bekerja.
- [ ] Consultation dapat dibuat, dibaca, diedit, dihapus, dan diubah status.
- [ ] Pertanyaan dapat dibuat, diprioritaskan maksimal tiga, dan ditandai dibahas.
- [ ] Catatan hasil dapat disimpan dan diedit.
- [ ] Tindak lanjut dapat dibuat, diedit, dihapus, dan diselesaikan.
- [ ] History dan filter status bekerja.
- [ ] Data persisten setelah relaunch.
- [ ] Loading, empty, error, invalid input, duplicate action, dan storage failure tertangani.
- [ ] Core journey bekerja pada airplane mode.
- [ ] Build Android berjalan pada Redmi Note 10S.
- [ ] Tidak ada critical defect atau crash pada scripted regression test.
- [ ] Disclaimer dan batas produk konsisten di UI dan dokumentasi.

## 20. Acceptance Criteria

| AC ID | Given | When | Then |
|---|---|---|---|
| AC-01 | First launch | User acknowledges notice | Home opens; flag persists |
| AC-02 | Valid consultation form | User taps Save repeatedly | Exactly one record is created |
| AC-03 | Required field empty | User taps Save | No write occurs; inline error appears |
| AC-04 | Three priority questions exist | User sets a fourth as priority | Save is blocked with limit message |
| AC-05 | Same question exists | User submits identical normalized text | Duplicate is rejected |
| AC-06 | Undiscussed question exists | User checks it | State persists after relaunch |
| AC-07 | At least one note field valid | User saves | Note preview and updated time appear |
| AC-08 | All note fields empty | User saves | Write is blocked with guidance |
| AC-09 | Open follow-up exists | User completes it | Status and completedAt persist |
| AC-10 | Consultation has child data | User confirms delete | Parent and children are removed atomically |
| AC-11 | Storage write fails | User submits | Draft remains; error and retry appear |
| AC-12 | Filter is Selesai | User opens History | Only completed consultations display |
| AC-13 | No data exists | User opens Home/History | Contextual empty state and CTA appear |
| AC-14 | App is offline | User completes core flow | No network error blocks any P0 feature |
| AC-15 | User exits dirty form | System back is pressed | Stay/Discard dialog appears |
| AC-16 | Redmi Note 10S connected via USB | Developer runs app | Application installs and opens successfully |

## 21. Testing Requirements

### 21.1 Test Levels

| Area | Scope | Linked AC/FR |
|---|---|---|
| Unit Testing | Validation, priority count, duplicate normalization, sorting, status transitions | FR-008, FR-009, FR-013–015 |
| Repository Testing | CRUD, cascade delete, persistence, error mapping | FR-003–005, FR-017 |
| Widget/UI Testing | Form error, buttons, state views, dialog, navigation | FR-001, FR-019–020 |
| Integration Testing | Full consultation journey with local DB | AC-02–AC-14 |
| Error Handling | Storage failure, missing record, retry, duplicate tap | AC-02, AC-11 |
| Security Testing | No sensitive logs/public files; delete removes data | NFR-013–018 |
| Compatibility | Android target version, Redmi Note 10S, emulator cadangan | NFR-019–023 |
| Accessibility | Touch target, semantics, font scaling, non-color status | NFR-009–012 |
| UAT | Patient/pendamping completes core scenario | Success metric |

### 21.2 Minimum Test Cases

- TC-001 First launch acknowledgement.
- TC-002 Create valid consultation.
- TC-003 Reject invalid consultation.
- TC-004 Prevent duplicate on rapid taps.
- TC-005 Add/edit/delete question.
- TC-006 Enforce three-priority limit.
- TC-007 Reject duplicate question.
- TC-008 Persist discussed status.
- TC-009 Upsert result notes.
- TC-010 Create and complete follow-up.
- TC-011 Allow past due date with warning.
- TC-012 Filter history.
- TC-013 Cascade delete.
- TC-014 Recover from simulated storage failure.
- TC-015 Complete entire journey in airplane mode.
- TC-016 Relaunch and verify all data.
- TC-017 Inspect logs and app storage behavior.
- TC-018 Verify 360 × 640 dp layout and large font.
- TC-019 Install and run on Redmi Note 10S.
- TC-020 Run smoke test on emulator Android cadangan.

## 22. Risks & Mitigations

| Risk | Impact | Probability | Mitigation | Owner |
|---|---|---|---|---|
| Scope creep before Day-2 | High | High | Freeze P0 UI; move new requests to change log | PM |
| Backend pursued despite offline MVP | High | Medium | Repository abstraction; local DB first | Tech lead |
| Technology decision late | Medium | Medium | Confirm toolchain on Day 1; keep PRD technology-neutral | Tech lead |
| Mock model differs from final DB | High | Medium | Define domain model before screens | BA/Tech lead |
| Too many screens for two days | High | Medium | Reuse forms/components; no visual experimentation | UI/FE lead |
| Health notes leak to logs/export | High | Medium | Private storage; no analytics/export MVP | Developer/QA |
| Medical wording implies advice | High | Medium | Content review; disclaimer; generic prompts only | PM/Content |
| Data loss during schema change | High | Low | Schema versioning and migration test | Developer |
| Perangkat utama gagal terdeteksi | Medium | Medium | Siapkan emulator Android dan kabel cadangan; uji device setup di Day 1 | Tech lead |
| UAT delayed | Medium | Medium | Recruit 5 participants by week 3; **ASSUMPTION** | PM/QA |

## 23. Dependencies

| Dependency | Stage | Bottleneck? | Requirement/Action |
|---|---|---|---|
| Product naming and wording approval | Day 1 | Medium | Confirm “Teman Kontrol” |
| Mobile framework/toolchain | Day 1 | High | Confirm Flutter/alternative and Android target |
| Basic UI theme | Day 1 | Medium | Select tokens, not full branding exercise |
| Repository interface | Day 1 | High | Freeze before form implementation |
| Local database library | Phase 2 | High | Select based on framework and migration support |
| Android test device/emulator | Day 1 onward | High | Verify build and screen constraints |
| Redmi Note 10S + USB debugging | Day 1 onward | High | Verify physical-device installation and testing |
| Question suggestion copy | Phase 3 | Low | Generic, non-clinical wording |
| UAT participants | Phase 5 | Medium | Arrange before testing week |
| Backend/API | Post-MVP | No for MVP | API CONTRACT TO BE DEFINED |

## 24. Open Questions

### 24.1 Blocking

| ID | Question | Why It Matters | Blocking? | Decision Needed By |
|---|---|---|---|---|
| OQ-01 | Apakah Flutter benar-benar menjadi framework final? | Menentukan setup dan library | Ya | Day 1 |
| OQ-02 | Apakah target minimum Android 11 disetujui tim sebagai baseline resmi? | Build config dan compatibility | Ya | Day 1 |
| OQ-03 | Apakah Day-2 dinilai sebagai UI saja atau harus menyimpan data setelah restart? | Mengubah beban milestone | Ya | Day 1 |
| OQ-04 | Apakah aplikasi wajib memiliki identitas institusi/kelompok? | Mempengaruhi theme dan About | Ya | Day 1 |

### 24.2 Non-Blocking

| ID | Question | Why It Matters | Blocking? | Decision Needed By |
|---|---|---|---|---|
| OQ-05 | Apakah reminder lokal perlu masuk MVP? | Izin OS dan test tambahan | Tidak | 21 Okt |
| OQ-06 | Apakah riwayat perlu pencarian teks? | Scope P2 | Tidak | Setelah MVP |
| OQ-07 | Apakah data perlu ekspor PDF/share? | Privasi dan permission | Tidak | Setelah MVP |
| OQ-08 | Apakah app lock diperlukan untuk demo? | Security versus timeline | Tidak | Setelah MVP |
| OQ-09 | Apakah pendamping perlu profil pasien terpisah? | Mengubah model ke multi-profile | Tidak; default tidak | Setelah MVP |
| OQ-10 | Apakah perlu satu perangkat Android cadangan selain Redmi Note 10S? | Mengurangi risiko demo | Tidak | Sebelum UAT |

## 25. Assumptions

| ID | Assumption | Impact if Wrong | Validation Needed? |
|---|---|---|---|
| A-001 | Deadline final 6 November 2026 | Roadmap bergeser | Ya |
| A-002 | Flutter + Android + local storage | Setup dan estimasi berubah | Ya, Day 1 |
| A-003 | Single-user dan single-device | Model akun/sync berubah besar | Ya |
| A-004 | Pengguna utama pasien umum | Copy dan scenario dapat berubah | Ya |
| A-005 | Core flow: prepare → record → follow up | Prioritas fitur berubah | Ya |
| A-006 | Tidak ada backend pada MVP | API/auth menjadi scope baru jika salah | Ya |
| A-007 | Maksimum tiga priority questions diterima sebagai UX rule | Validation dan UI berubah | Ya |
| A-008 | Lima peserta tersedia untuk UAT ringan | Success target perlu disesuaikan | Ya, week 3 |
| A-009 | Data demo tidak berisi informasi pasien nyata | Risiko privacy naik jika salah | Ya |
| A-010 | Bahasa utama Bahasa Indonesia | Copy dan localization berubah | Ya |
| A-011 | Redmi Note 10S dapat dijadikan primary demo device | Strategi testing/demo perlu berubah jika salah | Ya, Day 1 |

## 26. Final Requirement Traceability

| Problem → Goal | User Need / Story | Feature / FR | Screen | API/Data | AC / Test |
|---|---|---|---|---|---|
| P-01 → pertanyaan tidak terlewat | US-02, US-03 | F-03; FR-007–010 | SCR-04, SCR-05 | QuestionRepository / Question | AC-04–06; TC-005–008 |
| P-02 → hasil terstruktur | US-04 | F-04; FR-011 | SCR-04, SCR-06 | NoteRepository / Note | AC-07–08; TC-009 |
| P-03 → action terlacak | US-05, US-06 | F-05; FR-012–013 | SCR-04, SCR-07 | FollowUpRepository / Action | AC-09; TC-010–011 |
| P-01–03 → satu konteks kunjungan | US-01, US-07 | F-02; FR-003–006 | SCR-03, SCR-04 | ConsultationRepository / Consultation | AC-02–03, AC-10; TC-002–004, TC-013 |
| P-03 → riwayat mudah ditinjau | US-08 | F-06; FR-014–015 | SCR-08 | ConsultationRepository | AC-12–13; TC-012 |
| P-04 → tetap sederhana/offline | Semua core stories | FR-017–022; NFR-021 | Semua | Local repositories | AC-11, AC-14–16; TC-014–020 |
| Blank-page saat menulis | US-09 | F-07; FR-016 | SCR-05 | SuggestionCatalog | Feature AC; manual fallback test |

## 27. Scope Control

### Scope Control Rules

1. P0 tidak boleh dihilangkan tanpa keputusan eksplisit project lead dan pencatatan dampak terhadap core journey.[file:34]
2. P2/P3 otomatis ditunda bila milestone Day-2 atau MVP terancam.[file:34]
3. Setiap change request harus memuat alasan, user value, affected requirements, affected screens/data, estimasi, risiko, dan keputusan.[file:34]
4. Perubahan model data setelah Day-2 harus memperbarui repository contract dan traceability matrix.[file:34]
5. Fitur baru tidak boleh ditambahkan hanya karena “bagus untuk ada”.[file:34]
6. Login, backend, cloud, notifikasi, dan export tidak boleh menyusup ke P0 tanpa pengurangan scope yang sebanding.[file:34]
7. Deadline frontend 8 Oktober 2026 adalah hard milestone.[file:34]
8. Deadline final 6 November 2026 adalah target delivery berdasarkan A-001.[file:34]
9. UI polish tidak boleh mendahului functional completeness dan state handling.[file:34]
10. Data pasien nyata tidak digunakan untuk demo atau testing.[file:34]
11. iOS compatibility tidak boleh masuk scope tanpa perubahan timeline dan resource.[web:37][file:34]

## 28. Recommended Prioritization

### MUST HAVE

- Notice dan batas penggunaan.[file:34]
- Consultation CRUD serta status.[file:34]
- Pertanyaan, maksimal tiga prioritas, dan discussed checklist.[file:34]
- Hasil konsultasi versi pengguna.[file:34]
- Follow-up CRUD dan completion state.[file:34]
- Local persistence dan offline core journey.[file:34]
- Loading, empty, error, invalid input, duplicate submit, and storage-failure handling.[file:34]
- Build Android berjalan pada Redmi Note 10S.[web:66]

### SHOULD HAVE

- History filter.[file:34]
- Static question suggestions.[file:34]
- Unsaved-change protection.[file:34]
- Basic accessibility checks dan UAT ringan.[file:34]

### COULD HAVE

- Reminder lokal.[file:34]
- Export/share ringkasan.[file:34]
- App lock/PIN/biometric.[file:34]
- Search history.[file:34]

### WON'T HAVE

- Diagnosis, triase, rekomendasi obat/terapi.[file:34]
- Chat atau konsultasi tenaga kesehatan.[file:34]
- Booking fasilitas kesehatan.[file:34]
- Login, admin, dashboard klinik.[file:34]
- Cloud sync dan multi-device pada MVP.[file:34]
- Rekam medis resmi atau integrasi sistem rumah sakit.[file:34]
- iOS build/release pada fase proyek ini.[web:37][file:34]

## Quality Gate

Sebelum status dokumen dinaikkan dari Draft menjadi Approved, OQ-01 sampai OQ-04 harus dijawab.[file:34] Sebelum release, seluruh checklist MVP, acceptance criteria P0, security/privacy checks, scripted core journey, dan install test pada Redmi Note 10S wajib lulus.[web:66][file:34]
