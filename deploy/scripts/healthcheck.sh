#!/usr/bin/env bash
# 多端静态站点 + 后端健康巡检；可接入 Prometheus textfile / 宝塔监控
set -uo pipefail
ENV_FILE="${ENV_FILE:-/etc/meiye/meiye.env}"
[[ -f "$ENV_FILE" ]] && source "$ENV_FILE"

BASE="http://127.0.0.1:${SERVER_PORT:-8081}"
FAIL=0

check() {
  local name="$1" url="$2" expect="${3:-200}"
  local code
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 8 "$url" || echo 000)"
  if [[ "$code" == "$expect" ]]; then
    echo "OK   $name ($code)"
  else
    echo "FAIL $name (expected $expect, got $code)"
    FAIL=1
  fi
}

echo "== 美业平台健康巡检 $(date '+%F %T') =="
check "backend health" "$BASE/api/health" 200
check "public portal" "$BASE/api/public/portal/5MY/home" 200
check "admin spa" "https://admin.example.com/" 200
check "portal spa" "https://www.example.com/" 200
check "mp h5" "https://m.example.com/" 200
check "worker h5" "https://worker.example.com/" 200

echo "== 系统资源 =="
free -h | sed -n '1,2p'
df -h /opt | tail -1
systemctl is-active meiye-backend nginx || FAIL=1

echo "== 最近错误 =="
grep -c ' ERROR ' /opt/meiye/logs/meiye.log 2>/dev/null | awk '{print "ERROR lines: "$1}'

exit "$FAIL"
