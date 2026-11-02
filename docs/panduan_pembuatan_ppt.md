# 📊 Panduan Pembuatan Slide Presentasi (PPT) Teman Kontrol

Dokumen ini disusun sebagai panduan lengkap pembuatan slide presentasi PowerPoint / Google Slides / Canva untuk aplikasi **Teman Kontrol**. Format, tata letak visual, hierarki teks, dan gaya desain mengikuti struktur presentasi profesional pada referensi `Kel.4 Sistem informasi kesehatan (1).pdf` (latar belakang gelap elegan, kartu konten kontras, mockup smartphone di setiap slide, dan narasi presenter terstruktur).

---

## 🎨 Panduan Desain & Template Visual (Design Tokens)

Agar tampilan PPT terlihat modern, premium, dan konsisten:
1. **Rasio Slide:** Widescreen 16:9.
2. **Latar Belakang (Background):** Gelap modern (*Dark Theme*):
   - Warna Utama: `#0C1315` (Deep Dark Teal Charcoal) atau `#121212` (Pure Dark).
   - Bentuk Dekorasi: Aksen lengkungan / glow halus berwarna Teal `#0F7C80` dan Ungu `#8A4FFF` di sudut slide.
3. **Palet Warna Teks & Elemen:**
   - **Teks Utama (Heading):** Putih bersih `#FFFFFF`, tebal (*Bold/SemiBold*).
   - **Teks Pendukung (Body):** Abu-abu terang `#D0D7D9` atau `#E0E0E0` (rasio kontras > 4.5:1).
   - **Aksen Utama (Brand / Highlight):** Teal Tenang `#0F7C80` & Teal Terang `#4DB6AC`.
   - **Aksen Sekunder (Prioritas / Badge):** Ungu Lembut `#8A4FFF` / `#CDB4FF`.
   - **Status Warning / Alert:** Oranye-Kuning `#FFB95C`.
4. **Tipografi:** Gunakan font sans-serif modern:
   - Alternatif 1: **Poppins** (Heading) + **Inter** (Body).
   - Alternatif 2: **Roboto** (bawaan Android) untuk seluruh teks.
5. **Elemen Mockup Smartphone:**
   - Gunakan bingkai smartphone (*phone mockup frame* seperti Google Pixel atau iPhone hitam minimalis).
   - Masukkan gambar tangkapan layar (*screenshot*) asli yang telah tersedia di folder `docs/screenshots/` ke dalam bingkai tersebut.

---

## 📑 Struktur Slide Presentasi (Total 16 Slide)

Berikut adalah panduan slide demi slide, lengkap dengan tata letak, teks siap salin (*copy-paste*), referensi screenshot, dan skrip bicara presenter (*speaker notes*).

---

### SLIDE 1: Cover / Judul Presentasi

- **Tujuan:** Menarik perhatian audiens, memperkenalkan identitas aplikasi, kategori sistem informasi, dan kredensial karya.
- **Tata Letak Visual:**
  - **Kiri (60%):** Judul aplikasi besar, sub-judul, deskripsi satu kalimat, dan badge teknologi offline.
  - **Kanan (40%):** Mockup smartphone miring/tegak menampilkan halaman Beranda ([02_home_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/02_home_screen.png)) dan pembuka ([01_notice_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/01_notice_screen.png)).
  - **Pojok Kiri Atas:** Teks kecil nama kelompok (misal: *Kelompok X — Sistem Informasi Kesehatan*).
- **Aset Gambar:**
  - `docs/screenshots/02_home_screen.png`
  - `docs/screenshots/01_notice_screen.png`
- **Teks Slide:**
  ```text
  [Kelompok X]

  Teman Kontrol
  Aplikasi Mobile Personal Health Record Pendamping Kontrol Medis Pasien Penyakit Kronis

  Platform Android Offline-First untuk Mempersiapkan Pertanyaan, 
  Mencatat Arahan Dokter, dan Melacak Tindak Lanjut Pasca Konsultasi.

  100% Offline • Bebas Kuota • Privasi Terjamin
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Selamat pagi/siang Bapak/Ibu dosen dan rekan-rekan sekalian. Hari ini kami mempresentasikan karya sistem informasi kesehatan kami yang berjudul 'Teman Kontrol'. Teman Kontrol adalah aplikasi Personal Health Record berbasis Android yang dirancang khusus untuk mendampingi pasien penyakit kronis dan caregiver mereka dalam mengorganisasi konsultasi medis—mulai dari persiapan sebelum masuk ruang periksa, saat berhadapan dengan dokter, hingga menindaklanjuti rencana pengobatan setelah pulang."*

---

### SLIDE 2: Nama Anggota Kelompok

- **Tujuan:** Memperkenalkan tim penyusun proyek.
- **Tata Letak Visual:**
  - **Kiri (60%):** Judul "Nama Anggota Kelompok", diikuti kartu/daftar nomor urut dengan badge bernomor (01, 02, 03, dst.), nama lengkap, dan NIM.
  - **Kanan (40%):** Ilustrasi smartphone 3D atau ikon kolaborasi tim medis.
- **Teks Slide:**
  ```text
  Nama Anggota Kelompok:

  01  [Nama Anggota 1]          [NIM 1]
  02  [Nama Anggota 2]          [NIM 2]
  03  [Nama Anggota 3]          [NIM 3]
  04  [Nama Anggota 4]          [NIM 4]
  05  [Nama Anggota 5]          [NIM 5]
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Proyek ini dikembangkan secara kolaboratif oleh tim kami, yang beranggotakan [Sebutkan nama-nama anggota kelompok]. Masing-masing anggota memegang peranan penting mulai dari analisis kebutuhan medis, perancangan arsitektur antarmuka, hingga implementasi perangkat lunak."*

