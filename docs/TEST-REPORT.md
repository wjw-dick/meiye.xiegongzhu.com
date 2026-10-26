# 测试与验收报告（TEST REPORT）

- 报告日期：2026-10-27
- 被测版本：仓库当前 HEAD（`backend/`、`admin-web/`、`portal-web/`、`mp-h5/`、`worker-h5/`、`miniprogram/`、`worker-miniprogram/`、`app-flutter/`、`deploy/`、`docs/`）
- 验收依据：根目录 `checklist.md`（唯一验收标准）、`PRO.md`、`tasks.md`

---

## 1. 测试环境

| 项 | 值 |
| --- | --- |
| 操作系统 | Linux（glibc 2.17） |
| JDK | 17（`/opt/jdk-17`） |
| Maven | 3.6.3 |
| Node.js | 16.20.2 |
| 前端构建 | Vite 4 + Vue 3 + TypeScript |
| 后端 | Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 |
| MySQL | 8.x，库 `meiye` |
| Redis / RabbitMQ / MinIO | 远程实例，配置见 `.env.local` |
| 后端端口 | 8081（8080 被其他项目占用） |
| 浏览器验证 | Playwright + Chromium（Docker 容器，宿主 glibc 过旧） |
| 未具备 | Flutter / Dart / Android SDK、微信正式资质、备案域名 |

---

## 2. 构建结果

| 模块 | 命令 | 结果 |
| --- | --- | --- |
| 后端 | `JAVA_HOME=/opt/jdk-17 mvn -DskipTests compile` | ✅ 成功，无编译错误 |
| 管理端 | `cd admin-web && npm run build` | ✅ 成功，产物 `admin-web/dist` |
| 门户 | `cd portal-web && npm run build` | ✅ 成功 |
| 客户 H5 | `cd mp-h5 && npm run build` | ✅ 成功 |
| 技师 H5 | `cd worker-h5 && npm run build` | ✅ 成功 |
| 客户小程序 | 微信开发者工具 | ⚠️ 未构建（无 AppID / 未安装开发者工具） |
| 技师小程序 | 微信开发者工具 | ⚠️ 未构建（同上） |
| Flutter App | `flutter build apk` | ⚠️ 未构建（环境无 Flutter/Android SDK） |

---

## 3. 数据库

| 项 | 结果 |
| --- | --- |
| 表数量 | 57 张 |
| 初始化脚本 | `schema.sql` + `data.sql` 执行无错误 |
| 演示租户 | 2 个（`5MY`、`2HL`） |
| 实体类 | 59 个（含 `BaseEntity` / `TenantEntity`） |
| 索引/唯一键 | 租户内业务编号唯一索引已建立 |

验证命令：

```bash
mysql -h<host> -u<user> -p meiye -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='meiye';"
# 57
```

---

## 4. 接口自动化测试

脚本：`/tmp/opencode/smoke.py`（基础链路，86 项）与 `/tmp/opencode/smoke2.py`（扩展、安全与存储，23 项），合计 **109 项**。
均通过真实 HTTP 调用验证，不 mock 业务逻辑。

### 4.1 基础链路（86/86 通过）

覆盖范围：

| 分组 | 内容 |
| --- | --- |
| 健康与登录 | `/api/health`、平台/租户/客户/技师四种登录 |
| 平台端 | 看板、租户分页、品类插件、参数、多语言、菜单、审计、报表 |
| 租户端 | 门店、客户、技师、项目、套餐、卡种、预约、订单、会员卡、优惠券、积分、营销、结算、提成、提现、库存、消息、微信配置、报表导出（Excel/PDF） |
| 租户隔离 | 平台下钻只读、租户 A 仅见本租户、跨租户头伪造被拦截 |
| 品类插件 | 关闭品类后列表/详情/下单全链路拦截，重新开启恢复 |
| 客户自助 | 资料、首页、项目、套餐、门店、技师、时段、预约、订单、卡包、券、积分、消息、评价、售后 |
| 技师端 | 工作台、预约、核销、排班、业绩、提现、日志、证书、消息 |
| 业务闭环 | 建单 → 现金支付 → 核销 → 详情；预约 → 确认 → 取消；结算生成 |
| 安全 | 微信配置密钥脱敏、导出文件类型正确 |

结果：`==== RESULT: 86 passed, 0 failed`

