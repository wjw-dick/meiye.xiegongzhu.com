# 安全与备份（SECURITY & BACKUP）

本文覆盖安全基线、多租户隔离验证、密钥与敏感数据管理、备份恢复与灾难演练。

---

## 1. 安全基线

### 1.1 网络与传输

- 全站 HTTPS（TLS 1.2/1.3），HTTP 301 跳转；HSTS 一年；
- 仅暴露 80/443，后端 8081、MySQL 3306、Redis 6379、RabbitMQ 5672、MinIO 9000 一律不对公网开放；
- 云安全组/防火墙按最小权限放行，数据库仅允许应用服务器 IP；
- 管理端与门户、API 分域名部署，便于独立限流与证书管理。

### 1.2 认证与授权

- JWT 无状态认证，`JWT_SECRET` ≥ 32 字节随机值，有效期默认 24 小时；
- 密码使用 BCrypt 加盐存储，禁止明文/可逆加密；
- 登录接口独立限流（Nginx 5r/s + 应用层计数），连续失败锁定；
- 两级 RBAC：平台角色 + 租户角色；接口按角色与租户双重校验；
- 平台账号跨租户**只读下钻**，写操作返回 403 并记录审计；
- 客户/技师端与后台端账号体系隔离，`userType` 决定登录入口。

### 1.3 多租户隔离

核心机制：MyBatis-Plus `TenantLineInnerInterceptor` 自动为 SQL 追加 `tenant_id` 条件；
平台级表在 `MybatisPlusConfig` 白名单中显式豁免。

平台表白名单（仅这些表不带租户条件）：

```
sys_tenant, sys_platform_user, biz_type_plugin, sys_global_dict,
platform_param, sys_language, sys_role
```

验证方法（可重复执行）：

```bash
# 1) 租户 A token 访问租户 B 数据，应被自动过滤或 403
curl -H "Authorization: Bearer $TENANT_A_TOKEN" \
     -H "X-Tenant-Id: 2" \
     "https://api.example.com/api/tenant/customers/page?current=1&size=5"

# 2) 平台 token 未下钻访问租户接口，应 403
curl -H "Authorization: Bearer $PLATFORM_TOKEN" \
     "https://api.example.com/api/tenant/customers/page"

# 3) 平台下钻写操作，应 403 且审计表新增「越权拦截」
curl -X POST -H "Authorization: Bearer $PLATFORM_TOKEN" -H "X-Tenant-Id: 1" \
     -H "Content-Type: application/json" \
     -d '{"name":"x","phone":"13700000000"}' \
     "https://api.example.com/api/tenant/customers"
```

审计记录：

```sql
SELECT module, action, target, detail, operator_name, ip, created_at
FROM audit_log WHERE action = '越权拦截' ORDER BY id DESC LIMIT 20;
```

> 注意：`audit_log` 表名不带 `sys_` 前缀；平台表清单以 `MybatisPlusConfig` 代码为准。
> 任何新增平台级表都必须显式加入白名单，否则会被租户条件误过滤。

### 1.4 敏感数据

- 手机号、证件号、银行卡、微信 UnionID 等使用 AES 加密存储，密文带 `ENC:` 前缀；
- 加密为确定性加密，支持等值查询；查询时同时匹配明文与密文以兼容历史数据；
- 展示层统一脱敏（手机号 `138****0000`、证件号保留首尾）；
- 日志与审计详情对 `password/secret/token/mpAesKey` 等字段自动打码；
- `DATA_AES_KEY` 仅存于 `/etc/meiye/meiye.env`（600），不进入代码仓库与镜像。

**密钥轮换风险**：更换 `DATA_AES_KEY` 将导致历史密文无法解密。如需轮换，必须先离线解密 →
更换密钥 → 重新加密，并在维护窗口执行，禁止直接替换。

### 1.5 接口防护

| 风险 | 措施 |
| --- | --- |
| 暴力登录 | 登录限流、失败计数、账号锁定 |
| 重复提交 | `X-Request-Id` / `X-Idempotency-Key` 或参数摘要 + Redis 短时锁，重复返回 429 |
| 刷单/刷券 | 业务规则校验 + 接口限流 |
| 越权 | 租户插件 + 角色校验 + 平台下钻只读 |
| 注入 | MyBatis 参数绑定，禁止拼接 SQL |
| XSS | 前端转义 + 后端输出编码；富文本白名单 |
| 文件上传 | 类型/大小/后缀校验，对象存储独立域名 |
| 敏感信息泄露 | 统一异常返回，堆栈仅落日志；配置接口脱敏 |
| CSRF | 前后端分离 + Bearer Token，不使用 Cookie 会话 |