---

### SLIDE 3: Latar Belakang Masalah

- **Tujuan:** Menjelaskan alasan fundamental mengapa Teman Kontrol perlu dibangun (masalah riil di lapangan).
- **Tata Letak Visual:**
  - **Kiri (35%):** Mockup smartphone menampilkan halaman Beranda ([02_home_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/02_home_screen.png)).
  - **Kanan (65%):** Judul "Latar Belakang", uraian 3 poin masalah kritis pasien, dan solusi yang diusulkan.
- **Aset Gambar:**
  - `docs/screenshots/02_home_screen.png`
- **Teks Slide:**
  ```text
  Latar Belakang

  Pasien penyakit kronis (seperti Hipertensi, Diabetes, dan Jantung) sering menghadapi kendala signifikan saat menjalani kontrol rutin ke fasilitas kesehatan:

  1. Lupa Pertanyaan Kritis: Waktu konsultasi dokter yang terbatas (rata-rata 5–10 menit) sering membuat pasien panik dan lupa menanyakan keluhan penting.
  2. Arahan Dokter Tercecer: Instruksi verbal mengenai dosis obat, pantangan, dan diagnosis sering kali terlupakan begitu pasien keluar dari ruang periksa.
  3. Tindak Lanjut Terabaikan: Tugas lanjutan seperti cek laboratorium rutin atau tebus obat berkala tidak terlacak dalam satu media terstruktur.
  4. Kerumitan Aplikasi Berbasis Server: Banyak aplikasi kesehatan mewajibkan registrasi akun rumit dan koneksi internet stabil, yang membebani lansia dan memicu kekhawatiran kebocoran data privasi medis.

  Teman Kontrol hadir sebagai solusi pendamping mandiri yang menjembatani komunikasi pasien-dokter secara terstruktur, aman, dan 100% offline.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Latar belakang proyek ini berangkat dari riset dan panduan standar NHS serta AHRQ. Pasien penyakit kronis rata-rata hanya memiliki waktu konsultasi dokter yang sangat singkat. Akibatnya, pertanyaan penting kerap terlupakan, catatan instruksi dokter tercecer di kertas resep yang hilang, dan kepatuhan tindak lanjut menurun. Di sisi lain, aplikasi kesehatan komersial saat ini terlalu rumit, mewajibkan akun, dan menyimpan data riwayat sakit pasien di cloud server pihak ketiga yang rentan privasi. Teman Kontrol menjawab masalah ini dengan pendekatan personal dan privat."*

---

### SLIDE 4: Tujuan Aplikasi

- **Tujuan:** Menjabarkan 5 sasaran strategis dari aplikasi Teman Kontrol.
- **Tata Letak Visual:**
  - **Kiri (65%):** Judul "Tujuan Aplikasi", diikuti 5 kartu poin horizontal berurutan (badge 01–05).
  - **Kanan (35%):** Mockup smartphone menampilkan SCR-04 Detail Kunjungan ([04_detail_kunjungan.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/04_detail_kunjungan.png)).
- **Aset Gambar:**
  - `docs/screenshots/04_detail_kunjungan.png`
- **Teks Slide:**
  ```text
  Tujuan Aplikasi

  01  Membantu Pasien Menyiapkan Pertanyaan
      Menyediakan sarana penyusunan daftar pertanyaan sebelum hari kontrol agar konsultasi lebih terarah.

  02  Memfokuskan Konsultasi dengan Batas Prioritas
      Membatasi maksimal 3 pertanyaan prioritas tertinggi agar waktu periksa yang singkat dapat dimanfaatkan optimal.

  03  Mendokumentasikan Hasil dan Instruksi Dokter
      Menyediakan formulir catatan hasil konsultasi terstruktur (ringkasan kondisi, instruksi dokter, dan catatan tambahan).

  04  Melacak Rencana Tindak Lanjut Pasca Kontrol
      Menyediakan daftar periksa (checklist) tugas pasca kontrol lengkap dengan tanggal tenggat (deadline).

  05  Menjamin Kerahasiaan Data Kesehatan (Privacy-by-Design)
      Menyimpan seluruh data medis secara lokal tanpa akses internet, tanpa registrasi akun, dan tanpa pelacakan server.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Ada lima tujuan utama yang dicapai aplikasi ini. Pertama, membantu pasien menyusun pertanyaan sebelum jadwal tiba. Kedua, membatasi maksimal 3 prioritas—ini adalah kaidah behavioral health agar pasien fokus pada keluhan paling urgen. Ketiga, mencatat arahan dokter begitu keluar ruangan. Keempat, melacak checklist tindak lanjut dengan indikator tenggat. Dan kelima, menjamin privasi absolut dengan prinsip Privacy-by-Design."*

