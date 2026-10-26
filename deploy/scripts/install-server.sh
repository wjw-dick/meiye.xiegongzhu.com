#!/usr/bin/env bash
# ============================================================
# 裸机 / 云主机初始化（CentOS 7+/Rocky/Ubuntu 22.04）
# 仅首次执行；幂等，可重复运行。不会自动申请证书。
# ============================================================
set -euo pipefail

if [[ "$(id -u)" != "0" ]]; then
  echo "请使用 root 执行" >&2
  exit 1
fi

APP_HOME="${APP_HOME:-/opt/meiye}"
APP_USER="${APP_USER:-meiye}"

echo "[1/8] 创建用户与目录"
id "$APP_USER" >/dev/null 2>&1 || useradd -r -m -d "$APP_HOME" -s /sbin/nologin "$APP_USER"
mkdir -p "$APP_HOME"/{app,logs,data/{uploads,backup,logs},deploy}
mkdir -p /var/www/meiye/{admin,portal,mp-h5,worker-h5,verify}
mkdir -p /etc/meiye
chown -R "$APP_USER:$APP_USER" "$APP_HOME"
chmod 750 /etc/meiye

echo "[2/8] 安装基础软件"
if command -v dnf >/dev/null 2>&1; then
  dnf install -y java-17-openjdk-headless nginx mysql redis rabbitmq-server tar gzip curl rsync logrotate policycoreutils-python-utils || true
elif command -v yum >/dev/null 2>&1; then
  yum install -y java-17-openjdk-headless nginx mysql redis rabbitmq-server tar gzip curl rsync logrotate || true
elif command -v apt-get >/dev/null 2>&1; then
  apt-get update
  apt-get install -y openjdk-17-jre-headless nginx mysql-client redis-tools rabbitmq-server tar gzip curl rsync logrotate
fi

echo "[3/8] 校验 Java 17"
JAVA_BIN="$(command -v java || true)"
if [[ -n "$JAVA_BIN" ]]; then
  "$JAVA_BIN" -version 2>&1 | head -1
else
  echo "WARN: 未找到 java，请安装 JDK 17 并调整 systemd 中的 JAVA_HOME"
fi

echo "[4/8] 安装 systemd 服务"
install -m 644 deploy/systemd/meiye-backend.service /etc/systemd/system/meiye-backend.service
systemctl daemon-reload

echo "[5/8] 安装 Nginx 配置"
install -d /etc/nginx/snippets
install -m 644 deploy/nginx/snippets/meiye-proxy.conf /etc/nginx/snippets/meiye-proxy.conf
install -m 644 deploy/nginx/meiye.conf /etc/nginx/conf.d/meiye.conf
nginx -t

echo "[6/8] 安装日志轮转与备份脚本"
install -m 644 deploy/logrotate/meiye /etc/logrotate.d/meiye
install -m 750 deploy/scripts/backup.sh "$APP_HOME/deploy/backup.sh"
install -m 750 deploy/scripts/restore.sh "$APP_HOME/deploy/restore.sh"
install -m 750 deploy/scripts/healthcheck.sh "$APP_HOME/deploy/healthcheck.sh"

echo "[7/8] 环境变量"
if [[ ! -f /etc/meiye/meiye.env ]]; then
  install -m 600 deploy/env/meiye.env.example /etc/meiye/meiye.env
  echo "已生成 /etc/meiye/meiye.env，请立即替换 CHANGE_ME 后重启后端"
fi
chown root:root /etc/meiye/meiye.env

echo "[8/8] 完成"
cat <<'EOF'
下一步：
  1. 编辑 /etc/meiye/meiye.env（数据库、Redis、RabbitMQ、MinIO、密钥）
  2. 导入数据库：mysql -h... -u... -p meiye < backend/src/main/resources/db/schema.sql
     再导入 data.sql（首次）
  3. 构建并上传：deploy/scripts/build-all.sh && deploy/scripts/deploy-local.sh
  4. 申请证书：certbot --nginx -d api.example.com -d admin.example.com ...
  5. systemctl enable --now meiye-backend nginx
  6. 配置 crontab：0 2 * * * /opt/meiye/deploy/backup.sh
EOF
