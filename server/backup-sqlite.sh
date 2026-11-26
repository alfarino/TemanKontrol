#!/usr/bin/env bash
set -euo pipefail

# Zero-downtime daily backup for Teman Kontrol SQLite WAL + attachments
DATE_TAG="$(date +%F_%H-%M)"
BACKUP_DIR="/data/backups"
mkdir -p "$BACKUP_DIR"

sqlite3 /data/db/temankontrol.db ".backup '$BACKUP_DIR/temankontrol-$DATE_TAG.db'"
tar -czf "$BACKUP_DIR/attachments-$DATE_TAG.tar.gz" -C /data attachments

# Keep last 14 days of snapshots
find "$BACKUP_DIR" -type f -mtime +14 -delete
echo "[Backup OK] $DATE_TAG"
