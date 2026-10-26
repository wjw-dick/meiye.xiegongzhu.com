# 文档索引

| 文档 | 说明 |
| --- | --- |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 系统架构、模块划分、技术选型、数据流 |
| [CONFIGURATION.md](./CONFIGURATION.md) | 环境变量、配置文件、驱动切换、参数说明 |
| [TENANT-ISOLATION.md](./TENANT-ISOLATION.md) | 多租户行级隔离、平台白名单、下钻只读、越权审计 |
| [BIZ-PLUGIN.md](./BIZ-PLUGIN.md) | 品类插件化设计、启停流程、全链路拦截 |
| [API.md](./API.md) | REST API 约定、认证、分页、错误码、主要端点 |
| [WECHAT.md](./WECHAT.md) | 公众号/小程序/App 登录、UnionID、回调、模板消息 |
| [MULTI-CLIENT.md](./MULTI-CLIENT.md) | 管理端、门户、H5、小程序、Flutter App 的设计与构建 |
| [DEPLOYMENT.md](./DEPLOYMENT.md) | 服务器初始化、数据库导入、构建发布、HTTPS、回滚 |
| [OPERATIONS.md](./OPERATIONS.md) | 日常巡检、租户运营、业务操作、故障处理 |
| [SECURITY-BACKUP.md](./SECURITY-BACKUP.md) | 安全基线、密钥与敏感数据、备份恢复、应急响应 |
| [TEST-REPORT.md](./TEST-REPORT.md) | 构建结果、接口用例、页面验收、未覆盖项 |

其他入口：

- 根目录 `README.md`：项目总览、快速启动、演示账号
- `deploy/`：Nginx / systemd / 环境变量模板 / 备份恢复脚本 / 宝塔说明
- `backend/src/main/resources/db/`：`schema.sql`、`data.sql`
- `tasks.md` / `checklist.md`：任务拆解与验收清单
