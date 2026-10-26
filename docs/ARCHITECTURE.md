# 系统架构

> 项目：美业大健康数字化平台（业务品类插件化）SaaS
> 版本：1.0.0 · 2026-10

## 1. 总体拓扑

```
                         ┌──────────────────────────────┐
                         │          Nginx 443           │
                         │ api / admin / www / m / worker│
                         └───────┬───────────┬──────────┘
            静态 SPA 直出              │ /api 反向代理
     ┌──────────┬──────────┬──────────┤
     │          │          │          │
 admin-web   portal-web   mp-h5   worker-h5
 (平台+租户)  (SEO 门户)  (客户H5)  (技师H5)
     │          │          │          │
     └──────────┴────┬─────┴──────────┘
                     ▼
         ┌───────────────────────────┐
         │ Spring Boot 3.2 / Java 17 │
         │ JWT + 租户上下文 + 审计     │
         │ MyBatis-Plus 租户插件      │
         └───┬───────┬───────┬───────┘
             │       │       │
        MySQL 8   Redis   RabbitMQ
        业务数据  缓存/锁  异步/延迟
             │
         MinIO/S3/OSS/COS/本地
```

客户端：微信小程序（客户）、技师小程序、Flutter App（客户），均通过 HTTPS 访问 `/api`，由 JWT 与 `tenantCode` 识别租户。

## 2. 技术栈

| 层 | 选型 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2.x、Spring Security、MyBatis-Plus 3.5.x |
| 数据库 | MySQL 8.0（utf8mb4），HikariCP maximumPoolSize=20 |
| 缓存 | Redis 7（参数缓存、限流、防重、编号、分布式锁） |
| 消息 | RabbitMQ（通知、预约超时释放延迟队列、手动 ACK、重试） |
| 对象存储 | 本地 / S3 / 阿里 OSS / 腾讯 COS / MinIO 五驱动 |
| 管理端 | Vue 3 + Vite 4 + TypeScript + Element Plus + ECharts |
| 门户/H5 | Vue 3 + Vite 4 + 原生响应式样式（无重型 UI 库） |
| 小程序 | 微信原生 WXML/WXSS/JS，自定义 tabBar |
| App | Flutter 3.27+（Dart 3.6+），Material 3 |
| 部署 | Nginx + systemd，兼容宝塔面板 |

## 3. 后端分层

```
com.meiye
├── common
│   ├── audit        # @Audit 注解 + AOP 审计（操作人/租户/IP/耗时/结果）
│   ├── context      # TenantContext / LoginUser（ThreadLocal）
│   ├── crypto       # AES TypeHandler、密钥持有、加密前缀 ENC:
│   ├── exception    # BizException + 全局异常处理（统一 R 响应）
│   ├── security     # JWT 过滤器、限流、防重、租户守卫拦截器
│   ├── util         # 编号生成、脱敏、Excel、PDF
│   └── web          # R / PageResult / BaseCrudController
├── config           # MyBatis-Plus 租户插件、Security、RabbitMQ
├── domain
│   ├── entity       # 57 张表对应实体（BaseEntity / TenantEntity 基类）
│   └── mapper       # MyBatis-Plus Mapper
├── storage          # StorageManager + Local/S3 驱动（OSS/COS/MinIO 复用 S3）
├── message          # 统一消息发送、模板、日志
└── module
    ├── auth         登录/JWT/微信登录/改密
    ├── platform     租户、品类插件、字典、参数、看板、审计、语言、菜单
    ├── tenant       租户资料、门店、工位、用户角色、字典、审计、品类开关
    ├── customer     客户档案、导入导出、关联视图
    ├── technician   技师档案、证书、排班、轮牌
    ├── service      项目、套餐、服务流程
    ├── appointment  排班、分时预约、超时释放、看板
    ├── order        下单、支付、核销、交接班、状态机
    ├── card         卡类型、会员卡、流水、充值、欠款、优惠券
    ├── settlement   提成规则、结算单、分账、提现、积分
    ├── marketing    优惠券、活动、评价、回访、售后、服务日志
    ├── inventory    耗材/产品进销存
    ├── report       租户/平台报表、预警、PDF
    ├── portal       公开门户、客户自助、技师工作端、CMS
    ├── wechat       微信配置、code2Session、模板消息、菜单、素材
    ├── message      消息配置与发送日志
    └── file         上传、下载、存储配置
```

## 4. 关键请求链路

### 4.1 登录

1. 前端提交 `username/password/tenantCode/userType`。
2. `AuthService` 按类型查询平台用户、租户用户、客户或技师；BCrypt 校验密码。
3. 校验租户状态与到期时间；停用/到期直接拒绝。
4. 生成 JWT：`userId/username/name/userType/role/tenantId/tenantCode/storeId/technicianId/customerId`。
5. 返回 `token/user/tenant/permissions`；前端持久化。

### 4.2 租户请求

```
Authorization: Bearer <jwt>
        │
JwtAuthFilter ── 解析 JWT ── TenantContext.set(user, tenantId, ignore=false)
        │
TenantGuardInterceptor ── /api/tenant/** 必须已选租户；下钻只读校验
        │
Controller ── @PreAuthorize 角色校验
        │
Service ── 业务校验（品类、库存、卡余额、状态机）
        │
Mapper ── TenantLineInnerInterceptor 自动追加 tenant_id = ?
        │
MySQL
```

### 4.3 平台下钻