---

### SLIDE 5: Target Pengguna & Karakteristik Bentuk Aplikasi

- **Tujuan:** Menjelaskan segmentasi pengguna dan spesifikasi teknis platform.
- **Tata Letak Visual:**
  - **Kiri (50%):** Target Pengguna (Pasien kronis, lansia, caregiver keluarga).
  - **Kanan (50%):** Bentuk & Karakteristik Arsitektur Aplikasi.
- **Aset Gambar:**
  - `docs/screenshots/01_notice_screen.png` (sebagai thumbnail pendukung di sudut).
- **Teks Slide:**
  ```text
  Target Pengguna & Bentuk Aplikasi

  🎯 Target Pengguna:
  • Pasien Penyakit Kronis: Individu dengan Hipertensi, Diabetes Melitus, Jantung, atau penyakit degeneratif yang memerlukan kontrol dokter rutin.
  • Pendamping / Caregiver: Anggota keluarga atau perawat yang mendampingi pasien lansia saat kontrol.
  • Pengguna yang Mengutamakan Privasi: Pasien yang tidak ingin data kesehatannya diunggah ke server komersial.

  📱 Bentuk & Karakteristik Aplikasi:
  • Native Android Application: Dikembangkan menggunakan Kotlin & Jetpack Compose modern (Material 3).
  • 100% Offline-First: Berjalan penuh tanpa pulsa, kuota, atau sinyal internet.
  • Tanpa Izin Berbahaya: Tanpa akses internet (INTERNET permission ditiadakan), tanpa GPS, tanpa akses kontak/kamera.
  • Desain Ramah Pasien: Ukuran font terbaca jelas, tombol sentuh lega (≥48 dp), dan kontras warna memenuhi standar aksesibilitas WCAG.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Target audiens kami adalah pasien kronis dewasa hingga lansia, serta keluarga yang bertindak sebagai caregiver. Karena digunakan oleh rentang usia yang luas, bentuk aplikasi dibuat berupa aplikasi native Android dengan antarmuka Material 3 bertema Teal yang menenangkan, teks berkontras tinggi, tombol sentuh besar, serta nol kebutuhan internet."*

---

### SLIDE 6: Tampilan Pembuka & Batas Penggunaan (Disclaimer Medis)

- **Tujuan:** Menunjukkan transparansi etika medis dan komitmen privasi lokal saat pertama kali dibuka.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan SCR-01 Notice Screen ([01_notice_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/01_notice_screen.png)).
  - **Kanan (60%):** Judul "Halaman Pembuka (Batas Medis & Privasi)" dan penjelasan 3 komitmen utama aplikasi.
- **Aset Gambar:**
  - `docs/screenshots/01_notice_screen.png`
- **Teks Slide:**
  ```text
  Halaman Pembuka (Batas Penggunaan & Privasi)

  Transparansi Etika & Batasan Hukum Medis:

  📌 Tujuan Alat Bantu
  Aplikasi secara tegas menyatakan fungsinya sebagai alat bantu pengorganisasi catatan kontrol pribadi pasien, bukan pengganti tenaga kesehatan.

  ⚠️ Batas Medis Eksplisit
  Bukan alat diagnosis, tidak meresepkan terapi/obat mandiri, dan bukan layanan panggilan darurat (dilengkapi rujukan darurat 119).

  🔒 Komitmen Data Lokal
  Penegasan bahwa seluruh catatan ditulis sendiri oleh pengguna dan hanya tersimpan di memori lokal smartphone.

  Persistensi Satu Kali (One-Time Gate):
  Status persetujuan disimpan permanen di DataStore Preferences, sehingga tidak mengganggu pengguna pada peluncuran berikutnya.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Saat pertama kali diinstal, aplikasi menampilkan Halaman Batas Penggunaan. Ini adalah bentuk kepatuhan hukum dan etika medis yang ketat. Teman Kontrol secara transparan menegaskan bahwa aplikasi ini adalah alat bantu pengatur jadwal dan pertanyaan, bukan alat diagnosis klinis. Begitu pengguna menyetujui, statusnya tersimpan di DataStore dan pengguna langsung dialihkan ke Beranda."*

---

### SLIDE 7: Dashboard / Halaman Beranda

