#!/usr/bin/env bash
# ============================================================
# 备份恢复演练 / 灾难恢复
# 用法：
#   ./restore.sh /opt/meiye/data/backup/20261026/mysql-20261026-020001.sql.gz
#   FORCE=1 ./restore.sh ...          # 跳过二次确认
#   RESTORE_REDIS=1 RESTORE_MINIO=1 ./restore.sh ...
# 注意：会覆盖当前数据库，务必在维护窗口执行并先停后端。
# ============================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-/etc/meiye/meiye.env}"
# shellcheck disable=SC1090
source "$ENV_FILE"

FILE="${1:-}"
if [[ -z "$FILE" || ! -f "$FILE" ]]; then
  echo "用法：$0 <mysql-*.sql.gz>" >&2
  exit 1
fi

if [[ "${FORCE:-0}" != "1" ]]; then
  echo "即将用 $FILE 覆盖数据库 ${DB_NAME}（${DB_HOST}）。"
  read -r -p "输入 RESTORE 确认：" ans
  [[ "$ans" == "RESTORE" ]] || { echo "已取消"; exit 1; }
fi

echo "[1/5] 停止后端"
systemctl stop meiye-backend 2>/dev/null || true

echo "[2/5] 恢复 MySQL"
MYSQL_PWD="${MYSQL_BACKUP_PASSWORD:-$DB_PASSWORD}" mysql \
  -h "${DB_HOST}" -P "${MYSQL_PORT:-3306}" -u "${MYSQL_BACKUP_USER:-$DB_USER}" \
  --default-character-set=utf8mb4 "${DB_NAME}" < <(gzip -dc "$FILE")

if [[ "${RESTORE_REDIS:-0}" == "1" ]]; then
  echo "[3/5] 恢复 Redis（请先确保 Redis 已停止并替换 dump.rdb）"
  RDB="$(dirname "$FILE")/$(ls "$(dirname "$FILE")" | grep '^redis-.*\.rdb$' | tail -1 || true)"
  if [[ -n "$RDB" && -f "$RDB" ]]; then
    redis-cli -h "${REDIS_HOST}" -p "${REDIS_PORT:-6379}" -a "${REDIS_PASSWORD:-}" --no-auth-warning FLUSHALL
    echo "Redis 数据需由运维将 $RDB 复制到 Redis dir 后重启；为避免误清空，本脚本未自动替换。"
  fi
else
  echo "[3/5] 跳过 Redis"
fi

if [[ "${RESTORE_MINIO:-0}" == "1" ]] && command -v mc >/dev/null 2>&1; then
  echo "[4/5] 恢复 MinIO"
  mc alias set meiye-restore "http://${MINIO_HOST}:${MINIO_PORT:-9000}" "${MINIO_USER}" "${MINIO_PASSWORD}" >/dev/null
  mc mirror --overwrite "$(dirname "$FILE")/minio" "meiye-restore/${MINIO_BUCKET:-meiye}"
else
  echo "[4/5] 跳过 MinIO"
fi

echo "[5/5] 启动后端并健康检查"
systemctl start meiye-backend
for i in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT:-8081}/api/health" >/dev/null; then
    echo "恢复完成，后端健康。"
    exit 0
  fi
  sleep 2
done
echo "WARN: 后端 60s 内未就绪，请查看 /opt/meiye/logs/" >&2
exit 2
