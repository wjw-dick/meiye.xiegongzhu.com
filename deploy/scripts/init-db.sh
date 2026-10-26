#!/usr/bin/env bash
# 首次导入数据库（谨慎：含 DROP DATABASE）
# 用法：./init-db.sh /path/to/meiye.env
set -euo pipefail

ENV_FILE="${1:-/etc/meiye/meiye.env}"
source "$ENV_FILE"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
SCHEMA="$ROOT/backend/src/main/resources/db/schema.sql"
DATA="$ROOT/backend/src/main/resources/db/data.sql"

[[ -f "$SCHEMA" ]] || { echo "缺少 $SCHEMA" >&2; exit 1; }
read -r -p "将重建数据库 ${DB_NAME}，输入 INIT 确认：" ans
[[ "$ans" == "INIT" ]] || { echo "已取消"; exit 1; }

export MYSQL_PWD="${DB_PASSWORD}"
mysql -h "$DB_HOST" -P "${MYSQL_PORT:-3306}" -u "$DB_USER" --default-character-set=utf8mb4 < "$SCHEMA"
mysql -h "$DB_HOST" -P "${MYSQL_PORT:-3306}" -u "$DB_USER" "$DB_NAME" --default-character-set=utf8mb4 < "$DATA"
echo "数据库初始化完成：${DB_NAME}"
