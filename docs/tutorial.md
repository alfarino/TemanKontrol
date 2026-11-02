# 📘 Panduan Alur Kerja Pengembangan (Workflow) Teman Kontrol
## Dari Konsep Hingga Rilis Siap Demo Berbasis *AI-Assisted Engineering*

> **Tujuan Dokumen:**  
> Dokumen ini disusun sebagai panduan strategi dan alur kerja (*development workflow*) bagi tim mahasiswa. Dokumen ini bertujuan untuk membekali setiap anggota kelompok dengan pemahaman menyeluruh tentang bagaimana aplikasi mobile **Teman Kontrol** dirancang, diorkestrasi bersama AI IDE, diuji, hingga dikompilasi menjadi berkas rilis siap pakai. Panduan ini dirancang khusus agar mahasiswa mampu mempresentasikan metodologi pengembangan secara akademis, percaya diri, dan elegan di hadapan **Dosen Pembimbing & Dosen Penguji**.

---

## 🏛️ Filosofi Utama: *Human-in-the-Loop & AI-Assisted Engineering*

Dalam proyek ini, tim mahasiswa tidak memosisikan kecerdasan buatan (*Artificial Intelligence*) sebagai pembuat keputusan mandiri, melainkan menerapkan paradigma **AI-Assisted Engineering** dengan prinsip **Human-in-the-Loop**:

```
┌────────────────────────────────────────────────────────┐
│           MAHASISWA (Human-in-the-Loop)                │
│  - System Architect & Product Owner                   │
│  - Formulator Kebutuhan Klinis & Batasan Privasi       │
│  - Quality Controller, Tester, & Decision Maker       │
└───────────────────────────┬────────────────────────────┘
                            │ Memberikan Instruksi & Batasan Ketat
                            ▼
┌────────────────────────────────────────────────────────┐
│                AI IDE (Copilot / Engine)               │
│  - Intelligent Code Implementation & Scaffolding       │
│  - Pembangkit Skema & Boilerplate Kompatibel           │
│  - Refactoring & Bug Fixing Berdasarkan Log Error      │
└───────────────────────────┬────────────────────────────┘
                            │ Menghasilkan Kode & Artefak
                            ▼
┌────────────────────────────────────────────────────────┐
│              PRODUK: APLIKASI TEMAN KONTROL            │
│  - 100% Offline-First (Tanpa Akses Internet)          │
│  - Aman, Tervalidasi, & Ramping (~6,8 MB APK Release)  │
└────────────────────────────────────────────────────────┘
```

* **Mahasiswa adalah Pemimpin Arsitektur (*System Architect & Product Owner*):** Menentukan domain masalah medis, merumuskan batasan etika & hukum privasi, merancang arsitektur perangkat lunak, memvalidasi hasil kerja, dan mengambil keputusan rekayasa.
* **AI IDE adalah Mesin Implementasi (*Intelligent Pair Programmer*):** Berfungsi mengeksekusi penulisan kode, menyusun *boilerplate*, menerapkan pustaka standar Google, dan mempercepat siklus pembuatan antarmuka berdasarkan instruksi terarah.

---

## 📑 Tahapan Alur Kerja Pengembangan (Development Stages)

```
[Tahap 1: Setup Lingkungan] ➔ [Tahap 2: Perumusan PRD] ➔ [Tahap 3: Review & Lock PRD]
                                                                   │
[Tahap 6: Kompilasi Release] ◄─ [Tahap 5: Testing Debug] ◄─ [Tahap 4: Eksekusi AI Bertahap]
```

---

### Tahap 1: Persiapan Lingkungan Pengembangan (Environment Setup)

Sebelum menulis spesifikasi atau menghasilkan kode, tim menyiapkan fondasi *toolchain* pengembangan perangkat lunak modern untuk ekosistem Android native.

#### 1. Komponen Fondasi yang Disiapkan
1. **Android Studio (Official IDE):**
   * Berfungsi sebagai ekosistem kompilasi resmi, pengelola Gradle Build System, dan penyedia perangkat inspeksi performa (*Profiler*, *Layout Inspector*, *Device Manager*).
   * Konfigurasi Java Development Kit (JDK 17 / JBR Android Studio) yang stabil untuk kompatibilitas kompilasi Kotlin 2.x.
