# 美业大健康数字化平台（业务品类插件化）SaaS

面向美业与大健康连锁机构的 SaaS 平台：一套后端 + 平台管理端 + 租户管理端 + 品牌门户 + 客户 H5 + 技师 H5 + 微信小程序 + 技师小程序 + Flutter 客户 App。

核心特点是 **SaaS 行级租户隔离** 与 **业务品类插件化**：美容、美发、美甲美睫、皮肤管理、SPA、足浴推拿、中医养生、口腔、体检、医美轻护理、瑜伽健身等品类由平台插件库统一维护，租户按授权启停，业务数据统一带 `biz_type`，未启用品类全链路拦截。

> 当前状态：后端、五套 Web 前端已构建并通过接口/浏览器验收；小程序与 Flutter App 源码完成。微信正式资质、备案域名、Flutter/Android SDK 等外部条件未提供，相关真机/上架项在 `checklist.md` 中保持未勾选。

---

## 1. 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.2.5、Spring Security + JWT |
| 数据 | MyBatis-Plus 3.5.7（`TenantLineInnerInterceptor`）、MySQL 8、Redis、RabbitMQ |
| 存储 | 本地 / MinIO / S3 / 阿里云 OSS / 腾讯云 COS，五驱动可切换 |
| 管理端 | Vue 3 + Vite 4 + TypeScript + Element Plus + ECharts + WangEditor |
| 门户 / H5 | Vue 3 + Vite 4 + TypeScript，自建响应式样式 |
| 小程序 | 微信原生 WXML / WXSS / JS（客户端、技师端） |
| App | Flutter 3.27+ / Dart 3.6+（源码完成，未构建） |
| 部署 | Nginx 多子域名、systemd、宝塔、每日备份脚本 |

---

## 2. 目录结构

```text
.
├── backend/                 Spring Boot 后端（53 个 Controller、59 个实体、57 张表）
│   └── src/main/resources/db/
│       ├── schema.sql       建库建表
│       └── data.sql         演示数据（平台/两个租户/业务样例）
├── admin-web/               平台 + 租户管理端（Vue3）
├── portal-web/              品牌门户 / 落地页 / 分享查询（SEO + 双语）
├── mp-h5/                   客户 H5（预约、订单、卡包、积分、评价、售后）
├── worker-h5/               技师 H5（排班、接单、核销、业绩、提现）
├── miniprogram/             客户微信小程序
├── worker-miniprogram/      技师微信小程序
├── app-flutter/             客户 App（Flutter 源码）
├── deploy/                  env 模板、Nginx、systemd、备份/恢复/构建/部署脚本
├── docs/                    架构、配置、隔离、插件、API、微信、多端、部署、运维、安全、测试报告
├── tools/                   实体/Mapper 生成脚本
├── PRO.md                   产品需求
├── tasks.md                 开发任务 T01~T68
└── checklist.md             上线验收清单（唯一验收依据）
```

---

## 3. 快速启动

### 3.1 依赖

- JDK 17、Maven 3.6+
- Node.js 16+（本项目按 Node 16.20.2 验证，前端统一 Vite 4）
- MySQL 8、Redis、RabbitMQ、MinIO（可选，默认本地存储）
- 微信开发者工具 / Flutter SDK（仅小程序和 App 需要）

### 3.2 环境变量

仓库根目录 `.env.local` 为本地联调配置（含敏感信息，**禁止提交**）。后端支持直接读取环境变量：

```bash
cd backend
set -a && source /tmp/meiye.env && set +a
```

生产环境请复制 `deploy/env/meiye.env.example` 到 `/etc/meiye/meiye.env`，替换全部 `CHANGE_ME`，权限设为 `600`。

### 3.3 初始化数据库

```bash
mysql -h<host> -u<user> -p < backend/src/main/resources/db/schema.sql
mysql -h<host> -u<user> -p < backend/src/main/resources/db/data.sql
```

脚本会创建 `meiye` 库、57 张表和演示数据。也可使用：

```bash
cd deploy/scripts && ./init-db.sh
```

### 3.4 启动后端

```bash
cd backend
JAVA_HOME=/opt/jdk-17 mvn -DskipTests spring-boot:run
# 默认端口 8080，本机 8080 被占用时：
SERVER_PORT=8081 JAVA_HOME=/opt/jdk-17 mvn -DskipTests spring-boot:run
```

健康检查：`curl http://127.0.0.1:8081/api/health`。

### 3.5 启动前端

```bash
# 管理端
cd admin-web && npm install && npm run dev -- --port 5183
# 品牌门户
cd portal-web && npm install && npm run dev -- --port 5184
# 客户 H5
cd mp-h5 && npm install && npm run dev -- --port 5185
# 技师 H5
cd worker-h5 && npm install && npm run dev -- --port 5186
```

Vite 已代理 `/api` 到 `http://127.0.0.1:8081`。生产构建：

```bash
npm run build    # 产物在各模块 dist/
```

### 3.6 小程序 / App

- 小程序：使用微信开发者工具导入 `miniprogram/`、`worker-miniprogram/`，上线前修改 `project.config.json` 的 `appid`、关闭 `urlCheck` 并替换 `baseUrl`。
- App：`cd app-flutter && flutter pub get && flutter run`。当前环境未安装 Flutter/Android SDK，未执行构建。

---

## 4. 演示账号

