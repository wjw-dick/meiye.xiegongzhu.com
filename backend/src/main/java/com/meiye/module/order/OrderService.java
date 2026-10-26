package com.meiye.module.order;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.NumberGenerator;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.card.CardService;
import com.meiye.module.message.MessageService;
import com.meiye.module.tenant.BizTypeGuard;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final CashierShiftMapper shiftMapper;
    private final CustomerMapper customerMapper;
    private final ServiceItemMapper itemMapper;
    private final ServicePackageMapper packageMapper;
    private final CouponMapper couponMapper;
    private final DebtMapper debtMapper;
    private final PointRecordMapper pointRecordMapper;
    private final CommissionRuleMapper commissionRuleMapper;
    private final TechnicianMapper technicianMapper;
    private final AppointmentMapper appointmentMapper;
    private final PlatformParamMapper platformParamMapper;
    private final CardService cardService;
    private final NumberGenerator numberGenerator;
    private final BizTypeGuard bizTypeGuard;
    private final MessageService messageService;

    @Transactional
    public Order create(OrderReq req) {
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null || customer.getStatus() == null || customer.getStatus() != 1) {
            throw new BizException(404, "客户不存在或已禁用");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException(400, "请选择服务项目");
        }
        boolean hasCard = !cardService.activeCards(customer.getId()).isEmpty();
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        String bizType = req.getBizType();
        for (OrderItemReq line : req.getItems()) {
            OrderItem oi = new OrderItem();
            oi.setTechnicianId(line.getTechnicianId() == null ? req.getTechnicianId() : line.getTechnicianId());
            oi.setQuantity(line.getQuantity() == null ? 1 : line.getQuantity());
            if (line.getItemId() != null) {
                ServiceItem item = itemMapper.selectById(line.getItemId());
                if (item == null || item.getStatus() == null || item.getStatus() != 1) {
                    throw new BizException(404, "服务项目不存在或已下架");
                }
                bizType = bizType == null ? item.getBizType() : bizType;
                oi.setItemId(item.getId());
                oi.setName(item.getName());
                BigDecimal price = hasCard && item.getMemberPrice() != null && item.getMemberPrice().signum() > 0
                        ? item.getMemberPrice() : item.getPrice();
                oi.setPrice(price);
                oi.setCommissionRate(item.getCommissionRate() == null ? BigDecimal.ZERO : item.getCommissionRate());
            } else if (line.getPackageId() != null) {
                ServicePackage pkg = packageMapper.selectById(line.getPackageId());
                if (pkg == null || pkg.getStatus() == null || pkg.getStatus() != 1) {
                    throw new BizException(404, "套餐不存在或已下架");
                }
                bizType = bizType == null ? pkg.getBizType() : bizType;
                oi.setPackageId(pkg.getId());
                oi.setName(pkg.getName());
                oi.setPrice(pkg.getPrice());
                oi.setCommissionRate(BigDecimal.ZERO);
            } else {
                throw new BizException(400, "项目或套餐不能为空");
            }
            oi.setAmount(oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity())));
            oi.setCommissionAmount(BigDecimal.ZERO);
            total = total.add(oi.getAmount());
            items.add(oi);
        }
        if (bizType == null || bizType.isBlank()) {
            throw new BizException(400, "缺少 biz_type");
        }
        bizTypeGuard.assertEnabled(bizType);
        Order o = new Order();
        o.setOrderNo(numberGenerator.next("ORD"));
        o.setBizType(bizType);
        o.setCustomerId(customer.getId());
        o.setStoreId(req.getStoreId());
        o.setTechnicianId(req.getTechnicianId());
        o.setAppointmentId(req.getAppointmentId());
        o.setTotalAmount(total);
        o.setDiscountAmount(BigDecimal.ZERO);
        o.setPaidAmount(BigDecimal.ZERO);
        o.setStatus("PENDING");
        o.setRemark(req.getRemark());
        orderMapper.insert(o);
        for (OrderItem oi : items) {
            oi.setOrderId(o.getId());
            orderItemMapper.insert(oi);
        }
        return o;
    }

    @Transactional
    public Order pay(Long orderId, PayReq req) {
        Long tid = TenantContext.requireTenantId();
        Order o = orderMapper.selectById(orderId);
        if (o == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!"PENDING".equals(o.getStatus())) {
            throw new BizException(400, "订单当前状态不可支付：" + o.getStatus());
        }
        Customer customer = customerMapper.selectById(o.getCustomerId());
        BigDecimal total = o.getTotalAmount();
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Coupon coupon = null;
        if (req.getCouponId() != null) {
            coupon = couponMapper.selectById(req.getCouponId());
            if (coupon == null || !"UNUSED".equals(coupon.getStatus())) {
                throw new BizException(400, "优惠券不可用");
            }
            if (coupon.getExpireAt() != null && coupon.getExpireAt().isBefore(LocalDateTime.now())) {
                throw new BizException(400, "优惠券已过期");
            }
            if (coupon.getMinAmount() != null && total.compareTo(coupon.getMinAmount()) < 0) {
                throw new BizException(400, "未达到优惠券使用门槛");
            }
            if ("CASH".equals(coupon.getType())) {
                couponDiscount = coupon.getAmount();
            } else {
                couponDiscount = total.multiply(BigDecimal.valueOf(100 - coupon.getDiscount().intValue()))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            }
        }
        int pointsUse = req.getPointsUse() == null ? 0 : req.getPointsUse();
        if (pointsUse > 0 && (customer == null || customer.getPoints() == null || customer.getPoints() < pointsUse)) {
            throw new BizException(400, "积分不足");
        }
        BigDecimal pointsDiscount = BigDecimal.valueOf(pointsUse).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal discount = couponDiscount.add(pointsDiscount).min(total);
        BigDecimal payable = total.subtract(discount).max(BigDecimal.ZERO);

        List<PayLine> lines = req.getPayments();
        if (lines == null || lines.isEmpty()) {
            PayLine line = new PayLine();
            line.setChannel(req.getPayType() == null ? "CASH" : req.getPayType());
            line.setAmount(payable);
            line.setCardId(req.getCardId());
            lines = List.of(line);
        }
        BigDecimal paid = BigDecimal.ZERO;
        for (PayLine line : lines) {
            BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
            if (amount.signum() <= 0) {
                continue;
            }
            if ("BALANCE".equals(line.getChannel()) || "CARD".equals(line.getChannel())) {
                if (line.getCardId() == null) {
                    throw new BizException(400, "请选择支付会员卡");
                }
                cardService.consume(line.getCardId(), amount, line.getTimes() == null ? 0 : line.getTimes(), o.getId());
            }
            Payment p = new Payment();
            p.setOrderId(o.getId());
            p.setPayNo(numberGenerator.next("PAY"));
            p.setChannel(line.getChannel());
            p.setAmount(amount);
            p.setStatus("SUCCESS");
            p.setPaidAt(LocalDateTime.now());
            paymentMapper.insert(p);
            paid = paid.add(amount);
        }
        BigDecimal debt = BigDecimal.ZERO;
        if (paid.compareTo(payable) < 0) {
            debt = payable.subtract(paid);
            if (!Boolean.TRUE.equals(req.getAllowDebt())) {
                throw new BizException(400, "支付金额不足，还需支付 " + debt);
            }
            Debt d = new Debt();
            d.setCustomerId(o.getCustomerId());
            d.setOrderId(o.getId());
            d.setAmount(debt);
            d.setPaidAmount(BigDecimal.ZERO);
            d.setStatus("UNPAID");
            d.setRemark("收银挂账");
            debtMapper.insert(d);
            if (customer != null) {
                customer.setDebt((customer.getDebt() == null ? BigDecimal.ZERO : customer.getDebt()).add(debt));
            }
        } else if (paid.compareTo(payable) > 0) {
            throw new BizException(400, "支付金额超过应收金额");
        }
        if (coupon != null) {
            coupon.setStatus("USED");
            couponMapper.updateById(coupon);
        }
        if (pointsUse > 0) {
            customer.setPoints(customer.getPoints() - pointsUse);
            PointRecord pr = new PointRecord();
            pr.setCustomerId(customer.getId());
            pr.setPoints(-pointsUse);
            pr.setType("USE");
            pr.setOrderId(o.getId());
            pr.setRemark("订单抵扣积分");
            pointRecordMapper.insert(pr);
        }
        calculateCommission(o);
        o.setDiscountAmount(discount);
        o.setPaidAmount(paid);
        o.setPayType(req.getPayType() == null ? lines.get(0).getChannel() : req.getPayType());
        o.setStatus("PAID");
        o.setPaidAt(LocalDateTime.now());
        if (TenantContext.user() != null) {
            o.setCashierId(TenantContext.user().getUserId());
        }
        orderMapper.updateById(o);

        if (customer != null) {
            customer.setTotalConsume((customer.getTotalConsume() == null ? BigDecimal.ZERO : customer.getTotalConsume()).add(paid));
            int perYuan = paramInt("points_per_yuan", 1);
            int earn = paid.multiply(BigDecimal.valueOf(perYuan)).intValue();
            customer.setPoints((customer.getPoints() == null ? 0 : customer.getPoints()) + earn);
            customer.setGrowth((customer.getGrowth() == null ? 0 : customer.getGrowth()) + paid.intValue());
            customer.setLastVisitAt(LocalDateTime.now());
            upgradeLevel(customer);
            customerMapper.updateById(customer);
            if (earn > 0) {
                PointRecord pr = new PointRecord();
                pr.setCustomerId(customer.getId());
                pr.setPoints(earn);
                pr.setType("EARN");
                pr.setOrderId(o.getId());
                pr.setRemark("消费获得积分");
                pointRecordMapper.insert(pr);
            }
            messageService.send(tid, customer.getId(), "VERIFY", "付款成功",
                    "订单 " + o.getOrderNo() + " 已支付 " + paid + " 元" + (debt.signum() > 0 ? "，挂账 " + debt : ""));
        }
        return o;
    }

    @Transactional
    public Order verify(Long id, String orderNo) {
        Order o = id != null ? orderMapper.selectById(id)
                : orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
        if (o == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!List.of("PAID", "VERIFIED").contains(o.getStatus())) {
            throw new BizException(400, "订单当前状态不可核销：" + o.getStatus());
        }
        if (!"VERIFIED".equals(o.getStatus())) {
            o.setStatus("VERIFIED");
            orderMapper.updateById(o);
        }
        if (o.getAppointmentId() != null) {
            Appointment a = appointmentMapper.selectById(o.getAppointmentId());
            if (a != null && !"DONE".equals(a.getStatus())) {
                a.setStatus("DONE");
                appointmentMapper.updateById(a);
            }
        }
        messageService.send(TenantContext.requireTenantId(), o.getCustomerId(), "VERIFY", "核销成功",
                "订单 " + o.getOrderNo() + " 已核销");
        return o;
    }

    @Transactional
    public Order complete(Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!List.of("VERIFIED", "DONE").contains(o.getStatus())) {
            throw new BizException(400, "订单当前状态不可完成");
        }
        o.setStatus("DONE");
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Order cancel(Long id, String reason) {
        Order o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException(404, "订单不存在");
        }
        if (!"PENDING".equals(o.getStatus())) {
            throw new BizException(400, "已支付订单请走售后流程");
        }
        o.setStatus("CANCELLED");
        o.setRemark(reason);
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public CashierShift openShift(Long storeId, BigDecimal openingCash) {
        CashierShift open = shiftMapper.selectOne(new LambdaQueryWrapper<CashierShift>()
                .eq(CashierShift::getStoreId, storeId).eq(CashierShift::getStatus, "OPEN").last("limit 1"));
        if (open != null) {
            throw new BizException(400, "当前门店已有未关闭的交接班");
        }
        CashierShift s = new CashierShift();
        s.setStoreId(storeId);
        s.setCashierId(TenantContext.user() == null ? null : TenantContext.user().getUserId());
        s.setCashierName(TenantContext.user() == null ? "system" : TenantContext.user().getName());
        s.setStartAt(LocalDateTime.now());
        s.setOpeningCash(openingCash == null ? BigDecimal.ZERO : openingCash);
        s.setSystemAmount(BigDecimal.ZERO);
        s.setStatus("OPEN");
        shiftMapper.insert(s);
        return s;
    }

    @Transactional
    public CashierShift closeShift(Long id, BigDecimal closingCash, String remark) {
        CashierShift s = shiftMapper.selectById(id);
        if (s == null) {
            throw new BizException(404, "交接班不存在");
        }
        if (!"OPEN".equals(s.getStatus())) {
            throw new BizException(400, "交接班已关闭");
        }
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStoreId, s.getStoreId())
                .in(Order::getStatus, List.of("PAID", "VERIFIED", "DONE"))
                .ge(Order::getPaidAt, s.getStartAt())
                .le(Order::getPaidAt, LocalDateTime.now()));
        BigDecimal systemAmount = orders.stream().map(o -> o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cash = orders.stream().filter(o -> "CASH".equals(o.getPayType()))
                .map(o -> o.getPaidAmount() == null ? BigDecimal.ZERO : o.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        s.setEndAt(LocalDateTime.now());
        s.setClosingCash(closingCash == null ? BigDecimal.ZERO : closingCash);
        s.setSystemAmount(systemAmount);
        s.setDiffAmount(s.getClosingCash().subtract(s.getOpeningCash()).subtract(cash));
        s.setStatus("CLOSED");
        s.setRemark(remark);
        shiftMapper.updateById(s);
        return s;
    }

    public CashierShift currentShift(Long storeId) {
        return shiftMapper.selectOne(new LambdaQueryWrapper<CashierShift>()
                .eq(CashierShift::getStoreId, storeId).eq(CashierShift::getStatus, "OPEN").last("limit 1"));
    }

    private void calculateCommission(Order o) {
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, o.getId()));
        for (OrderItem oi : items) {
            if (oi.getCommissionAmount() != null && oi.getCommissionAmount().signum() > 0) {
                continue;
            }
            BigDecimal rate = oi.getCommissionRate();
            if (rate == null || rate.signum() <= 0) {
                rate = resolveRate(o.getBizType(), oi.getAmount());
            }
            oi.setCommissionRate(rate);
            oi.setCommissionAmount(oi.getAmount().multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            orderItemMapper.updateById(oi);
        }
    }

    private BigDecimal resolveRate(String bizType, BigDecimal amount) {
        List<CommissionRule> rules = commissionRuleMapper.selectList(new LambdaQueryWrapper<CommissionRule>()
                .eq(CommissionRule::getBizType, bizType).eq(CommissionRule::getStatus, 1)
                .orderByAsc(CommissionRule::getMinAmount));
        BigDecimal rate = BigDecimal.ZERO;
        for (CommissionRule r : rules) {
            if (amount.compareTo(r.getMinAmount()) >= 0 && (r.getMaxAmount() == null || amount.compareTo(r.getMaxAmount()) <= 0)) {
                rate = r.getRate();
            }
        }
        return rate;
    }

    private void upgradeLevel(Customer c) {
        BigDecimal total = c.getTotalConsume() == null ? BigDecimal.ZERO : c.getTotalConsume();
        if (total.compareTo(new BigDecimal("50000")) >= 0) {
            c.setLevel("黑金会员");
        } else if (total.compareTo(new BigDecimal("20000")) >= 0) {
            c.setLevel("金卡会员");
        } else if (total.compareTo(new BigDecimal("5000")) >= 0) {
            c.setLevel("银卡会员");
        } else {
            c.setLevel("普通会员");
        }
    }

    private int paramInt(String key, int def) {
        PlatformParam p = platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, key));
        try {
            return p == null ? def : Integer.parseInt(p.getParamValue());
        } catch (Exception e) {
            return def;
        }
    }

    @Data
    public static class OrderReq {
        private Long customerId;
        private Long storeId;
        private Long technicianId;
        private Long appointmentId;
        private String bizType;
        private String remark;
        private List<OrderItemReq> items;
    }

    @Data
    public static class OrderItemReq {
        private Long itemId;
        private Long packageId;
        private Long technicianId;
        private Integer quantity;
    }

    @Data
    public static class PayReq {
        private String payType;
        private Long cardId;
        private Long couponId;
        private Integer pointsUse;
        private Boolean allowDebt;
        private List<PayLine> payments;
    }

    @Data
    public static class PayLine {
        private String channel;
        private BigDecimal amount;
        private Long cardId;
        private Integer times;
    }
}