2. **Android SDK Platform & Build Tools:**
   * Menetapkan batas target sistem operasi: **Minimum SDK 30 (Android 11.0)** hingga **Compile SDK 34/37** guna menjangkau mayoritas gawai pengguna di Indonesia dengan stabilitas API modern.
3. **Testbed & Target Perangkat Pengujian:**
   * **Android Virtual Device (AVD / Emulator):** Dikonfigurasi dengan profil Google Pixel (layar 1080×2400 dp, API 33+) untuk simulasi cepat tanpa ketergantungan perangkat keras.
   * **Smartphone Fisik (Redmi Note 10S):** Dihubungkan melalui *USB Debugging* (Mode Pengembang) untuk validasi langsung terhadap respon sentuhan layar, perilaku keyboard virtual (*soft-keyboard*), dan uji konsumsi memori nyata.
4. **Integrasi AI IDE (*Intelligent Coding Assistant*):**
   * Pemanfaatan AI IDE modern yang memiliki akses langsung terhadap sistem berkas (*workspace*) dan terminal pengembang, memungkinkan AI membaca konteks proyek secara holistik tanpa proses *copy-paste* manual yang rentan galat.

#### 💡 Cara Membahasakan ke Dosen
> *"Izin menjelaskan, Bapak/Ibu Dosen. Pada tahap awal, kami tidak langsung melompat ke penulisan kode. Kami terlebih dahulu membangun fondasi lingkungan rekayasa perangkat lunak standar industri. Kami mengintegrasikan Android Studio sebagai 'official compiler' dan 'profiling tool', serta menghubungkan AI IDE sebagai 'intelligent pair programmer'. Dengan integrasi ini, AI memiliki pemahaman mendalam terhadap struktur berkas proyek kami, sementara kami tetap mengendalikan eksekusi kompilasi secara ketat menggunakan Gradle dan Java Development Kit resmi."*

---

### Tahap 2: Perumusan Kebutuhan & Pembuatan PRD (Product Requirements Document)

Banyak proyek berbasis AI gagal karena pengembang memberikan instruksi yang ambigu (*vague prompt*). Untuk mencegah hal tersebut, tim merumuskan dokumen spesifikasi formal terlebih dahulu.

#### 1. Perumusan Masalah Nyata Pasien Kronis
Tim mengidentifikasi masalah utama pasien penyakit kronis (Hipertensi, Diabetes, Jantung):
* Pasien sering panik dan lupa menanyakan keluhan penting saat berhadapan dengan dokter.
* Instruksi dokter pasca-konsultasi tercecer di secarik kertas resep dan sulit dilacak.
* Mayoritas aplikasi kesehatan komersial mewajibkan pendaftaran akun daring (*cloud login*) yang membebani lansia serta menimbulkan risiko kebocoran data privasi medis.

#### 2. Orkestrasi Perintah (*Prompting*) Pembuatan PRD
Tim menyusun *system prompt* terstruktur kepada AI IDE untuk menyusun dokumen **PRD (Product Requirements Document)** komprehensif, mencakup:
* **Batasan Sistem & Keamanan (*System Boundary*):** Sistem wajib beroperasi **100% Offline-First**, tanpa server eksternal, tanpa akun, dan tanpa izin internet (`android.permission.INTERNET` dilarang).
* **Inventarisasi Layar (SCR-01 s.d. SCR-09):**
  * `SCR-01` (First Launch Notice / Batas Tanggung Jawab Medis).
  * `SCR-02` (Beranda / Dashboard Kunjungan & Tindak Lanjut).
  * `SCR-03` (Formulir Tambah/Edit Kunjungan Medis).
  * `SCR-04` (Detail Kunjungan: Persiapan ➔ Catatan Hasil ➔ Tindak Lanjut).
  * `SCR-05` (Kelola & Prioritisasi Pertanyaan Pasien).
  * `SCR-06` (Catatan Hasil Konsultasi Medis).
  * `SCR-07` (Daftar Tindak Lanjut & Tanggal Tenggat).
  * `SCR-08` (Riwayat Kunjungan Medis & Filter Status).
  * `SCR-09` (Halaman Tentang & Pernyataan Privasi Lokal).