- **Tujuan:** Menjelaskan dashboard utama sebagai pusat kendali seluruh aktivitas kontrol.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan SCR-02 Home Screen ([02_home_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/02_home_screen.png)).
  - **Kanan (60%):** Judul "Dashboard / Beranda" dan rincian fitur ringkasan informasi.
- **Aset Gambar:**
  - `docs/screenshots/02_home_screen.png`
- **Teks Slide:**
  ```text
  Dashboard / Beranda

  Pusat Informasi Kontrol Medis dalam Satu Layar:

  ⚠️ Banner Peringatan Kunjungan Lewat Jadwal
  Mendeteksi otomatis jadwal kontrol yang telah melewati tanggal hari ini namun belum diperbarui statusnya, mencegah pasien lalai kontrol ulang.

  📅 Kartu Kunjungan Terdekat (Upcoming)
  Menampilkan jadwal kontrol terdekat lengkap dengan tanggal, jam, fasilitas kesehatan, serta indikator jumlah pertanyaan & tindak lanjut.

  ☑️ Daftar Tindak Lanjut Terbuka (Open Actions)
  Menampilkan maksimal 5 tugas pasca kontrol yang belum diselesaikan beserta tanggal tenggat (deadline) dan status keterlambatan.

  ➕ Tombol Aksi Cepat (Extended FAB)
  Tombol "+ Tambah Kunjungan" yang selalu mudah dijangkau satu tangan untuk mendaftarkan jadwal periksa baru.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Pada halaman Beranda, pengguna langsung disajikan dashboard cerdas. Jika ada jadwal yang terlewat, muncul banner peringatan kuning di bagian atas. Di bawahnya, terdapat kartu kontrol terdekat dan daftar tugas pasca kontrol yang belum dicentang. Pengguna bahkan dapat langsung mencentang tugas selesai langsung dari halaman Beranda ini."*

---

### SLIDE 8: Fitur 1 — Persiapan Pertanyaan & Batas 3 Prioritas

- **Tujuan:** Memperlihatkan fitur unggulan persiapan pertanyaan dan kaidah batas maksimal 3 prioritas.
- **Tata Letak Visual:**
  - **Kiri (50%):** 2 Mockup smartphone berdampingan: Daftar Pertanyaan ([05_kelola_pertanyaan.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/05_kelola_pertanyaan.png)) & Dialog Form ([05b_form_pertanyaan_saran.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/05b_form_pertanyaan_saran.png)).
  - **Kanan (50%):** Penjelasan logika bisnis batas prioritas dan pencegahan pertanyaan duplikat.
- **Aset Gambar:**
  - `docs/screenshots/05_kelola_pertanyaan.png`
  - `docs/screenshots/05b_form_pertanyaan_saran.png`
- **Teks Slide:**
  ```text
  Fitur 1: Kelola Pertanyaan & Prioritas

  Menyiapkan Konsultasi yang Fokus dan Efektif:

  ⭐ Kaidah Maksimal 3 Prioritas (Aturan VR-07)
  Pengguna dapat menandai pertanyaan terpenting dengan badge ungu "Prioritas". Sistem mengunci batas maksimal 3 pertanyaan prioritas agar sesi konsultasi dokter tetap terfokus pada keluhan utama.

  🚫 Proteksi Pertanyaan Duplikat (Aturan VR-08)
  Basis data lokal SQLite Room memiliki indeks unik ternormalisasi yang secara otomatis menolak pertanyaan kembar dalam kunjungan yang sama.

  ☑️ Checklist Interaktif Saat Konsultasi
  Saat berhadapan dengan dokter, pertanyaan yang telah dijawab dapat dicentang langsung. Posisi teks tetap stabil (tidak melompat) dengan format tercoret rapi.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Fitur pertama adalah Kelola Pertanyaan. Di sini diterapkan kaidah ilmiah: maksimal 3 pertanyaan prioritas. Mengapa 3? Karena waktu periksa dokter rata-rata pendek. Jika pasien membawa 20 pertanyaan tanpa prioritas, dokter tidak akan sempat menjawab semuanya. Selain itu, sistem kami memiliki proteksi otomatis anti-duplikat di tingkat database SQLite Room."*

---

### SLIDE 9: Fitur 2 — Katalog Saran Pertanyaan Cerdas

- **Tujuan:** Menunjukkan bagaimana aplikasi membantu pasien yang bingung atau awam medis menyusun pertanyaan.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan Bottom Sheet Katalog Saran ([05c_saran_pertanyaan_sheet.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/05c_saran_pertanyaan_sheet.png)).
  - **Kanan (60%):** Penjelasan katalog template pertanyaan non-klinis berbasis AHRQ QuestionBuilder.
- **Aset Gambar:**
  - `docs/screenshots/05c_saran_pertanyaan_sheet.png`
