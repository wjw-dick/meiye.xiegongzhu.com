# 微信接入与多端账号

> 覆盖：客户小程序、服务号（H5/网页授权/模板消息/菜单/素材/粉丝）、Flutter App 微信登录与 UnionID 互通。
> 无正式资质时后端保持 `WECHAT_MOCK=true`，全流程可演示；上线前切换为 false 并完成下方配置。

## 1. 账号与资质准备

| 项 | 用途 | 备注 |
| --- | --- | --- |
| 微信开放平台账号 | UnionID 互通 | 必须企业认证 |
| 移动应用（App） | Flutter App 登录/支付 | 与小程序、公众号绑定同一开放平台 |
| 小程序（客户） | 客户预约/卡包 | 认证 + 类目：美容/美发/医疗（按实际） |
| 小程序（技师端） | 内部工作台 | 建议企业主体，仅员工使用 |
| 服务号 | 公众号 H5、模板消息、菜单 | 认证服务号，网页授权域名 |
| 微信支付商户号 | 在线支付/储值 | 与 AppID 关联，配置 API v3 密钥与证书 |
| 服务器域名 | API/H5/门户 | 已备案，HTTPS |

## 2. 后台配置

路径：租户后台 → 微信配置。

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| 小程序 AppID/Secret | 小程序后台 → 开发管理 | 加密保存，页面掩码 |
| 公众号 AppID/Secret | 公众号后台 → 基本配置 | 网页授权/模板消息 |
| 服务器 URL | `https://api.example.com/api/public/wechat/{tenantCode}/callback` | 每租户独立 |
| Token | 自定义随机串 | 与公众号后台一致 |
| EncodingAESKey | 公众号后台生成 | 安全模式使用 |
| 开放平台 UnionID AppID | 开放平台 → 应用 | 记录绑定关系 |
| App AppID/Secret | 开放平台 → 移动应用 | Flutter App 登录 |
| 模板 ID | 公众号 → 模板消息 | 预约确认/提醒/核销/储值/到期/回访 |
| 推送通道 | `WECHAT_MP`/`IN_APP`/`SMS` | 按租户启用 |

保存后：

- Secret/AESKey 使用 `ENC:` AES 加密入库；
- 页面返回 `******`，不泄露明文；
- 写审计（操作人、字段变更，不含密钥明文）；
- 「连接测试」调用微信接口校验；
- 「同步菜单」把 `sys_menu_config(target=WECHAT_MENU)` 的 JSON 推送到微信。

## 3. 回调

### 3.1 URL

```
https://api.example.com/api/public/wechat/{tenantCode}/callback
```

`{tenantCode}` 如 `5MY`、`2HL`。Nginx 必须放行该路径且不做缓存。

### 3.2 验证

GET 参数 `signature/timestamp/nonce/echostr`：

```
sha1(sort(token, timestamp, nonce).join("")) == signature
```

通过则原样返回 `echostr`。

### 3.3 事件

POST XML：

- `subscribe`：新增/恢复粉丝，更新关注时间；如已绑定客户则发送欢迎语；
- `unsubscribe`：标记取关；
- 文本消息关键词自动回复：预约、订单、卡包、门店、项目、客服；
- 3 秒内相同 openid+回复内容去重，避免重复回复；
- 签名不匹配拒绝处理。

### 3.4 安全建议

- 公众号后台启用安全模式（AES），生产必须校验签名；
- 回调接口限流、防重放（timestamp ±300s）；
- 不返回敏感信息；
- 失败记录日志并告警。

## 4. UnionID 互通

```
微信开放平台（UnionID）
   ├── 小程序  openid_mini  ─┐
   ├── 公众号  openid_mp    ─┼── 同一 UnionID ──> customer.union_id
   └── App     openid_app   ─┘
```

- 登录：`POST /api/auth/wechat/{MINI|MP|APP}`，后端 `code2Session` 获取 `openid/unionid`。
- 首次登录自动创建客户档案（姓名/UnionID/会员等级/编号），后续按 UnionID 命中同一客户。
- 绑定关系：小程序/公众号/App 必须在开放平台同一主体下绑定；否则 UnionID 为空/不一致。
- 客户端仅能看本人数据；多端 Token 独立但客户 ID 相同。

### 各端 openid 获取

| 端 | 接口 | 说明 |
| --- | --- | --- |
| 小程序 | `sns/jscode2session` | `wx.login` 的 code |
| 公众号 H5 | `sns/oauth2/access_token` | 网页授权 code（snsapi_userinfo） |
| App | `sns/oauth2/access_token` | 开放平台移动应用授权 code（WXApi） |

## 5. 小程序

### 5.1 配置

- `miniprogram/app.js`：`baseUrl` 改为 `https://api.example.com`；`tenantCode` 默认 `5MY`。
- `project.config.json`：`appid` 改为正式 AppID；`urlCheck` 生产保持 true（开发可 false）。
- 微信后台 → 开发管理 → 服务器域名：
  - request 合法域名：`https://api.example.com`
  - uploadFile/downloadFile：对象存储或 API 域名
  - socket 合法域名：无