* **Rancangan Skema Basis Data Lokal:** Memodelkan entitas relasional SQLite Room (`Consultation`, `Question`, `ConsultationNote`, `FollowUpAction`) lengkap dengan mekanisme *Foreign Key CASCADE Delete* dan indeks pencarian.
* **Aturan Bisnis & Validasi Ketat (VR-01 s.d. VR-12):**
  * Pembatasan kuota **maksimal 3 pertanyaan prioritas** per kunjungan untuk menjaga efektivitas durasi konsultasi dokter.
  * Proteksi aksi ganda (*double-tap submit prevention*).
  * Peringatan jadwal lampau (*past-due notification*) tanpa memblokir input pencatatan historis.

#### 💡 Cara Membahasakan ke Dosen
> *"Sebelum menuliskan satu baris kode pun, kami memposisikan diri sebagai System Architect dengan menyusun PRD atau Product Requirements Document sebagai 'Single Source of Truth'. Kami memandu AI IDE melalui prompt berbasis analisis kebutuhan pasien kronis untuk mendefinisikan batasan arsitektur offline-first, skema basis data Room, 9 alur layar, hingga 12 aturan validasi bisnis. Pendekatan ini memastikan bahwa arah pengembangan memiliki landasan metodologis yang kokoh dan terdokumentasi secara ilmiah."*

---

### Tahap 3: Review & Persetujuan PRD (PRD Approval & Architecture Lock)

Tahap ini merupakan wujud nyata prinsip *Human-in-the-Loop*. Tim mahasiswa meninjau draf PRD yang disusun bersama AI sebelum fase pengkodean dimulai.

#### 1. Validasi Manusia terhadap Aspek Klinis & Ergonomi
* **Desain Ramah Lansia & Aksesibilitas:** Memastikan antarmuka menggunakan palet warna Material 3 bertema *Teal Tenang* (`#0F7C80`) dengan rasio kontras tinggi (> 4.5:1), tombol sentuh lega (minimal 48 dp), dan ukuran teks mudah dibaca (minimal 16 sp) agar nyaman bagi pasien senior.
* **Disclaimer Medis Tegas:** Memastikan teks penafian pada `SCR-01` secara eksplisit menegaskan bahwa Teman Kontrol adalah alat bantu pengorganisasi catatan pribadi (*Personal Health Record*), bukan alat diagnosis medis atau pengganti pertimbangan dokter.
* **Penguncian Keputusan Rekayasa (*Decision Log* DEC-001 s.d. DEC-021):** Mengunci pilihan teknologi native modern (Kotlin, Jetpack Compose, Room SQLite, Hilt Dependency Injection, DataStore Preferences).

#### 2. Mengapa Tahap Penguncian Ini Sangat Krusial?
* Mencegah fenomena **AI Hallucination** (kondisi di mana AI mengarang fitur yang tidak diminta atau menambahkan dependensi pustaka yang tidak kompatibel).
* Mencegah **Scope Creep** (pembengkakan cakupan proyek di luar kesepakatan awal).
* Menjamin bahwa setiap baris kode yang akan diinstruksikan memiliki referensi bab dan pasal yang jelas pada dokumen PRD.

#### 💡 Cara Membahasakan ke Dosen
> *"Tahap peninjauan PRD adalah titik krusial di mana kendali manusia memegang peranan mutlak. Kami memvalidasi draf PRD untuk memastikan aplikasi benar-benar ramah lansia, kontras warnanya memenuhi standar aksesibilitas, dan klausul hukum privasinya jelas. Dengan 'mengunci' arsitektur dan aturan bisnis di dalam PRD sebelum implementasi, kami berhasil mengeliminasi risiko halusinasi AI sehingga AI hanya bekerja dalam koridor spesifikasi yang telah kami setujui."*

---

### Tahap 4: Eksekusi Pengembangan Bertahap bersama AI IDE (*Iterative Step-by-Step Execution*)

