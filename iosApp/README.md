# Panduan Build & Jalankan Teman Kontrol di iPhone / iOS (Opsi 2A KMP)

1. Buka folder `TemanKontrol` di Mac yang terpasang **Xcode 16+**.
2. Jalankan perintah berikut untuk membangun framework iOS dari `:composeApp`:
   ```bash
   ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
   # atau untuk iPhone fisik:
   ./gradlew :composeApp:linkReleaseFrameworkIosArm64
   ```
3. Buat/buka target App iOS di Xcode dengan `Bundle Identifier = id.temankontrol.app` yang mengarah ke folder `iosApp/iosApp/` (`iOSApp.swift`, `ContentView.swift`, `Info.plist`).
4. Tambahkan file audio alarm berdurasi 30 detik bernama `alarm_teman_kontrol.caf` ke *Copy Bundle Resources* di Xcode agar pengingat Time-Sensitive berbunyi seperti alarm.
5. Tekan **Run (Cmd + R)** untuk menjalankan di iOS Simulator atau iPhone.
