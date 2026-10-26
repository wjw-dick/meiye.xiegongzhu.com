# 多端说明

> 管理端、门户、客户 H5、技师 H5、客户小程序、技师小程序、Flutter App 共用一套后端与租户体系。
> 各端构建状态见文末。

## 1. 端与职责

| 端 | 目录 | 用户 | 主要功能 | 构建 |
| --- | --- | --- | --- | --- |
| 平台/租户管理端 | `admin-web` | SYSTEM / OPERATOR / 租户角色 | 平台运营、租户配置、全业务管理、报表、审计 | Vite |
| 品牌门户 | `portal-web` | 公网访客 | SEO 落地、项目/套餐/门店/资讯、分享查询 | Vite |
| 客户 H5 | `mp-h5` | 客户 | 登录、预约、订单、卡包、优惠券、积分、评价、售后 | Vite |
| 技师 H5 | `worker-h5` | 技师 | 工作台、服务流转、核销、排班、业绩、提现 | Vite |
| 客户小程序 | `miniprogram` | 客户 | 微信登录、预约、支付、卡包、消息、动态 tabBar | 微信开发者工具 |
| 技师小程序 | `worker-miniprogram` | 技师 | 接单、核销、排班、业绩、日志 | 微信开发者工具 |
| 客户 App | `app-flutter` | 客户 | 全流程 + 微信登录/支付 + 推送 | Flutter |

## 2. 账号与租户

- 每个端登录都提交 `tenantCode`（App/小程序内置默认，可切换；管理端登录页选择租户）。
- 平台账号不传 `tenantCode`，可跨租户；租户账号/客户/技师必须传。
- JWT 内含 `tenantId`，后续请求无需重复传；平台下钻额外带 `X-Tenant-Id`。
- 多端同一客户通过 UnionID 识别，会员卡/积分/订单数据一致。

演示账号：

| 端 | 账号 | 密码 | 租户 |
| --- | --- | --- | --- |
| 平台 | admin | admin123 | - |
| 租户管理员 | admin | admin123 | 5MY |
| 客户 | 13700000001 | 123456 | 5MY |
| 技师 | 13900000001 | 123456 | 5MY |
| 口腔租户管理员 | admin | admin123 | 2HL |

## 3. 管理端

- 布局：左侧菜单折叠、顶部面包屑、多页签、租户切换器、语言切换。
- 页面：平台（租户/插件/字典/参数/看板/报表/审计/语言/存储）、租户（门店/工位/用户/客户/技师/项目/套餐/预约/收银/卡/营销/结算/库存/报表/微信/消息/菜单）、个人中心。
- 通用 CRUD：`GenericCrud.vue` + `resources.ts`。
- 富文本：WangEditor；图片上传独立卡片、拖拽/排序/预览；附件列表。
- 对象存储：5 驱动卡片 + 测试通过后保存；AK/SK 掩码。
- 权限：路由守卫 + 按钮权限 + 后端 `@PreAuthorize`。

启动：

```bash
cd admin-web
npm install
npm run dev        # http://localhost:5173，代理 8081
npm run build      # dist/
```

## 4. 门户（SEO）

- 路由：`/`、`/items`、`/items/:id`、`/packages`、`/packages/:id`、`/stores`、`/stores/:id`、`/articles`、`/articles/:id`、`/share/order`、404。
- 动态 SEO：title/description/keywords/OG/JSON-LD/canonical；中英 `hreflang`。
- 租户解析：`?tenant=5MY` > 子域名 > localStorage > 构建变量。
- 响应式：PC 双栏、移动单栏；图片懒加载；预约跳转 H5（`VITE_MP_H5_URL`）。
- 分享查询：`/share/order?orderNo=...&tenant=...`，展示脱敏订单状态。

```bash
cd portal-web
npm install
npm run dev        # 5174
npm run build
```

## 5. 客户 H5

- 页面：登录、首页、项目、预约、预约列表、订单、卡包、优惠券、积分、消息、评价、售后、资料、改密、协议。
- 预约：门店/日期/技师/时段/备注；未登录跳登录并回跳。
- 支付：微信/支付宝/余额；演示环境后端记账。
- 移动优先，底部导航；401 自动回登录。
- 公众号内打开时可通过网页授权获取 code 登录（需配置）。

```bash
cd mp-h5
npm run dev        # 5175
npm run build
```

