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
import com.meiye.module.order.OrderService;
import com.meiye.module.settlement.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 技师工作端（技师小程序 / 移动 H5）：仅本人数据。
 */
@RestController
@RequestMapping("/api/worker")
@PreAuthorize("hasRole('TECHNICIAN')")
@Audit(module = "技师工作端")
@RequiredArgsConstructor
public class WorkerController {

    private final TechnicianMapper technicianMapper;
    private final ScheduleMapper scheduleMapper;
    private final TechRotationMapper rotationMapper;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentService appointmentService;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final TechnicianCertMapper certMapper;
    private final TechServiceLogMapper serviceLogMapper;
    private final ReviewMapper reviewMapper;
    private final MessageLogMapper messageLogMapper;
    private final WithdrawalMapper withdrawalMapper;
    private final SettlementService settlementService;
    private final JdbcTemplate jdbc;

    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        Technician t = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("technician", t);
        m.put("certs", certMapper.selectList(new LambdaQueryWrapper<TechnicianCert>()
                .eq(TechnicianCert::getTechnicianId, t.getId())));
        m.put("todaySchedules", scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getTechnicianId, t.getId()).eq(Schedule::getWorkDate, LocalDate.now())));
        m.put("rotation", rotationMapper.selectList(new LambdaQueryWrapper<TechRotation>()
                .eq(TechRotation::getTechnicianId, t.getId())
                .eq(TechRotation::getRotationDate, LocalDate.now())));
        return R.ok(m);
    }

    @PutMapping("/profile")
    @Audit(action = "技师修改资料")
    public R<Technician> updateProfile(@RequestBody Map<String, Object> body) {
        Technician t = me();
        if (body.get("bio") != null) {
            t.setBio(String.valueOf(body.get("bio")));
        }
        if (body.get("avatar") != null) {
            t.setAvatar(String.valueOf(body.get("avatar")));
        }
        if (body.get("skills") != null) {
            t.setSkills(String.valueOf(body.get("skills")));
        }
        technicianMapper.updateById(t);
        return R.ok(t);
    }

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        Technician t = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("todayAppointments", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getTechnicianId, t.getId())
                .ge(Appointment::getStartTime, LocalDate.now().atStartOfDay())
                .lt(Appointment::getStartTime, LocalDate.now().plusDays(1).atStartOfDay())));
        m.put("servingCount", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getTechnicianId, t.getId()).eq(Appointment::getStatus, "SERVING")));
        m.put("rating", t.getRating());
        m.put("serviceCount", t.getServiceCount());
        m.put("monthCommission", commission("month"));
        m.put("income", settlementService.techIncome(t.getId()));
        return R.ok(m);
    }

    @GetMapping("/schedules")
    public R<List<Schedule>> schedules(@RequestParam(required = false) String date) {
        Technician t = me();
        LambdaQueryWrapper<Schedule> qw = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getTechnicianId, t.getId());
        if (date != null && !date.isBlank()) {
            qw.eq(Schedule::getWorkDate, LocalDate.parse(date));
        } else {
            qw.ge(Schedule::getWorkDate, LocalDate.now()).orderByAsc(Schedule::getWorkDate);
        }
        return R.ok(scheduleMapper.selectList(qw));
    }

    @GetMapping("/appointments/page")
    public R<PageResult<Appointment>> appointments(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String date) {
        Technician t = me();
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getTechnicianId, t.getId())
                .eq(status != null && !status.isBlank(), Appointment::getStatus, status);
        if (date != null && !date.isBlank()) {
            LocalDate d = LocalDate.parse(date);
            qw.ge(Appointment::getStartTime, d.atStartOfDay()).lt(Appointment::getStartTime, d.plusDays(1).atStartOfDay());
        }
        qw.orderByAsc(Appointment::getStartTime);
        return R.ok(PageResult.of(appointmentMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), qw)));
    }

    @PostMapping("/appointments/{id}/arrive")
    @Audit(action = "技师确认到店")
    public R<Appointment> arrive(@PathVariable Long id) {
        Appointment a = ownAppointment(id);
        return R.ok(appointmentService.changeStatus(a.getId(), "ARRIVED"));
    }

    @PostMapping("/appointments/{id}/start")
    @Audit(action = "技师开始服务")
    public R<Appointment> start(@PathVariable Long id) {
        Appointment a = ownAppointment(id);
        return R.ok(appointmentService.changeStatus(a.getId(), "SERVING"));
    }

    @PostMapping("/appointments/{id}/done")
    @Audit(action = "技师完成服务")
    public R<Appointment> done(@PathVariable Long id) {
        Appointment a = ownAppointment(id);
        return R.ok(appointmentService.changeStatus(a.getId(), "DONE"));
    }

    @GetMapping("/orders/page")
    public R<PageResult<Order>> orders(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String status) {
        Technician t = me();
        return R.ok(PageResult.of(orderMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getTechnicianId, t.getId())
                        .eq(status != null && !status.isBlank(), Order::getStatus, status)
                        .orderByDesc(Order::getId))));
    }

    @PostMapping("/orders/{id}/verify")
    @Audit(action = "技师核销订单")
    public R<Order> verify(@PathVariable Long id) {
        Technician t = me();
        Order o = orderMapper.selectById(id);
        if (o == null || !t.getId().equals(o.getTechnicianId())) {
            throw BizException.forbidden("无权核销该订单");
        }
        return R.ok(orderService.verify(id, o.getOrderNo()));
    }

    @GetMapping("/performance")
    public R<Map<String, Object>> performance() {
        Technician t = me();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("today", commission("day"));
        m.put("week", commission("week"));
        m.put("month", commission("month"));
        m.put("total", commission("all"));
        m.put("todayServices", jdbc.queryForObject("select count(*) from t_order_item oi join t_order o on o.id=oi.order_id " +
                "where oi.technician_id=? and oi.deleted=0 and o.status in ('PAID','VERIFIED','DONE') and date(o.paid_at)=curdate()",
                Long.class, t.getId()));
        m.put("monthServices", jdbc.queryForObject("select count(*) from t_order_item oi join t_order o on o.id=oi.order_id " +
                "where oi.technician_id=? and oi.deleted=0 and o.status in ('PAID','VERIFIED','DONE') " +
                "and date_format(o.paid_at,'%Y-%m')=date_format(curdate(),'%Y-%m')", Long.class, t.getId()));
        m.put("income", settlementService.techIncome(t.getId()));
        m.put("reviews", jdbc.queryForMap("select count(*) total, coalesce(avg(score),0) avgScore from t_review " +
                "where technician_id=? and deleted=0", t.getId()));
        return R.ok(m);
    }

    @GetMapping("/withdrawals")
    public R<List<Withdrawal>> withdrawals() {
        return R.ok(withdrawalMapper.selectList(new LambdaQueryWrapper<Withdrawal>()
                .eq(Withdrawal::getTechId, me().getId()).orderByDesc(Withdrawal::getId)));
    }

    @PostMapping("/withdrawals")
    @Audit(action = "技师申请提现")
    public R<Withdrawal> applyWithdrawal(@RequestBody Map<String, Object> body) {
        Technician t = me();
        BigDecimal amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", "0")));
        if (amount.signum() <= 0) {
            throw new BizException(400, "提现金额必须大于 0");
        }
        Map<String, Object> income = settlementService.techIncome(t.getId());
        BigDecimal available = (BigDecimal) income.get("available");
        if (amount.compareTo(available) > 0) {
            throw new BizException(400, "可提现余额不足，当前 " + available);
        }
        BigDecimal rate = new BigDecimal("1");
        try {
            String v = jdbc.queryForObject("select param_value from platform_param where param_key='withdrawal_fee_rate' limit 1", String.class);
            if (v != null) {
                rate = new BigDecimal(v);
            }
        } catch (Exception ignored) {
        }
        BigDecimal fee = amount.multiply(rate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        Withdrawal w = new Withdrawal();
        w.setTechId(t.getId());
        w.setAmount(amount);
        w.setFee(fee);
        w.setActualAmount(amount.subtract(fee));
        w.setStatus("APPLIED");
        w.setRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        withdrawalMapper.insert(w);
        return R.ok(w);
    }

    @PostMapping("/service-logs")
    @Audit(action = "技师填写服务日志")
    public R<TechServiceLog> log(@RequestBody TechServiceLog body) {
        Technician t = me();
        body.setTechnicianId(t.getId());
        serviceLogMapper.insert(body);
        return R.ok(body);
    }

    @GetMapping("/service-logs")
    public R<List<TechServiceLog>> logs(@RequestParam(required = false) Long appointmentId) {
        Technician t = me();
        return R.ok(serviceLogMapper.selectList(new LambdaQueryWrapper<TechServiceLog>()
                .eq(TechServiceLog::getTechnicianId, t.getId())
                .eq(appointmentId != null, TechServiceLog::getAppointmentId, appointmentId)
                .orderByDesc(TechServiceLog::getId)));
    }

    @GetMapping("/reviews")
    public R<List<Review>> reviews() {
        return R.ok(reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getTechnicianId, me().getId()).orderByDesc(Review::getId)));
    }

    @GetMapping("/certs")
    public R<List<TechnicianCert>> certs() {
        return R.ok(certMapper.selectList(new LambdaQueryWrapper<TechnicianCert>()
                .eq(TechnicianCert::getTechnicianId, me().getId())));
    }

    @GetMapping("/messages")
    public R<List<Map<String, Object>>> messages() {
        Technician t = me();
        // 技师消息中心：本人预约与订单节点提醒（站内）
        return R.ok(jdbc.queryForList("select 'APPOINTMENT' type, a.appointment_no bizNo, concat('预约提醒：', a.start_time) title, " +
                "a.status, a.start_time createdAt from t_appointment a " +
                "where a.technician_id=? and a.deleted=0 and a.start_time>=date_sub(now(), interval 7 day) " +
                "order by a.start_time desc limit 50", t.getId()));
    }

    private Technician me() {
        if (TenantContext.user() == null || TenantContext.user().getTechnicianId() == null) {
            throw BizException.forbidden("请先登录技师账号");
        }
        Technician t = technicianMapper.selectById(TenantContext.user().getTechnicianId());
        if (t == null) {
            throw BizException.notFound("技师不存在");
        }
        return t;
    }

    private Appointment ownAppointment(Long id) {
        Appointment a = appointmentMapper.selectById(id);
        if (a == null || !me().getId().equals(a.getTechnicianId())) {
            throw BizException.forbidden("无权操作该预约");
        }
        return a;
    }

    private BigDecimal commission(String period) {
        String sql = "select coalesce(sum(oi.commission_amount),0) from t_order_item oi " +
                "join t_order o on o.id=oi.order_id where oi.technician_id=? and oi.deleted=0 " +
                "and o.status in ('PAID','VERIFIED','DONE')";
        if ("day".equals(period)) {
            sql += " and date(o.paid_at)=curdate()";
        } else if ("week".equals(period)) {
            sql += " and yearweek(o.paid_at,1)=yearweek(curdate(),1)";
        } else if ("month".equals(period)) {
            sql += " and date_format(o.paid_at,'%Y-%m')=date_format(curdate(),'%Y-%m')";
        }
        BigDecimal v = jdbc.queryForObject(sql, BigDecimal.class, me().getId());
        return v == null ? BigDecimal.ZERO : v;
    }
}
