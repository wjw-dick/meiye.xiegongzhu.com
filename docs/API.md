# API 摘要

> 基础地址：`https://api.example.com`（本地 `http://127.0.0.1:8081`）
> 所有接口返回 JSON；除特别说明外，HTTP 状态通常为 200，业务结果看 `code`。

## 1. 统一响应

```json
{ "code": 200, "msg": "success", "data": {} }
```

| code | 含义 | 前端处理 |
| --- | --- | --- |
| 200 | 成功 | 取 `data` |
| 400 | 参数/业务校验失败 | Toast `msg` |
| 401 | 未登录/Token 过期 | 清除登录态，跳登录 |
| 403 | 无权限/越权/品类未开通/下钻只读 | Toast + 返回 |
| 404 | 数据不存在 | 提示 |
| 500 | 系统异常 | 提示稍后重试，记录 traceId |

分页：

```json
{ "records": [], "total": 100, "page": 1, "size": 10 }
```

分页参数：`page`（默认 1）、`size`（默认 10）、`sort=createdAt,desc`。
通用列表还支持：

- `kw` 关键字（部分接口）
- `like_字段=值` 模糊
- `ge_字段` / `le_字段` 范围
- 其他字段名精确匹配

## 2. 认证

### 2.1 登录

`POST /api/auth/login`

```json
// 平台超管
{ "username": "admin", "password": "admin123" }

// 租户用户
{ "username": "admin", "password": "admin123", "tenantCode": "5MY" }

// 客户
{ "username": "13700000001", "password": "123456", "tenantCode": "5MY", "userType": "CUSTOMER" }

// 技师
{ "username": "13900000001", "password": "123456", "tenantCode": "5MY", "userType": "TECHNICIAN" }
```

响应：

```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOi...",
    "user": {
      "userId": 1, "username": "admin", "name": "系统管理员",
      "userType": "TENANT", "role": "TENANT_ADMIN",
      "tenantId": 1, "tenantCode": "5MY", "storeId": null,
      "technicianId": null, "customerId": null
    },
    "tenant": { "id": 1, "code": "5MY", "name": "五月美业连锁" },
    "permissions": ["tenant:*"]
  }
}
```

### 2.2 微信登录

`POST /api/auth/wechat/{channel}`，`channel` 取 `MINI` / `MP` / `APP`。

```json
{ "tenantCode": "5MY", "code": "wx_code", "nickname": "微信用户" }
```

### 2.3 当前用户 / 改密 / 退出

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/auth/me` | 当前登录信息 |
| POST | `/api/auth/password` | `{oldPassword,newPassword}` |
| POST | `/api/auth/logout` | 前端清除 Token（JWT 无状态） |

### 2.4 请求头

| 头 | 说明 |
| --- | --- |
| `Authorization: Bearer <token>` | 必填（公开接口除外） |
| `X-Tenant-Id: 1` | 仅平台超管下钻时携带，只读 |
| `Content-Type: application/json` | JSON 请求 |
| `Accept-Language: zh-CN/en` | 多语言内容（可选） |

## 3. 公开接口（无需登录）

前缀 `/api/public`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/health` | 健康检查（DB/Redis） |
| GET | `/api/public/portal/{tenantCode}/home` | 门户首页（品牌/门店/项目/资讯） |
| GET | `/api/public/portal/{tenantCode}/items` | 项目列表（`bizType` 可选） |
| GET | `/api/public/portal/{tenantCode}/items/{id}` | 项目详情 |
| GET | `/api/public/portal/{tenantCode}/packages` | 套餐列表 |
| GET | `/api/public/portal/{tenantCode}/packages/{id}` | 套餐详情 |
| GET | `/api/public/portal/{tenantCode}/stores` | 门店列表 |
| GET | `/api/public/portal/{tenantCode}/stores/{id}` | 门店详情 |
| GET | `/api/public/portal/{tenantCode}/articles` | 资讯/公告 |
| GET | `/api/public/portal/{tenantCode}/articles/{id}` | 资讯详情 |
| GET | `/api/public/portal/{tenantCode}/technicians` | 技师列表 |
| GET | `/api/public/portal/{tenantCode}/slots` | 可约时段（`storeId,date,itemId,techId`） |
| GET | `/api/public/portal/{tenantCode}/share/order?orderNo=` | 分享订单查询（脱敏） |
| POST | `/api/public/wechat/{tenantCode}/callback` | 微信事件回调 |
| POST | `/api/auth/login` 等 | 登录接口 |
| GET | `/api/files/**` | 本地存储文件 |

