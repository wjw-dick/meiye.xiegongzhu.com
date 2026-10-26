package com.meiye.module.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.appointment.AppointmentService;
import com.meiye.module.card.CardService;
import com.meiye.module.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户自助端（小程序 / 公众号 H5 / App 共用）：仅本人数据。
 */
@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@Audit(module = "客户自助")
@RequiredArgsConstructor
public class CustomerSelfController {

    private final CustomerMapper customerMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentService appointmentService;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final MemberCardMapper memberCardMapper;
    private final CardFlowMapper cardFlowMapper;
    private final CardService cardService;
    private final CouponMapper couponMapper;
    private final PointRecordMapper pointRecordMapper;
    private final MessageLogMapper messageLogMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServicePackageMapper servicePackageMapper;
    private final StoreMapper storeMapper;
    private final TechnicianMapper technicianMapper;
    private final CmsArticleMapper cmsArticleMapper;
    private final ReviewMapper reviewMapper;
    private final AfterSaleMapper afterSaleMapper;

    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        Customer c = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customer", c);
        m.put("cards", cardService.activeCards(c.getId()));
        m.put("points", c.getPoints());
        m.put("growth", c.getGrowth());
        return R.ok(m);
    }

    @PutMapping("/profile")
    @Audit(action = "修改个人资料")
    public R<Customer> updateProfile(@RequestBody Map<String, Object> body) {
        Customer c = me();
        if (body.get("name") != null) {
            c.setName(String.valueOf(body.get("name")));
        }
        if (body.get("gender") != null) {
            c.setGender(String.valueOf(body.get("gender")));
        }
        if (body.get("birthday") != null && !String.valueOf(body.get("birthday")).isBlank()) {
            c.setBirthday(java.time.LocalDate.parse(String.valueOf(body.get("birthday"))));
        }
        if (body.get("remark") != null) {
            c.setRemark(String.valueOf(body.get("remark")));
        }
        customerMapper.updateById(c);
        return R.ok(c);
    }

    @GetMapping("/home")
    public R<Map<String, Object>> home() {
        Customer c = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customer", c);
        m.put("items", serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, 1).orderByAsc(ServiceItem::getId).last("limit 8")));
        m.put("packages", servicePackageMapper.selectList(new LambdaQueryWrapper<ServicePackage>()
                .eq(ServicePackage::getStatus, 1).last("limit 6")));
        m.put("stores", storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)));
        m.put("cards", cardService.activeCards(c.getId()));
        m.put("appointments", appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getCustomerId, c.getId())
                .in(Appointment::getStatus, List.of("PENDING", "CONFIRMED", "ARRIVED", "SERVING"))
                .orderByAsc(Appointment::getStartTime).last("limit 5")));
        m.put("coupons", couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCustomerId, c.getId()).eq(Coupon::getStatus, "UNUSED")));
        m.put("notices", cmsArticleMapper.selectList(new LambdaQueryWrapper<CmsArticle>()
                .eq(CmsArticle::getType, "NOTICE").eq(CmsArticle::getStatus, 1)
                .orderByDesc(CmsArticle::getSort).last("limit 3")));
        m.put("unreadMessages", messageLogMapper.selectCount(new LambdaQueryWrapper<MessageLog>()
                .eq(MessageLog::getCustomerId, c.getId())));
        return R.ok(m);
    }

    @GetMapping("/items")
    public R<List<ServiceItem>> items(@RequestParam(required = false) String bizType) {
        return R.ok(serviceItemMapper.selectList(new LambdaQueryWrapper<ServiceItem>()
                .eq(ServiceItem::getStatus, 1)
                .eq(bizType != null && !bizType.isBlank(), ServiceItem::getBizType, bizType)
                .orderByAsc(ServiceItem::getId)));
    }

    @GetMapping("/packages")
    public R<List<ServicePackage>> packages() {
        return R.ok(servicePackageMapper.selectList(new LambdaQueryWrapper<ServicePackage>()
                .eq(ServicePackage::getStatus, 1).orderByAsc(ServicePackage::getId)));
    }

    @GetMapping("/stores")
    public R<List<Store>> stores() {
        return R.ok(storeMapper.selectList(new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)));
    }

    @GetMapping("/technicians")
    public R<List<Technician>> technicians(@RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) String bizType) {
        return R.ok(technicianMapper.selectList(new LambdaQueryWrapper<Technician>()
                .eq(Technician::getStatus, 1)
                .eq(storeId != null, Technician::getStoreId, storeId)
                .like(bizType != null && !bizType.isBlank(), Technician::getSkills, bizType)
                .orderByDesc(Technician::getRating)));
    }

    @GetMapping("/slots")
    public R<List<Map<String, Object>>> slots(@RequestParam Long storeId,
                                              @RequestParam(required = false) Long itemId,
                                              @RequestParam String date,
                                              @RequestParam(required = false) Long techId) {
        return R.ok(appointmentService.slots(storeId, itemId, date, techId));
    }

    @PostMapping("/appointments")
    @Audit(action = "客户预约")
    public R<Appointment> createAppointment(@RequestBody AppointmentService.AppointmentReq req) {
        Customer c = me();
        req.setCustomerId(c.getId());
        return R.ok(appointmentService.create(req, "MINI"));
    }

    @GetMapping("/appointments/page")
    public R<PageResult<Appointment>> appointments(@RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size,
                                                   @RequestParam(required = false) String status) {
        Customer c = me();
        return R.ok(PageResult.of(appointmentMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getCustomerId, c.getId())
                        .eq(status != null && !status.isBlank(), Appointment::getStatus, status)
                        .orderByDesc(Appointment::getStartTime))));
    }

    @PostMapping("/appointments/{id}/cancel")
    @Audit(action = "客户取消预约")
    public R<Appointment> cancelAppointment(@PathVariable Long id, @RequestParam(required = false) String reason) {
        Appointment a = appointmentMapper.selectById(id);
        if (a == null || !me().getId().equals(a.getCustomerId())) {
            throw BizException.forbidden("无权操作该预约");
        }
        return R.ok(appointmentService.cancel(id, reason == null ? "客户取消" : reason));
    }

    @GetMapping("/orders/page")
    public R<PageResult<Map<String, Object>>> orders(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String status) {
        Customer c = me();
        var p = orderMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<Order>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getCustomerId, c.getId())
                        .eq(status != null && !status.isBlank(), Order::getStatus, status)
                        .orderByDesc(Order::getId));
        List<Map<String, Object>> rows = p.getRecords().stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("order", o);
            return m;
        }).toList();
        PageResult<Map<String, Object>> pr = new PageResult<>();
        pr.setRecords(rows);
        pr.setTotal(p.getTotal());
        pr.setPage(p.getCurrent());
        pr.setSize(p.getSize());
        return R.ok(pr);
    }

    @PostMapping("/orders")
    @Audit(action = "客户下单")
    public R<Order> createOrder(@RequestBody OrderService.OrderReq req) {
        req.setCustomerId(me().getId());
        return R.ok(orderService.create(req));
    }

    @GetMapping("/orders/{id}")
    public R<Map<String, Object>> orderDetail(@PathVariable Long id) {
        Order o = orderMapper.selectById(id);
        if (o == null || !me().getId().equals(o.getCustomerId())) {
            throw BizException.forbidden("无权查看该订单");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("order", o);
        m.put("items", orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, id)));
        m.put("payments", paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, id)));
        return R.ok(m);
    }

    @PostMapping("/orders/{id}/pay")
    @Audit(action = "客户支付订单")
    public R<Order> payOrder(@PathVariable Long id, @RequestBody OrderService.PayReq req) {
        Order o = orderMapper.selectById(id);
        if (o == null || !me().getId().equals(o.getCustomerId())) {
            throw BizException.forbidden("无权操作该订单");
        }
        return R.ok(orderService.pay(id, req));
    }

    @GetMapping("/cards")
    public R<List<MemberCard>> cards() {
        return R.ok(cardService.activeCards(me().getId()));
    }

    @GetMapping("/cards/{id}/flows")
    public R<List<CardFlow>> cardFlows(@PathVariable Long id) {
        MemberCard card = memberCardMapper.selectById(id);
        if (card == null || !me().getId().equals(card.getCustomerId())) {
            throw BizException.forbidden("无权查看该卡");
        }
        return R.ok(cardFlowMapper.selectList(new LambdaQueryWrapper<CardFlow>()
                .eq(CardFlow::getCardId, id).orderByDesc(CardFlow::getId)));
    }

    @PostMapping("/cards/{id}/recharge")
    @Audit(action = "客户充值")
    public R<MemberCard> recharge(@PathVariable Long id,
                                  @RequestParam BigDecimal amount,
                                  @RequestParam(required = false, defaultValue = "0") BigDecimal bonus) {
        MemberCard card = memberCardMapper.selectById(id);
        if (card == null || !me().getId().equals(card.getCustomerId())) {
            throw BizException.forbidden("无权操作该卡");
        }
        return R.ok(cardService.recharge(id, amount, bonus));
    }

    @GetMapping("/coupons")
    public R<List<Coupon>> coupons(@RequestParam(required = false) String status) {
        return R.ok(couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getCustomerId, me().getId())
                .eq(status != null && !status.isBlank(), Coupon::getStatus, status)
                .orderByDesc(Coupon::getId)));
    }

    @GetMapping("/points")
    public R<Map<String, Object>> points() {
        Customer c = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("balance", c.getPoints());
        m.put("growth", c.getGrowth());
        m.put("records", pointRecordMapper.selectList(new LambdaQueryWrapper<PointRecord>()
                .eq(PointRecord::getCustomerId, c.getId()).orderByDesc(PointRecord::getId).last("limit 100")));
        return R.ok(m);
    }

    @GetMapping("/messages")
    public R<List<MessageLog>> messages() {
        return R.ok(messageLogMapper.selectList(new LambdaQueryWrapper<MessageLog>()
                .eq(MessageLog::getCustomerId, me().getId())
                .orderByDesc(MessageLog::getId).last("limit 100")));
    }

    @PostMapping("/reviews")
    @Audit(action = "客户评价")
    public R<Review> review(@RequestBody Review body) {
        Customer c = me();
        body.setCustomerId(c.getId());
        body.setStatus("PUBLISHED");
        if (body.getScore() == null || body.getScore() < 1 || body.getScore() > 5) {
            throw new BizException(400, "评分需在 1~5 之间");
        }
        reviewMapper.insert(body);
        return R.ok(body);
    }

    @GetMapping("/reviews")
    public R<List<Review>> reviews() {
        return R.ok(reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getCustomerId, me().getId()).orderByDesc(Review::getId)));
    }

    @PostMapping("/after-sales")
    @Audit(action = "客户申请售后")
    public R<AfterSale> afterSale(@RequestBody AfterSale body) {
        Customer c = me();
        body.setCustomerId(c.getId());
        body.setStatus("PENDING");
        afterSaleMapper.insert(body);
        return R.ok(body);
    }

    @GetMapping("/after-sales")
    public R<List<AfterSale>> afterSales() {
        return R.ok(afterSaleMapper.selectList(new LambdaQueryWrapper<AfterSale>()
                .eq(AfterSale::getCustomerId, me().getId()).orderByDesc(AfterSale::getId)));
    }

    private Customer me() {
        if (TenantContext.user() == null || TenantContext.user().getCustomerId() == null) {
            throw BizException.forbidden("请先登录客户账号");
        }
        Customer c = customerMapper.selectById(TenantContext.user().getCustomerId());
        if (c == null) {
            throw BizException.notFound("客户不存在");
        }
        return c;
    }
}
