# 配置说明

> 后端配置来源优先级：环境变量 > `application.yml` 默认值。
> 生产环境统一使用 `/etc/meiye/meiye.env`（systemd `EnvironmentFile`），权限 600。

## 1. 后端环境变量

| 变量 | 默认值 | 必填 | 说明 |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | 是 | 生产使用 `prod` |
| `SERVER_PORT` | `8080` | 是 | 生产建议 8081（避开其他项目） |
| `DB_HOST` / `MYSQL_PORT` | `127.0.0.1` / `3306` | 是 | MySQL 地址 |
| `DB_NAME` | `meiye` | 是 | 数据库名，utf8mb4 |
| `DB_USER` / `DB_PASSWORD` | `root` / `root` | 是 | 建议独立账号，最小权限 |
| `REDIS_HOST` / `REDIS_PORT` | `127.0.0.1` / `6379` | 是 | Redis |
| `REDIS_PASSWORD` | 空 | 生产必填 | 禁止无密码暴露公网 |
| `RABBIT_HOST` / `RABBIT_PORT` | `127.0.0.1` / `5672` | 是 | RabbitMQ |
| `RABBIT_USER` / `RABBIT_PASSWORD` | `guest` / `guest` | 是 | 生产独立账号 |
| `RABBIT_VHOST` | `/` | 否 | 建议 `/meiye` |
| `MINIO_HOST` / `MINIO_PORT` | `127.0.0.1` / `9000` | 是 | 默认对象存储 |
| `MINIO_USER` / `MINIO_PASSWORD` | `admin` / 空 | 是 | 加密保存于租户存储配置 |
| `MINIO_BUCKET` | `meiye` | 否 | 桶名 |
| `JWT_SECRET` | 开发默认 | 生产必填 | ≥32 字节随机串 |
| `JWT_EXPIRE_HOURS` | `24` | 否 | Token 有效期 |
| `DATA_AES_KEY` | 开发默认 | 生产必填 | 16/24/32 字节；变更将导致旧密文不可解 |
| `WECHAT_MOCK` | `true` | 否 | 无正式资质时保持 true |
| `STORAGE_LOCAL_DIR` | `./data/uploads` | 否 | LOCAL 驱动目录 |
| `STORAGE_PUBLIC_BASE` | `/api/files` | 否 | LOCAL 驱动访问前缀 |
| `LOG_FILE` | `./data/logs/meiye.log` | 否 | 日志文件 |
| `BACKUP_DIR` | `./data/backup` | 否 | 备份目录 |

### 生成安全密钥

```bash
openssl rand -base64 48     # JWT_SECRET
openssl rand -base64 24     # DATA_AES_KEY（24 字节）
```

> `DATA_AES_KEY` 上线后不可随意变更；如必须轮换，需编写迁移脚本解密旧值后用新密钥重写。

## 2. 平台参数（`platform_param`）

平台超管在「平台设置 → 平台参数」维护，租户侧实时生效（Redis 缓存 60s 或保存后主动刷新）。

| 参数键 | 示例 | 说明 |
| --- | --- | --- |
| `tenant_expire_warn_days` | `30` | 租户到期预警天数 |
| `appointment_release_minutes` | `30` | 预约开始后多少分钟未到店释放 |
| `default_commission_rate` | `5` | 默认抽成/提成比例（%） |
| `withdraw_fee_rate` | `0.6` | 提现手续费（%） |
| `card_expire_policy` | `WARN` | 储值卡过期策略 |
| `tenant_expire_policy` | `DISABLE` | 到期策略：停用/只读 |
| `points_per_yuan` | `1` | 每消费 1 元获得积分 |

## 3. 租户配置

「租户后台 → 系统设置」保存到 `sys_tenant` 与 `tenant_config`：

| 配置 | 说明 |
| --- | --- |
| 名称 / Logo / 客服电话 / 简介 | 门户、H5、小程序展示 |
| 门店 | 多门店，支持跨店消费/分账开关 |
| 公众号 / 小程序 / App | AppID、AppSecret（AES 加密，页面掩码） |
| 微信支付 | 商户号、API 密钥、证书（加密） |
| 对象存储 | 5 驱动卡片 → 表单 → 连接测试 → 保存 |
| 通知开关 | 站内信、公众号模板、短信、App Push |
| 结算抽成 | 默认比例，可按项目/技师覆盖 |

### 对象存储配置