- **Teks Slide:**
  ```text
  Fitur 2: Katalog Saran Pertanyaan

  Inspirasi Pertanyaan Medis Non-Klinis:

  💡 Bantuan untuk Pasien Awam
  Banyak pasien merasa bingung apa yang harus ditanyakan kepada dokter. Fitur ini menyediakan template pertanyaan siap pakai yang disusun berdasarkan panduan AHRQ (Agency for Healthcare Research and Quality).

  📂 4 Kategori Relevan:
  1. Gejala: "Apa kemungkinan penyebab keluhan saya?", "Aktivitas apa yang perlu dihindari?"
  2. Pemeriksaan: "Pemeriksaan apa yang perlu dijalani?", "Apa yang perlu disiapkan?"
  3. Perawatan & Obat: "Bagaimana cara pakai obat ini?", "Efek samping apa yang diwaspadai?"
  4. Langkah Berikutnya: "Kapan saya perlu kontrol lagi?", "Tanda apa yang mengharuskan segera kembali?"

  ⚡ Tipe Data Statis Berkualitas
  Disimpan sebagai konstanta type-safe di memori lokal, menghasilkan waktu muat instan (0 milidetik) tanpa kuota internet.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Seringkali pasien awam merasa bingung ingin bertanya apa. Teman Kontrol menyediakan Bottom Sheet Katalog Saran Pertanyaan. Diadaptasi dari standar QuestionBuilder internasional, saran dikelompokkan ke dalam 4 kategori: Gejala, Pemeriksaan, Perawatan Obat, dan Langkah Berikutnya. Pengguna cukup memilih template, dan teks akan langsung masuk ke form untuk disunting."*

---

### SLIDE 10: Fitur 3 — Detail Kunjungan & 3 Alur Kronologis

- **Tujuan:** Menjelaskan struktur detail kunjungan yang membagi alur periksa secara kronologis.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan SCR-04 Detail Kunjungan ([04_detail_kunjungan.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/04_detail_kunjungan.png)).
  - **Kanan (60%):** Penjelasan struktur 3 tahap kronologis kontrol.
- **Aset Gambar:**
  - `docs/screenshots/04_detail_kunjungan.png`
  - `docs/screenshots/04b_detail_tindak_lanjut.png`
- **Teks Slide:**
  ```text
  Fitur 3: Detail Kunjungan (Alur Kronologis)

  Integrasi 3 Tahapan Kontrol Medis dalam Satu Tampilan:

  1️⃣ Tahap 1: Persiapan (Sebelum Kontrol)
  Menampilkan kartu daftar pertanyaan, status prioritas, dan persentase pertanyaan yang sudah dibahas bersama dokter.

  2️⃣ Tahap 2: Hasil Konsultasi (Saat / Setelah Kontrol)
  Menampilkan pratinjau rangkuman diagnosis dan instruksi dokter yang baru saja dicatat oleh pasien atau pendamping.

  3️⃣ Tahap 3: Rencana Lanjutan (Tindak Lanjut Pasca Kontrol)
  Menampilkan daftar tugas kepatuhan pengobatan pasien yang harus diselesaikan setelah pulang dari fasilitas kesehatan.

  🔄 Kontrol Status Fleksibel
  Status kunjungan dapat diperbarui dengan mudah (Terjadwal, Selesai, Dibatalkan) dilengkapi dialog konfirmasi pencegahan salah tekan.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Halaman Detail Kunjungan didesain mengikuti perjalanan nyata pasien. Dibagi menjadi tiga tahap kronologis: Tahap Persiapan sebelum berangkat, Tahap Hasil Konsultasi saat keluar ruang periksa, dan Tahap Tindak Lanjut untuk rencana pengobatan di rumah. Seluruh rekam perjalanan satu janji temu ada di satu layar terpadu."*

---

### SLIDE 11: Fitur 4 — Pencatatan Hasil Konsultasi Pasien

- **Tujuan:** Menunjukkan bagaimana hasil pembicaraan dokter dicatat secara terstruktur.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan SCR-06 Hasil Konsultasi ([06_hasil_konsultasi.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/06_hasil_konsultasi.png)).
  - **Kanan (60%):** Penjelasan 3 bagian formulir catatan dan validasi integritas data.
- **Aset Gambar:**
  - `docs/screenshots/06_hasil_konsultasi.png`