Alih-alih memerintahkan AI dengan instruksi monolitik yang berisiko gagal (*"buatkan seluruh aplikasi Teman Kontrol sekarang"*), tim menerapkan metodologi **rekayasa bertahap (*incremental software development*)**.

```
┌────────────────────────────────────────────────────────┐
│ 1. Kerangka Dasar (Kotlin + Compose + M3 + Hilt)       │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│ 2. Lapisan Persistensi Data (Room Database + DataStore)│
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│ 3. Konstruksi UI Layar demi Layar (SCR-01 s/d SCR-09)  │
└───────────────────────────┬────────────────────────────┘
                            ▼
┌────────────────────────────────────────────────────────┐
│ 4. Integrasi Logika Bisnis, Validasi, & Katalog Statis │
└────────────────────────────────────────────────────────┘
```

#### Rincian 4 Siklus Eksekusi:
1. **Langkah 4.1: Inisialisasi Kerangka & Injeksi Dependensi:**
   * AI IDE diinstruksikan menyusun arsitektur proyek modular berbasis pola **Clean Architecture** & **MVI/MVVM**.
   * Menyiapkan dependensi *modern Android stack*: Kotlin Serialization, Jetpack Compose Navigation dengan rute *type-safe*, serta Hilt untuk pembalikan dependensi (*Dependency Inversion*).
2. **Langkah 4.2: Pembangunan Fondasi Data Lokal (Room & DataStore):**
   * Menginstruksikan pembuatan tabel SQLite lokal via Room (`ConsultationEntity`, `QuestionEntity`, `NoteEntity`, `FollowUpEntity`).
   * Mengonfigurasi relasi *one-to-many* dengan integritas referensial *CASCADE Delete* (jika kunjungan dihapus, seluruh pertanyaan dan tindak lanjut terkait terhapus bersih secara atomik).
   * Menerapkan DataStore Preferences untuk mencatat konfirmasi persetujuan pengguna pada peluncuran pertama.
3. **Langkah 4.3: Perancangan Antarmuka Layar demi Layar (*Screen-by-Screen*):**
   * Membangun pustaka komponen guna ulang (*reusable design system*): `AppScaffold`, `SectionCard`, `ChecklistItem`, `StatusChip`, dan `PriorityBadge`.
   * Mengimplementasikan 9 layar secara bertahap sesuai cetak biru PRD, menjamin penanganan status lengkap di setiap layar: *Loading*, *Success/Content*, *Empty State*, dan *Error State*.
4. **Langkah 4.4: Integrasi Logika Bisnis & Validasi Form:**
   * Menerapkan validasi input di ViewModel menggunakan Kotlin `StateFlow`: memeriksa kolom wajib, membatasi teks, dan menolak penambahan prioritas ke-4 jika kuota 3 prioritas sudah terpenuhi.
   * Menanamkan katalog saran pertanyaan kontrol dokter ke dalam memori aplikasi sebagai konstanta Kotlin yang efisien, bebas biaya I/O jaringan.

#### 💡 Cara Membahasakan ke Dosen
> *"Dalam proses koding bersama AI IDE, kami menerapkan pendekatan modular dan bertahap (iterative execution). Kami tidak meminta AI bekerja sekaligus, melainkan membaginya ke dalam 4 lapisan terpisah: pertama kerangka arsitektur, kedua persistensi Room database, ketiga konstruksi antarmuka Compose, dan keempat penyuntikan logika bisnis. Setiap lapisan kami uji dan kompilasi terlebih dahulu sebelum melangkah ke lapisan berikutnya. Pola ini menghasilkan arsitektur yang terstruktur rapi, minim bug, dan mudah dipelihara."*

---

### Tahap 5: Pengujian & Evaluasi via APK Debug (*Testing & Debugging*)

Pada tahap ini, tim menguji keandalan aplikasi secara empiris menggunakan kompilasi **APK Debug** (`app-debug.apk`) pada emulator dan gawai fisik.

