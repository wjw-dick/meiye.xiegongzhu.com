# 美业臻选 · 客户 App（Flutter）

面向 C 端客户的独立 App，与客户小程序、公众号 H5、门户共用同一套后端与 UnionID 账号体系。支持 iOS / Android。

## 功能范围

| 模块 | 说明 |
| --- | --- |
| 登录 | 手机号 + 密码登录（租户编码可切换）；微信开放平台移动应用登录桥接 |
| 首页 | 会员信息、积分/卡包/优惠券概览、快捷入口、即将到来的预约、公告、推荐项目与套餐 |
| 项目 | 项目列表/搜索/品类筛选、详情、会员价、套餐礼包 |
| 预约 | 选项目/门店/技师/日期/时段/备注，提交预约、成功回执、取消预约 |
| 订单 | 全部/待支付/已支付/已核销/已完成筛选，订单详情、混合支付、评价、售后 |
| 卡包 | 储值/计次/套餐卡、余额与次数、流水、储值卡充值 |
| 营销 | 优惠券、积分余额与明细 |
| 消息 | 预约确认/提醒、核销、储值、到期、回访消息 |
| 我的 | 资料编辑、协议、隐私政策、修改密码、退出登录 |
| 多语言 | 中文文案；预留 i18n 资源目录（后续接入 `flutter_localizations`） |

## 目录结构

```
app-flutter/
├── lib/
│   ├── main.dart                 # 入口 + 登录态路由
│   ├── core/
│   │   ├── api.dart              # HTTP 客户端、统一响应、401 处理
│   │   ├── auth.dart             # 登录态持久化（SharedPreferences）
│   │   ├── config.dart           # API 地址 / 租户编码（dart-define）
│   │   ├── format.dart           # 金额、日期、状态、品类、脱敏
│   │   ├── payment.dart          # 订单支付（微信/支付宝/余额）
│   │   ├── theme.dart            # 品牌主题
│   │   └── wechat.dart           # 微信 SDK MethodChannel 桥接
│   ├── pages/                    # 15 个业务页面
│   └── widgets/                  # 通用组件
└── test/format_test.dart         # 单元测试
```

## 本地运行

```bash
# 1. 安装 Flutter 3.27+（Dart 3.6+）
flutter --version

# 2. 拉取依赖
cd app-flutter
flutter pub get

# 3. 运行（Android 模拟器访问宿主机用 10.0.2.2）
flutter run \
  --dart-define=API_BASE_URL=http://10.0.2.2:8081 \
  --dart-define=TENANT_CODE=5MY

# iOS 模拟器 / 真机使用局域网地址
flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8081
```

演示账号：`13700000001 / 123456`（租户 `5MY`）。

## 构建

```bash
# 代码检查与测试
flutter analyze
flutter test

# Android APK / AAB
flutter build apk --release \
  --dart-define=API_BASE_URL=https://api.example.com \
  --dart-define=TENANT_CODE=5MY
flutter build appbundle --release --dart-define=API_BASE_URL=https://api.example.com

# iOS（需 macOS + Xcode + 开发者证书）
flutter build ios --release --dart-define=API_BASE_URL=https://api.example.com
```

> 平台工程目录（android/ios）由 `flutter create . --org com.meiye --project-name meiye_app` 在装有 Flutter 的机器上生成；本交付包仅包含 Dart 源码、配置与测试，未包含二进制与平台签名文件。

## 原生能力接入

### 1. 微信登录 / 支付

`lib/core/wechat.dart` 通过 `MethodChannel('com.meiye/wechat')` 调用原生 SDK：

| 方法 | 参数 | 返回 |
| --- | --- | --- |
| `isInstalled` | - | `bool` |
| `login` | - | `{code, state}` |
| `pay` | `prepayId/partnerId/nonceStr/timeStamp/sign` | `{errCode, errStr}` |

Android 需在 `AndroidManifest.xml` 注册 `WXEntryActivity`，iOS 配置 URL Scheme / Universal Link，并在微信开放平台绑定 App、小程序、公众号到同一账号以打通 UnionID。未接入时 App 会提示"微信登录未启用"，不影响手机号登录。

### 2. 支付宝

`lib/core/payment.dart` 预留 `MethodChannel('com.meiye/alipay')` 的 `pay` 方法，原生侧接入支付宝 App SDK 即可。

### 3. 推送

建议接入厂商推送（华为/小米/OPPO/vivo）+ APNs，后端通过 `/api/tenant/message-logs` 与消息配置统一发送。App 侧在启动后上报设备 Token（接口预留）。

### 4. 生产支付流程

当前演示环境后端 `/api/customer/orders/{id}/pay` 直接记账。生产环境应改为：

1. App 调后端"统一下单"接口，返回微信/支付宝 prepay 参数；
2. 原生 SDK 唤起收银台；
3. 支付平台异步回调后端，验签后落账；
4. App 轮询/查询订单状态并展示结果。

## 安全说明

- Token 保存在 `SharedPreferences`，退出登录立即清除；401 自动回登录页。
- 金额由后端按订单应收计算，客户端不传支付金额。
- 手机号等敏感信息由后端加密存储、脱敏返回。
- 租户编码随登录请求提交，后端通过 JWT 与 MyBatis-Plus 租户插件强制隔离。
- 正式包必须使用 HTTPS，并在 Android `network_security_config` / iOS ATS 中关闭明文流量。

## 已知限制

- 本环境未安装 Flutter/Dart 与 Android SDK，未执行构建与真机验证；源码按 Flutter 3.27+ 编写。相关验收项保持未勾选。
- 微信登录、微信/支付宝支付、推送依赖运营方提供开放平台资质、商户号与证书，代码中已预留桥接与说明。
- 多语言目前为中文，后端已支持语言表与内容多语言字段，App 后续可接入 `flutter_localizations` + 语言接口。