- **Teks Slide:**
  ```text
  Fitur 4: Pencatatan Hasil Konsultasi

  Dokumentasi Instruksi Medis yang Rapi dan Terarah:

  📝 Pembagian 3 Bagian Catatan:
  • Ringkasan Pembahasan: Catatan umum kondisi fisik dan evaluasi keluhan dari dokter.
  • Arahan Tenaga Kesehatan: Instruksi resmi mengenai perubahan dosis obat, jadwal minum obat, atau rujukan lab.
  • Catatan Tambahan: Hal-hal khusus seperti saran pola makan, pantangan, atau anjuran olahraga ringan.

  🛡️ Validasi Pintar (Aturan VR-09 & VR-10):
  • Mencegah data sampah: Formulir menolak penyimpanan jika ketiga kolom dibiarkan kosong.
  • Kapasitas lega hingga 2.000 karakter per bagian dilengkapi penghitung karakter otomatis (character counter).
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Fitur berikutnya adalah Pencatatan Hasil Konsultasi. Kami tidak menggunakan kotak catatan bebas tanpa pola, melainkan membaginya menjadi tiga area terstruktur: Ringkasan Kondisi, Arahan Khusus Dokter, dan Catatan Tambahan. Desain ini memastikan poin-poin krusial seperti dosis obat tidak tertumpuk dengan catatan pola makan."*

---

### SLIDE 12: Fitur 5 — Pelacakan Tindak Lanjut & Tenggat Waktu

- **Tujuan:** Menjelaskan pengelolaan tugas pasca periksa medis.
- **Tata Letak Visual:**
  - **Kiri (50%):** 2 Mockup smartphone: Daftar Tindak Lanjut ([07_tindak_lanjut.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/07_tindak_lanjut.png)) & Dialog Form ([07b_form_tindak_lanjut.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/07b_form_tindak_lanjut.png)).
  - **Kanan (50%):** Uraian mekanisme tenggat waktu dan pengelompokan otomatis.
- **Aset Gambar:**
  - `docs/screenshots/07_tindak_lanjut.png`
  - `docs/screenshots/07b_form_tindak_lanjut.png`
- **Teks Slide:**
  ```text
  Fitur 5: Pelacakan Tindak Lanjut

  Meningkatkan Kepatuhan Pengobatan (Treatment Adherence):

  📋 Manajemen Tugas Mandiri
  Mencatat tugas medis pasca kontrol, seperti: 'Tebus obat rutin Amlodipine di apotek', 'Cek laboratorium gula darah puasa', atau 'Ukur tensi berkala'.

  ⏰ Pemilih Tenggat Waktu (Date Picker)
  Setiap tugas dapat diberikan tenggat target penyelesaian opsional menggunakan Material 3 DatePicker.

  ⏳ Pemantauan Status Terlambat (Overdue)
  Tugas yang belum selesai dan telah melewati tenggat akan otomatis diberi label peringatan visual oranye "Lewat tenggat".

  ✅ Pengelompokan Otomatis
  Saat checkbox dicentang, tugas otomatis berpindah ke kelompok 'Selesai' dengan pencatatan waktu penyelesaian (*timestamp*).
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Tindak lanjut adalah bagian krusial bagi kesembuhan pasien kronis. Fitur ini berfungsi sebagai checklist kepatuhan pengobatan. Pasien dapat menambahkan tenggat waktu, dan sistem akan memberi label 'Lewat tenggat' jika tugas tersebut terlupakan. Begitu selesai, tugas cukup dicentang dan akan berpindah ke riwayat penyelesaian."*

---

### SLIDE 13: Fitur 6 — Riwayat Kontrol & Filter Status

- **Tujuan:** Menunjukkan bagaimana seluruh arsip kunjungan dapat ditelusuri dengan cepat.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan SCR-08 Riwayat ([08_riwayat_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/08_riwayat_screen.png)).
  - **Kanan (60%):** Penjelasan fitur filter chip dinamis dan pengurutan kronologis.
- **Aset Gambar:**
  - `docs/screenshots/08_riwayat_screen.png`