### 4.2 扩展、安全与存储（23/23 通过）

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 客户订单详情返回 `order/items/payments` | ✅ |
| 2 | 访问他人/不存在订单被拒绝 | ✅ |
| 3 | 微信 APP 渠道 mock 登录 | ✅ |
| 4 | 微信 MINI 渠道 mock 登录 | ✅ |
| 5 | 微信 MP 渠道 mock 登录 | ✅ |
| 6 | 相同 code 登录幂等（同一客户） | ✅ |
| 7 | 公众号回调错误签名被拒绝 | ✅ |
| 8 | 公众号回调 echostr 原样返回 | ✅ |
| 9 | 平台下钻读请求允许 | ✅ |
| 10 | 平台下钻写请求 403 | ✅ |
| 11 | 租户 token 伪造 `X-Tenant-Id` 无法越权 | ✅ |
| 12 | 品类开关列表可读 | ✅ |
| 13 | 相同参数 3 秒内重复创建订单被 429 拦截（仅首单成功） | ✅ |
| 14 | 客户 token 调用租户后台接口被拒 | ✅ |
| 15 | 租户 token 调用平台接口被拒 | ✅ |
| 16 | 技师 token 调用平台接口被拒 | ✅ |
| 17 | 租户 token 调用客户自助接口被拒 | ✅ |
| 18 | 对象存储配置读取、密钥为空/掩码 | ✅ |
| 19 | 存储连接测试（LOCAL） | ✅ |
| 20 | 存储配置保存并回显 | ✅ |
| 21 | 平台对象存储配置可读 | ✅ |
| 22 | 图片上传到对象存储并可下载 | ✅ |
| 23 | 上传文件 Content-Type 正确 | ✅ |

结果：`==== EXTENDED RESULT: 23 passed, 0 failed`

### 4.3 合计

| 脚本 | 通过 | 失败 |
| --- | --- | --- |
| `smoke.py` | 86 | 0 |
| `smoke2.py` | 23 | 0 |
| **合计** | **109** | **0** |

### 4.4 越权审计验证

平台下钻写操作返回 HTTP 403，同时写入审计：

```sql
SELECT id, tenant_id, operator_name, module, action, target, detail, ip, created_at
FROM audit_log WHERE action = '越权拦截' ORDER BY id DESC LIMIT 5;
```

实测记录示例：

| id | tenant_id | operator | action | target | detail |
| --- | --- | --- | --- | --- | --- |
| 387 | 1 | 平台超级管理员 | 越权拦截 | `POST /api/tenant/customers` | 平台下钻租户为只读模式，禁止写操作 |
| 311 | 0 | 平台超级管理员 | 越权拦截 | `GET /api/tenant/customers/page` | 平台账号未选择下钻租户即访问租户接口 |

### 4.5 补充验证（配额 / 到期任务 / 差评介入）

在 109 项自动化之外，对新增的运营闭环做了 3 项真实调用验证：

| # | 用例 | 结果 |
| --- | --- | --- |
| 1 | 将 2HL 租户客户/技师配额临时置为 1，新增客户与技师均返回业务码 403「配额上限」 | ✅ |
| 2 | 插入一个已过期试用租户，调用 `POST /api/platform/tenants/expire-check`，状态自动置为 `EXPIRED` 并写审计「到期自动停用」 | ✅ |
| 3 | 提交 2 分差评，`t_follow_up` 自动生成 `COMPLAINT` 待办，内容含评分与客户反馈 | ✅ |

> 测试数据均已清理，租户配额已恢复为 2000/40。

### 4.6 防重与错误路径复核

- `POST /api/tenant/orders/create` 使用相同参数在 3 秒内连续提交：首次 `code=200` 并生成 1 单，第二次返回 `code=429`「请勿重复提交」；
- 传入相同 `X-Request-Id`、但请求体已变化时，第二次仍返回 429，证明请求头优先作为防重键；
- 访问不存在的接口（如未加 `/create` 的订单地址）返回 HTTP 404 + `code=404`，不再以 HTTP 200/业务码 500 掩盖路由错误。

---

## 5. 页面级验收（Playwright）

使用 Docker 内 Chromium 对五套前端进行真实浏览器渲染，采集：

- 页面 HTTP 状态、正文文本长度；
- 浏览器 console error、未捕获异常；
- 失败请求（≥400，忽略 favicon）；
- 登录流程（租户后台、平台后台、客户 H5、技师 H5）；
- 管理端 21 个业务页 + 9 个平台页、门户 8 个页面、客户 H5 12 页、技师 H5 11 页。

