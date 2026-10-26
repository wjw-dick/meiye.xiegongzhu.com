# 多租户隔离与权限

> 最高优先级验收项。本系统采用 **共享库 + 共享表 + tenant_id 行级隔离**，由 MyBatis-Plus `TenantLineInnerInterceptor` 在 SQL 层强制注入，业务代码即使漏写 `where tenant_id` 也不会跨租户。

## 1. 隔离模型

| 层级 | 数据 | 隔离方式 |
| --- | --- | --- |
| 平台级 | 租户、平台用户、品类插件、全局字典、平台参数、语言、角色定义 | 白名单，不注入 tenant_id |
| 租户级 | 门店、客户、技师、项目、预约、订单、卡、结算、营销、报表 | 自动注入 `tenant_id = 当前租户` |
| 用户级 | 客户本人、技师本人 | Service 层按 `customer_id` / `technician_id` 二次过滤 |
| 门店级 | 店长/收银/技师 | 通过 `store_id` 与数据权限（角色 + 门店）限制 |

平台级白名单（`MybatisPlusConfig.PLATFORM_TABLES`）：

```java
sys_tenant, sys_platform_user, biz_type_plugin, sys_global_dict,
platform_param, sys_language, sys_role
```

> 新增平台级表必须加入白名单；新增业务表必须继承 `TenantEntity` 并包含 `tenant_id`，否则会被插件误加条件或产生泄漏。

## 2. 租户上下文

```java
public final class TenantContext {
    ThreadLocal<Ctx> HOLDER; // tenantId, ignoreTenant, drilldown, LoginUser
}
```

- JWT 解析后写入：`tenantId`、`user`。
- 平台账号默认 `ignoreTenant=true`，可跨租户统计。
- 平台账号携带 `X-Tenant-Id` 下钻：`tenantId=目标`、`drilldown=true`。
- 请求结束 `finally { TenantContext.clear(); }`，防止线程池串租户。

## 3. SQL 注入效果

业务代码：

```java
orderMapper.selectPage(page, new LambdaQueryWrapper<Order>()
    .eq(Order::getStatus, "PAID"));
```

实际 SQL：

```sql
SELECT ... FROM t_order
WHERE deleted = 0 AND tenant_id = 1 AND status = 'PAID';
```

插入时若实体未设置 `tenant_id`，`MetaObjectHandler` 自动填充当前租户；`ignoreInsert` 避免与手写列冲突。

## 4. 平台下钻（system 超管）

| 场景 | 行为 |
| --- | --- |
| 平台登录，不带头 | 跨租户查询/统计；访问 `/api/tenant/**` 返回 403「请先选择要下钻的租户」 |
| 平台登录 + `X-Tenant-Id: 1` | 只读查看租户 1 数据；GET 放行，POST/PUT/DELETE 返回 403 |
| 平台写操作 | 必须走 `/api/platform/**`，并二次确认 + 审计 |
| 租户管理员 | 仅本租户；访问其他租户 ID（路径/参数）返回 403 并审计 |

前端管理端顶部「租户切换器」写入 `X-Tenant-Id`，页面显示「只读下钻」标识，退出下钻清除请求头。

## 5. 越权拦截点

1. **JWT 过滤器**：无 token → 401；过期/伪造 → 401。
2. **Spring Security**：`@PreAuthorize("hasRole('TENANT_ADMIN')")` 等角色校验。
3. **TenantGuardInterceptor**：
   - `/api/tenant/**` 平台无下钻 → 403；
   - 下钻非 GET → 403。
4. **Service 归属校验**：客户订单/预约/卡操作校验 `customerId == 当前客户`；技师校验 `technicianId`。
5. **租户插件**：SQL 层兜底。
6. **审计**：`@Audit` 记录操作人、角色、租户、目标 ID、IP、UA、耗时、结果；越权异常也落库。

## 6. 客户端与技师端

- 客户登录 `userType=CUSTOMER`，JWT 只带 `customerId`；`/api/customer/**` 全部以 `me()` 取当前客户，不接收他人 ID。
- 技师登录 `userType=TECHNICIAN`，`/api/worker/**` 以当前技师过滤预约、订单、业绩、提现。
- 客户端/技师端不可访问 `/api/tenant/**`、`/api/platform/**`（角色不匹配 → 403）。

## 7. 平台账号跨租户只读示例

```bash
# 平台登录
TOKEN=$(curl -s -X POST https://api.example.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.token)

# 平台总览（跨租户）
curl -s https://api.example.com/api/platform/dashboard -H "Authorization: Bearer $TOKEN"

# 下钻租户 1 只读
curl -s https://api.example.com/api/tenant/customers/page?page=1&size=10 \
  -H "Authorization: Bearer $TOKEN" -H 'X-Tenant-Id: 1'

# 下钻写操作 → code 403
curl -s -X POST https://api.example.com/api/tenant/customers \
  -H "Authorization: Bearer $TOKEN" -H 'X-Tenant-Id: 1' \
  -H 'Content-Type: application/json' -d '{"name":"x"}'
```

## 8. 验收证据（86 项烟测节选）

| 用例 | 预期 | 结果 |
| --- | --- | --- |
| 租户 A 查询客户列表，B 客户不在结果 | 隔离 | PASS |
| 租户 A 用 B 的订单 ID 查询详情 | 404/403 | PASS |
| 平台无下钻访问 `/api/tenant/stores/page` | 403 | PASS |
| 平台下钻租户 GET 门店 | 200 | PASS |
| 平台下钻租户 POST 门店 | 403 | PASS |
| 平台跨租户看板 | 200，含两租户 | PASS |
| 客户访问他人订单支付 | 403 | PASS |
| 技师访问他人业绩 | 403 | PASS |
| 未启用品类下单 | 403 | PASS |
| 租户停用后登录 | 403 | PASS |

完整脚本：`/tmp/opencode/smoke.py`（86 passed / 0 failed）；报告：`docs/TEST-REPORT.md`。

## 9. 已知边界与运维要求

- **共享库模式**：超管拥有数据库权限时可绕过应用隔离；生产应将 DBA 权限与应用账号分离。
- **跨租户统计**：仅平台接口可 `ignoreTenant=true`，且必须 `@PreAuthorize` 平台角色。
- **异步线程**：RabbitMQ 消费者/定时任务需显式 `TenantContext.runAs(tenantId, ...)`；新增异步逻辑必须遵循。
- **缓存 key**：必须包含 `tenantId`，禁止跨租户共用业务缓存。
- **文件 URL**：附件表含 `tenant_id`，下载接口校验归属；MinIO 桶策略不得公共写。
- **审计留存**：建议 ≥ 1 年，定期归档；越权告警接入监控。
- **压测**：多租户并发下验证 ThreadLocal 清理、连接池、慢 SQL；禁止在日志打印完整 JWT。