### 1.6 审计

- 关键操作（登录、增删改、品类开关、结算、提现、退款、配置变更）均写审计；
- 审计字段：租户、操作人、角色、模块、动作、目标、详情、IP、时间、成功/失败；
- 审计日志仅平台超管与租户管理员可查，**不提供删除接口**；
- 建议保留 ≥ 6 个月，按等保/行业要求可延长并异地归档。

---

## 2. 备份策略

### 2.1 备份内容

| 对象 | 方式 | 频率 | 保留 |
| --- | --- | --- | --- |
| MySQL | `mysqldump --single-transaction` + gzip | 每日 02:00 | 7 天（可配） |
| Redis | RDB 快照 | 每日 | 7 天 |
| MinIO/对象存储 | `mc mirror` | 每日 | 7 天 |
| 配置 | `/etc/meiye`、Nginx、systemd、deploy | 每日 | 7 天 |
| 代码 | Git 仓库 + release 包 | 每次发布 | 最近 3 个版本 |

脚本：`deploy/scripts/backup.sh`，输出目录 `$BACKUP_DIR/YYYYMMDD/`，含
`MANIFEST.txt` 与 `SHA256SUMS-*.txt` 校验文件。支持 `OFFSITE_RSYNC` 异地同步。

### 2.2 建议的 RPO / RTO

| 指标 | 目标 |
| --- | --- |
| RPO（可容忍数据丢失） | ≤ 24 小时（每日全量）；核心业务建议开启 binlog 做到 ≤ 5 分钟 |
| RTO（恢复时间） | ≤ 2 小时（单机 + 每日备份） |

生产建议：

- MySQL 开启 `binlog`，每日全量 + binlog 增量，支持按时间点恢复（PITR）；
- 备份文件加密后上传对象存储/异地机房，至少一份离线或不可变存储；
- 每季度至少一次恢复演练，记录耗时与问题。

### 2.3 恢复流程

```bash
# 0) 通知与停服
systemctl stop meiye-backend

# 1) 校验备份完整性
cd /opt/meiye/data/backup/20261026
sha256sum -c SHA256SUMS-*.txt

# 2) 恢复数据库（脚本会二次确认并自动启动后端）
FORCE=1 /opt/meiye/deploy/restore.sh mysql-20261026-020001.sql.gz

# 3) 可选恢复对象存储
RESTORE_MINIO=1 FORCE=1 /opt/meiye/deploy/restore.sh mysql-20261026-020001.sql.gz

# 4) 验证
curl -fsS http://127.0.0.1:8081/api/health
```

Redis 仅存放缓存、限流与编号序列，丢失可重建；如恢复 RDB，需停 Redis、替换 `dump.rdb` 后启动，
脚本默认不自动覆盖以避免误清空。

### 2.4 备份检查清单

- [ ] 每日备份任务成功，文件大小与昨日差异合理
- [ ] 校验和通过，`MANIFEST.txt` 完整
- [ ] 异地/离线副本存在且可读
- [ ] 恢复演练成功，RTO 达标
- [ ] 备份目录权限 700，仅运维可读
- [ ] 备份中包含 `meiye.env` 时已加密存储（含密钥，敏感）

---

## 3. 安全事件响应

1. **发现**：监控告警、审计异常、用户反馈；
2. **遏制**：隔离主机、封禁 IP、停用账号、轮换密钥；
3. **取证**：保留日志、审计、备份快照，记录时间线；
4. **恢复**：从可信备份恢复，验证完整性；
5. **复盘**：定位根因、修补漏洞、更新规则与演练；
6. **通报**：按法规与合同要求通知受影响租户与监管。

高危场景优先动作：

| 场景 | 处置 |
| --- | --- |
| 密钥泄露 | 立即轮换 JWT/AES/数据库密码，强制重新登录 |
| 数据库被拖 | 断网、封禁、取证、评估数据范围、依法通报 |
| 越权访问 | 查审计定位账号与路径，停用账号，修补白名单/权限 |
| 恶意刷接口 | Nginx 封 IP、收紧限流、加验证码 |
| 勒索软件 | 隔离、禁用共享、从离线备份恢复，不支付赎金 |

---

## 4. 合规与数据治理

- 收集个人信息遵循最小必要原则，隐私政策与授权链路在小程序/H5/App 内可见；
- 提供账号注销与数据导出能力（客户自助/后台工单）；
- 敏感数据访问留痕，内部人员按最小权限授权；
- 租户退租后数据保留期与销毁方式在合同中约定；
- 日志、备份、审计的跨境传输需符合当地法规。