## 4. 平台端（`SYSTEM` / `OPERATOR`）

| 模块 | 方法与路径 |
| --- | --- |
| 租户 | `GET /api/platform/tenants/options`、`POST /api/platform/tenants/create`、`GET /api/platform/tenants/page`、`POST /api/platform/tenants/{id}/status`、`POST /api/platform/tenants/{id}/renew`、`GET /api/platform/tenants/{id}/overview`、`GET /api/platform/tenants/{id}/biz-types` |
| 品类插件 | `GET /api/platform/biz-plugins/page`、`POST/PUT /api/platform/biz-plugins`、`POST /api/platform/biz-plugins/{id}/toggle`、`POST /api/platform/biz-plugins/{id}/default` |
| 全局字典 | `GET /api/platform/dicts/page`、`GET /api/platform/dicts/type/{type}`、`POST/PUT/DELETE` |
| 平台参数 | `GET /api/platform/params/page`、`PUT /api/platform/params/batch`、`GET /api/platform/params/value/{key}` |
| 平台看板 | `GET /api/platform/dashboard`、`GET /api/platform/dashboard/trend` |
| 平台报表 | `GET /api/platform/reports/overview`、`/compare`、`/export` |
| 审计 | `GET /api/platform/audit/page`、`/export` |
| 语言 | `GET/POST/PUT /api/platform/languages` |
| 菜单 | `GET /api/menus?target=ADMIN`、`GET /api/menus/list`、`PUT /api/menus` |
| 存储 | `GET /api/platform/storage`、`POST /api/platform/storage/test`、`POST /api/platform/storage` |

平台下钻：对 `/api/tenant/**` 加 `X-Tenant-Id`，仅 GET。

## 5. 租户端

### 5.1 基础设置

| 模块 | 路径 |
| --- | --- |
| 租户资料/看板 | `GET/PUT /api/tenant/profile`、`GET /api/tenant/dashboard` |
| 品类开关 | `GET /api/tenant/biz-types`、`POST /api/tenant/biz-types/{bizType}/toggle?enabled=0|1`、`PUT /api/tenant/biz-types/{bizType}` |
| 门店 | `/api/tenant/stores`（CRUD + `/page`） |
| 工位 | `/api/tenant/workstations` |
| 用户与角色 | `/api/tenant/users`、`GET /api/tenant/users/roles` |
| 租户字典 | `GET /api/tenant/dicts/type/{type}`、`POST /api/tenant/dicts` |
| 租户审计 | `/api/tenant/audit/page`、`/export` |
| 菜单 | `GET /api/menus?target=ADMIN|MINIAPP|H5|WORKER` |
| 微信配置 | `/api/tenant/wechat/config`、`/test`、`/auth-url`、`/menu/sync`、`/signature` |
| 消息配置 | `/api/tenant/message/templates`、`/test`、`/logs/page`、`/logs/{id}/retry`、`/codes` |
| 存储 | `/api/tenant/storage`、`/test` |
| 微信内容 | `/api/tenant/wechat/fans/page`、`/tags`、`/materials`、`/mass-send` |

### 5.2 客户 / 技师

| 模块 | 路径 |
| --- | --- |
| 客户 | `/api/tenant/customers/page`、`POST/PUT`、`/{id}/disable`、`/{id}/related`、`/{id}/points`、`/export`、`/template`、`/import` |
| 技师 | `/api/tenant/technicians/page`、`POST/PUT`、`/{id}/disable`、`/{id}/certs`、`/{id}/schedules`、`/rotation`、`/{id}/stats`、`/{id}/logs`、`/export`、`/import` |

### 5.3 项目 / 套餐