支持：`LOCAL`、`MINIO`、`S3`、`OSS`、`COS`。

| 驱动 | Endpoint 示例 | 说明 |
| --- | --- | --- |
| LOCAL | - | 存本机 `STORAGE_LOCAL_DIR`，经 `/api/files` 访问 |
| MINIO | `http://127.0.0.1:9000` | 内网桶，预签名或公共读 |
| S3 | `https://s3.ap-east-1.amazonaws.com` | AWS S3 |
| OSS | `https://oss-cn-shenzhen.aliyuncs.com` | 阿里云（S3 兼容模式） |
| COS | `https://cos.ap-guangzhou.myqcloud.com` | 腾讯云（S3 兼容模式） |

规则：

1. 先选驱动卡片，表单才展开；
2. AK/SK 密码框 + 眼睛切换；
3. 「连接测试」通过前「保存」按钮灰置；
4. AK/SK 使用 `ENC:` 前缀 AES 加密入库；
5. 切换驱动后历史文件仍可读（附件表记录 `storage_driver` 与 URL），新文件写新驱动。

## 4. 微信配置（`wechat_config`）

| 字段 | 说明 |
| --- | --- |
| `mini_app_id` / `mini_secret` | 客户小程序 |
| `mp_app_id` / `mp_secret` | 服务号 |
| `mp_token` / `mp_aes_key` / `mp_url` | 公众号服务器配置 |
| `union_app_id` | 开放平台 UnionID 账号 |
| `app_app_id` / `app_secret` | 移动应用（Flutter App） |
| `subscribe_ids` | 订阅消息模板 ID JSON |
| `push_channel` | 推送通道 JSON |

详见 `docs/WECHAT.md`。

## 5. 菜单与多语言

| 配置 | 表 | 前端 |
| --- | --- | --- |
| 后台导航 | `sys_menu_config(target=ADMIN)` | 管理端左侧菜单 |
| 小程序 tabBar | `sys_menu_config(target=MINIAPP)` | 客户端小程序 |
| 技师小程序 tabBar | `sys_menu_config(target=WORKER)` | 技师端小程序 |
| H5 底部导航 | `sys_menu_config(target=H5)` | 客户 H5 |
| 语言 | `sys_language` | 顶栏语言切换器 |
| 内容多语言 | `*_en` 字段 / `ext_json` | 门户/CMS 双语 |

修改后无需发版：小程序在 `onShow` 拉取 `/api/menus?target=...`，H5/管理端登录后拉取。

## 6. 前端构建变量

| 项目 | 变量 | 默认 | 说明 |
| --- | --- | --- | --- |
| admin-web | `VITE_API_BASE` | `/api` | 后端前缀 |
| portal-web | `VITE_DEFAULT_TENANT` | `5MY` | 默认租户 |
| portal-web | `VITE_MP_H5_URL` | `http://127.0.0.1:5175` | 预约跳转 |
| mp-h5 | 相对 `/api` | - | 由 Nginx 同域反代 |
| worker-h5 | 相对 `/api` | - | 同上 |
| app-flutter | `API_BASE_URL` | `http://10.0.2.2:8081` | `--dart-define` |
| app-flutter | `TENANT_CODE` | `5MY` | `--dart-define` |
| 小程序 | `utils/config.js` / `app.js` | `127.0.0.1:8081` | 上线改 HTTPS 域名 |

生产构建示例：

```bash
VITE_DEFAULT_TENANT=5MY VITE_MP_H5_URL=https://m.example.com npm run build
```

## 7. 日志与审计

- 应用日志：`/opt/meiye/logs/meiye.log`（logback 按天滚动，保留 30 天）。
- stdout/stderr：systemd 重定向到 `/opt/meiye/logs/stdout.log` / `stderr.log`。
- Nginx：`/var/log/nginx/meiye-*.log`。
- 审计日志：`audit_log` 表，管理端可检索、导出；保留建议 ≥ 1 年。
- 日志中禁止打印密码、Token、完整手机号/身份证；排障时使用脱敏值。

## 8. 配置变更流程

1. 在测试环境修改并验证；
2. 生产修改 `/etc/meiye/meiye.env`（先备份）；
3. `systemctl restart meiye-backend`；
4. `curl /api/health` 与关键接口回归；
5. 业务配置（平台参数、租户配置、品类开关）通过后台修改，自动写审计，无需重启。
