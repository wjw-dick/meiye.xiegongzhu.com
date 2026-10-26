# 部署手册（DEPLOYMENT）

本文面向运维与实施人员，描述从空白服务器到全端上线的完整流程。
项目结构、模块说明见 [ARCHITECTURE.md](./ARCHITECTURE.md)，环境变量逐项说明见
[CONFIGURATION.md](./CONFIGURATION.md)。

---

## 1. 部署拓扑

| 组件 | 说明 | 默认端口 |
| --- | --- | --- |
| `meiye-backend` | Spring Boot 3.2 单体后端（API + 定时任务 + 消息消费） | 8081 |
| MySQL 8 | 业务库 `meiye`，行级多租户 | 3306 |
| Redis | 缓存、限流、编号序列、登录态 | 6379 |
| RabbitMQ | 消息通知、异步任务 | 5672 |
| MinIO（可选） | 对象存储，5 种驱动可切换 | 9000 |
| Nginx | TLS 终止、静态站点、反向代理、限流 | 80/443 |
| 静态站点 | `admin` / `portal` / `mp-h5` / `worker-h5` | — |
| 小程序 | `miniprogram`（客户）、`worker-miniprogram`（技师） | 微信平台 |
| App | `app-flutter`（Flutter） | 应用市场 |

推荐单机 4C8G 起步；数据库与对象存储建议独立或使用云服务。

域名规划（示例，实际以备案域名为准）：

| 域名 | 用途 |
| --- | --- |
| `api.example.com` | 后端 API |
| `admin.example.com` | 平台 + 租户管理端 |
| `www.example.com` | 品牌门户（SEO / 分享落地页） |
| `m.example.com` | 客户 H5（公众号菜单入口） |
| `worker.example.com` | 技师工作端 H5 |

> 微信要求：小程序服务器域名、公众号网页授权域名、JS 安全域名必须为 **已备案的 HTTPS 域名**，
> 且需在对应平台后台配置。未取得资质时保持 `WECHAT_MOCK=true`。
> 微信登录依赖域名与资质，**本地 IP + HTTP 无法完成真机联调**。

---

## 2. 前置条件

- Linux（CentOS 7+ / Rocky 8+ / Ubuntu 22.04+），root 或 sudo
- JDK 17（本项目使用 `/opt/jdk-17` 验证）
- Maven 3.8+
- Node.js 16.20+（前端固定 Vite 4，**不要升级到 Vite 5**）
- MySQL 8.0+、Redis 6+、RabbitMQ 3.10+
- Nginx 1.20+
- 可选：Flutter 3.27+ / Dart 3.6+（构建 App）、微信开发者工具（小程序）

---

## 3. 首次部署

### 3.1 初始化系统

```bash
git clone <repo> /opt/src/meiye && cd /opt/src/meiye
sudo bash deploy/scripts/install-server.sh
```

脚本会创建 `meiye` 用户、目录、systemd 服务、Nginx 配置、日志轮转与备份脚本，
并生成 `/etc/meiye/meiye.env`（权限 600）。脚本幂等，可重复执行。

### 3.2 配置环境变量

```bash
sudo vi /etc/meiye/meiye.env
```

必须替换全部 `CHANGE_ME`：

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 24   # DATA_AES_KEY（16/24/32 字节，上线后不可变更）
```

注意：

- `DATA_AES_KEY` 用于敏感字段（手机号、证件号、银行卡等）AES 加密。**上线后更换会导致历史数据无法解密**，
  如确需轮换，先执行数据迁移脚本。
- `SPRING_PROFILES_ACTIVE=prod` 时不会加载演示数据逻辑，务必先导入初始化 SQL。
- 生产禁止使用演示账号密码，首次登录后立即修改平台与租户管理员密码。

### 3.3 初始化数据库

```bash
mysql -h <host> -u root -p -e "CREATE DATABASE meiye DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -h <host> -u root -p meiye < backend/src/main/resources/db/schema.sql
mysql -h <host> -u root -p meiye < backend/src/main/resources/db/data.sql   # 仅首次
```

也可使用脚本（读取环境变量）：

```bash
DB_HOST=... DB_USER=... DB_PASSWORD=... bash deploy/scripts/init-db.sh
```

初始化后核对：

```sql
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='meiye';  -- 57
SELECT code,name FROM sys_tenant;                                           -- 5MY / 2HL
```

> `data.sql` 含演示租户、账号与业务数据，生产环境可按需裁剪，但**平台账号与品类插件字典不可删除**。

### 3.4 构建

```bash
export JAVA_HOME=/opt/jdk-17
bash deploy/scripts/build-all.sh
```

产物输出到 `release/`：

```
release/meiye-backend.jar
release/admin/  release/portal/  release/mp-h5/  release/worker-h5/
release/source/（小程序与 Flutter 源码）
```

如仅前端改动，可单独构建：

```bash
cd admin-web && npm ci && npm run build
```

### 3.5 部署

```bash
bash deploy/scripts/deploy-local.sh --skip-build
```

脚本执行：备份旧 jar → 原子替换静态站点 → 替换 jar → `nginx -t` → 重载 Nginx →
重启后端 → 轮询健康检查；失败自动回滚到上一个 jar。

### 3.6 配置 HTTPS

```bash
sudo certbot --nginx -d api.example.com -d admin.example.com -d www.example.com \
  -d m.example.com -d worker.example.com