`/api/tenant/service-items`、`/api/tenant/service-packages`、`/api/tenant/service-flows`（CRUD，项目支持 `/{id}/status`）。

### 5.4 预约 / 收银

| 动作 | 路径 |
| --- | --- |
| 预约列表/看板 | `GET /api/tenant/appointments/page`、`/board` |
| 可约时段/技师 | `GET /api/tenant/appointments/slots`、`/available-techs` |
| 创建/改约 | `POST /api/tenant/appointments/create`、`/{id}/reschedule` |
| 状态流转 | `/{id}/confirm|arrive|start|done|no-show|cancel` |
| 提醒/轮牌 | `POST /{id}/remind`、`/rotation/next` |
| 订单 | `GET /api/tenant/orders/page`、`/{id}`、`POST /create`、`/{id}/pay`、`/{id}/verify`、`/verify`、`/{id}/complete`、`/{id}/cancel` |
| 交接班 | `GET /shifts/current`、`POST /shifts/open`、`/shifts/{id}/close`、`GET /shifts/page` |
| 订单导出 | `GET /api/tenant/orders/export` |

下单示例：

```json
POST /api/tenant/orders/create
{
  "customerId": 1,
  "storeId": 1,
  "technicianId": 1,
  "bizType": "BEAUTY",
  "items": [{ "itemId": 1, "quantity": 1 }],
  "remark": "会员到店"
}
```

支付示例（混合支付）：

```json
POST /api/tenant/orders/1/pay
{
  "payType": "WECHAT",
  "couponId": 1,
  "pointsUse": 100,
  "payments": [
    { "channel": "WECHAT", "amount": 100 },
    { "channel": "BALANCE", "amount": 50, "cardId": 1 }
  ]
}
```

> 不传 `payments` 时按 `payType` 自动生成一条应收金额支付行；客户端支付不传金额，由后端计算。

### 5.5 会员卡 / 营销 / 结算

| 模块 | 路径 |
| --- | --- |
| 卡类型 | `/api/tenant/card-types` |
| 会员卡 | `/api/tenant/member-cards/page`、`/issue`、`/{id}/recharge`、`/{id}/consume`、`/{id}/flows`、`/expiring`、`/low-balance` |
| 优惠券 | `/api/tenant/coupons/issue`、`/{id}/use`、`/available` |
| 欠款 | `/api/tenant/debts/{id}/repay`、`/customer/{customerId}` |
| 提成规则 | `/api/tenant/commission-rules` |
| 结算 | `/api/tenant/settlements/page`、`/generate`、`/{id}/confirm`、`/{id}/pay`、`/tech-income`、`/report` |
| 分账 | `/api/tenant/distributions/settle/{orderId}`、`/{id}/settle`、`/summary` |
| 提现 | `/api/tenant/withdrawals/page`、`/apply`、`/{id}/approve`、`/{id}/reject` |
| 积分 | `/api/tenant/points/page`、`/grant`、`/exchange` |
| 活动 | `/api/tenant/marketing/activities`、`/{id}/publish|stop|join` |
| 评价 | `/api/tenant/reviews/submit`、`/{id}/reply`、`/stats` |
| 回访 | `/api/tenant/follow-ups/generate`、`/{id}/complete`、`/due` |
| 售后 | `/api/tenant/after-sales/apply`、`/{id}/handle` |
| 服务日志 | `/api/tenant/service-logs/by-appointment/{id}`、`/by-technician/{id}` |
| 库存 | `/api/tenant/inventory`、`/{id}/in|out|use`、`/flows`、`/warnings`、`/summary` |

### 5.6 报表 / 预警 / 打印

| 模块 | 路径 |
| --- | --- |
| 租户报表 | `/api/tenant/reports/overview|appointments|orders|members|technicians|marketing|settlements` |
| 导出 | `GET /api/tenant/reports/export?type=orders`（Excel） |
| 预警 | `GET /api/tenant/warnings`、`/certificates` |
| PDF | `/api/tenant/pdf/settlement/{id}`、`/card/{id}`、`/service-report/{orderId}`、`/contract/{customerId}`、`/report` |