- **Teks Slide:**
  ```text
  Fitur 6: Riwayat Kontrol & Filter

  Arsip Medis Pribadi yang Tersusun Kronologis:

  🔍 Filter Chips Cepat
  Memungkinkan penyaringan instan riwayat kunjungan berdasarkan status:
  • Semua: Menampilkan seluruh riwayat kontrol tanpa kecuali.
  • Terjadwal: Memantau kontrol yang akan datang.
  • Selesai: Mengarsipkan kontrol medis yang telah tuntas.
  • Dibatalkan: Mencatat janji temu yang dibatalkan oleh pasien/dokter.

  📉 Pengurutan Otomatis
  Seluruh riwayat disusun terurut dari tanggal periksa terbaru ke terlama (*descending order*).

  📊 Ringkasan Statistik Kartu
  Setiap kartu riwayat menampilkan informasi ringkas: tanggal, fasilitas kesehatan, jumlah pertanyaan, dan jumlah tindak lanjut terbuka.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Pada halaman Riwayat, seluruh arsip kontrol pengguna tersimpan rapi. Pengguna dapat menyaring kunjungan menggunakan chip filter instan: Semua, Terjadwal, Selesai, atau Dibatalkan. Setiap kartu memberikan ringkasan statistik yang informatif tanpa perlu membuka detail satu per satu."*

---

### SLIDE 14: Arsitektur Sistem & Keunggulan Privasi Lokal

- **Tujuan:** Menjelaskan keunggulan teknis, arsitektur basis data, dan proteksi privasi tingkat tinggi.
- **Tata Letak Visual:**
  - **Kiri (40%):** Mockup smartphone menampilkan SCR-09 Tentang Aplikasi ([09_tentang_screen.png](file:///d:/User/Documents/TemanKontrol/docs/screenshots/09_tentang_screen.png)).
  - **Kanan (60%):** Diagram / poin arsitektur database lokal SQLite Room dan proteksi keamanan manifest.
- **Aset Gambar:**
  - `docs/screenshots/09_tentang_screen.png`
- **Teks Slide:**
  ```text
  Arsitektur Sistem & Privasi Data

  Teknologi Modern dengan Prinsip Privacy-by-Design:

  🏛️ Arsitektur Data Lokal (Room SQLite v1)
  • 4 Entitas Terintegrasi: Consultations, Questions, Notes, dan Follow-Up Actions.
  • Relasi Aman (CASCADE Delete): Menghapus satu kunjungan otomatis membersihkan seluruh pertanyaan dan catatan terkait secara atomik.
  • Indeks Cepat: Query daftar dan kartu diproses di bawah 50 milidetik.

  🛡️ Nol Kebocoran Data (Zero Data Leakage)
  • Bebas Akses Internet: Manifest menghapus izin jaringan (android.permission.INTERNET ditiadakan).
  • Proteksi Backup Cloud: Fitur allowBackup dimatikan (false), data tidak dapat disedot ke Google Cloud cadangan.
  • Penyimpanan App-Private: Basis data hanya dapat diakses oleh aplikasi Teman Kontrol sendiri.

  📦 Performa Sangat Ringan
  Ukuran file APK rilis resmi hanya 6,8 MB (sangat hemat memori smartphone).
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Dari segi arsitektur teknis, Teman Kontrol mengadopsi standar modern Android. Basis data menggunakan Room SQLite dengan relasi foreign key cascade yang menjamin integritas data. Yang terpenting: aspek privasi. Kami secara eksplisit menonaktifkan izin INTERNET di manifest dan mematikan auto backup cloud. Data kesehatan pengguna 100% terkunci di smartphone mereka sendiri, dan ukuran aplikasinya sangat ramping, hanya 6,8 Megabyte."*

---

### SLIDE 15: Kelebihan dan Kekurangan Aplikasi

- **Tujuan:** Memberikan analisis objektif, transparan, dan komprehensif mengenai kekuatan produk serta ruang pengembangannya di masa depan.
- **Tata Letak Visual:**
  - **Tengah/Dua Kolom (50% : 50%):** 
    - Kolom Kiri: Kartu Hijau/Teal untuk **Kelebihan Aplikasi**.
    - Kolom Kanan: Kartu Abu-abu/Gelap untuk **Kekurangan & Potensi Masa Depan**.