> 宿主 glibc 2.17 无法运行官方 Chromium，改用 `mcr.microsoft.com/playwright:v1.40.1-jammy`
> 容器 + `--network host` 访问本机服务。截图见 `/tmp/opencode/shots/`，结构化结果见
> `/tmp/opencode/ui-report.json`。

### 5.1 结果汇总

| 范围 | 页面数 | 结果 |
| --- | --- | --- |
| 管理端（租户） | 21 | 21/21 通过 |
| 管理端（平台） | 9 | 9/9 通过 |
| 品牌门户 | 8（含详情/404） | 8/8 通过 |
| 客户 H5 | 12 | 12/12 通过 |
| 技师 H5 | 11 | 11/11 通过 |
| **合计** | **64** | **64/64 通过，0 控制台错误、0 失败请求** |

登录流程：租户后台、平台后台、客户 H5、技师 H5 均登录成功并跳转正确页面。

> 2026-10-27 最终回归：在 `GenericCrud`（筛选折叠/骨架屏/空状态）与平台看板统计卡改动后重新构建并复跑，页面验收仍为 64/64、交互验收 17/17，0 控制台错误、0 失败请求。交互脚本临时新增的菜单节点已从 `sys_menu_config` 还原为原始 4 项。

### 5.2 交互级验收（17/17 通过）

在真实浏览器中执行关键交互，验证 UI 不是静态占位：

| 用例 | 结果 |
| --- | --- |
| 对象存储页展示 5 张驱动卡片 | ✅ |
| 未测试前「保存配置」置灰 | ✅ |
| 「连接测试」执行通过 | ✅ |
| 连接测试通过后保存按钮可用 | ✅ |
| 保存配置成功并提示 | ✅ |
| 门户内容弹窗出现 WangEditor 工具栏 | ✅ |
| 门户内容弹窗出现 WangEditor 编辑区 | ✅ |
| 封面上传到对象存储并显示缩略图 | ✅ |
| 富文本可输入并保留内容 | ✅ |
| 菜单可视化树节点渲染 | ✅ |
| 菜单属性面板渲染 | ✅ |
| 底部导航实时预览 | ✅ |
| 新增一级菜单并保存 | ✅ |
| 租户配置页图片上传组件 | ✅ |
| 租户配置页快捷入口（微信/存储/菜单） | ✅ |
| 全流程无控制台错误 | ✅ |
| 全流程无未捕获异常 | ✅ |

### 5.3 说明

- 管理端登录页默认填充演示账号（`5MY / admin / admin123`），点击登录即可；
- 平台登录通过「平台管理」Tab 切换后登录；
- 客户/技师 H5 点击「演示账号」自动填入手机号与密码后登录；
- 门户详情页通过列表页真实链接进入，验证动态路由与 SEO 注入；
- 小程序与 App 不在浏览器验收范围。

---

## 6. 安全专项

| 项目 | 验证方式 | 结果 |
| --- | --- | --- |
| 行级租户隔离 | 跨租户 token/请求头组合测试 | ✅ |
| 平台表白名单 | 平台接口可跨租户读取，业务表自动过滤 | ✅ |
| 平台下钻只读 | 写请求 403 + 审计 | ✅ |
| 越权审计 | 查询 `audit_log` | ✅ |
| 敏感字段加密 | 数据库抽查手机号密文 `ENC:` 前缀 | ✅ |
| 展示脱敏 | 微信配置接口返回 `******` | ✅ |
| 登录限流/防重 | Nginx 配置 + 应用层防重 | ✅（配置级） |
| 密码存储 | BCrypt 哈希 | ✅ |
| 审计脱敏 | 登录参数密码字段打码 | ✅ |
| 文件上传校验 | 后端类型/大小校验 | ✅（代码级） |
| 模块级越权 | 客户/技师/租户/平台 token 交叉调用 | ✅ |
| 对象存储密钥回显 | 配置接口返回掩码 | ✅ |
| 微信回调签名 | 错误签名拒绝、正确签名放行 | ✅ |

---

## 7. 未覆盖 / 受限项

以下项目因外部资质或环境缺失**未完成真机/线上验证**，`checklist.md` 中保持未勾选：

