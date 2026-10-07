# 📋 Panduan Pengujian & Penggunaan Aplikasi Teman Kontrol

Dokumen ini disusun untuk mempermudah klien, penguji (*tester*), atau dosen pembimbing/penguji dalam menginstal, menguji seluruh fitur, dan memvalidasi alur kerja aplikasi **Teman Kontrol**.

---

## 1. Tentang Teman Kontrol

**Teman Kontrol** adalah aplikasi *Personal Health Record* (PHR) pendamping kontrol medis yang dirancang khusus untuk pasien penyakit kronis (seperti Hipertensi, Diabetes, Jantung) dan pendampingnya (*caregiver*). 

Aplikasi ini beroperasi **100% Offline (Tanpa Internet)** untuk menjaga kerahasiaan riwayat medis pasien, dengan prinsip desain yang ramah lansia: teks kontras, ukuran tombol sentuh nyaman, serta antarmuka yang bersih berbasis Material 3 (*Teal Theme*).

---

## 2. Persyaratan Sistem & Perangkat

| Komponen | Persyaratan Minimum | Rekomendasi |
|---|---|---|
| **Sistem Operasi** | Android 11.0 (API Level 30) | Android 12, 13, 14, atau 15 |
| **Koneksi Internet** | **Tidak memerlukan internet** (Bebas pulsa/kuota) | Offline |
| **Izin Aplikasi** | Tidak membutuhkan izin berbahaya (*No special permissions*) | - |
| **Target Pengujian** | Emulator Android Studio atau Smartphone Android fisik | Smartphone Android fisik / Emulator Pixel API 33+ |

---

## 3. Lokasi File APK & Cara Instalasi

File APK hasil kompilasi siap pakai telah tersedia pada direktori berikut:

1. **APK Rilis Resmi (Sangat Direkomendasikan - Ringan & Cepat):**
   ```text
   app/build/outputs/apk/release/app-release.apk
   ```
   - Ukuran: **~3,3 MB** (dioptimasi dengan R8 code minification & resource shrinking).
   - Telah ditandatangani (*signed*) sehingga dapat langsung dipasang di HP fisik maupun emulator.

2. **APK Debug (Versi Pengembangan):**
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
   - Ukuran: ~64 MB (berisi simbol debug dan tooling pengembang).

### Metode A: Instalasi di HP Android Fisik (Rekomendasi Utama untuk Klien)
1. Kirimkan file **`app-release.apk`** ke smartphone Anda (melalui kabel USB data, WhatsApp, Telegram, atau Google Drive).
2. Buka file manager di HP, lalu ketuk file `app-release.apk`.
3. Jika muncul dialog keamanan *"Izinkan instalasi dari sumber ini"* (*Allow unknown sources*), pilih **Izinkan** (*Allow*).
4. Pilih **Instal / Pasang**, kemudian ketuk **Buka**.