- **Teks Slide:**
  ```text
  Kelebihan dan Kekurangan Aplikasi

  ✨ Kelebihan Aplikasi:
  • 100% Offline & Bebas Kuota: Berfungsi penuh di mana saja tanpa memerlukan sinyal seluler atau Wi-Fi.
  • Privasi Mutlak (Privacy-by-Design): Bebas kebocoran data medis karena tidak ada server atau pihak ketiga.
  • Antarmuka Ramah Lansia: Kontras warna teruji (Material 3 Teal), ukuran tombol lega, dan bebas iklan.
  • Struktur Kronologis Jelas: Mengatur alur sebelum, saat, dan sesudah periksa secara sistematis.
  • Ukuran Ekstra Ramping: APK rilis hanya 6,8 MB, ringan dipasang di ponsel dengan memori terbatas.
  • Bebas Hambatan Masuk: Tanpa perlu registrasi, email, atau password.

  ⚠️ Kekurangan & Pengembangan Lanjutan:
  • Belum Ada Pengingat Notifikasi Sistem: Alarm pengingat jam kontrol masih masuk dalam roadmap lanjutan.
  • Belum Ada Fitur Ekspor PDF: Rangkuman catatan belum bisa diekspor langsung ke dokumen cetak/WhatsApp.
  • Terbatas Single-Device: Data tidak otomatis tersinkronisasi jika pengguna berganti smartphone baru.
  • Khusus Platform Android: Belum tersedia versi iOS untuk pengguna iPhone.
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Sebagai evaluasi produk, kelebihan utama Teman Kontrol terletak pada kedaulatan privasi lokalnya, keandalan tanpa internet, kemudahan penggunaan bagi lansia, serta ukuran APK yang sangat kecil tanpa iklan. Sementara keterbatasannya saat ini adalah belum adanya notifikasi pengingat otomatis dan fitur cetak PDF, yang menjadi prioritas roadmap pengembangan kami berikutnya."*

---

### SLIDE 16: Penutup & Terima Kasih (Thank You Slide)

- **Tujuan:** Menutup presentasi dengan kesan profesional, pesan penutup yang bermakna, serta membuka sesi tanya-jawab.
- **Tata Letak Visual:**
  - **Kiri (50%):** Komposisi 3 mockup smartphone miring (Beranda, Detail, dan Katalog Saran).
  - **Kanan (50%):** Judul "Terima Kasih / Thank You", kutipan penutup (*closing quote*), nama aplikasi, dan ajakan sesi diskusi.
- **Aset Gambar:**
  - `docs/screenshots/02_home_screen.png`
  - `docs/screenshots/04_detail_kunjungan.png`
  - `docs/screenshots/05c_saran_pertanyaan_sheet.png`
- **Teks Slide:**
  ```text
  Terima Kasih

  "Setiap kontrol medis adalah langkah berharga menuju kesembuhan,
  dan setiap pertanyaan berhak mendapatkan jawaban terbaik."

  Teman Kontrol
  Sahabat Setia di Setiap Kunjungan Medis Anda.

  Sesi Tanya & Jawab (Q&A) Dipersilakan
  ```
- **Catatan Presenter (Speaker Notes):**
  > *"Demikian presentasi dari kelompok kami mengenai aplikasi Teman Kontrol. Kami berharap inovasi ini dapat memberikan dampak nyata bagi kualitas hidup pasien penyakit kronis di Indonesia. Terima kasih atas perhatian Bapak/Ibu dosen dan rekan-rekan sekalian. Kami membuka sesi tanya jawab jika ada saran atau pertanyaan."*

---

## 🗂️ Daftar Aset Tangkapan Layar (Screenshots Reference)

Seluruh file tangkapan layar asli beresolusi tinggi (1080×2340 px) telah tersimpan di direktori `docs/screenshots/` dan siap dimasukkan ke slide Anda:

| File Name | Deskripsi Layar | Rekomendasi Nomor Slide |
|---|---|:---:|
| [`01_notice_screen.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/01_notice_screen.png) | Halaman Pembuka / Batas Medis & Privasi (SCR-01) | Slide 1, Slide 5, Slide 6 |
| [`02_home_screen.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/02_home_screen.png) | Dashboard Beranda / Kunjungan Terdekat & Tindak Lanjut (SCR-02) | Slide 1, Slide 3, Slide 7, Slide 16 |
| [`03_form_kunjungan.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/03_form_kunjungan.png) | Formulir Tambah Jadwal Kunjungan Baru (SCR-03) | Slide Tambahan / Lampiran Demo |
| [`04_detail_kunjungan.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/04_detail_kunjungan.png) | Detail Kunjungan Bagian 1 & 2 (Pertanyaan & Hasil) (SCR-04) | Slide 4, Slide 10, Slide 16 |
| [`04b_detail_tindak_lanjut.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/04b_detail_tindak_lanjut.png) | Detail Kunjungan Bagian 3 (Tindak Lanjut) (SCR-04) | Slide 10 |
| [`05_kelola_pertanyaan.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/05_kelola_pertanyaan.png) | Daftar Pertanyaan Dokter & Prioritas (SCR-05) | Slide 8 |
| [`05b_form_pertanyaan_saran.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/05b_form_pertanyaan_saran.png) | Dialog Tambah Pertanyaan & Switch Prioritas (SCR-05) | Slide 8 |
| [`05c_saran_pertanyaan_sheet.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/05c_saran_pertanyaan_sheet.png) | Bottom Sheet Katalog Saran Pertanyaan Cerdas (SCR-05) | Slide 9, Slide 16 |
| [`06_hasil_konsultasi.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/06_hasil_konsultasi.png) | Formulir 3 Bagian Catatan Hasil Konsultasi (SCR-06) | Slide 11 |
| [`07_tindak_lanjut.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/07_tindak_lanjut.png) | Layar Kelola Daftar Tindak Lanjut (SCR-07) | Slide 12 |
| [`07b_form_tindak_lanjut.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/07b_form_tindak_lanjut.png) | Dialog Tambah Tindak Lanjut & Tanggal Tenggat (SCR-07) | Slide 12 |
| [`08_riwayat_screen.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/08_riwayat_screen.png) | Halaman Riwayat & Filter Status Chips (SCR-08) | Slide 13 |
| [`09_tentang_screen.png`](file:///d:/User/Documents/TemanKontrol/docs/screenshots/09_tentang_screen.png) | Halaman Tentang & Komitmen Privasi Lokal (SCR-09) | Slide 14 |

---

## 💡 Tips Tambahan Saat Presentasi di Depan Penguji / Klien

1. **Jelaskan Nilai Medisnya Lebih Dulu Baru Teknisnya:** Awali presentasi dengan menyoroti masalah psikologis pasien yang sering blank/panik saat kontrol dokter, lalu perkenalkan Teman Kontrol sebagai solusinya.
2. **Soroti Aspek Privacy-by-Design:** Berikan penekanan kuat pada ketiadaan izin internet. Ini menjadi nilai jual (*unique selling point*) utama dibanding aplikasi lain yang sering menyalahgunakan data medis pengguna.
3. **Bawa File APK Resmi:** APK produksi resmi telah siap di `app/build/outputs/apk/release/app-release.apk` (~6,8 MB) dan dapat langsung dipasang di HP penguji untuk demo langsung setelah presentasi.
