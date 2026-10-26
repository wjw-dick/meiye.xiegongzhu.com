#!/usr/bin/env bash
# ============================================================
# 美业平台每日备份：MySQL 全量 + Redis RDB + MinIO 镜像 + 配置
# 建议 crontab：0 2 * * * /opt/meiye/deploy/scripts/backup.sh >> /opt/meiye/logs/backup.log 2>&1
# ============================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-/etc/meiye/meiye.env}"
# shellcheck disable=SC1090
source "$ENV_FILE"

BACKUP_ROOT="${BACKUP_DIR:-/opt/meiye/data/backup}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"
TS="$(date +%Y%m%d-%H%M%S)"
DAY="$(date +%Y%m%d)"
TARGET="${BACKUP_ROOT}/${DAY}"
mkdir -p "$TARGET"
chmod 700 "$BACKUP_ROOT" "$TARGET"
umask 077

log() { echo "[$(date '+%F %T')] $*"; }

log "备份开始 -> $TARGET"

# ---------- 1. MySQL ----------
log "导出 MySQL ${DB_NAME}"
MYSQL_PWD="${MYSQL_BACKUP_PASSWORD:-$DB_PASSWORD}" mysqldump \
  -h "${DB_HOST}" -P "${MYSQL_PORT:-3306}" -u "${MYSQL_BACKUP_USER:-$DB_USER}" \
  --single-transaction --quick --routines --triggers --events \
  --default-character-set=utf8mb4 --no-tablespaces \
  "${DB_NAME}" | gzip -9 > "${TARGET}/mysql-${TS}.sql.gz"
log "MySQL 备份完成：$(du -h "${TARGET}/mysql-${TS}.sql.gz" | cut -f1)"

# ---------- 2. Redis ----------
if command -v redis-cli >/dev/null 2>&1; then
  log "导出 Redis RDB"
  redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT:-6379}" -a "${REDIS_PASSWORD:-}" --no-auth-warning \
    --rdb "${TARGET}/redis-${TS}.rdb" >/dev/null 2>&1 \
    && log "Redis 备份完成" \
    || log "WARN: Redis 备份失败（可能禁用了 SYNC），跳过"
else
  log "WARN: 未安装 redis-cli，跳过 Redis 备份"
fi

# ---------- 3. MinIO ----------
if command -v mc >/dev/null 2>&1; then
  log "镜像 MinIO bucket ${MINIO_BUCKET:-meiye}"
  mc alias set meiye-backup "http://${MINIO_HOST}:${MINIO_PORT:-9000}" "${MINIO_USER}" "${MINIO_PASSWORD}" >/dev/null
  mc mirror --overwrite --remove "meiye-backup/${MINIO_BUCKET:-meiye}" "${TARGET}/minio" >/dev/null \
    && log "MinIO 备份完成" || log "WARN: MinIO 备份失败"
else
  log "WARN: 未安装 mc，跳过 MinIO 备份"
fi

# ---------- 4. 配置与证书 ----------
log "归档配置"
tar -czf "${TARGET}/config-${TS}.tar.gz" \
  -C / etc/meiye \
  -C / etc/nginx/conf.d \
  -C / etc/systemd/system/meiye-backend.service \
  -C /opt/meiye/deploy 2>/dev/null || true

# ---------- 5. 校验与清单 ----------
( cd "$TARGET" && sha256sum ./* > "SHA256SUMS-${TS}.txt" )
{
  echo "backup_time=${TS}"
  echo "db=${DB_NAME}"
  echo "mysql_host=${DB_HOST}"
  echo "files:"
  ls -lh "$TARGET"
} > "${TARGET}/MANIFEST.txt"

# ---------- 6. 清理过期 ----------
find "$BACKUP_ROOT" -maxdepth 1 -type d -name '20*' -mtime "+${KEEP_DAYS}" -exec rm -rf {} +
log "清理 ${KEEP_DAYS} 天前备份完成"

# ---------- 7. 异地（可选） ----------
if [[ -n "${OFFSITE_RSYNC:-}" ]]; then
  log "异地同步 -> ${OFFSITE_RSYNC}"
  rsync -az --delete "${TARGET}" "${OFFSITE_RSYNC}/" || log "WARN: 异地同步失败"
fi

log "备份成功：${TARGET}"
