package com.meiye.module.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.security.RepeatSubmit;
import com.meiye.common.util.ExcelUtil;
import com.meiye.common.web.R;
import com.meiye.domain.entity.CashierShift;
import com.meiye.domain.entity.Order;
import com.meiye.domain.entity.OrderItem;
import com.meiye.domain.entity.Payment;
import com.meiye.domain.mapper.CashierShiftMapper;
import com.meiye.domain.mapper.OrderItemMapper;
import com.meiye.domain.mapper.OrderMapper;
import com.meiye.domain.mapper.PaymentMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/tenant/orders")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','FINANCE')")
@Audit(module = "收银订单")
@RequiredArgsConstructor
public class OrderController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final CashierShiftMapper shiftMapper;
    private final OrderService orderService;
    private final JdbcTemplate jdbc;

    @GetMapping("/page")
    public R<Map<String, Object>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long storeId,
                                       @RequestParam(required = false) Long customerId,
                                       @RequestParam(required = false) String kw,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate) {
        Long tid = TenantContext.requireTenantId();
        StringBuilder where = new StringBuilder(" where o.tenant_id=? and o.deleted=0");
        List<Object> args = new ArrayList<>();
        args.add(tid);
        if (status != null && !status.isBlank()) {
            where.append(" and o.status=?");
            args.add(status);
        }
        if (storeId != null) {
            where.append(" and o.store_id=?");
            args.add(storeId);
        }
        if (customerId != null) {
            where.append(" and o.customer_id=?");
            args.add(customerId);
        }
        if (kw != null && !kw.isBlank()) {
            where.append(" and (o.order_no like ? or c.name like ?)");
            args.add("%" + kw + "%");
            args.add("%" + kw + "%");
        }
        if (startDate != null && !startDate.isBlank()) {
            where.append(" and o.created_at>=?");
            args.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            where.append(" and o.created_at<=?");
            args.add(endDate + " 23:59:59");
        }
        String base = " from t_order o left join t_customer c on c.id=o.customer_id " +
                "left join t_technician t on t.id=o.technician_id left join t_store s on s.id=o.store_id " + where;
        Long total = jdbc.queryForObject("select count(*)" + base, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add((page - 1) * size);
        List<Map<String, Object>> records = jdbc.queryForList(
                "select o.*, c.name customer_name, t.name technician_name, s.name store_name" + base +
                        " order by o.id desc limit ? offset ?", listArgs.toArray());
        return R.ok(Map.of("records", records, "total", total == null ? 0 : total, "page", page, "size", size));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null) {
            throw BizException.notFound("订单不存在");
        }
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o);
        m.put("items", orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id)));
        m.put("payments", paymentMapper.selectList(new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, id)));
        m.put("customer", jdbc.queryForMap("select id,name,customer_no,phone,level,points from t_customer where id=? and tenant_id=?",
                o.getCustomerId(), tid));
        return R.ok(m);
    }

    @PostMapping("/create")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "创建订单")
    public R<Order> create(@RequestBody OrderService.OrderReq req) {
        return R.ok(orderService.create(req));
    }

    @PostMapping("/{id}/pay")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "收银支付")
    public R<Order> pay(@PathVariable Long id, @RequestBody OrderService.PayReq req) {
        return R.ok(orderService.pay(id, req));
    }

    @PostMapping("/{id}/verify")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "订单核销")
    public R<Order> verify(@PathVariable Long id) {
        return R.ok(orderService.verify(id, null));
    }

    @PostMapping("/verify")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "扫码核销")
    public R<Order> verifyByNo(@RequestBody Map<String, String> body) {
        return R.ok(orderService.verify(null, body.get("orderNo")));
    }

    @PostMapping("/{id}/complete")
    @Audit(action = "完成订单")
    public R<Order> complete(@PathVariable Long id) {
        return R.ok(orderService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @Audit(action = "取消订单")
    public R<Order> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return R.ok(orderService.cancel(id, reason));
    }

    @GetMapping("/shifts/current")
    public R<CashierShift> currentShift(@RequestParam Long storeId) {
        return R.ok(orderService.currentShift(storeId));
    }

    @PostMapping("/shifts/open")
    @Audit(action = "开始交接班")
    public R<CashierShift> openShift(@RequestBody Map<String, Object> body) {
        Long storeId = Long.valueOf(String.valueOf(body.get("storeId")));
        BigDecimal cash = body.get("openingCash") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("openingCash")));
        return R.ok(orderService.openShift(storeId, cash));
    }

    @PostMapping("/shifts/{id}/close")
    @Audit(action = "结束交接班")
    public R<CashierShift> closeShift(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal cash = body.get("closingCash") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("closingCash")));
        return R.ok(orderService.closeShift(id, cash, body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/shifts/page")
    public R<List<CashierShift>> shifts(@RequestParam(required = false) Long storeId) {
        LambdaQueryWrapper<CashierShift> qw = new LambdaQueryWrapper<CashierShift>().orderByDesc(CashierShift::getId);
        if (storeId != null) {
            qw.eq(CashierShift::getStoreId, storeId);
        }
        return R.ok(shiftMapper.selectList(qw));
    }

    @GetMapping("/export")
    @Audit(action = "导出订单")
    public void export(@RequestParam(required = false) String status,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) {
        Long tid = TenantContext.requireTenantId();
        StringBuilder sql = new StringBuilder("select o.order_no, c.name, o.biz_type, o.total_amount, o.discount_amount, o.paid_amount, o.pay_type, o.status, o.paid_at from t_order o left join t_customer c on c.id=o.customer_id where o.tenant_id=? and o.deleted=0");
        List<Object> args = new ArrayList<>();
        args.add(tid);
        if (status != null && !status.isBlank()) {
            sql.append(" and o.status=?");
            args.add(status);
        }
        if (startDate != null && !startDate.isBlank()) {
            sql.append(" and o.created_at>=?");
            args.add(startDate);
        }
        if (endDate != null && !endDate.isBlank()) {
            sql.append(" and o.created_at<=?");
            args.add(endDate + " 23:59:59");
        }
        sql.append(" order by o.id desc");
        List<Map<String, Object>> list = jdbc.queryForList(sql.toString(), args.toArray());
        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> m : list) {
            rows.add(Arrays.asList(m.get("order_no"), m.get("name"), m.get("biz_type"), m.get("total_amount"),
                    m.get("discount_amount"), m.get("paid_amount"), m.get("pay_type"), m.get("status"), m.get("paid_at")));
        }
        ExcelUtil.write(response, "orders.xlsx", "订单",
                List.of("订单号", "客户", "品类", "应收", "优惠", "实收", "支付方式", "状态", "支付时间"), rows);
    }
}
