package com.meiye.module.report;

import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.util.ExcelUtil;
import com.meiye.common.web.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户报表：预约/订单/会员/技师/营销/结算多维度统计 + Excel 导出。
 */
@RestController
@RequestMapping("/api/tenant/reports")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','FINANCE','MARKETING')")
@Audit(module = "租户报表")
@RequiredArgsConstructor
public class TenantReportController {

    private final JdbcTemplate jdbc;
    private final StatsService statsService;

    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        return R.ok(statsService.tenantDashboard());
    }

    @GetMapping("/appointments")
    public R<Map<String, Object>> appointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long storeId) {
        Long tid = TenantContext.requireTenantId();
        List<Object> args = new ArrayList<>();
        args.add(tid);
        StringBuilder where = new StringBuilder(" where deleted=0 and tenant_id=?");
        if (start != null) {
            where.append(" and date(start_time)>=?");
            args.add(start);
        }
        if (end != null) {
            where.append(" and date(start_time)<=?");
            args.add(end);
        }
        if (storeId != null) {
            where.append(" and store_id=?");
            args.add(storeId);
        }
        Map<String, Object> summary = jdbc.queryForMap("select count(*) total, " +
                "sum(case when status in ('ARRIVED','SERVING','DONE') then 1 else 0 end) arrived, " +
                "sum(case when status='NO_SHOW' then 1 else 0 end) noShow, " +
                "sum(case when status='CANCELLED' then 1 else 0 end) cancelled, " +
                "sum(case when status='DONE' then 1 else 0 end) done " +
                "from t_appointment" + where, args.toArray());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", summary);
        m.put("byDay", jdbc.queryForList("select date(start_time) d, count(*) total, " +
                "sum(case when status in ('ARRIVED','SERVING','DONE') then 1 else 0 end) arrived " +
                "from t_appointment" + where + " group by date(start_time) order by d", args.toArray()));
        m.put("byStore", jdbc.queryForList("select s.id storeId, s.name storeName, count(a.id) total, " +
                "sum(case when a.status in ('ARRIVED','SERVING','DONE') then 1 else 0 end) arrived " +
                "from t_store s left join t_appointment a on a.store_id=s.id and a.deleted=0 and a.tenant_id=s.tenant_id " +
                "where s.deleted=0 and s.tenant_id=? group by s.id, s.name order by total desc", tid));
        m.put("byStatus", jdbc.queryForList("select status, count(*) total from t_appointment" + where +
                " group by status", args.toArray()));
        return R.ok(m);
    }

    @GetMapping("/orders")
    public R<Map<String, Object>> orders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String bizType) {
        Long tid = TenantContext.requireTenantId();
        List<Object> args = new ArrayList<>();
        args.add(tid);
        StringBuilder where = new StringBuilder(" where deleted=0 and tenant_id=? and status in ('PAID','VERIFIED','DONE')");
        if (start != null) {
            where.append(" and date(paid_at)>=?");
            args.add(start);
        }
        if (end != null) {
            where.append(" and date(paid_at)<=?");
            args.add(end);
        }
        if (storeId != null) {
            where.append(" and store_id=?");
            args.add(storeId);
        }
        if (bizType != null && !bizType.isBlank()) {
            where.append(" and biz_type=?");
            args.add(bizType);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", jdbc.queryForMap("select count(*) orders, coalesce(sum(paid_amount),0) revenue, " +
                "coalesce(avg(paid_amount),0) avgOrder, coalesce(sum(discount_amount),0) discount " +
                "from t_order" + where, args.toArray()));
        m.put("byDay", jdbc.queryForList("select date(paid_at) d, count(*) orders, coalesce(sum(paid_amount),0) amount " +
                "from t_order" + where + " group by date(paid_at) order by d", args.toArray()));
        m.put("byPayType", jdbc.queryForList("select pay_type payType, count(*) orders, coalesce(sum(paid_amount),0) amount " +
                "from t_order" + where + " group by pay_type order by amount desc", args.toArray()));
        m.put("byBizType", jdbc.queryForList("select biz_type bizType, count(*) orders, coalesce(sum(paid_amount),0) amount " +
                "from t_order" + where + " group by biz_type order by amount desc", args.toArray()));
        m.put("byStore", jdbc.queryForList("select s.id storeId, s.name storeName, count(o.id) orders, " +
                "coalesce(sum(o.paid_amount),0) amount from t_store s left join t_order o on o.store_id=s.id " +
                "and o.deleted=0 and o.tenant_id=s.tenant_id and o.status in ('PAID','VERIFIED','DONE') " +
                "where s.deleted=0 and s.tenant_id=? group by s.id, s.name order by amount desc", tid));
        return R.ok(m);
    }

    @GetMapping("/members")
    public R<Map<String, Object>> members() {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", jdbc.queryForMap("select " +
                "(select count(*) from t_customer where deleted=0 and tenant_id=?) customers, " +
                "(select count(*) from t_customer where deleted=0 and tenant_id=? and last_visit_at>=date_sub(now(), interval 30 day)) activeCustomers, " +
                "(select coalesce(sum(balance),0) from t_member_card where deleted=0 and tenant_id=? and status='ACTIVE') storedBalance, " +
                "(select count(*) from t_member_card where deleted=0 and tenant_id=?) cards, " +
                "(select count(*) from t_member_card where deleted=0 and tenant_id=? and status='ACTIVE' and expire_at is not null and expire_at<=date_add(now(), interval 30 day)) expiringCards",
                tid, tid, tid, tid, tid));
        m.put("cardSales", jdbc.queryForList("select ct.name cardType, count(mc.id) sold, coalesce(sum(ct.price),0) amount " +
                "from t_member_card mc join t_card_type ct on ct.id=mc.card_type_id " +
                "where mc.deleted=0 and mc.tenant_id=? group by ct.name order by sold desc", tid));
        m.put("recharge", jdbc.queryForList("select date(created_at) d, coalesce(sum(amount),0) amount from t_card_flow " +
                "where deleted=0 and tenant_id=? and type='RECHARGE' group by date(created_at) order by d desc limit 30", tid));
        m.put("consume", jdbc.queryForList("select date(created_at) d, coalesce(sum(amount),0) amount, sum(times) times " +
                "from t_card_flow where deleted=0 and tenant_id=? and type='CONSUME' group by date(created_at) order by d desc limit 30", tid));
        m.put("levelDistribution", jdbc.queryForList("select level, count(*) total from t_customer where deleted=0 and tenant_id=? group by level", tid));
        m.put("points", jdbc.queryForList("select type, coalesce(sum(points),0) points from t_point_record " +
                "where deleted=0 and tenant_id=? group by type", tid));
        return R.ok(m);
    }

    @GetMapping("/technicians")
    public R<Map<String, Object>> technicians() {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("list", jdbc.queryForList("select t.id, t.tech_no techNo, t.name, t.position, t.service_count serviceCount, " +
                "t.total_revenue totalRevenue, t.rating, t.rotation_status rotationStatus, " +
                "(select coalesce(sum(si.commission_amount),0) from t_settlement_item si " +
                " join t_settlement s on s.id=si.settlement_id and s.status in ('CONFIRMED','PAID') " +
                " where si.technician_id=t.id and si.deleted=0) commission " +
                "from t_technician t where t.deleted=0 and t.tenant_id=? order by commission desc", tid));
        m.put("byPosition", jdbc.queryForList("select position, count(*) total from t_technician " +
                "where deleted=0 and tenant_id=? group by position", tid));
        return R.ok(m);
    }

    @GetMapping("/marketing")
    public R<Map<String, Object>> marketing() {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("coupons", jdbc.queryForList("select status, count(*) total from t_coupon where deleted=0 and tenant_id=? group by status", tid));
        m.put("distributions", jdbc.queryForList("select status, count(*) total, coalesce(sum(commission),0) amount " +
                "from t_distribution where deleted=0 and tenant_id=? group by status", tid));
        m.put("activities", jdbc.queryForList("select type, count(*) total from t_marketing_activity " +
                "where deleted=0 and tenant_id=? group by type", tid));
        m.put("reviews", jdbc.queryForMap("select count(*) total, coalesce(avg(score),0) avgScore from t_review " +
                "where deleted=0 and tenant_id=?", tid));
        return R.ok(m);
    }

    @GetMapping("/settlements")
    public R<Map<String, Object>> settlements() {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", jdbc.queryForMap("select count(*) total, coalesce(sum(revenue),0) revenue, " +
                "coalesce(sum(tech_commission),0) techCommission, coalesce(sum(platform_share),0) platformShare, " +
                "coalesce(sum(store_share),0) storeShare from t_settlement where deleted=0 and tenant_id=?", tid));
        m.put("list", jdbc.queryForList("select * from t_settlement where deleted=0 and tenant_id=? order by id desc limit 50", tid));
        m.put("withdrawals", jdbc.queryForList("select status, count(*) total, coalesce(sum(amount),0) amount " +
                "from t_withdrawal where deleted=0 and tenant_id=? group by status", tid));
        return R.ok(m);
    }

    @GetMapping("/export")
    public void export(@RequestParam String type,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                       HttpServletResponse response) {
        Long tid = TenantContext.requireTenantId();
        List<Object> args = new ArrayList<>();
        args.add(tid);
        switch (type) {
            case "orders" -> {
                StringBuilder where = new StringBuilder(" where deleted=0 and tenant_id=?");
                if (start != null) {
                    where.append(" and date(paid_at)>=?");
                    args.add(start);
                }
                if (end != null) {
                    where.append(" and date(paid_at)<=?");
                    args.add(end);
                }
                List<Map<String, Object>> list = jdbc.queryForList("select order_no, biz_type, total_amount, " +
                        "discount_amount, paid_amount, pay_type, status, paid_at from t_order" + where + " order by id desc", args.toArray());
                ExcelUtil.write(response, "orders.xlsx", "订单",
                        List.of("订单号", "品类", "原价", "优惠", "实付", "支付方式", "状态", "支付时间"),
                        list.stream().map(r -> List.<Object>of(nz(r.get("order_no")), nz(r.get("biz_type")), nz(r.get("total_amount")),
                                nz(r.get("discount_amount")), nz(r.get("paid_amount")), nz(r.get("pay_type")), nz(r.get("status")), nz(r.get("paid_at")))).toList());
            }
            case "customers" -> {
                List<Map<String, Object>> list = jdbc.queryForList("select customer_no, name, level, points, total_consume, " +
                        "total_recharge, debt, status, created_at from t_customer where deleted=0 and tenant_id=? order by id desc", tid);
                ExcelUtil.write(response, "customers.xlsx", "会员",
                        List.of("会员编号", "姓名", "等级", "积分", "累计消费", "累计充值", "欠款", "状态", "创建时间"),
                        list.stream().map(r -> List.<Object>of(nz(r.get("customer_no")), nz(r.get("name")), nz(r.get("level")),
                                nz(r.get("points")), nz(r.get("total_consume")), nz(r.get("total_recharge")), nz(r.get("debt")),
                                nz(r.get("status")), nz(r.get("created_at")))).toList());
            }
            case "technicians" -> {
                List<Map<String, Object>> list = jdbc.queryForList("select tech_no, name, position, service_count, " +
                        "total_revenue, rating, status from t_technician where deleted=0 and tenant_id=? order by id desc", tid);
                ExcelUtil.write(response, "technicians.xlsx", "技师",
                        List.of("技师编号", "姓名", "岗位", "服务次数", "业绩", "评分", "状态"),
                        list.stream().map(r -> List.<Object>of(nz(r.get("tech_no")), nz(r.get("name")), nz(r.get("position")),
                                nz(r.get("service_count")), nz(r.get("total_revenue")), nz(r.get("rating")), nz(r.get("status")))).toList());
            }
            case "settlements" -> {
                List<Map<String, Object>> list = jdbc.queryForList("select settlement_no, period_type, start_date, end_date, " +
                        "revenue, tech_commission, platform_share, store_share, status from t_settlement " +
                        "where deleted=0 and tenant_id=? order by id desc", tid);
                ExcelUtil.write(response, "settlements.xlsx", "结算",
                        List.of("结算单号", "周期", "开始", "结束", "收入", "技师提成", "平台抽成", "门店分成", "状态"),
                        list.stream().map(r -> List.<Object>of(nz(r.get("settlement_no")), nz(r.get("period_type")),
                                nz(r.get("start_date")), nz(r.get("end_date")), nz(r.get("revenue")), nz(r.get("tech_commission")),
                                nz(r.get("platform_share")), nz(r.get("store_share")), nz(r.get("status")))).toList());
            }
            default -> throw new IllegalArgumentException("不支持的导出类型");
        }
    }

    private Object nz(Object v) {
        return v == null ? "" : v;
    }
}
