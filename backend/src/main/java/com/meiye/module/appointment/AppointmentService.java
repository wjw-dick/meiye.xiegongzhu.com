package com.meiye.module.appointment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.NumberGenerator;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.message.MessageService;
import com.meiye.module.tenant.BizTypeGuard;
import com.meiye.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentLogMapper appointmentLogMapper;
    private final ScheduleMapper scheduleMapper;
    private final TechRotationMapper rotationMapper;
    private final CustomerMapper customerMapper;
    private final TechnicianMapper technicianMapper;
    private final ServiceItemMapper itemMapper;
    private final StoreMapper storeMapper;
    private final WorkstationMapper workstationMapper;
    private final PlatformParamMapper platformParamMapper;
    private final TenantMapper tenantMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final NumberGenerator numberGenerator;
    private final BizTypeGuard bizTypeGuard;
    private final MessageService messageService;
    private final RabbitTemplate rabbitTemplate;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    public static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.contains("T")) {
                return LocalDateTime.parse(value.length() > 19 ? value.substring(0, 19) : value);
            }
            if (value.length() <= 10) {
                return LocalDate.parse(value).atStartOfDay();
            }
            return LocalDateTime.parse(value, FMT);
        } catch (Exception e) {
            throw new BizException(400, "时间格式错误：" + value);
        }
    }

    @Transactional
    public Appointment create(AppointmentReq req, String source) {
        Long tid = TenantContext.requireTenantId();
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null || customer.getStatus() == null || customer.getStatus() != 1) {
            throw new BizException(404, "客户不存在或已禁用");
        }
        Store store = storeMapper.selectById(req.getStoreId());
        if (store == null || store.getStatus() == null || store.getStatus() != 1) {
            throw new BizException(404, "门店不存在或已停业");
        }
        ServiceItem item = null;
        String bizType = req.getBizType();
        LocalDateTime start = parse(req.getStartTime());
        int duration = req.getDuration() == null ? 60 : req.getDuration();
        BigDecimal amount = BigDecimal.ZERO;
        if (req.getItemId() != null) {
            item = itemMapper.selectById(req.getItemId());
            if (item == null || item.getStatus() == null || item.getStatus() != 1) {
                throw new BizException(404, "服务项目不存在或已下架");
            }
            bizType = item.getBizType();
            duration = item.getDuration() == null ? duration : item.getDuration();
            amount = item.getMemberPrice() != null && item.getMemberPrice().compareTo(BigDecimal.ZERO) > 0
                    ? item.getMemberPrice() : item.getPrice();
        }
        if (bizType == null || bizType.isBlank()) {
            throw new BizException(400, "请选择服务项目或品类");
        }
        bizTypeGuard.assertEnabled(bizType);
        if (start == null) {
            throw new BizException(400, "请选择预约时段");
        }
        if (start.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new BizException(400, "不能预约过去的时间");
        }
        LocalDateTime end = parse(req.getEndTime());
        if (end == null) {
            end = start.plusMinutes(duration);
        }
        if (!end.isAfter(start)) {
            throw new BizException(400, "结束时间必须晚于开始时间");
        }
        Technician tech = null;
        if (req.getTechnicianId() != null) {
            tech = technicianMapper.selectById(req.getTechnicianId());
            if (tech == null || tech.getStatus() == null || tech.getStatus() != 1) {
                throw new BizException(404, "技师不存在或已停用");
            }
            if (tech.getStoreId() != null && !tech.getStoreId().equals(req.getStoreId())) {
                throw new BizException(400, "技师不属于该门店");
            }
            boolean blockExpired = paramBool("cert_block_booking", true);
            if (blockExpired && "EXPIRED".equals(tech.getCertStatus())) {
                throw BizException.forbidden("该技师证书/健康证已过期，暂不可接单");
            }
            assertSchedule(tech.getId(), start, end);
            assertNoConflict(tech.getId(), null, start, end);
        }
        if (req.getWorkstationId() != null) {
            Workstation ws = workstationMapper.selectById(req.getWorkstationId());
            if (ws == null || !ws.getStoreId().equals(req.getStoreId())) {
                throw new BizException(404, "工位不存在");
            }
            Long overlap = appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getWorkstationId, req.getWorkstationId())
                    .notIn(Appointment::getStatus, List.of("CANCELLED", "NO_SHOW", "DONE"))
                    .lt(Appointment::getStartTime, end)
                    .gt(Appointment::getEndTime, start));
            if (overlap > 0) {
                throw new BizException(409, "该工位时段已被占用");
            }
        }
        Appointment a = new Appointment();
        a.setAppointmentNo(numberGenerator.next("APT"));
        a.setBizType(bizType);
        a.setCustomerId(customer.getId());
        a.setStoreId(req.getStoreId());
        a.setTechnicianId(req.getTechnicianId());
        a.setItemId(req.getItemId());
        a.setPackageId(req.getPackageId());
        a.setWorkstationId(req.getWorkstationId());
        a.setStartTime(start);
        a.setEndTime(end);
        a.setStatus("PENDING");
        a.setSource(source == null ? "STAFF" : source);
        a.setAmount(amount);
        a.setRemark(req.getRemark());
        a.setCreatedBy(TenantContext.user() == null ? null : TenantContext.user().getUserId());
        a.setReleased(0);
        appointmentMapper.insert(a);
        log(a.getId(), "CREATE", null, a, "创建预约");
        messageService.send(tid, customer.getId(), "APPOINTMENT_CONFIRM", "预约已提交",
                "预约号 " + a.getAppointmentNo() + "，时间 " + start.format(FMT) + "，请按时到店");
        publishDelay(a.getId(), start);
        return a;
    }

    public Appointment confirm(Long id) {
        Appointment a = require(id);
        if (!List.of("PENDING", "CONFIRMED").contains(a.getStatus())) {
            throw new BizException(400, "当前状态不可确认");
        }
        a.setStatus("CONFIRMED");
        appointmentMapper.updateById(a);
        log(id, "CONFIRM", null, a, "确认预约");
        messageService.send(TenantContext.getTenantId(), a.getCustomerId(), "APPOINTMENT_CONFIRM",
                "预约已确认", "预约号 " + a.getAppointmentNo() + " 已确认，请按时到店");
        return a;
    }

    public Appointment changeStatus(Long id, String status) {
        Appointment a = require(id);
        Map<String, List<String>> allowed = Map.of(
                "CONFIRMED", List.of("PENDING"),
                "ARRIVED", List.of("PENDING", "CONFIRMED"),
                "SERVING", List.of("ARRIVED"),
                "DONE", List.of("ARRIVED", "SERVING"),
                "NO_SHOW", List.of("PENDING", "CONFIRMED"),
                "CANCELLED", List.of("PENDING", "CONFIRMED")
        );
        List<String> from = allowed.getOrDefault(status, List.of());
        if (!from.contains(a.getStatus())) {
            throw new BizException(400, "预约状态不允许从 " + a.getStatus() + " 变更为 " + status);
        }
        Appointment before = copy(a);
        a.setStatus(status);
        appointmentMapper.updateById(a);
        log(id, "STATUS_" + status, before, a, null);
        if ("DONE".equals(status)) {
            onDone(a);
        }
        return a;
    }

    @Transactional
    public Appointment reschedule(Long id, String newStart, Long newTechId, Long newWorkstationId, String remark) {
        Appointment a = require(id);
        if (List.of("DONE", "CANCELLED", "NO_SHOW", "SERVING").contains(a.getStatus())) {
            throw new BizException(400, "当前预约不可改约");
        }
        Appointment before = copy(a);
        LocalDateTime start = parse(newStart);
        long minutes = Duration.between(a.getStartTime(), a.getEndTime()).toMinutes();
        LocalDateTime end = start.plusMinutes(minutes);
        Long techId = newTechId == null ? a.getTechnicianId() : newTechId;
        if (techId != null) {
            assertNoConflict(techId, id, start, end);
            assertSchedule(techId, start, end);
        }
        a.setStartTime(start);
        a.setEndTime(end);
        a.setTechnicianId(techId);
        if (newWorkstationId != null) {
            a.setWorkstationId(newWorkstationId);
        }
        if (remark != null) {
            a.setRemark(remark);
        }
        appointmentMapper.updateById(a);
        log(id, "RESCHEDULE", before, a, remark);
        messageService.send(TenantContext.getTenantId(), a.getCustomerId(), "APPOINTMENT_REMIND",
                "预约已改期", "预约号 " + a.getAppointmentNo() + " 新时间 " + start.format(FMT));
        return a;
    }

    public Appointment cancel(Long id, String reason) {
        Appointment a = require(id);
        if (List.of("DONE", "CANCELLED", "NO_SHOW", "SERVING").contains(a.getStatus())) {
            throw new BizException(400, "当前状态不可取消");
        }
        Appointment before = copy(a);
        a.setStatus("CANCELLED");
        a.setRemark(reason);
        appointmentMapper.updateById(a);
        log(id, "CANCEL", before, a, reason);
        return a;
    }

    public Appointment require(Long id) {
        Appointment a = appointmentMapper.selectById(id);
        if (a == null) {
            throw BizException.notFound("预约不存在");
        }
        return a;
    }

    private void onDone(Appointment a) {
        if (a.getTechnicianId() != null) {
            Technician t = technicianMapper.selectById(a.getTechnicianId());
            if (t != null) {
                t.setServiceCount((t.getServiceCount() == null ? 0 : t.getServiceCount()) + 1);
                t.setTotalRevenue((t.getTotalRevenue() == null ? BigDecimal.ZERO : t.getTotalRevenue())
                        .add(a.getAmount() == null ? BigDecimal.ZERO : a.getAmount()));
                t.setRotationStatus("IDLE");
                technicianMapper.updateById(t);
            }
        }
        Long exists = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .eq(Order::getAppointmentId, a.getId()));
        if (exists == 0) {
            Order o = new Order();
            o.setOrderNo(numberGenerator.next("ORD"));
            o.setBizType(a.getBizType());
            o.setCustomerId(a.getCustomerId());
            o.setStoreId(a.getStoreId());
            o.setTechnicianId(a.getTechnicianId());
            o.setAppointmentId(a.getId());
            o.setTotalAmount(a.getAmount() == null ? BigDecimal.ZERO : a.getAmount());
            o.setDiscountAmount(BigDecimal.ZERO);
            o.setPaidAmount(BigDecimal.ZERO);
            o.setStatus("PENDING");
            orderMapper.insert(o);
            OrderItem oi = new OrderItem();
            oi.setOrderId(o.getId());
            oi.setItemId(a.getItemId());
            oi.setName(a.getItemId() == null ? "服务项目" : Optional.ofNullable(itemMapper.selectById(a.getItemId()))
                    .map(ServiceItem::getName).orElse("服务项目"));
            oi.setPrice(o.getTotalAmount());
            oi.setQuantity(1);
            oi.setAmount(o.getTotalAmount());
            oi.setTechnicianId(a.getTechnicianId());
            oi.setCommissionRate(BigDecimal.ZERO);
            oi.setCommissionAmount(BigDecimal.ZERO);
            orderItemMapper.insert(oi);
        }
        messageService.send(TenantContext.getTenantId(), a.getCustomerId(), "VERIFY",
                "服务已完成", "预约号 " + a.getAppointmentNo() + " 服务完成，欢迎评价");
    }

    private void assertNoConflict(Long techId, Long excludeId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getTechnicianId, techId)
                .notIn(Appointment::getStatus, List.of("CANCELLED", "NO_SHOW"))
                .lt(Appointment::getStartTime, end)
                .gt(Appointment::getEndTime, start);
        if (excludeId != null) {
            qw.ne(Appointment::getId, excludeId);
        }
        if (appointmentMapper.selectCount(qw) > 0) {
            throw new BizException(409, "技师该时段已有预约，请选择其他时段");
        }
    }

    private void assertSchedule(Long techId, LocalDateTime start, LocalDateTime end) {
        List<Schedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getTechnicianId, techId)
                .eq(Schedule::getWorkDate, start.toLocalDate()));
        if (schedules.isEmpty()) {
            return;
        }
        boolean ok = schedules.stream().anyMatch(s -> "WORK".equals(s.getStatus())
                && !start.toLocalTime().isBefore(s.getStartTime())
                && !end.toLocalTime().isAfter(s.getEndTime()));
        if (!ok) {
            throw new BizException(400, "该时段不在技师排班内");
        }
    }

    private void publishDelay(Long apptId, LocalDateTime start) {
        try {
            long timeout = paramLong("appointment_timeout_minutes", 30);
            long delay = Duration.between(LocalDateTime.now(), start.plusMinutes(timeout)).toMillis();
            if (delay <= 0) {
                delay = 1000;
            }
            final long ttl = delay;
            MessagePostProcessor processor = message -> {
                message.getMessageProperties().setExpiration(String.valueOf(ttl));
                return message;
            };
            Map<String, Object> payload = new HashMap<>();
            payload.put("appointmentId", apptId);
            payload.put("tenantId", TenantContext.getTenantId());
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.DELAY_ROUTE, payload, processor);
        } catch (Exception e) {
            log.warn("预约超时延迟消息投递失败（定时任务兜底）: {}", e.getMessage());
        }
    }

    @Transactional
    public boolean releaseIfTimeout(Long id) {
        Appointment a = appointmentMapper.selectById(id);
        if (a == null) {
            return false;
        }
        if (!List.of("PENDING", "CONFIRMED").contains(a.getStatus())) {
            return false;
        }
        long timeout = paramLong("appointment_timeout_minutes", 30);
        if (a.getStartTime().plusMinutes(timeout).isAfter(LocalDateTime.now())) {
            return false;
        }
        a.setStatus("CANCELLED");
        a.setReleased(1);
        a.setRemark("超时未到店自动释放");
        appointmentMapper.updateById(a);
        log(id, "AUTO_RELEASE", null, a, "超时未到店自动释放");
        return true;
    }

    @Scheduled(fixedDelay = 60000)
    public void scanTimeout() {
        eachTenant(tenantId -> {
            long timeout = paramLong("appointment_timeout_minutes", 30);
            List<Appointment> list = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                    .in(Appointment::getStatus, List.of("PENDING", "CONFIRMED"))
                    .lt(Appointment::getStartTime, LocalDateTime.now().minusMinutes(timeout))
                    .last("limit 200"));
            for (Appointment a : list) {
                releaseIfTimeout(a.getId());
            }
        });
    }

    @Scheduled(fixedDelay = 300000)
    public void scanReminder() {
        eachTenant(tenantId -> {
            List<Appointment> list = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                    .in(Appointment::getStatus, List.of("PENDING", "CONFIRMED"))
                    .between(Appointment::getStartTime, LocalDateTime.now(), LocalDateTime.now().plusHours(1)));
            for (Appointment a : list) {
                String key = "appt:remind:" + tenantId + ":" + a.getId() + ":" + a.getStartTime();
                try {
                    Boolean first = redis.opsForValue().setIfAbsent(key, "1", Duration.ofHours(2));
                    if (Boolean.FALSE.equals(first)) {
                        continue;
                    }
                } catch (Exception ignored) {
                }
                messageService.send(tenantId, a.getCustomerId(), "APPOINTMENT_REMIND",
                        "预约提醒", "您预约的 " + a.getAppointmentNo() + " 将于 " + a.getStartTime().format(FMT) + " 开始，请准时到店");
            }
        });
    }

    private void eachTenant(java.util.function.Consumer<Long> consumer) {
        try {
            List<Tenant> tenants = TenantContext.runAs(null, true, () -> tenantMapper.selectList(
                    new LambdaQueryWrapper<Tenant>().in(Tenant::getStatus, List.of("NORMAL", "TRIAL"))));
            for (Tenant tenant : tenants) {
                try {
                    TenantContext.runAs(tenant.getId(), false, () -> {
                        consumer.accept(tenant.getId());
                        return null;
                    });
                } catch (Exception e) {
                    log.warn("租户 {} 预约任务失败: {}", tenant.getCode(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("预约定时任务失败: {}", e.getMessage());
        }
    }

    public List<Map<String, Object>> board(Long storeId, LocalDate date) {
        Long tid = TenantContext.requireTenantId();
        return jdbc.queryForList("select a.*, c.name customer_name, c.phone customer_phone, t.name technician_name, " +
                        "i.name item_name, w.name workstation_name from t_appointment a " +
                        "left join t_customer c on c.id=a.customer_id and c.tenant_id=a.tenant_id " +
                        "left join t_technician t on t.id=a.technician_id and t.tenant_id=a.tenant_id " +
                        "left join t_service_item i on i.id=a.item_id and i.tenant_id=a.tenant_id " +
                        "left join t_workstation w on w.id=a.workstation_id and w.tenant_id=a.tenant_id " +
                        "where a.tenant_id=? and a.deleted=0 and date(a.start_time)=?" +
                        (storeId == null ? "" : " and a.store_id=" + storeId) + " order by a.start_time",
                tid, date);
    }

    public List<Map<String, Object>> slots(Long storeId, Long itemId, String dateStr, Long techId) {
        LocalDate date = dateStr == null || dateStr.isBlank() ? LocalDate.now() : LocalDate.parse(dateStr);
        ServiceItem item = itemId == null ? null : itemMapper.selectById(itemId);
        int duration = item == null || item.getDuration() == null ? 30 : item.getDuration();
        List<Technician> techs = technicianMapper.selectList(new LambdaQueryWrapper<Technician>()
                .eq(Technician::getStatus, 1)
                .eq(Technician::getStoreId, storeId)
                .eq(techId != null, Technician::getId, techId)
                .ne(Technician::getCertStatus, "EXPIRED")
                .orderByAsc(Technician::getRotationSeq));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Technician tech : techs) {
            List<Schedule> schedules = scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                    .eq(Schedule::getTechnicianId, tech.getId())
                    .eq(Schedule::getWorkDate, date)
                    .eq(Schedule::getStatus, "WORK"));
            List<Appointment> booked = appointmentMapper.selectList(new LambdaQueryWrapper<Appointment>()
                    .eq(Appointment::getTechnicianId, tech.getId())
                    .notIn(Appointment::getStatus, List.of("CANCELLED", "NO_SHOW"))
                    .ge(Appointment::getStartTime, date.atStartOfDay())
                    .lt(Appointment::getStartTime, date.plusDays(1).atStartOfDay()));
            for (Schedule s : schedules) {
                LocalDateTime cursor = date.atTime(s.getStartTime());
                LocalDateTime limit = date.atTime(s.getEndTime());
                while (!cursor.plusMinutes(duration).isAfter(limit)) {
                    final LocalDateTime cs = cursor;
                    final LocalDateTime ce = cursor.plusMinutes(duration);
                    boolean conflict = booked.stream().anyMatch(b -> b.getStartTime().isBefore(ce) && b.getEndTime().isAfter(cs));
                    if (!conflict && cs.isAfter(LocalDateTime.now())) {
                        result.add(Map.of("time", cs.format(FMT), "technicianId", tech.getId(),
                                "technicianName", tech.getName(), "endTime", ce.format(FMT)));
                    }
                    cursor = cursor.plusMinutes(30);
                }
            }
        }
        result.sort(Comparator.comparing(m -> String.valueOf(m.get("time"))));
        return result;
    }

    public List<Technician> availableTechs(Long storeId, String bizType) {
        LambdaQueryWrapper<Technician> qw = new LambdaQueryWrapper<Technician>()
                .eq(Technician::getStatus, 1)
                .eq(storeId != null, Technician::getStoreId, storeId)
                .ne(Technician::getCertStatus, "EXPIRED")
                .orderByAsc(Technician::getRotationSeq);
        if (bizType != null && !bizType.isBlank()) {
            qw.like(Technician::getSkills, bizType);
        }
        return technicianMapper.selectList(qw);
    }

    @Transactional
    public TechRotation rotate(Long storeId, String bizType) {
        LocalDate today = LocalDate.now();
        TechRotation next = rotationMapper.selectOne(new LambdaQueryWrapper<TechRotation>()
                .eq(TechRotation::getStoreId, storeId)
                .eq(TechRotation::getRotationDate, today)
                .eq(TechRotation::getStatus, "WAITING")
                .eq(bizType != null && !bizType.isBlank(), TechRotation::getBizType, bizType)
                .orderByAsc(TechRotation::getSeq).last("limit 1"));
        if (next == null) {
            throw new BizException(404, "当前无可轮牌技师");
        }
        next.setStatus("SERVING");
        rotationMapper.updateById(next);
        Technician t = technicianMapper.selectById(next.getTechnicianId());
        if (t != null) {
            t.setRotationStatus("SERVING");
            technicianMapper.updateById(t);
        }
        return next;
    }

    private void log(Long appointmentId, String action, Appointment before, Appointment after, String remark) {
        AppointmentLog l = new AppointmentLog();
        l.setAppointmentId(appointmentId);
        l.setAction(action);
        l.setBeforeJson(before == null ? null : before.getStatus() + "|" + before.getStartTime());
        l.setAfterJson(after == null ? null : after.getStatus() + "|" + after.getStartTime());
        l.setOperator(TenantContext.user() == null ? "system" : TenantContext.user().getName());
        l.setRemark(remark);
        appointmentLogMapper.insert(l);
    }

    private Appointment copy(Appointment a) {
        Appointment c = new Appointment();
        org.springframework.beans.BeanUtils.copyProperties(a, c);
        return c;
    }

    private boolean paramBool(String key, boolean def) {
        PlatformParam p = platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, key));
        return p == null ? def : Boolean.parseBoolean(p.getParamValue());
    }

    private long paramLong(String key, long def) {
        PlatformParam p = platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, key));
        try {
            return p == null ? def : Long.parseLong(p.getParamValue());
        } catch (Exception e) {
            return def;
        }
    }

    @lombok.Data
    public static class AppointmentReq {
        private Long customerId;
        private Long storeId;
        private Long technicianId;
        private Long itemId;
        private Long packageId;
        private Long workstationId;
        private String bizType;
        private String startTime;
        private String endTime;
        private Integer duration;
        private String remark;
    }
}