#### 1. Pengujian Otomatis (*Automated Testing Suite*)
* Menjalankan **43 kasus Unit & ViewModel Tests** menggunakan Gradle CLI (`./gradlew testDebugUnitTest`), mencakup:
  * Pengujian aturan validasi bisnis (VR-01 s/d VR-12) pada `ValidationTest.kt`.
  * Pengujian pemetaan entitas basis data pada `MappersTest.kt`.
  * Pengujian logika filter riwayat dan pengorganisasian kartu beranda pada ViewModel.
* Menjalankan audit kualitas kode statis menggunakan **Android Lint** (`./gradlew lintDebug`) dengan hasil **0 error**.

#### 2. Pengujian Skenario Batas (*Edge Cases*) & Pengalaman Pengguna
* **Uji Kuota Prioritas:** Mencoba menandai 4 pertanyaan sebagai prioritas; aplikasi berhasil memblokir aksi dan menampilkan pesan edukatif sesuai ketentuan PRD.
* **Uji Perlindungan Data Belum Tersimpan (*Unsaved Changes Guard*):** Mencoba menekan tombol kembali saat form telah diisi; aplikasi memunculkan dialog konfirmasi untuk mencegah data pengguna hilang tidak sengaja.
* **Uji Kasus KSP 2 & Room Compilation:** Ketika terjadi benturan kompatibilitas antara Kotlin Symbol Processing (KSP 2) dan signature method Room DAO pada fase awal, tim mahasiswa menganalisis pesan galat JVM dan mengarahkan AI untuk memberikan tipe kembalian eksplisit (`Long` untuk Insert, `Int` untuk Update/Delete).

#### 💡 Cara Membahasakan ke Dosen
> *"Kualitas sistem kami buktikan melalui pengujian berjenjang. Kami menjalankan 43 skenario unit test otomatis serta audit Android Lint dengan hasil nol kesalahan. Kami juga menguji skenario batas di smartphone fisik, seperti upaya melanggar batas 3 pertanyaan prioritas atau menutup formulir secara tiba-tiba. Ketika terjadi kendala teknis pada kompilator KSP Room, kami menunjukkan kapasitas rekayasa dengan menganalisis 'root-cause' pada log build Gradle dan memandu AI memperbaiki arsitektur DAO hingga lulus kompilasi 100%."*

---

### Tahap 6: Finalisasi & Kompilasi APK Release (*Production Build*)

Tahap akhir adalah mengunci sistem dan memproduksi artefak rilis siap edar (*production-ready artifact*) yang aman, ringan, dan optimal.

#### 1. Audit Keamanan Berkas Manifest
* **Penghapusan Akses Internet:** Menambahkan instruksi `tools:node="remove"` pada izin `android.permission.INTERNET` di dalam `AndroidManifest.xml`. Hal ini memberi garansi teknis mutlak kepada dosen dan pengguna bahwa aplikasi tidak dapat membocorkan data kesehatan ke jaringan luar.
* **Pencegahan Backup Otomatis:** Mengonfigurasi `android:allowBackup="false"` untuk memastikan riwayat medis pasien tidak diunggah ke pencadangan awan (*Google Cloud Backup*) pihak ketiga.

#### 2. Optimasi Kode & Sumber Daya (R8 Minification)
* Mengaktifkan mesin optimasi **R8 Shrinker & Resource Minifier** melalui `app/build.gradle.kts`.
* R8 menganalisis seluruh pohon kode, membuang kode pustaka yang tidak terpakai (*dead-code elimination*), serta mengompresi aset vektor.
* **Hasil Nyata:** Ukuran berkas APK menyusut drastis dari **~64 MB (versi Debug)** menjadi hanya **~6,8 MB (versi Release - `app-release.apk`)**, jauh di bawah ambang batas anggaran PRD yaitu 10 MB.

#### 3. Penandatanganan (*APK Signing*) & Kesiapan Demo
* Berkas `app-release.apk` ditandatangani (*signed*) sehingga siap ditransfer langsung ke gawai Dosen Penguji melalui kabel USB atau Bluetooth untuk didemokan secara instan tanpa memerlukan koneksi Wi-Fi maupun Android Studio.

