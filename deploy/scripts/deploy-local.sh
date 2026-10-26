#!/usr/bin/env bash
# ============================================================
# 本机部署（应用服务器）：原子替换 jar 与静态资源，失败可回滚
# 用法：./deploy-local.sh [--skip-build]
# 前置：release/ 已由 build-all.sh 生成
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RELEASE="$ROOT/release"
APP_HOME="${APP_HOME:-/opt/meiye}"
WEB_HOME="${WEB_HOME:-/var/www/meiye}"
STAMP="$(date +%Y%m%d-%H%M%S)"

if [[ "${1:-}" != "--skip-build" ]]; then
  "$ROOT/deploy/scripts/build-all.sh"
fi

[[ -f "$RELEASE/meiye-backend.jar" ]] || { echo "缺少 release/meiye-backend.jar" >&2; exit 1; }

echo "==> 备份当前版本"
mkdir -p "$APP_HOME/releases"
if [[ -f "$APP_HOME/app/meiye-backend.jar" ]]; then
  cp "$APP_HOME/app/meiye-backend.jar" "$APP_HOME/releases/meiye-backend-${STAMP}.jar"
  ls -1t "$APP_HOME/releases"/meiye-backend-*.jar | tail -n +4 | xargs -r rm -f
fi

echo "==> 部署静态站点"
for site in admin portal mp-h5 worker-h5; do
  rm -rf "$WEB_HOME/$site.new"
  cp -r "$RELEASE/$site" "$WEB_HOME/$site.new"
  rm -rf "$WEB_HOME/$site.old"
  [[ -d "$WEB_HOME/$site" ]] && mv "$WEB_HOME/$site" "$WEB_HOME/$site.old"
  mv "$WEB_HOME/$site.new" "$WEB_HOME/$site"
  chown -R root:root "$WEB_HOME/$site"
  find "$WEB_HOME/$site" -type d -exec chmod 755 {} +
  find "$WEB_HOME/$site" -type f -exec chmod 644 {} +
done

echo "==> 替换后端 jar"
install -m 750 "$RELEASE/meiye-backend.jar" "$APP_HOME/app/meiye-backend.jar.new"
mv "$APP_HOME/app/meiye-backend.jar.new" "$APP_HOME/app/meiye-backend.jar"
chown -R meiye:meiye "$APP_HOME/app" "$APP_HOME/data" "$APP_HOME/logs"

echo "==> 重载 Nginx + 重启后端"
nginx -t
systemctl reload nginx
systemctl restart meiye-backend

echo "==> 健康检查"
for i in $(seq 1 40); do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT:-8081}/api/health" >/dev/null; then
    echo "部署成功：http://127.0.0.1:${SERVER_PORT:-8081}/api/health"
    exit 0
  fi
  sleep 2
done

echo "ERROR: 后端未就绪，开始回滚" >&2
PREV="$(ls -1t "$APP_HOME/releases"/meiye-backend-*.jar 2>/dev/null | head -1 || true)"
if [[ -n "$PREV" ]]; then
  cp "$PREV" "$APP_HOME/app/meiye-backend.jar"
  systemctl restart meiye-backend
  echo "已回滚到 $PREV"
fi
exit 1
