package com.meiye.module.report;

import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.R;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 到期预警清单：证书、会员卡、预约超时、待回访、库存不足、欠款。
 */
@RestController
@RequestMapping("/api/tenant/warnings")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','FINANCE','TECHNICIAN')")
@Audit(module = "到期预警")
@RequiredArgsConstructor
public class WarningController {

    private final JdbcTemplate jdbc;

    @GetMapping
    public R<Map<String, Object>> list(@RequestParam(required = false) Integer days) {
        Long tid = TenantContext.requireTenantId();
        int certDays = days == null ? paramInt("cert_warn_days", 30) : days;
        int cardDays = paramInt("card_warn_days", 15);
        int timeout = paramInt("appointment_timeout_minutes", 30);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("certificates", jdbc.queryForList("select tc.id, tc.technician_id technicianId, t.name technicianName, " +
                "tc.cert_type certType, tc.cert_no certNo, tc.expire_at expireAt, " +
                "datediff(tc.expire_at, curdate()) remainDays " +
                "from t_technician_cert tc left join t_technician t on t.id=tc.technician_id " +
                "where tc.deleted=0 and tc.tenant_id=? and tc.expire_at is not null " +
                "and tc.expire_at<=date_add(curdate(), interval ? day) order by tc.expire_at", tid, certDays));
        m.put("cards", jdbc.queryForList("select mc.id, mc.card_no cardNo, mc.name, c.name customerName, mc.balance, " +
                "mc.times, mc.expire_at expireAt, datediff(mc.expire_at, curdate()) remainDays " +
                "from t_member_card mc left join t_customer c on c.id=mc.customer_id " +
                "where mc.deleted=0 and mc.tenant_id=? and mc.status='ACTIVE' and mc.expire_at is not null " +
                "and mc.expire_at<=date_add(now(), interval ? day) order by mc.expire_at", tid, cardDays));
        m.put("appointments", jdbc.queryForList("select a.id, a.appointment_no appointmentNo, c.name customerName, " +
                "a.start_time startTime, a.status, timestampdiff(minute, a.start_time, now()) overdueMinutes " +
                "from t_appointment a left join t_customer c on c.id=a.customer_id " +
                "where a.deleted=0 and a.tenant_id=? and a.status in ('PENDING','CONFIRMED') " +
                "and a.start_time<date_sub(now(), interval ? minute) order by a.start_time", tid, timeout));
        m.put("followUps", jdbc.queryForList("select f.id, c.name customerName, f.type, f.due_at dueAt, f.status " +
                "from t_follow_up f left join t_customer c on c.id=f.customer_id " +
                "where f.deleted=0 and f.tenant_id=? and f.status='PENDING' and f.due_at<=now() order by f.due_at", tid));
        m.put("lowStock", jdbc.queryForList("select id, name, sku, stock, warn_stock warnStock, unit from t_inventory_item " +
                "where deleted=0 and tenant_id=? and stock<=warn_stock order by stock", tid));
        m.put("debts", jdbc.queryForList("select d.id, c.name customerName, d.amount, d.status, d.created_at createdAt " +
                "from t_debt d left join t_customer c on c.id=d.customer_id " +
                "where d.deleted=0 and d.tenant_id=? and d.status='UNPAID' order by d.amount desc", tid));
        m.put("summary", jdbc.queryForMap("select ? certificates, ? cards, ? appointments, ? followUps, ? lowStock, ? debts",
                size(m.get("certificates")), size(m.get("cards")), size(m.get("appointments")),
                size(m.get("followUps")), size(m.get("lowStock")), size(m.get("debts"))));
        return R.ok(m);
    }

    @GetMapping("/certificates")
    public R<List<Map<String, Object>>> certificates(@RequestParam(required = false) Integer days) {
        Long tid = TenantContext.requireTenantId();
        int d = days == null ? paramInt("cert_warn_days", 30) : days;
        return R.ok(jdbc.queryForList("select tc.*, t.name technicianName, datediff(tc.expire_at, curdate()) remainDays " +
                "from t_technician_cert tc left join t_technician t on t.id=tc.technician_id " +
                "where tc.deleted=0 and tc.tenant_id=? and tc.expire_at is not null " +
                "and tc.expire_at<=date_add(curdate(), interval ? day) order by tc.expire_at", tid, d));
    }

    private int size(Object o) {
        return o instanceof List<?> l ? l.size() : 0;
    }

    private int paramInt(String key, int def) {
        try {
            Integer v = jdbc.queryForObject("select param_value from platform_param where param_key=? limit 1", Integer.class, key);
            return v == null ? def : v;
        } catch (Exception e) {
            return def;
        }
    }
}
