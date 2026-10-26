package com.meiye.module.report;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> overview(Long tenantId) {
        String where = tenantId == null ? "" : " and tenant_id=" + tenantId;
        String tw = tenantId == null ? "" : " where tenant_id=" + tenantId;
        Map<String, Object> m = new HashMap<>();
        m.put("customers", count("select count(*) from t_customer where deleted=0" + where));
        m.put("technicians", count("select count(*) from t_technician where deleted=0" + where));
        m.put("stores", count("select count(*) from t_store where deleted=0" + where));
        m.put("appointments", count("select count(*) from t_appointment where deleted=0" + where));
        m.put("orders", count("select count(*) from t_order where deleted=0" + where));
        m.put("reviews", count("select count(*) from t_review where deleted=0" + where));
        m.put("storedBalance", decimal("select coalesce(sum(balance),0) from t_member_card where deleted=0 and status='ACTIVE'" + where));
        m.put("revenue", decimal("select coalesce(sum(paid_amount),0) from t_order where deleted=0 and status in ('PAID','VERIFIED','DONE')" + where));
        m.put("settlement", decimal("select coalesce(sum(revenue),0) from t_settlement where deleted=0" + where));
        long totalAppt = count("select count(*) from t_appointment where deleted=0" + where);
        long doneAppt = count("select count(*) from t_appointment where deleted=0 and status in ('ARRIVED','SERVING','DONE')" + where);
        m.put("fulfillRate", totalAppt == 0 ? 0 : Math.round(doneAppt * 1000.0 / totalAppt) / 10.0);
        m.put("avgOrder", count("select count(*) from t_order where deleted=0 and status in ('PAID','VERIFIED','DONE')" + where) == 0 ? 0
                : decimal("select coalesce(avg(paid_amount),0) from t_order where deleted=0 and status in ('PAID','VERIFIED','DONE')" + where));
        m.put("todayAppointments", count("select count(*) from t_appointment where deleted=0 and date(start_time)=curdate()" + where));
        m.put("todayRevenue", decimal("select coalesce(sum(paid_amount),0) from t_order where deleted=0 and date(paid_at)=curdate()" + where));
        m.put("pendingFollowUps", count("select count(*) from t_follow_up where deleted=0 and status='PENDING'" + where));
        m.put("lowStock", count("select count(*) from t_inventory_item where deleted=0 and stock<=warn_stock" + where));
        return m;
    }

    public List<Map<String, Object>> trend(Long tenantId, int days) {
        String where = tenantId == null ? "" : " and tenant_id=" + tenantId;
        return jdbc.queryForList("select date(paid_at) d, count(*) orders, coalesce(sum(paid_amount),0) amount " +
                "from t_order where deleted=0 and paid_at is not null and paid_at>=date_sub(curdate(), interval ? day)" + where +
                " group by date(paid_at) order by d", days);
    }

    public List<Map<String, Object>> bizDistribution() {
        return jdbc.queryForList("select biz_type, count(*) tenants from tenant_biz_type where deleted=0 and enabled=1 group by biz_type order by tenants desc");
    }

    public List<Map<String, Object>> tenantRank() {
        return jdbc.queryForList("select t.id, t.code, t.name, " +
                "(select count(*) from t_customer c where c.tenant_id=t.id and c.deleted=0) customers, " +
                "(select count(*) from t_order o where o.tenant_id=t.id and o.deleted=0) orders, " +
                "(select coalesce(sum(o.paid_amount),0) from t_order o where o.tenant_id=t.id and o.deleted=0) revenue " +
                "from sys_tenant t where t.deleted=0 order by revenue desc");
    }

    public Map<String, Object> tenantDashboard() {
        return overview(com.meiye.common.context.TenantContext.getTenantId());
    }

    private long count(String sql) {
        Long v = jdbc.queryForObject(sql, Long.class);
        return v == null ? 0 : v;
    }

    private BigDecimal decimal(String sql) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class);
        return v == null ? BigDecimal.ZERO : v;
    }
}