- 业务域名（可选）：门户/H5 域名。

### 5.2 登录流程

1. `wx.login` 获取 code；
2. `POST /api/auth/wechat/MINI` `{tenantCode, code, nickname}`；
3. 后端 mock 或真实 code2Session；
4. 返回 token + customer；
5. 前端存储并加载 `/api/menus?target=MINIAPP` 动态 tabBar。

### 5.3 动态 tabBar

`custom-tab-bar/index.js` 从菜单配置读取 `text/icon/pagePath`，后台修改后小程序 `onShow` 重新拉取，无需发版。图标需放在小程序包内或使用合法 CDN。

## 6. 公众号

### 6.1 网页授权

1. 后台配置网页授权域名：`m.example.com`；
2. 获取授权 URL：`GET /api/tenant/wechat/config/auth-url?redirect=https://m.example.com/#/login`；
3. 用户同意后回跳 `?code=...&state=...`；
4. H5 调 `/api/auth/wechat/MP` 完成登录；
5. UnionID 与小程序/App 一致。

### 6.2 JS-SDK

`GET /api/tenant/wechat/config/signature?url=` 返回 `appId/timestamp/nonceStr/signature`，前端 `wx.config` 注入。仅授必要接口（chooseImage、previewImage、scanQRCode、getLocation）。

### 6.3 菜单

- 后台「菜单可视化」编辑 `WECHAT_MENU` JSON；
- 「同步菜单」调用微信 `menu/create`；
- 支持 view/miniprogram/click/media_id/view_limited；
- 修改后一键同步，失败返回微信错误码。

### 6.4 模板消息

| 业务节点 | 模板编码 | 变量 |
| --- | --- | --- |
| 预约确认 | `APPOINTMENT_CONFIRM` | 项目、门店、时间、技师 |
| 预约提醒 | `APPOINTMENT_REMIND` | 时间、技师、工位 |
| 核销确认 | `VERIFY` | 订单、完成状态 |
| 储值到账 | `STORED` | 金额、余额 |
| 卡项到期 | `EXPIRY` | 卡种、剩余次数、余额 |
| 回访提醒 | `FOLLOWUP` | 客户、上次服务 |

- 按租户配置模板 ID；
- 发送前校验客户 UnionID/openid；
- Redis 限流（单客户/单模板/分钟）+ 失败重试 3 次；
- 全量记录 `message_log`（渠道、模板、结果、错误、重试次数）；
- 后台可重试失败消息。

### 6.5 素材/粉丝/群发

- 粉丝列表、标签、图文素材 CRUD；
- 定向群发按标签筛选，单次最多 500 人；
- 群发前二次确认，记录操作审计；
- 遵守微信频控与内容规范。

## 7. App（Flutter）

- 开放平台移动应用 AppID 填入 `wechat_config.app_app_id`；
- App 原生接入微信 OpenSDK，实现 `com.meiye/wechat` 的 `login` 方法；
- 获取 code 后调 `/api/auth/wechat/APP`；
- 微信支付：后端统一下单 → 原生 `pay` → 回调验签落账；
- 未接入 SDK 时 App 会提示，可继续手机号登录。

详见 `app-flutter/README.md`。

## 8. 环境与 mock

| 环境 | `WECHAT_MOCK` | 行为 |
| --- | --- | --- |
| 开发/演示 | true | 不请求微信，按 tenant+code+channel 生成稳定 openid/unionid；模板消息/菜单打日志 |
| 预发 | false + 测试号 | 真实接口，使用测试 AppID |
| 生产 | false | 正式资质，必须 HTTPS/备案 |

切换到真实环境检查清单：

- [ ] AppID/Secret 正确且未过期
- [ ] 服务器 URL Token 与后台一致
- [ ] 开放平台已绑定三端
- [ ] 服务器域名已配置
- [ ] 模板 ID 已审核通过
- [ ] 支付商户号/证书/回调配置
- [ ] 真机验证登录、支付、消息、菜单

## 9. 常见问题

| 现象 | 排查 |
| --- | --- |
| `invalid signature` | Token 不一致/参数排序错误/URL 编码 |
| `code2Session` 40029 | code 已使用/过期，重新 wx.login |
| `unionid` 为空 | 未绑定开放平台或未关注公众号 |
| 模板消息 43004 | 用户未关注/未授权 |
| 菜单同步 40001 | access_token 失效，检查 Secret/IP 白名单 |
| 小程序请求被拦 | 合法域名未配置/非 HTTPS/证书链不完整 |
| 支付回调未到 | 回调 URL 公网可达、验签、返回 SUCCESS |
| H5 授权回调 loop | state/redirect 编码、域名与后台一致 |

## 10. 验收边界

- 代码与配置页面已完成；真实微信资质、AppID、模板 ID、合法域名、真机验证由运营方提供后执行。
- 未提供前，`checklist.md` 中微信/App 外部环境项保持未勾选；内部 mock 链路已通过烟测。