## 6. 技师 H5

- 页面：登录、工作台、预约（到店/开始/完成）、订单核销、排班、业绩提成、提现、服务日志、评价、证书、消息、资料、改密。
- 仅本人数据；核销需订单号/核销码。
- 提现申请 → 后台审核 → 打款；手续费按平台参数。
- 业绩：今日/本周/本月、提成明细、结算单。

```bash
cd worker-h5
npm run dev        # 5176
npm run build
```

## 7. 客户小程序

- 原生 WXML/WXSS/JS；`app.js` 管理登录态、baseUrl、动态 tabBar。
- 页面：登录、首页、项目、详情、预约（含支付）、订单（评价/售后）、卡包（充值/流水）、我的、消息、改密、协议。
- 自定义 tabBar 从 `/api/menus?target=MINIAPP` 加载，后台可视化配置实时生效。
- 微信登录：`wx.login` → `/api/auth/wechat/MINI`；mock 环境可直接体验。
- 配置：`project.config.json` 的 `appid`、`app.js` 的 `baseUrl`/`tenantCode`。

上线：

1. 微信开发者工具导入 `miniprogram/`；
2. 修改 AppID 与 HTTPS 域名；
3. 配置合法域名（request/uploadFile/downloadFile）；
4. 真机预览 → 上传审核 → 发布。

## 8. 技师小程序

- 页面：登录、工作台、预约、订单核销、业绩/排班/提现、我的、服务日志、消息、改密。
- 自定义 tabBar 从 `/api/menus?target=WORKER` 加载。
- 仅技师角色；登录 `userType=TECHNICIAN`。
- 证书到期红黄灯、禁止接单（按后台开关）。

## 9. Flutter App

- 15 个页面，完整客户旅程；Material 3 品牌主题。
- 登录：手机号 + 密码；微信开放平台桥接（MethodChannel）。
- 支付：微信/支付宝/余额；金额由后端计算。
- 构建：`flutter build apk/appbundle/ios`；详见 `app-flutter/README.md`。
- 当前环境未安装 Flutter/Android SDK，**未执行构建与真机验证**，相关验收项未勾选。

## 10. 统一能力

### 动态菜单

```http
GET /api/menus?target=ADMIN|MINIAPP|H5|WORKER|WECHAT_MENU
```

返回树/列表，前端渲染；后台「菜单可视化」编辑，保存写审计。小程序 `onShow` 刷新，无需发版。

### 多语言

- 平台语言表 `sys_language`（启用/停用/排序）。
- 后台顶栏切换语言；内容字段「中/EN」tab，未填红点。
- 门户 hreflang + 内容 `*_en` 字段。
- H5/小程序/App 当前中文文案；可接入语言包。

### 消息

- 站内信：`message_log`，客户/技师消息中心。
- 公众号模板消息：按节点 + 模板 ID。
- App Push：预留通道与设备 Token。
- 短信：预留服务商配置。
- 失败重试 + 日志 + 后台重发。

### 文件

- 统一上传 `/api/files/upload`，按租户/日期分目录。
- 5 种存储驱动；附件表记录 URL/驱动/大小/MIME。
- 证书、服务报告、合同、海报均走对象存储。

## 11. 构建状态

| 端 | 构建 | 说明 |
| --- | --- | --- |
| backend | ✅ `mvn package` / 运行中 | 8081，`/api/health` UP |
| admin-web | ✅ `npm run build` | dist 已生成 |
| portal-web | ✅ `npm run build` | dist 已生成 |
| mp-h5 | ✅ `npm run build` | dist 已生成 |
| worker-h5 | ✅ `npm run build` | dist 已生成 |
| miniprogram | ⚠️ 源码完成 | 需微信开发者工具导入/真机 |
| worker-miniprogram | ⚠️ 源码完成 | 同上 |
| app-flutter | ⚠️ 源码完成，未构建 | 环境无 Flutter/Android SDK |

## 12. 联调检查

- [x] 同一租户下客户 H5/小程序/App 数据一致（API 层）
- [x] 客户仅本人数据；技师仅本人服务与收入
- [x] 管理端下钻只读
- [x] 动态菜单接口返回
- [x] 401/403 统一处理
- [ ] 微信真机登录/支付/模板消息（外部资质）
- [ ] App 真机（环境无 Flutter）
- [ ] 小程序真机（需开发者工具与 AppID）