### Metode B: Instalasi di Emulator Android Studio (Virtual Device)
1. Buka Android Studio dan jalankan Virtual Device (AVD) Anda (misalnya Pixel 5 API 30+).
2. Buka Windows Explorer ke folder project: `D:\User\Documents\TemanKontrol\app\build\outputs\apk\release\`.
3. Tarik (*drag and drop*) file **`app-release.apk`** langsung ke atas layar emulator yang menyala.
4. Tunggu beberapa detik hingga aplikasi terinstal, lalu ikon **Teman Kontrol** akan muncul di menu aplikasi emulator.

---

## 4. Alur & Skenario Pengujian Bertahap (Step-by-Step)

Aplikasi telah dilengkapi dengan fitur **Otomatis Seeding Data Contoh (D-SAMPLE)** saat pertama kali dijalankan, sehingga klien tidak perlu repot mengetik data dari awal untuk langsung merasakan pengalaman lengkap aplikasi.

---

### Skenario 1: Halaman Pembuka & Persetujuan Batas Medis (SCR-01)
*Tujuan: Memastikan pengguna memahami bahwa aplikasi ini adalah alat bantu pengorganisasi kontrol, bukan pengganti diagnosis medis.*

1. **Langkah:** Buka aplikasi Teman Kontrol pertama kali.
2. **Amati:**
   - Muncul halaman **Batas Penggunaan & Privasi**.
   - Terdapat ringkasan tanggung jawab dan pernyataan privasi lokal.
   - Tombol **"Saya Mengerti & Lanjutkan"**.
3. **Aksi:** Ketuk tombol *"Saya Mengerti & Lanjutkan"*.
4. **Hasil yang Diharapkan:** Aplikasi berpindah ke halaman Beranda. Jika aplikasi ditutup dan dibuka kembali, halaman pembuka ini **tidak akan muncul lagi** (tersimpan permanen di DataStore).

---

### Skenario 2: Halaman Beranda (Home - SCR-02)
*Tujuan: Menguji dashboard utama yang menampilkan kunjungan terdekat dan tindak lanjut mendesak.*

1. **Amati Elemen Beranda:**
   - **Kartu Kunjungan Mendatang:** Menampilkan jadwal kontrol terdekat dengan tanggal, jam, faskes, serta statistik pertanyaan dan tindak lanjut.
   - **Daftar Tindak Lanjut Terbuka:** Menampilkan tugas/tindakan pasca-kontrol yang belum selesai, lengkap dengan indikator tenggat waktu (*deadline*).
   - **Banner Peringatan (Jika ada jadwal lampau):** Jika ada jadwal yang terlewat, muncul banner kuning peringatan untuk segera memperbarui status.
   - **Tombol Tambah (+ Tambah Jadwal):** Floating Action Button di pojok kanan bawah.

---

### Skenario 3: Menambah Jadwal Kontrol Baru (SCR-03)
*Tujuan: Menguji pembuatan janji temu kontrol dokter dan validasi form input.*

1. **Langkah:** Ketuk tombol **`+ Tambah Jadwal`** di halaman Beranda.
2. **Uji Validasi Form:**
   - Biarkan kolom *Judul Kunjungan* dan *Alasan* kosong, lalu ketuk **Simpan Jadwal**.
   - *Amati:* Muncul pesan kesalahan inline berwarna merah (*"Judul kunjungan wajib diisi"* dan *"Alasan kontrol wajib diisi"*).
3. **Isi Form Valid:**
   - **Judul Kunjungan:** `Kontrol Rutin Hipertensi`
   - **Tanggal & Jam:** Ketuk pemilih tanggal & waktu, pilih tanggal esok hari pukul 09:00 WIB.
   - **Alasan Kontrol:** `Evaluasi tensi darah dan ambil resep rutin Amlodipine`
   - **Fasilitas Kesehatan (Opsional):** `RSUD Harapan Sehat`
   - **Nama Dokter (Opsional):** `dr. Siti Rahma, Sp.PD`
4. **Langkah:** Ketuk **Simpan Jadwal**.
5. **Hasil yang Diharapkan:** Form tertutup, kembali ke Beranda, dan jadwal baru langsung tampil di urutan paling atas kunjungan mendatang.

---

### Skenario 4: Detail Kunjungan & Tiga Alur Kontrol (SCR-04)
*Tujuan: Memastikan pembagian alur kronologis kunjungan (Sebelum -> Saat/Setelah -> Rencana Lanjutan).*

1. **Langkah:** Di halaman Beranda, ketuk kartu kunjungan yang baru dibuat atau kartu contoh yang ada.
2. **Amati 3 Kartu Alur Kronologis:**
   - **Tahap 1 (Sebelum Kunjungan):** Kartu *Daftar Pertanyaan* untuk dokter.
   - **Tahap 2 (Hasil Konsultasi):** Kartu ringkasan diagnosa & arahan dokter.
   - **Tahap 3 (Rencana Lanjutan):** Kartu daftar tindak lanjut (*action items*).
3. **Uji Penggantian Status Kunjungan:**
   - Ketuk menu titik tiga (⋮) di pojok kanan atas top bar.
   - Pilih opsi ganti status: **Tandai Selesai** atau **Batalkan Kunjungan**.
   - *Amati:* Badge status kunjungan langsung berubah warna (Hijau untuk *Selesai*, Abu-abu untuk *Dibatalkan*).

---

### Skenario 5: Mengelola Pertanyaan & Saran Pintar (SCR-05)
*Tujuan: Menguji persiapan daftar pertanyaan agar pasien tidak lupa saat berhadapan dengan dokter.*

1. **Langkah:** Pada halaman Detail Kunjungan, ketuk tombol **`Kelola Pertanyaan`**.
2. **Uji Tambah Pertanyaan Manual:**
   - Ketuk kolom input di bagian bawah, ketik: `Apakah pusing di pagi hari berhubungan dengan dosis obat?`.
   - Aktifkan ikon bintang/sakelar **Prioritas**.
   - Ketuk tombol kirim/tambah. Pertanyaan masuk ke daftar dengan badge **Prioritas**.
3. **Uji Batas Maksimal 3 Prioritas (Aturan VR-07):**
   - Tambahkan 3 pertanyaan dengan status Prioritas.
   - Coba tambahkan atau ubah pertanyaan ke-4 menjadi Prioritas.
   - *Amati:* Sistem menolak dan menampilkan peringatan bahwa batas maksimal pertanyaan prioritas adalah **3 pertanyaan** (agar konsultasi fokus).
4. **Uji Proteksi Duplikat (Aturan VR-08):**
   - Coba ketik pertanyaan yang sama persis (meski berbeda huruf kapital/spasi).
   - *Amati:* Sistem mendeteksi duplikat dan menolak input pertanyaan ganda.
5. **Uji Katalog Saran Pertanyaan:**
   - Ketuk tombol **`Lihat Saran Pertanyaan`**.
   - Muncul lembar (*bottom sheet*) kategori: *Gejala*, *Obat & Efek Samping*, *Gaya Hidup*.
   - Ketuk salah satu saran (misal: *"Apa efek samping utama dari obat baru ini?"*).
   - *Amati:* Pertanyaan otomatis ditambahkan ke daftar pertanyaan Anda.
6. **Uji Checklist Saat Konsultasi:**
   - Centang kotak di samping pertanyaan yang sudah dijawab dokter. Teks akan tercoret tanda sudah dibahas.

---

### Skenario 6: Mencatat Hasil Konsultasi (SCR-06)
*Tujuan: Mencatat rangkuman arahan dokter setelah keluar dari ruang periksa.*

1. **Langkah:** Pada halaman Detail Kunjungan, ketuk tombol **`Catat Hasil`**.
2. **Uji Validasi (Minimal 1 Kolom Terisi):**
   - Biarkan ketiga kolom kosong, lalu ketuk **Simpan Catatan**.
   - *Amati:* Muncul peringatan bahwa minimal salah satu bagian harus diisi.
3. **Isi Catatan:**
   - **Ringkasan Kondisi:** `Tensi darah terukur 130/85, sudah membaik.`
   - **Arahan / Instruksi Dokter:** `Lanjutkan Amlodipine 5mg setiap pagi setelah makan.`
   - **Catatan Tambahan:** `Kurangi konsumsi makanan bergaram tinggi dan kecap.`
4. **Langkah:** Ketuk **Simpan Catatan**.
5. **Hasil yang Diharapkan:** Catatan tersimpan di database lokal Room dan tampil rapi pada kartu Tahap 2 di halaman Detail.

---

### Skenario 7: Tindak Lanjut Pasca Kontrol (SCR-07)
*Tujuan: Mencatat tugas dan kewajiban pasien setelah kontrol dokter.*

1. **Langkah:** Pada halaman Detail Kunjungan, ketuk tombol **`Kelola Tindak Lanjut`** (atau buka tab *Tindak Lanjut* di menu bawah).
2. **Tambah Tugas Baru:**
   - Masukkan teks: `Tebus resep obat di apotek faskes`
   - Pilih tenggat waktu: Tanggal hari ini atau besok.
   - Ketuk **Tambah**.
3. **Uji Penyelesaian Tindak Lanjut:**
   - Ketuk kotak centang (*checkbox*) pada tugas tersebut.
   - *Amati:* Tugas berpindah ke kelompok **"Sudah Selesai"**, teks dicoret, dan tercatat waktu penyelesaiannya.

---

### Skenario 8: Riwayat & Filter Status (SCR-08)
*Tujuan: Menguji penelusuran kontrol medis yang pernah dilakukan sebelumnya.*

1. **Langkah:** Buka tab menu bawah **`Riwayat`**.
2. **Uji Filter Kunjungan:**
   - Ketuk chip filter: **Semua**, **Terjadwal**, **Selesai**, **Dibatalkan**.
   - *Amati:* Daftar kartu riwayat langsung tersaring secara dinamis sesuai status yang dipilih.
   - Seluruh riwayat terurut berdasarkan tanggal kunjungan terbaru.

---

### Skenario 9: Menu Tentang & Penguji Data Mock Developer (SCR-09)
*Tujuan: Menguji informasi privasi dan fitur khusus pengujian UI untuk klien/demo.*

1. **Langkah:** Buka tab menu bawah **`Tentang`**.
2. **Amati:**
   - Informasi versi aplikasi dan pernyataan komitmen privasi lokal (data tidak dikirim ke server/cloud manapun).
   - Terdapat tombol untuk membaca ulang Ketentuan & Batas Penggunaan.
3. **Uji Skenario Pengujian Cepat (*Developer Mock Switcher*):**
   - Pada bagian *Developer Mock Scenario*, pilih radio button **"Kosong"**.
   - Buka kembali halaman **Beranda**, **Riwayat**, atau **Tindak Lanjut**.
   - *Amati:* Seluruh layar menampilkan antarmuka *Empty State* yang ramah (ikon informatif dan pesan bersahabat saat data masih kosong).
   - Kembali ke menu Tentang, pilih radio button **"Normal"**.
   - *Amati:* Database otomatis terisi kembali dengan data simulasi lengkap (*D-SAMPLE*).

---

## 5. Lembar Ceklis Pengujian Klien (Test Checklist)

Gunakan tabel di bawah ini untuk mencatat hasil pengujian bersama klien:

| No | Modul / Layar | Yang Diuji | Hasil yang Diharapkan | Status (Lulus/Gagal) |
|---|---|---|---|:---:|
| 1 | **Instalasi** | Pasang APK di emulator/HP | Aplikasi terpasang lancar tanpa crash | [  ] LULUS |
| 2 | **Disclaimer (SCR-01)** | Dialog persetujuan medis pertama kali | Muncul saat baru instal, tidak muncul lagi setelah disetujui | [  ] LULUS |
| 3 | **Beranda (SCR-02)** | Tampilan jadwal terdekat | Kunjungan terdekat dan checklist tindak lanjut tampil rapi | [  ] LULUS |
| 4 | **Form Input (SCR-03)** | Validasi judul & alasan kosong | Menolak simpan dan menampilkan pesan error merah | [  ] LULUS |
| 5 | **Form Input (SCR-03)** | Simpan kunjungan baru | Data tersimpan permanen di database lokal SQLite | [  ] LULUS |
| 6 | **Detail (SCR-04)** | Alur kronologis (3 tahap) | Terbagi jelas: Persiapan, Hasil, dan Tindak Lanjut | [  ] LULUS |
| 7 | **Pertanyaan (SCR-05)** | Maksimal 3 prioritas | Ditolak saat mencoba mengaktifkan prioritas ke-4 | [  ] LULUS |
| 8 | **Pertanyaan (SCR-05)** | Deteksi pertanyaan kembar/duplikat | Menolak pertanyaan ganda dalam satu kunjungan | [  ] LULUS |
| 9 | **Pertanyaan (SCR-05)** | Bottom sheet saran pertanyaan | Muncul kategori saran dan dapat ditambahkan sekali klik | [  ] LULUS |
| 10 | **Catatan (SCR-06)** | Rangkuman & instruksi dokter | Tersimpan rapi dan dapat disunting kembali kapan saja | [  ] LULUS |
| 11 | **Tindak Lanjut (SCR-07)** | Checkbox selesai tugas | Tugas berpindah ke bagian selesai dengan coretan waktu | [  ] LULUS |
| 12 | **Riwayat (SCR-08)** | Filter chips (Terjadwal/Selesai/Batal) | Daftar tersaring instan tanpa lag | [  ] LULUS |
| 13 | **Offline Persistence** | Matikan WiFi & Data HP, restart app | Seluruh data tetap tersimpan utuh dan aplikasi berjalan 100% | [  ] LULUS |
| 14 | **Scenario Switcher (SCR-09)** | Ganti mode "Kosong" & "Normal" | Database dapat dikosongkan dan diisi ulang otomatis untuk demo | [  ] LULUS |

---

## 6. Pertanyaan Umum / Solusi Masalah (Troubleshooting)

**Q: Apakah data di aplikasi akan hilang jika smartphone dimatikan atau di-restart?**  
> **A:** Tidak. Aplikasi menggunakan database lokal terenkripsi Room SQLite bawaan Android dan Jetpack DataStore yang tersimpan secara permanen di media penyimpanan internal perangkat.

**Q: Mengapa aplikasi tidak meminta izin internet atau izin lokasi?**  
> **A:** Sesuai prinsip desain *Privacy-by-Design* (PRD v1.0), aplikasi Teman Kontrol sengaja dibuat 100% tanpa izin jaringan internet (`INTERNET` permission ditiadakan) demi menjamin keamanan data riwayat penyakit pengguna tidak akan bocor ke pihak ketiga.

**Q: Bagaimana jika penguji ingin memulai pengujian dari kondisi aplikasi benar-benar bersih?**  
> **A:** Buka menu **Tentang** di aplikasi, lalu pada bagian *Developer Mock Scenario*, pilih **Kosong**. Atau melalui menu pengaturan Android di HP: *Pengaturan > Aplikasi > Teman Kontrol > Hapus Data (Clear Data)*.

---
*Dokumen ini dibuat otomatis sebagai panduan resmi pengujian aplikasi Teman Kontrol v1.0.*