```

或使用宝塔面板申请证书，参见 `deploy/baota/README.md`。
证书续期后执行 `systemctl reload nginx`。

### 3.7 启动与自检

```bash
sudo systemctl enable --now meiye-backend nginx
curl -fsS https://api.example.com/api/health
```

期望返回：

```json
{"code":200,"msg":"success","data":{"status":"UP","database":"UP"}}
```

### 3.8 配置备份定时任务

```bash
sudo crontab -e
# 每天 02:00 全量备份，保留 BACKUP_KEEP_DAYS 天
0 2 * * * /opt/meiye/deploy/backup.sh >> /opt/meiye/logs/backup.log 2>&1
# 每 5 分钟健康检查（失败告警）
*/5 * * * * /opt/meiye/deploy/healthcheck.sh >> /opt/meiye/logs/health.log 2>&1
```

---

## 4. 小程序与 App 发布

### 4.1 小程序

1. 修改 `miniprogram/project.config.json`、`worker-miniprogram/project.config.json` 的 `appid`；
2. 修改 `miniprogram/app.js`、`worker-miniprogram/app.js` 中 `baseUrl` 为 `https://api.example.com`；
3. 微信公众平台 → 开发管理 → 服务器域名，加入 `https://api.example.com`；
4. 微信开发者工具上传 → 提交审核 → 发布。

客户小程序与技师小程序是两个独立 AppID；若复用同一主体，请在后台分别配置小程序
`miniAppId/miniSecret` 与 App `appAppId/appSecret`。

### 4.2 App（Flutter）

```bash
cd app-flutter
flutter pub get
flutter build apk --release --dart-define=API_BASE=https://api.example.com
flutter build ios --release --dart-define=API_BASE=https://api.example.com
```

当前交付环境未安装 Flutter/Android SDK，**App 未在本机构建**，源码已按 Flutter 3.27+/Dart 3.6+
编写，需在具备 SDK 的机器上完成构建、签名与上架。微信开放平台需绑定移动应用并配置
包名/签名，才能使用微信登录与支付。

---

## 5. 升级与回滚

### 5.1 升级

```bash
cd /opt/src/meiye && git pull
export JAVA_HOME=/opt/jdk-17
bash deploy/scripts/build-all.sh
bash deploy/scripts/deploy-local.sh --skip-build
```

涉及表结构变更时，先备份数据库，再执行增量 SQL；本项目未引入 Flyway，
变更脚本需人工评审并在预发验证。

### 5.2 回滚

```bash
ls -1t /opt/meiye/releases/meiye-backend-*.jar | head
cp /opt/meiye/releases/meiye-backend-20260101-120000.jar /opt/meiye/app/meiye-backend.jar
systemctl restart meiye-backend
# 静态站点
mv /var/www/meiye/admin.old /var/www/meiye/admin
systemctl reload nginx
```

保留最近 3 个后端版本（部署脚本自动清理）。

---

## 6. 常见问题

| 现象 | 排查 |
| --- | --- |
| 健康检查 `database: DOWN` | 检查 `DB_HOST/DB_PASSWORD`、MySQL 白名单、库是否导入 |
| 启动报 `Unknown database` | 未执行 `schema.sql` |
| 登录 401 | JWT 密钥变更、时钟不同步、账号被停用 |
| 登录后接口 403「请先选择下钻租户」 | 平台账号直接访问 `/api/tenant/**`，需带 `X-Tenant-Id` |
| 下钻后写操作 403 | 平台下钻为只读设计，写操作请用租户账号 |
| 页面刷新 404 | Nginx 缺少 `try_files $uri $uri/ /index.html` |
| 小程序请求失败 | 未配置合法域名 / 未备案 / 证书链不完整 |
| 微信回调签名失败 | 公众号后台 Token 与 `WECHAT` 配置不一致 |
| Redis 连接超时 | `REDIS_PASSWORD` 错误或未开启远程访问 |
| 上传失败 | MinIO bucket 不存在或 `STORAGE_DRIVER` 配置不匹配 |

日志位置：

```
/opt/meiye/logs/backend.log        # systemd 重定向
/opt/meiye/data/logs/meiye.log     # 应用日志
/var/log/nginx/meiye-*.log         # 网关日志
journalctl -u meiye-backend -f     # 实时
```

---

## 7. 上线检查清单

- [ ] 全部 `CHANGE_ME` 已替换，`meiye.env` 权限 600
- [ ] 数据库已导入，57 张表齐全
- [ ] `WECHAT_MOCK=false` 且微信配置已验证（有资质时）
- [ ] HTTPS 证书有效，HTTP 自动跳转
- [ ] 五套前端静态资源可访问，SPA 刷新不 404
- [ ] 小程序/APP 域名、包名、签名已在微信开放平台配置
- [ ] 备份定时任务与恢复演练完成
- [ ] 平台/租户管理员默认密码已修改
- [ ] 健康检查接入监控与告警
- [ ] 审计日志可查询，越权拦截有记录