#### 💡 Cara Membahasakan ke Dosen
> *"Pada tahap akhir, kami melakukan 'hardening' keamanan dan optimasi performa. Kami mengunci AndroidManifest dengan menghapus izin INTERNET secara permanen dan menonaktifkan cloud backup demi privasi medis pasien. Kami juga menerapkan optimasi kompilasi R8 minification, yang berhasil memangkas ukuran aplikasi hingga hanya 6,8 Megabyte. Hasilnya adalah file 'app-release.apk' yang sangat ringan, aman, dan siap dipasang langsung di perangkat Bapak/Ibu Dosen untuk sesi pengujian hari ini."*

---

## 📊 Ringkasan Komparasi: Rekayasa Manual vs AI-Assisted Engineering

| Parameter Evaluasi | Pendekatan Konvensional (Manual Penuh) | Pendekatan Teman Kontrol (*AI-Assisted Engineering*) |
|---|---|---|
| **Peran Mahasiswa** | Mengetik sintaks kode baris-demi-baris secara manual | Bertindak sebagai *System Architect, Prompt Engineer, & QA Auditor* |
| **Kecepatan Scaffolding** | Butuh berhari-hari membuat boilerplate Room & UI | Selesai dalam hitungan jam dengan panduan PRD terstruktur |
| **Kepatuhan Arsitektur** | Rentan inkonsistensi penamaan dan struktur folder | Konsisten mengikuti *Clean Architecture* dan SSOT PRD |
| **Mitigasi Bug** | Bergantung pada *trial-and-error* manual | Didukung analisis log instan dan pengujian otomatis terpandu |
| **Fokus Utama Tim** | Habis waktu mengurus sintaks dasar | Fokus pada inovasi klinis, privasi medis, dan ergonomi lansia |

---

## 🎯 Cheat Sheet Tanya Jawab Dosen Penguji (FAQ)

Gunakan panduan jawaban ini saat Dosen Penguji melontarkan pertanyaan kritis mengenai metodologi pengembangan dan arsitektur aplikasi:

---

### Q1: *"Kalian menggunakan AI dalam membangun aplikasi ini. Lalu, apa kontribusi orisinal anggota kelompok jika kodingnya dibantu AI?"*

> **Rekomendasi Jawaban Mahasiswa:**  
> *"Terima kasih atas pertanyaannya, Bapak/Ibu Dosen. Kontribusi utama kami terletak pada ranah **High-Level System Architecture, Domain Modeling, dan Quality Assurance**. AI tidak memiliki kesadaran terhadap kebutuhan medis pasien kronis di Indonesia; kamilah yang merumuskan masalah, merancang solusi 100% offline-first demi privasi, menetapkan batasan 3 prioritas, dan memodelkan skema basis data relasional di dalam PRD.  
> 
> AI kami posisikan sebagai 'pair programmer' untuk mengimplementasikan sintaks teknis. Setiap kode yang dihasilkan AI tetap harus melalui peninjauan manusia (*Human-in-the-Loop*), kami uji menggunakan 43 test case otomatis, dan kami audit keamanannya. Jadi, integritas logika dan arsitektur sistem sepenuhnya merupakan buah pemikiran dan tanggung jawab kami sebagai mahasiswa rekayasa sistem informasi."*

---

### Q2: *"Mengapa repot-repot membuat PRD yang begitu tebal sebelum mulai koding bersama AI? Bukankah lebih cepat langsung memberi perintah ke AI?"*

> **Rekomendasi Jawaban Mahasiswa:**  
> *"Justru pembuatan PRD adalah kunci keberhasilan kami dalam mengorkestrasi AI, Bapak/Ibu. Dalam rekayasa berbasis AI (*AI Engineering*), terdapat risiko besar yang disebut 'Context Drift' dan 'Halusinasi', di mana AI cenderung melenceng atau menambahkan fitur yang tidak relevan jika diberi instruksi bebas.  
> 
> Dengan menyusun PRD sebagai **Single Source of Truth (SSOT)**, kami memiliki kontrak spesifikasi yang mengikat. PRD membatasi ruang gerak AI agar fokus hanya pada kebutuhan sistem yang sah: memandu penamaan entitas, membatasi ketiadaan akses internet, dan memastikan 9 layar terhubung sesuai alur klinis. Hasilnya, pengembangan berjalan sangat terarah, efisien, dan bebas dari halusinasi."*

