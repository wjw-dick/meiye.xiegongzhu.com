package com.meiye.module.report;

import com.meiye.common.audit.Audit;
import com.meiye.common.util.ExcelUtil;
import com.meiye.common.web.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台报表：system 超管跨租户/跨品类统计与对比（只读）。
 */
@RestController
@RequestMapping("/api/platform/reports")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "平台报表")
@RequiredArgsConstructor
public class PlatformReportController {

    private final JdbcTemplate jdbc;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", jdbc.queryForMap("select " +
                "(select count(*) from sys_tenant where deleted=0) tenants, " +
                "(select count(*) from sys_tenant where deleted=0 and status='NORMAL') normalTenants, " +
                "(select count(*) from t_customer where deleted=0) customers, " +
                "(select count(*) from t_order where deleted=0) orders, " +
                "(select coalesce(sum(paid_amount),0) from t_order where deleted=0 and status in ('PAID','VERIFIED','DONE')) revenue, " +
                "(select coalesce(sum(balance),0) from t_member_card where deleted=0 and status='ACTIVE') storedBalance, " +
                "(select coalesce(sum(platform_share),0) from t_settlement where deleted=0) platformShare"));
        m.put("byTenant", jdbc.queryForList("select t.id tenantId, t.code, t.name, t.status, " +
                "(select count(*) from t_customer c where c.tenant_id=t.id and c.deleted=0) customers, " +
                "(select count(*) from t_order o where o.tenant_id=t.id and o.deleted=0) orders, " +
                "(select coalesce(sum(o.paid_amount),0) from t_order o where o.tenant_id=t.id and o.deleted=0 and o.status in ('PAID','VERIFIED','DONE')) revenue, " +
                "(select count(*) from t_appointment a where a.tenant_id=t.id and a.deleted=0) appointments " +
                "from sys_tenant t where t.deleted=0 order by revenue desc"));
        m.put("byBizType", jdbc.queryForList("select biz_type bizType, count(*) tenants from tenant_biz_type " +
                "where deleted=0 and enabled=1 group by biz_type order by tenants desc"));
        m.put("byMonth", jdbc.queryForList("select date_format(paid_at,'%Y-%m') month, count(*) orders, " +
                "coalesce(sum(paid_amount),0) revenue from t_order where deleted=0 and paid_at is not null " +
                "and status in ('PAID','VERIFIED','DONE') group by date_format(paid_at,'%Y-%m') order by month desc limit 12"));
        m.put("bizTypeRevenue", jdbc.queryForList("select biz_type bizType, count(*) orders, coalesce(sum(paid_amount),0) revenue " +
                "from t_order where deleted=0 and status in ('PAID','VERIFIED','DONE') group by biz_type order by revenue desc"));
        return R.ok(m);
    }

    @GetMapping("/compare")
    public R<List<Map<String, Object>>> compare() {
        return R.ok(jdbc.queryForList("select t.id tenantId, t.code, t.name, " +
                "(select count(*) from t_customer c where c.tenant_id=t.id and c.deleted=0) customers, " +
                "(select count(*) from t_order o where o.tenant_id=t.id and o.deleted=0) orders, " +
                "(select coalesce(sum(o.paid_amount),0) from t_order o where o.tenant_id=t.id and o.deleted=0 and o.status in ('PAID','VERIFIED','DONE')) revenue, " +
                "(select coalesce(sum(s.platform_share),0) from t_settlement s where s.tenant_id=t.id and s.deleted=0) platformShare, " +
                "(select count(*) from t_technician te where te.tenant_id=t.id and te.deleted=0) technicians " +
                "from sys_tenant t where t.deleted=0 order by revenue desc"));
    }

    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<Map<String, Object>> list = jdbc.queryForList("select t.code, t.name, t.status, " +
                "(select count(*) from t_customer c where c.tenant_id=t.id and c.deleted=0) customers, " +
                "(select count(*) from t_order o where o.tenant_id=t.id and o.deleted=0) orders, " +
                "(select coalesce(sum(o.paid_amount),0) from t_order o where o.tenant_id=t.id and o.deleted=0 and o.status in ('PAID','VERIFIED','DONE')) revenue " +
                "from sys_tenant t where t.deleted=0 order by revenue desc");
        ExcelUtil.write(response, "platform-tenants.xlsx", "租户统计",
                List.of("租户编码", "租户名称", "状态", "客户数", "订单数", "营收"),
                list.stream().map(r -> List.<Object>of(r.get("code"), r.get("name"), r.get("status"),
                        r.get("customers"), r.get("orders"), r.get("revenue"))).toList());
    }
}
