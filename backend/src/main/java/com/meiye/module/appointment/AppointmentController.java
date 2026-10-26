package com.meiye.module.appointment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.security.RepeatSubmit;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Appointment;
import com.meiye.domain.entity.AppointmentLog;
import com.meiye.domain.entity.TechRotation;
import com.meiye.domain.entity.Technician;
import com.meiye.domain.mapper.AppointmentLogMapper;
import com.meiye.domain.mapper.AppointmentMapper;
import com.meiye.module.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/appointments")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','TECHNICIAN')")
@Audit(module = "预约排班")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentMapper appointmentMapper;
    private final AppointmentLogMapper logMapper;
    private final AppointmentService appointmentService;
    private final MessageService messageService;

    @GetMapping("/page")
    public R<PageResult<Appointment>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) Long technicianId,
                                           @RequestParam(required = false) String date,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate,
                                           @RequestParam(required = false) String kw) {
        LambdaQueryWrapper<Appointment> qw = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            qw.eq(Appointment::getStatus, status);
        }
        if (storeId != null) {
            qw.eq(Appointment::getStoreId, storeId);
        }
        if (technicianId != null) {
            qw.eq(Appointment::getTechnicianId, technicianId);
        }
        if (date != null && !date.isBlank()) {
            LocalDate d = LocalDate.parse(date);
            qw.ge(Appointment::getStartTime, d.atStartOfDay()).lt(Appointment::getStartTime, d.plusDays(1).atStartOfDay());
        }
        if (startDate != null && !startDate.isBlank()) {
            qw.ge(Appointment::getStartTime, AppointmentService.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            qw.le(Appointment::getStartTime, AppointmentService.parse(endDate));
        }
        if (kw != null && !kw.isBlank()) {
            qw.like(Appointment::getAppointmentNo, kw);
        }
        qw.orderByDesc(Appointment::getStartTime);
        IPage<Appointment> p = appointmentMapper.selectPage(new Page<>(page, size), qw);
        return R.ok(PageResult.of(p));
    }

    @GetMapping("/board")
    public R<List<Map<String, Object>>> board(@RequestParam(required = false) Long storeId,
                                              @RequestParam(required = false) String date) {
        return R.ok(appointmentService.board(storeId, date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date)));
    }

    @GetMapping("/slots")
    public R<List<Map<String, Object>>> slots(@RequestParam Long storeId,
                                              @RequestParam(required = false) Long itemId,
                                              @RequestParam(required = false) String date,
                                              @RequestParam(required = false) Long technicianId) {
        return R.ok(appointmentService.slots(storeId, itemId, date, technicianId));
    }

    @GetMapping("/available-techs")
    public R<List<Technician>> availableTechs(@RequestParam(required = false) Long storeId,
                                              @RequestParam(required = false) String bizType) {
        return R.ok(appointmentService.availableTechs(storeId, bizType));
    }

    @PostMapping("/create")
    @RepeatSubmit(seconds = 5)
    @Audit(action = "创建预约")
    public R<Appointment> create(@RequestBody AppointmentService.AppointmentReq req) {
        String source = "STAFF";
        if (TenantContext.user() != null && "CUSTOMER".equals(TenantContext.user().getUserType())) {
            source = "MINI";
            req.setCustomerId(TenantContext.user().getCustomerId());
        }
        return R.ok(appointmentService.create(req, source));
    }

    @PostMapping("/{id}/confirm")
    @Audit(action = "确认预约")
    public R<Appointment> confirm(@PathVariable Long id) {
        return R.ok(appointmentService.confirm(id));
    }

    @PostMapping("/{id}/arrive")
    @Audit(action = "到店")
    public R<Appointment> arrive(@PathVariable Long id) {
        return R.ok(appointmentService.changeStatus(id, "ARRIVED"));
    }

    @PostMapping("/{id}/start")
    @Audit(action = "开始服务")
    public R<Appointment> start(@PathVariable Long id) {
        return R.ok(appointmentService.changeStatus(id, "SERVING"));
    }

    @PostMapping("/{id}/done")
    @Audit(action = "完成服务")
    public R<Appointment> done(@PathVariable Long id) {
        return R.ok(appointmentService.changeStatus(id, "DONE"));
    }

    @PostMapping("/{id}/no-show")
    @Audit(action = "标记未到店")
    public R<Appointment> noShow(@PathVariable Long id) {
        return R.ok(appointmentService.changeStatus(id, "NO_SHOW"));
    }

    @PostMapping("/{id}/cancel")
    @Audit(action = "取消预约")
    public R<Appointment> cancel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return R.ok(appointmentService.cancel(id, reason));
    }

    @PostMapping("/{id}/reschedule")
    @Audit(action = "改约")
    public R<Appointment> reschedule(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String newStart = String.valueOf(body.get("startTime"));
        Long techId = body.get("technicianId") == null ? null : Long.valueOf(String.valueOf(body.get("technicianId")));
        Long wsId = body.get("workstationId") == null ? null : Long.valueOf(String.valueOf(body.get("workstationId")));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(appointmentService.reschedule(id, newStart, techId, wsId, remark));
    }

    @GetMapping("/{id}")
    public R<Appointment> get(@PathVariable Long id) {
        return R.ok(appointmentService.require(id));
    }

    @GetMapping("/{id}/logs")
    public R<List<AppointmentLog>> logs(@PathVariable Long id) {
        return R.ok(logMapper.selectList(new LambdaQueryWrapper<AppointmentLog>()
                .eq(AppointmentLog::getAppointmentId, id).orderByDesc(AppointmentLog::getId)));
    }

    @PostMapping("/{id}/remind")
    @Audit(action = "催到店提醒")
    public R<Void> remind(@PathVariable Long id) {
        Appointment a = appointmentService.require(id);
        messageService.send(TenantContext.requireTenantId(), a.getCustomerId(), "APPOINTMENT_REMIND",
                "预约提醒", "您预约的 " + a.getAppointmentNo() + " 将于 " + a.getStartTime().format(AppointmentService.FMT) + " 开始，请准时到店");
        return R.ok();
    }

    @PostMapping("/rotation/next")
    @Audit(action = "技师轮牌调度")
    public R<TechRotation> rotate(@RequestParam Long storeId, @RequestParam(required = false) String bizType) {
        return R.ok(appointmentService.rotate(storeId, bizType));
    }
}