---

### Q3: *"Bagaimana kalian menjamin keamanan data riwayat medis pasien, mengingat data kesehatan adalah data yang sangat sensitif?"*

> **Rekomendasi Jawaban Mahasiswa:**  
> *"Kami menerapkan prinsip **Security and Privacy by Design** dengan empat pilar teknis:  
> 1. **Nol Izin Internet:** Pada berkas `AndroidManifest.xml`, kami menghapus izin internet secara permanen menggunakan `tools:node='remove'`, sehingga aplikasi secara fisik mustahil mengirim data ke luar perangkat.  
> 2. **Penyimpanan Privat Lokal:** Seluruh data disimpan pada basis data SQLite Room di dalam *app-private internal storage* gawai pasien yang secara native dilindungi oleh *File-Based Encryption (FBE)* sistem operasi Android.  
> 3. **Nonaktifkan Cloud Backup:** Kami mengatur atribut `android:allowBackup='false'` untuk memastikan riwayat medis tidak ikut tersedot ke akun cloud pihak ketiga saat ponsel mencadangkan data.  
> 4. **Bebas SDK Pihak Ketiga:** Aplikasi bebas dari library analitik, iklan, atau pelacak daring."*

---

### Q4: *"Jika terjadi error atau bug pada kode yang dihasilkan oleh AI, bagaimana kalian mengatasinya? Apakah kalian paham alur kodenya?"*

> **Rekomendasi Jawaban Mahasiswa:**  
> *"Tentu kami sangat memahami alur kodenya, Bapak/Ibu. Sebagai bukti nyata, saat integrasi fase persistensi data, kami sempat mengalami kendala kompilasi pada KSP 2 dan interface Room DAO terkait return type void pada fungsi suspend. AI tidak bisa menyelesaikan masalah ini secara otomatis tanpa arahan yang tepat.  
> 
> Kami secara mandiri menganalisis log stack trace build Gradle, mengidentifikasi akar masalah pada JVM signature analysis, lalu menginstruksikan AI untuk merefaktor metode DAO dengan memberikan tipe kembalian Long dan Int eksplisit. Pemahaman mendalam terhadap arsitektur Android inilah yang memungkinkan kami mengarahkan dan mengoreksi AI secara efektif hingga proyek sukses terkompilasi."*

---

### Q5: *"Mengapa memilih Room Database dan Jetpack Compose dibandingkan teknologi lain seperti Flutter atau SQLite biasa?"*

> **Rekomendasi Jawaban Mahasiswa:**  
> *"Pilihan ini didasarkan pada keputusan rekayasa formal di PRD kami (DEC-001 dan DEC-003). Jetpack Compose adalah toolkit UI deklaratif modern resmi dari Google yang memungkinkan rendering antarmuka menjadi sangat cepat, reaktif terhadap perubahan state, dan bebas dari overhead runtime tambahan seperti pada cross-platform framework.  
> 
> Sedangkan Room Database kami pilih karena menyediakan abstraksi SQLite dengan keamanan tipe data saat waktu kompilasi (*compile-time SQL verification*), mendukung mekanisme pemantauan data reaktif menggunakan Kotlin Flow, serta mempermudah penerapan relasi antar-tabel dan integritas referensial CASCADE delete secara andal."*

---

## 🏁 Panduan Penggunaan Dokumen Ini untuk Anggota Kelompok

1. **Sebelum Presentasi:** Setiap anggota wajib membaca dan memahami kata kunci teknis pada setiap tahapan (*Offline-First, PRD as SSOT, Clean Architecture, Human-in-the-Loop, CASCADE Delete, R8 Minification*).
2. **Saat Presentasi Slide:** Bagi tugas pembicara mengikuti alur 6 tahap di atas. Gunakan analogi arsitektur dan orkestrasi untuk menunjukkan kematangan wawasan teknologi tim.
3. **Saat Tanya Jawab:** Tetap tenang, dengarkan pertanyaan dosen hingga tuntas, lalu gunakan kerangka jawaban pada **Cheat Sheet Q&A** dengan intonasi yang percaya diri, sopan, dan akademis.