| 端 | 租户编码 | 账号 | 密码 |
| --- | --- | --- | --- |
| 平台管理端 | 不填 | `admin` | `admin123` |
| 租户管理端（5MY） | `5MY` | `admin` | `admin123` |
| 租户管理端（2HL） | `2HL` | `admin` | `admin123` |
| 客户 H5 / 小程序 | `5MY` | `13700000001` | `123456` |
| 技师 H5 / 小程序 | `5MY` | `13900000001` | `123456` |

平台账号登录后可选择一个租户「下钻」，下钻为**只读**，写操作返回 HTTP 403 并写审计。

---

## 5. 验收结果

| 项目 | 结果 |
| --- | --- |
| 数据库 | 57 张表、演示数据导入成功；独立 MySQL 8 容器恢复演练一致 |
| 后端构建 | `mvn -DskipTests compile` 通过 |
| 接口自动化 | `smoke.py` 86/86 + `smoke2.py` 23/23 = **109/109** |
| 补充验证 | 客户/技师配额 403、租户到期自动停用 + 审计、差评自动生成客服回访任务，3/3 |
| 管理端构建 | `admin-web` 构建成功 |
| 门户 / 客户 H5 / 技师 H5 | 构建成功 |
| 浏览器验收 | **64/64 页面**通过，0 控制台错误、0 失败请求 |
| 关键交互 | **17/17** 通过（存储 5 驱动、保存门禁、富文本、图片上传、菜单编辑器等） |
| 小程序 / App | 源码完成；无正式 AppID / Flutter SDK，未真机验证 |

验收清单勾选结果与未勾选原因见 `checklist.md` 第十九、二十节；完整测试记录见 [`docs/TEST-REPORT.md`](docs/TEST-REPORT.md)。

---

## 6. 已验证的核心能力

- 租户 A/B 数据隔离、平台表白名单、平台下钻只读 403、模块级越权 403 + `audit_log`
- 客户/技师/平台/后台账号类型隔离，客户端仅本人数据
- 品类插件库、租户启停、`biz_type` 数据过滤、未启用品类下单/预约拦截
- 租户开通/停用/续期/配额（门店/客户/技师）与到期预警、定时自动停用
- 客户档案（加密手机号/身份证、脱敏、Excel 导入导出、关联视图）
- 技师档案、证书台账与到期红黄灯、排班/轮牌、服务日志、评价
- 分时预约、预约看板、改约/取消、超时释放、催到店提醒
- 收银、挂账、会员卡扣次/扣余额、优惠券/积分抵扣、交接班
- 会员卡/附属卡、储值流水、到期预警、跨店通兑（租户 + 门店双开关）
- 技师提成、阶梯规则、结算分账、股东分红、提现审核
- 评价/回访/售后、差评自动生成客服介入任务、库存进销存
- 租户/平台报表、Excel/PDF 导出、证书/卡项/预约/回访/库存预警
- 对象存储五驱动、连接测试门禁、密钥掩码、上传/预览/删除
- 菜单可视化（树 + 属性 + 实时预览）、富文本、图片上传、多语言内容
- 微信公众号/小程序/App 登录底座、UnionID、回调签名、模板消息模拟

---

## 7. 上线前必须替换 / 补充

1. `.env.local` / `/etc/meiye/meiye.env` 中的数据库、Redis、RabbitMQ、MinIO、JWT、AES 密钥；
2. 小程序 `appid`、`baseUrl`、合法 request/upload/download 域名；
3. 公众号 AppID/AppSecret/Token/EncodingAESKey、网页授权域名、JS 安全域名、模板 ID；
4. 微信开放平台移动应用 AppID、UnionID 互通、App 支付/推送证书；
5. 微信支付/支付宝/银联/花呗真实商户号与回调；
6. 短信通道、对象存储生产 Bucket 与 CDN 域名；
7. 生产 HTTPS 证书、备案域名、Nginx 多子域名与 systemd；
8. Flutter/Android/iOS 构建环境、签名证书、隐私政策与上架类目；
9. 生产压测、渗透测试、等保测评；
10. 管理端如需全站 i18n / 多页签，可按 `checklist.md` 第二十节继续增强。

> 根目录 `google.txt` 含疑似 GitHub Token，**不要提交或外泄**；当前目录不是 git 仓库。

---

## 8. 文档索引

| 文档 | 内容 |
| --- | --- |
| [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) | 架构、模块、数据流 |
| [`docs/CONFIGURATION.md`](docs/CONFIGURATION.md) | 环境变量与配置 |
| [`docs/TENANT-ISOLATION.md`](docs/TENANT-ISOLATION.md) | 多租户隔离与下钻只读 |
| [`docs/BIZ-PLUGIN.md`](docs/BIZ-PLUGIN.md) | 品类插件机制 |
| [`docs/API.md`](docs/API.md) | API 约定与主要端点 |
| [`docs/WECHAT.md`](docs/WECHAT.md) | 微信/UnionID/模板消息 |
| [`docs/MULTI-CLIENT.md`](docs/MULTI-CLIENT.md) | 五端设计与构建 |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | Nginx/systemd/宝塔部署 |
| [`docs/OPERATIONS.md`](docs/OPERATIONS.md) | 运维与日常操作 |
| [`docs/SECURITY-BACKUP.md`](docs/SECURITY-BACKUP.md) | 安全基线与备份恢复 |
| [`docs/TEST-REPORT.md`](docs/TEST-REPORT.md) | 完整测试与验收报告 |
| [`checklist.md`](checklist.md) | 逐项验收与未勾选原因 |
| [`tasks.md`](tasks.md) | T01~T68 任务拆解 |
