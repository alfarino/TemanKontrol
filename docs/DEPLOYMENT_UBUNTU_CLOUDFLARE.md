# Panduan Deployment Self-Hosted Teman Kontrol API di Private Ubuntu Server + Cloudflare (`unand.tech`)

Panduan ini menyiapkan container **`temankontrol-api`** (Ktor + **SQLite WAL Mode** + Penyimpanan Media Foto/VN) dengan batas memori **maksimal 256 MB** agar ringan dan aman berjalan berdampingan dengan container Docker lain di Ubuntu Server milikmu.

---

## 1. Prasyarat di Ubuntu Server

- Docker & Docker Compose (`docker compose version`)
- Tunnel `cloudflared` (atau dapat diatur melalui Dashboard **Cloudflare Zero Trust -> Networks -> Tunnels**)

---

## 2. Langkah Deploy Container `temankontrol-api`

1. Salin atau `git pull` repositori `TemanKontrol` ke Ubuntu Server:
   ```bash
   git clone https://github.com/alfarino/TemanKontrol.git
   cd TemanKontrol/server
   ```

2. Buat secret JWT yang kuat di file `.env` (bersebelahan dengan `docker-compose.yml`):
   ```bash
   echo "TEMANKONTROL_JWT_SECRET=$(openssl rand -hex 32)" > .env
   ```

3. Bangun dan jalankan container di background:
   ```bash
   docker compose up -d --build
   ```

4. Verifikasi healthcheck container & SQLite WAL Mode:
   ```bash
   curl -s http://127.0.0.1:8080/health
   # Output diharapkan: {"status":"UP","service":"temankontrol-api","database":"sqlite","journalMode":"wal",...}
   ```

5. Cek penggunaan RAM container (akan berkisar ~40–90 MB saat idle, dibatasi keras pada 256 MB):
   ```bash
   docker stats --no-stream temankontrol-api
   ```

---

## 3. Menghubungkan Subdomain Cloudflare (`unand.tech`)

Di **Cloudflare Zero Trust Dashboard -> Networks -> Tunnels -> pilih tunnel server Ubuntu -> Public Hostname -> Add a public hostname**:

| Subdomain | Service Type | URL |
|---|---|---|
| `api.temankontrol.unand.tech` | `HTTP` | `http://127.0.0.1:8080` |
| `temankontrol.unand.tech` | `HTTP` | `http://127.0.0.1:8080` |

> Pastikan di menu **SSL/TLS** Cloudflare mode diset ke **Full (Strict)**, dan tidak perlu membuka port `8080` di firewall/UFW publik karena `cloudflared` terhubung keluar secara terenkripsi.

Uji dari internet:
- `https://api.temankontrol.unand.tech/health`
- `https://temankontrol.unand.tech/privacy`

---

## 4. Jadwal Backup Harian Otomatis (Zero-Downtime SQLite Snapshot)

Tambahkan ke `crontab -e` di Ubuntu Server untuk menjalankan backup harian setiap jam 02:00 WIB:
```cron
0 2 * * * docker exec temankontrol-api sh -c "sqlite3 /data/db/temankontrol.db \".backup '/data/backups/temankontrol-$(date +\%F).db'\"" && tar -czf /path/ke/TemanKontrol/server/data/backups/attachments-$(date +\%F).tar.gz -C /path/ke/TemanKontrol/server/data attachments
```
