# 宝塔面板部署指南

适用于已在服务器安装宝塔 Linux 面板（8.x）并希望用面板管理站点/数据库/证书的场景。后端仍以 systemd 运行，面板只负责 Nginx、MySQL、Redis、证书与定时任务。

## 1. 软件商店安装

- Nginx 1.22+
- MySQL 8.0（字符集 utf8mb4）
- Redis 7.x
- RabbitMQ（如商店无，使用 `yum install rabbitmq-server` 或 Docker）
- Java 项目一键部署（可选）或 JDK 17
- MinIO（可选，或使用外部对象存储）

## 2. 创建站点

在「网站 → 添加站点」创建 5 个站点（均不创建数据库、不创建 FTP）：

| 域名 | 根目录 | 用途 |
| --- | --- | --- |
| api.example.com | /var/www/meiye/api（空） | 仅反向代理 |
| admin.example.com | /var/www/meiye/admin | 平台管理端 |
| www.example.com | /var/www/meiye/portal | 品牌门户 |
| m.example.com | /var/www/meiye/mp-h5 | 客户 H5 |
| worker.example.com | /var/www/meiye/worker-h5 | 技师 H5 |

把 `release/admin`、`release/portal`、`release/mp-h5`、`release/worker-h5` 分别上传解压到对应根目录。

## 3. 反向代理

对每个站点「设置 → 反向代理 → 添加反向代理」：

- 代理名称：`meiye-api`
- 目标 URL：`http://127.0.0.1:8081`
- 发送域名：`$host`
- 代理目录：`/api`
- 缓存：关闭

或直接把本仓库 `deploy/nginx/meiye.conf` 中对应 server 块贴入站点的「配置文件」，并 include `deploy/nginx/snippets/meiye-proxy.conf`（放到 `/etc/nginx/snippets/`）。

> 注意：宝塔默认会写入自己的 `location ~ .*\.(gif|jpg...)` 规则，SPA 的 `try_files $uri $uri/ /index.html` 必须位于 `location /`，且不能被伪静态规则覆盖。管理端/门户/H5 均使用 hash 路由或前端路由，建议保留 `try_files`。

## 4. SSL

「网站 → SSL → Let's Encrypt」为 5 个域名分别申请证书，开启「强制 HTTPS」。申请前确保域名已解析到服务器且 80 端口可访问。

## 5. 数据库

- 在「数据库」中添加 MySQL 数据库 `meiye`，字符集 `utf8mb4`。
- 导入 `backend/src/main/resources/db/schema.sql`，再导入 `data.sql`（首次）。
- 建议单独创建备份用户，仅授予 `SELECT, LOCK TABLES, SHOW VIEW, EVENT, TRIGGER` 权限。

## 6. 后端服务

```bash
useradd -r -m -d /opt/meiye -s /sbin/nologin meiye
mkdir -p /opt/meiye/{app,logs,data/{uploads,backup,logs}}
cp release/meiye-backend.jar /opt/meiye/app/
cp deploy/systemd/meiye-backend.service /etc/systemd/system/
cp deploy/env/meiye.env.example /etc/meiye/meiye.env
chmod 600 /etc/meiye/meiye.env
# 编辑 /etc/meiye/meiye.env 后：
systemctl daemon-reload
systemctl enable --now meiye-backend
curl http://127.0.0.1:8081/api/health
```

## 7. 定时任务

「计划任务」添加 Shell 脚本，每天 02:00：

```bash
/opt/meiye/deploy/backup.sh
```

保留 7 份，脚本内置清理与 SHA256 校验。建议再添加一条每日 07:00 的健康巡检，失败时通过宝塔「通知」或企业微信机器人告警。

## 8. 防火墙与安全

- 放行 80/443；8081、3306、6379、5672、9000 仅内网/本机。
- 面板「安全」开启 SSH 密钥登录、修改默认端口、安装 Fail2ban。
- 在「网站 → 日志」定期检查 4xx/5xx；在「计划任务」中清理 Nginx 日志。
- 上传目录不要放在 Web 根目录，统一走 MinIO 或后端 `/api/files`。

## 9. 升级与回滚

```bash
# 上传新 release 后
/opt/meiye/deploy/deploy-local.sh --skip-build
# 若健康检查失败，脚本会自动回滚 jar；静态资源保留 .old 目录可手工恢复
```

## 10. 与文档对应

- 详细架构：`docs/ARCHITECTURE.md`
- 配置项：`docs/CONFIGURATION.md`
- 上线步骤：`docs/DEPLOYMENT.md`
- 备份与安全：`docs/SECURITY-BACKUP.md`
