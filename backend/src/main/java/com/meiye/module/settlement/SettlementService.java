package com.meiye.module.settlement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.NumberGenerator;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 结算分账：按周/月生成结算单，门店/平台/技师多方分账，股东分红可选。
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementMapper settlementMapper;
    private final SettlementItemMapper settlementItemMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final TechnicianMapper technicianMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final PlatformParamMapper platformParamMapper;
    private final TenantConfigMapper tenantConfigMapper;
    private final NumberGenerator numberGenerator;
    private final JdbcTemplate jdbc;

    @Transactional
    public Map<String, Object> generate(String periodType, LocalDate start, LocalDate end, Long storeId) {
        Long tid = TenantContext.requireTenantId();
        if (start == null || end == null) {
            throw new BizException(400, "请选择结算周期");
        }
        if (end.isBefore(start)) {
            throw new BizException(400, "结束日期不能早于开始日期");
        }
        LambdaQueryWrapper<Order> qw = new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, "PAID").ge(Order::getPaidAt, start.atStartOfDay())
                .le(Order::getPaidAt, end.atTime(23, 59, 59));
        // 已核销/已完成订单同样计入
        qw = new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, List.of("PAID", "VERIFIED", "DONE"))
                .ge(Order::getPaidAt, start.atStartOfDay())
                .le(Order::getPaidAt, end.atTime(23, 59, 59));
        if (storeId != null) {
            qw.eq(Order::getStoreId, storeId);
        }
        List<Order> orders = orderMapper.selectList(qw);

        Map<Long, List<SettlementItem>> byTech = new LinkedHashMap<>();
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal techCommission = BigDecimal.ZERO;
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        if (!orderIds.isEmpty()) {
            for (Order o : orders) {
                revenue = revenue.add(nz(o.getPaidAmount()));
            }
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .in(OrderItem::getOrderId, orderIds));
            for (OrderItem oi : items) {
                if (oi.getTechnicianId() == null) {
                    continue;
                }
                SettlementItem si = new SettlementItem();
                si.setSettlementId(0L);
                si.setTechnicianId(oi.getTechnicianId());
                si.setOrderId(oi.getOrderId());
                si.setRevenue(nz(oi.getAmount()));
                si.setRate(nz(oi.getCommissionRate()));
                si.setCommissionAmount(nz(oi.getCommissionAmount()));
                byTech.computeIfAbsent(oi.getTechnicianId(), k -> new ArrayList<>()).add(si);
                techCommission = techCommission.add(nz(oi.getCommissionAmount()));
            }
        }

        BigDecimal platformRate = paramDecimal("platform_commission_rate", "5");
        BigDecimal dividendRate = paramDecimal("dividend_rate", "0");
        BigDecimal platformShare = revenue.multiply(platformRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal dividend = revenue.multiply(dividendRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal storeShare = revenue.subtract(techCommission).subtract(platformShare).subtract(dividend);
        if (storeShare.signum() < 0) {
            storeShare = BigDecimal.ZERO;
        }

        Settlement s = new Settlement();
        s.setSettlementNo(numberGenerator.next("STL"));
        s.setPeriodType(periodType == null ? "MONTH" : periodType.toUpperCase());
        s.setStartDate(start);
        s.setEndDate(end);
        s.setStoreId(storeId);
        s.setRevenue(revenue);
        s.setTechCommission(techCommission);
        s.setPlatformShare(platformShare);
        s.setStoreShare(storeShare);
        s.setDividend(dividend);
        s.setStatus("DRAFT");
        settlementMapper.insert(s);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("settlement", s);
        int count = 0;
        for (Map.Entry<Long, List<SettlementItem>> e : byTech.entrySet()) {
            for (SettlementItem si : e.getValue()) {
                si.setSettlementId(s.getId());
                settlementItemMapper.insert(si);
                count++;
            }
        }
        result.put("itemCount", count);
        result.put("orderCount", orders.size());
        return result;
    }

    public Map<String, Object> detail(Long id) {
        Settlement s = settlementMapper.selectById(id);
        if (s == null) {
            throw BizException.notFound("结算单不存在");
        }
        List<SettlementItem> items = settlementItemMapper.selectList(
                new LambdaQueryWrapper<SettlementItem>().eq(SettlementItem::getSettlementId, id));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SettlementItem it : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("item", it);
            Technician t = technicianMapper.selectById(it.getTechnicianId());
            m.put("technicianName", t == null ? "" : t.getName());
            rows.add(m);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("settlement", s);
        r.put("items", rows);
        return r;
    }

    public Settlement confirm(Long id) {
        Settlement s = require(id);
        if (!"DRAFT".equals(s.getStatus())) {
            throw new BizException(400, "仅草稿可确认");
        }
        s.setStatus("CONFIRMED");
        settlementMapper.updateById(s);
        return s;
    }

    public Settlement pay(Long id) {
        Settlement s = require(id);
        if (!"CONFIRMED".equals(s.getStatus())) {
            throw new BizException(400, "仅已确认结算单可打款");
        }
        s.setStatus("PAID");
        settlementMapper.updateById(s);
        return s;
    }

    /** 技师收入：累计提成、已提现、可提现余额、明细。 */
    public Map<String, Object> techIncome(Long techId) {
        Long tid = TenantContext.requireTenantId();
        if (techId == null) {
            LoginUserHolder holder = new LoginUserHolder();
            techId = holder.techId;
        }
        if (techId == null) {
            throw new BizException(400, "缺少技师");
        }
        Technician tech = technicianMapper.selectById(techId);
        String techName = tech == null ? "" : tech.getName();
        BigDecimal totalCommission = decimal("select coalesce(sum(si.commission_amount),0) from t_settlement_item si " +
                "join t_settlement s on s.id=si.settlement_id and s.deleted=0 " +
                "where si.tenant_id=? and si.deleted=0 and si.technician_id=? and s.status in ('CONFIRMED','PAID')", tid, techId);
        BigDecimal withdrawn = decimal("select coalesce(sum(amount),0) from t_withdrawal " +
                "where tenant_id=? and deleted=0 and tech_id=? and status in ('APPLIED','APPROVED','PAID')", tid, techId);
        BigDecimal paidOut = decimal("select coalesce(sum(actual_amount),0) from t_withdrawal " +
                "where tenant_id=? and deleted=0 and tech_id=? and status='PAID'", tid, techId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("technicianId", techId);
        m.put("technicianName", techName);
        m.put("totalCommission", totalCommission);
        m.put("withdrawing", withdrawn);
        m.put("paidOut", paidOut);
        m.put("available", totalCommission.subtract(withdrawn).max(BigDecimal.ZERO));
        m.put("records", jdbc.queryForList("select si.id, si.order_id orderId, si.revenue, si.rate, si.commission_amount commissionAmount, " +
                "s.settlement_no settlementNo, s.status, s.start_date startDate, s.end_date endDate " +
                "from t_settlement_item si join t_settlement s on s.id=si.settlement_id " +
                "where si.tenant_id=? and si.deleted=0 and si.technician_id=? order by si.id desc limit 100", tid, techId));
        m.put("withdrawals", withdrawalMapper.selectList(new LambdaQueryWrapper<Withdrawal>()
                .eq(Withdrawal::getTechId, techId).orderByDesc(Withdrawal::getId)));
        return m;
    }

    /** 平台跨租户结算报表（system 超管）。 */
    public Map<String, Object> platformReport(LocalDate start, LocalDate end) {
        String range = "";
        List<Object> args = new ArrayList<>();
        if (start != null) {
            range += " and start_date>=?";
            args.add(start);
        }
        if (end != null) {
            range += " and end_date<=?";
            args.add(end);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("summary", jdbc.queryForMap("select count(*) settlements, coalesce(sum(revenue),0) revenue, " +
                "coalesce(sum(tech_commission),0) techCommission, coalesce(sum(platform_share),0) platformShare, " +
                "coalesce(sum(store_share),0) storeShare, coalesce(sum(dividend),0) dividend " +
                "from t_settlement where deleted=0" + range, args.toArray()));
        m.put("byTenant", jdbc.queryForList("select s.tenant_id tenantId, t.name tenantName, count(*) settlements, " +
                "coalesce(sum(s.revenue),0) revenue, coalesce(sum(s.platform_share),0) platformShare, " +
                "coalesce(sum(s.tech_commission),0) techCommission from t_settlement s " +
                "left join sys_tenant t on t.id=s.tenant_id where s.deleted=0" + range.replace("start_date", "s.start_date").replace("end_date", "s.end_date") +
                " group by s.tenant_id, t.name order by revenue desc", args.toArray()));
        m.put("withdrawals", jdbc.queryForMap("select count(*) total, coalesce(sum(amount),0) amount, " +
                "coalesce(sum(fee),0) fee, coalesce(sum(actual_amount),0) actualAmount from t_withdrawal where deleted=0"));
        m.put("byBizType", jdbc.queryForList("select biz_type bizType, count(*) orders, coalesce(sum(paid_amount),0) revenue " +
                "from t_order where deleted=0 and status in ('PAID','VERIFIED','DONE') group by biz_type order by revenue desc"));
        return m;
    }

    public Map<String, Object> tenantSettlementReport(LocalDate start, LocalDate end) {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        List<Object> args = new ArrayList<>();
        args.add(tid);
        String range = "";
        if (start != null) {
            range += " and start_date>=?";
            args.add(start);
        }
        if (end != null) {
            range += " and end_date<=?";
            args.add(end);
        }
        m.put("summary", jdbc.queryForMap("select count(*) settlements, coalesce(sum(revenue),0) revenue, " +
                "coalesce(sum(tech_commission),0) techCommission, coalesce(sum(platform_share),0) platformShare, " +
                "coalesce(sum(store_share),0) storeShare, coalesce(sum(dividend),0) dividend " +
                "from t_settlement where tenant_id=? and deleted=0" + range, args.toArray()));
        m.put("byTech", jdbc.queryForList("select si.technician_id techId, t.name technicianName, count(*) items, " +
                "coalesce(sum(si.revenue),0) revenue, coalesce(sum(si.commission_amount),0) commission " +
                "from t_settlement_item si left join t_technician t on t.id=si.technician_id " +
                "where si.tenant_id=? and si.deleted=0 group by si.technician_id, t.name order by commission desc", tid));
        m.put("list", settlementMapper.selectList(new LambdaQueryWrapper<Settlement>()
                .eq(Settlement::getTenantId, tid).orderByDesc(Settlement::getId)));
        return m;
    }

    private Settlement require(Long id) {
        Settlement s = settlementMapper.selectById(id);
        if (s == null) {
            throw BizException.notFound("结算单不存在");
        }
        return s;
    }

    private BigDecimal paramDecimal(String key, String def) {
        try {
            PlatformParam p = TenantContext.runAs(null, true,
                    () -> platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                            .eq(PlatformParam::getParamKey, key).last("limit 1")));
            if (p != null && p.getParamValue() != null && !p.getParamValue().isBlank()) {
                return new BigDecimal(p.getParamValue());
            }
        } catch (Exception ignored) {
        }
        return new BigDecimal(def);
    }

    private BigDecimal decimal(String sql, Object... args) {
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, args);
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static class LoginUserHolder {
        private Long techId;

        LoginUserHolder() {
            if (TenantContext.user() != null) {
                this.techId = TenantContext.user().getTechnicianId();
            }
        }
    }
}