## 6. 客户端（`CUSTOMER`）

前缀 `/api/customer`，仅本人数据。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/profile` | 资料 + 卡 + 积分 |
| PUT | `/profile` | 修改姓名/性别/生日/备注 |
| GET | `/home` | 首页聚合 |
| GET | `/items`、`/packages`、`/stores`、`/technicians` | 项目/套餐/门店/技师 |
| GET | `/slots` | 可约时段 |
| POST | `/appointments` | 创建预约 |
| GET | `/appointments/page` | 我的预约 |
| POST | `/appointments/{id}/cancel` | 取消 |
| GET | `/orders/page` | 我的订单 |
| GET | `/orders/{id}` | 订单 + 明细 + 支付流水 |
| POST | `/orders` | 下单 |
| POST | `/orders/{id}/pay` | 支付 |
| GET | `/cards`、`/cards/{id}/flows` | 卡包/流水 |
| POST | `/cards/{id}/recharge?amount=&bonus=` | 充值 |
| GET | `/coupons`、`/points`、`/messages` | 营销/消息 |
| POST/GET | `/reviews` | 评价 |
| POST/GET | `/after-sales` | 售后 |

## 7. 技师端（`TECHNICIAN`）

前缀 `/api/worker`：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/PUT | `/profile` | 档案 |
| GET | `/dashboard` | 工作台 |
| GET | `/schedules` | 排班 |
| GET | `/appointments/page` | 我的预约 |
| POST | `/appointments/{id}/arrive|start|done` | 服务流转 |
| GET | `/orders/page` | 我的订单 |
| POST | `/orders/{id}/verify` | 核销 |
| GET | `/performance` | 业绩/提成 |
| GET/POST | `/withdrawals` | 提现 |
| GET/POST | `/service-logs` | 服务日志 |
| GET | `/reviews`、`/certs`、`/messages` | 评价/证书/消息 |

## 8. 文件与导出

- 上传：`POST /api/files/upload`（multipart，字段 `file`，可选 `dir/bizType`），返回对象 URL。
- 本地文件：`GET /api/files/{path}`。
- Excel 导出返回 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`，文件名 `Content-Disposition`。
- PDF 导出返回 `application/pdf`；中文使用内置字体。

## 9. 限流与防重

| 接口 | 限制 |
| --- | --- |
| 登录 | 5 次/秒/IP（Nginx）+ 后端滑动窗口 |
| 下单/支付/核销/结算/评价 | `@RepeatSubmit` 防重键（3~5s，按接口配置），重复请求返回业务码 429 |
| 通用 API | 30 次/秒/IP |
| 导出/上传 | 单独超时 120s |

防重请求头：`X-Request-Id` 或 `X-Idempotency-Key`（可选）；未传时后端按用户+接口+参数摘要生成。
相同键在窗口期内只允许首次请求执行，后续请求返回 `{"code":429,"msg":"请勿重复提交"}`；本系统不做结果回放。

## 10. curl 示例

```bash
BASE=https://api.example.com
TOKEN=$(curl -s -X POST $BASE/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123","tenantCode":"5MY"}' | jq -r .data.token)

# 客户列表
curl -s "$BASE/api/tenant/customers/page?page=1&size=10&kw=张" \
  -H "Authorization: Bearer $TOKEN"

# 创建预约
curl -s -X POST $BASE/api/tenant/appointments/create \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"customerId":1,"storeId":1,"itemId":1,"startTime":"2026-11-01 10:00:00","technicianId":1}'

# 公开门户
curl -s $BASE/api/public/portal/5MY/home
```

## 11. 对接建议

- 前端统一封装 401/403；403 不重试。
- 金额、数量使用字符串/数值均可，后端 BigDecimal 解析；禁止前端计算实付。
- 时间格式 `yyyy-MM-dd HH:mm:ss`（`startTime` 也接受 ISO `T`）。
- 列表分页上限建议 100；导出走异步大文件时轮询任务状态（当前同步导出，适合 ≤5 万行）。
- 生产必须 HTTPS；微信小程序需配置合法域名；公众号需配置网页授权域名与 JS 安全域名。