- 平台登录默认 `ignoreTenant=true`，可跨租户统计；访问 `/api/tenant/**` 时必须携带 `X-Tenant-Id`。
- 携带 `X-Tenant-Id` 后：`tenantId=目标租户`、`drilldown=true`。
- `TenantGuardInterceptor` 对所有非 GET 请求返回 403；平台下钻仅只读。
- 下钻操作写审计（操作人、目标租户、路径、结果）。

## 5. 数据模型

- 共 57 张表（59 个实体类，含 `BaseEntity` / `TenantEntity` 基类），见 `backend/src/main/resources/db/schema.sql`。业务表统一继承 `TenantEntity`：`id/tenant_id/created_at/updated_at/deleted`。
- 逻辑删除：`deleted=0/1`（MyBatis-Plus 全局配置）。
- 关键表：
  - 平台：`sys_tenant`、`sys_platform_user`、`biz_type_plugin`、`platform_param`、`sys_global_dict`、`sys_language`、`sys_role`
  - 租户：`sys_tenant_user`、`sys_user_role`、`t_store`、`t_workstation`、`tenant_biz_type`、`tenant_dict`、`tenant_config`、`sys_menu_config`、`wechat_config`、`message_template`、`storage_config`
  - 业务：`t_customer`、`t_technician`、`t_technician_cert`、`t_schedule`、`t_service_item`、`t_service_package`、`t_appointment`、`t_order`、`t_order_item`、`t_payment`、`t_card_type`、`t_member_card`、`t_card_flow`、`t_coupon`、`t_settlement`、`t_commission_rule`、`t_withdrawal`、`t_review`、`t_after_sale`、`t_inventory_item`、`t_stock_flow`
  - 治理：`audit_log`、`message_log`、`sys_attachment`、`cms_article`、`t_follow_up`、`t_tech_service_log`

## 6. 异步与定时任务

| 任务 | 触发 | 说明 |
| --- | --- | --- |
| 通知消费 | `meiye.notify.queue` | 预约确认/核销/储值等消息，手动 ACK，失败重试 3 次 |
| 预约超时释放 | 延迟队列 + 每分钟扫描 | 超过开始时间未到店置 `NO_SHOW` 并释放时段 |
| 预约提醒 | 每 5 分钟 | 临近预约发送提醒（幂等，防重复） |
| 卡项到期 | 每小时 | 过期置 `EXPIRED`，生成预警与消息 |
| 证书到期 | 预警接口实时计算 + 每日任务 | 红黄灯、禁止接单（可配） |
| 备份 | cron 02:00 | MySQL + Redis + MinIO + 配置，保留 7 天 |

## 7. 安全设计

- JWT 无状态认证，24h 过期；密钥环境变量注入。
- 角色：`SYSTEM`、`OPERATOR`、`TENANT_ADMIN`、`STORE_MANAGER`、`CASHIER`、`MARKETING`、`FINANCE`、`TECHNICIAN`、`CUSTOMER`。
- 敏感字段 AES 加密（确定性，支持等值查询），展示脱敏。
- 限流（Redis Lua）、防重（短时键，重复返回 429）、编号数据库唯一回查。
- 所有管理端写操作 `@Audit`；审计可按租户检索导出。
- 对象存储 AK/SK 加密保存、页面掩码；连接测试通过后才允许保存。
- 平台下钻只读；跨租户写操作一律 403。

## 8. 前端架构

### 管理端

- `src/layout`：左侧菜单折叠、顶部面包屑、多页签、租户切换器。
- `src/router`：平台端与租户端路由守卫。
- `src/config/resources.ts`：通用 CRUD 资源配置（字段、列、校验、权限）。
- `GenericCrud.vue`：列表筛选、分页、弹窗表单、导入导出。
- 复杂页面：预约看板、收银台、品类开关、对象存储、菜单编辑器、报表、微信配置等。

### 门户

- 动态 SEO：title/description/OG/JSON-LD/canonical/hreflang。
- 租户解析：`?tenant=` > 子域名 > localStorage > `VITE_DEFAULT_TENANT`。
- 双语切换（中/EN），PC + 移动响应式。

### H5 / 小程序 / App

- 同一套客户 API；JWT 存储与 401 自动回登录。
- 小程序自定义 tabBar 从 `/api/menus?target=MINIAPP|WORKER` 动态加载。
- App 使用 MethodChannel 桥接微信/支付宝原生 SDK。

## 9. 部署单元

| 单元 | 进程 | 说明 |
| --- | --- | --- |
| meiye-backend | systemd | 单 jar，端口 8081 |
| nginx | systemd | 5 个子域名 + 静态资源 + 反代 |
| mysql | 系统服务/云 RDS | 独立备份 |
| redis | 系统服务 | 密码 + 内网 |
| rabbitmq | 系统服务 | vhost `/meiye` |
| minio | 系统服务 | 仅内网，预签名访问 |

横向扩展：后端无状态，可多实例 + Nginx upstream；WebSocket 未使用；定时任务需通过 ShedLock/单实例开关避免重复（当前单实例部署）。

## 10. 相关文档

- 配置项：`docs/CONFIGURATION.md`
- 租户隔离：`docs/TENANT-ISOLATION.md`
- 品类插件：`docs/BIZ-PLUGIN.md`
- API：`docs/API.md`
- 部署：`docs/DEPLOYMENT.md`
- 运维：`docs/OPERATIONS.md`
- 安全备份：`docs/SECURITY-BACKUP.md`
- 测试报告：`docs/TEST-REPORT.md`
