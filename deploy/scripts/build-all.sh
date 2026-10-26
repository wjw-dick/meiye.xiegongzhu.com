#!/usr/bin/env bash
# ============================================================
# 一键构建：后端 jar + 管理端 + 门户 + 客户 H5 + 技师 H5
# 产物：release/{meiye-backend.jar,admin,portal,mp-h5,worker-h5}
# 要求：JDK 17、Maven 3.8+、Node 16.20+（前端固定 Vite 4）
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RELEASE="$ROOT/release"
export JAVA_HOME="${JAVA_HOME:-/opt/jdk-17}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> 清理 release"
rm -rf "$RELEASE"
mkdir -p "$RELEASE"

echo "==> 构建后端"
cd "$ROOT/backend"
mvn -q -DskipTests clean package
JAR="$(find target -maxdepth 1 -name '*.jar' ! -name '*.original' | head -1)"
cp "$JAR" "$RELEASE/meiye-backend.jar"

build_front() {
  local name="$1" dir="$2"
  echo "==> 构建 $name"
  cd "$ROOT/$dir"
  if [[ -f package-lock.json ]]; then
    npm ci --no-audit --no-fund
  else
    npm install --no-audit --no-fund
  fi
  npm run build
  cp -r "$dir/dist" "$RELEASE/$name"
}

build_front "admin" "admin-web"
build_front "portal" "portal-web"
build_front "mp-h5" "mp-h5"
build_front "worker-h5" "worker-h5"

# 小程序与 App 源码随包交付（需微信开发者工具 / Flutter 构建）
mkdir -p "$RELEASE/source"
cp -r "$ROOT/miniprogram" "$ROOT/worker-miniprogram" "$ROOT/app-flutter" "$RELEASE/source/"

echo "==> 完成"
du -sh "$RELEASE"/*
