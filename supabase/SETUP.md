# Setup Backend Supabase untuk Teman Kontrol

Project URL: `https://hqtrpbagnosmrznjiiph.supabase.co`

## Langkah 1 — Jalankan Skema Database, RLS, Storage & RPC
1. Buka **Supabase Dashboard → SQL Editor → New query**.
2. Salin seluruh isi [schema.sql](schema.sql) lalu klik **Run**.
3. Pastikan muncul pesan sukses (*Success. No rows returned*), dan di **Storage** terlihat bucket privat bernama `attachments`.

## Langkah 2 — Pengaturan Autentikasi (Email & Reset Sandi OTP)
1. Buka **Authentication → Sign In / Providers → Email**:
   - Untuk pengalaman daftar langsung pakai (tanpa perlu buka email konfirmasi dulu), **matikan** toggle **Confirm email** → klik **Save**. *(Jika ingin verifikasi email wajib, biarkan menyala; aplikasi tetap menanganinya dengan pesan ramah).*
2. Agar **Lupa Kata Sandi (kode OTP 6 digit)** di aplikasi berfungsi:
   - Buka **Authentication → Emails → Reset Password**.
   - Pastikan template berisi `{{ .Token }}` (bukan hanya `{{ .ConfirmationURL }}`), misalnya:
     `Kode OTP reset kata sandi Teman Kontrol Anda adalah: <b>{{ .Token }}</b>`
   - Klik **Save**.

## Langkah 3 — Deploy Edge Function `delete-account` (Hapus Akun Permanen)
1. Buka **Edge Functions → Deploy a new function → Via Editor**.
2. Beri nama function: `delete-account`.
3. Salin isi [functions/delete-account/index.ts](functions/delete-account/index.ts) lalu klik **Deploy function**.
4. *(Opsional)* Di tab **Details** function tersebut, pastikan JWT verification tetap **ON** (karena function memverifikasi Bearer token user).