1. 微信小程序真机登录、支付、订阅消息（无正式 AppID 与备案域名）；
2. 公众号网页授权、模板消息、菜单同步（无正式公众号资质）；
3. 微信开放平台 App 登录与支付（无移动应用资质）；
4. Flutter App 构建、签名、上架与真机运行（无 Flutter/Android SDK）；
5. 短信通道、支付通道真实扣款（当前为 mock/配置态）；
6. 生产 HTTPS 证书、备案域名、CDN、WAF；
7. 压测与长时间稳定性测试（建议上线前补做）；
8. 等保测评、渗透测试（建议第三方执行）；
9. 管理端未采用 vue-pure-admin/soybean-admin 现成模板，而是自建 pure-admin 风格布局；
10. 管理端全站 i18n 切换尚未覆盖，目前仅内容字段提供中/EN 双栏与缺失红点。

---

## 8. 备份恢复演练

在独立 MySQL 8 容器中执行真实恢复，避免影响生产库：

```bash
mysqldump -h<host> -u<user> -p --single-transaction --routines --triggers \
  --default-character-set=utf8mb4 --no-tablespaces meiye | gzip -9 > restore-drill.sql.gz
docker run -d --name meiye-mysql-drill -e MYSQL_ROOT_PASSWORD=drill \
  -e MYSQL_DATABASE=meiye mysql:8.0
gunzip -c restore-drill.sql.gz | docker exec -i meiye-mysql-drill \
  mysql -uroot -pdrill meiye
```

校验结果：

| 项 | 源库 | 恢复库 |
| --- | --- | --- |
| 表数量 | 57 | 57 |
| `t_order` | 9 | 9 |
| `t_customer` | 9 | 9 |
| `sys_tenant` | 2 | 2 |
| `t_service_item` | 16 | 16 |
| `t_member_card` | 5 | 5 |

结论：**备份文件可完整恢复，表结构与数据一致**；演练容器已销毁。生产恢复前仍需按
[SECURITY-BACKUP.md](./SECURITY-BACKUP.md) 校验 SHA256 并停服。

---

## 9. 复现方式

```bash
# 1) 启动依赖与后端
cd backend
set -a && source /tmp/meiye.env && set +a
SERVER_PORT=8081 JAVA_HOME=/opt/jdk-17 mvn -DskipTests spring-boot:run

# 2) 健康检查
curl -s http://127.0.0.1:8081/api/health

# 3) 接口测试
python3 /tmp/opencode/smoke.py
python3 /tmp/opencode/smoke2.py

# 4) 前端（开发模式）
cd admin-web && npm run dev -- --port 5183
cd portal-web && npm run dev -- --port 5184
cd mp-h5 && npm run dev -- --port 5185
cd worker-h5 && npm run dev -- --port 5186

# 5) 页面验收
cd /tmp/opencode/pw && PLAYWRIGHT_BROWSERS_PATH=/tmp/opencode/pw-browsers node ui_check.js
```

演示账号：

| 端 | 账号 | 密码 | 说明 |
| --- | --- | --- | --- |
| 平台 | `admin` | `admin123` | 不填租户编码 |
| 租户 5MY | `admin` | `admin123` | 租户编码 `5MY` |
| 租户 2HL | `admin` | `admin123` | 租户编码 `2HL` |
| 客户 | `13700000001` | `123456` | `userType=CUSTOMER` |
| 技师 | `13900000001` | `123456` | `userType=TECHNICIAN` |

---

## 10. 结论

- 后端 **109 项**接口用例全部通过，覆盖认证、租户隔离、模块越权、品类插件、业务闭环、报表、微信、存储与安全；另有 **3 项**补充验证（配额 403、租户到期自动停用、差评自动介入）通过；
- 防重与错误路径复核通过：同参数/同 `X-Request-Id` 二次提交返回 429，未知接口返回 HTTP 404；
- 五套 Web 前端构建成功，**64 个页面 + 17 项关键交互**在真实浏览器验收通过；
- 验收清单已逐项勾选并在第二十节列明未勾选原因，当前为 **101/157 项通过**，未通过项均为外部资质、真机/构建环境或明确未实现的增强项；
- 备份恢复演练在独立 MySQL 8 容器完成，57 张表与核心数据一致；
- 数据库 57 张表初始化成功，演示数据完整；
- 部署、运维、安全备份、API、微信、多端文档齐备；
- 受限项主要为**外部资质/构建环境**（微信真机、App 构建、备案域名）以及管理端模板/i18n 覆盖度，
  其余代码与配置已就绪，取得资质后按
  [DEPLOYMENT.md](./DEPLOYMENT.md) 第 4 节完成发布即可。
