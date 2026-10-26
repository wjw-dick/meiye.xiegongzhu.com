package com.meiye.module.technician;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.crypto.AesKeyHolder;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.ExcelUtil;
import com.meiye.common.util.NumberGenerator;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tenant/technicians")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER')")
@Audit(module = "技师档案")
@RequiredArgsConstructor
public class TechnicianController extends BaseCrudController<Technician> {

    private final TechnicianMapper technicianMapper;
    private final TechnicianCertMapper certMapper;
    private final ScheduleMapper scheduleMapper;
    private final TechRotationMapper rotationMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ReviewMapper reviewMapper;
    private final TechServiceLogMapper serviceLogMapper;
    private final AppointmentMapper appointmentMapper;
    private final NumberGenerator numberGenerator;
    private final PasswordEncoder passwordEncoder;
    private final TenantMapper tenantMapper;

    @Override
    protected BaseMapper<Technician> mapper() {
        return technicianMapper;
    }

    /** 技师数配额校验：平台开通租户时配置的 technician_limit。 */
    private void assertTechnicianQuota() {
        Long tid = TenantContext.requireTenantId();
        Tenant tenant = tenantMapper.selectById(tid);
        if (tenant == null || tenant.getTechnicianLimit() == null) {
            return;
        }
        long count = technicianMapper.selectCount(new LambdaQueryWrapper<Technician>()
                .eq(Technician::getTenantId, tid));
        if (count >= tenant.getTechnicianLimit()) {
            throw BizException.forbidden("技师数已达租户配额上限（" + tenant.getTechnicianLimit() + "）");
        }
    }

    @Override
    @GetMapping("/page")
    public R<PageResult<Technician>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String sort,
                                          @RequestParam Map<String, Object> params) {
        QueryWrapper<Technician> qw = new QueryWrapper<>();
        applyEq(qw, params, "storeId", "store_id");
        applyEq(qw, params, "status", "status");
        applyEq(qw, params, "position", "position");
        applyEq(qw, params, "certStatus", "cert_status");
        Object kw = params.get("kw");
        if (kw != null && !String.valueOf(kw).isBlank()) {
            String k = String.valueOf(kw).trim();
            qw.and(w -> {
                w.like("name", k).or().like("tech_no", k);
                if (k.matches("\\d{6,}")) {
                    w.or().eq("phone", AesKeyHolder.encrypt(k)).or().eq("phone", k);
                }
            });
        }
        qw.orderByDesc("id");
        return R.ok(PageResult.of(technicianMapper.selectPage(new Page<>(page, size), qw)));
    }

    @Override
    @PostMapping
    @Audit(action = "新增技师")
    public R<Technician> save(@RequestBody Technician body) {
        validate(body);
        checkIdCard(body.getIdCard(), null);
        assertTechnicianQuota();
        body.setTechNo(numberGenerator.next("TECH"));
        body.setCertStatus("OK");
        body.setRotationStatus("IDLE");
        body.setRating(body.getRating() == null ? new BigDecimal("5.00") : body.getRating());
        body.setServiceCount(body.getServiceCount() == null ? 0 : body.getServiceCount());
        body.setTotalRevenue(body.getTotalRevenue() == null ? BigDecimal.ZERO : body.getTotalRevenue());
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        if (body.getPassword() == null || body.getPassword().isBlank()) {
            body.setPassword(passwordEncoder.encode("123456"));
        } else {
            body.setPassword(passwordEncoder.encode(body.getPassword()));
        }
        technicianMapper.insert(body);
        return R.ok(body);
    }

    @Override
    @PutMapping
    @Audit(action = "修改技师")
    public R<Technician> update(@RequestBody Technician body) {
        validate(body);
        checkIdCard(body.getIdCard(), body.getId());
        body.setPassword(null);
        technicianMapper.updateById(body);
        return R.ok(body);
    }

    @PostMapping("/{id}/disable")
    @Audit(action = "禁用技师")
    public R<Void> disable(@PathVariable Long id) {
        Technician t = technicianMapper.selectById(id);
        t.setStatus(0);
        technicianMapper.updateById(t);
        return R.ok();
    }

    @GetMapping("/{id}/certs")
    public R<List<TechnicianCert>> certs(@PathVariable Long id) {
        return R.ok(certMapper.selectList(new LambdaQueryWrapper<TechnicianCert>()
                .eq(TechnicianCert::getTechnicianId, id).orderByAsc(TechnicianCert::getExpireAt)));
    }

    @PostMapping("/{id}/certs")
    @Audit(action = "新增证书")
    public R<TechnicianCert> addCert(@PathVariable Long id, @RequestBody TechnicianCert cert) {
        cert.setTechnicianId(id);
        certMapper.insert(cert);
        refreshCertStatus(id);
        return R.ok(cert);
    }

    @PutMapping("/{id}/certs/{certId}")
    @Audit(action = "修改证书")
    public R<TechnicianCert> updateCert(@PathVariable Long id, @PathVariable Long certId, @RequestBody TechnicianCert cert) {
        cert.setId(certId);
        cert.setTechnicianId(id);
        certMapper.updateById(cert);
        refreshCertStatus(id);
        return R.ok(cert);
    }

    @DeleteMapping("/{id}/certs/{certId}")
    @Audit(action = "删除证书")
    public R<Void> deleteCert(@PathVariable Long id, @PathVariable Long certId) {
        certMapper.deleteById(certId);
        refreshCertStatus(id);
        return R.ok();
    }

    @GetMapping("/{id}/schedules")
    public R<List<Schedule>> schedules(@PathVariable Long id,
                                       @RequestParam(required = false) String startDate,
                                       @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<Schedule> qw = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getTechnicianId, id).orderByAsc(Schedule::getWorkDate);
        if (startDate != null && !startDate.isBlank()) {
            qw.ge(Schedule::getWorkDate, LocalDate.parse(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            qw.le(Schedule::getWorkDate, LocalDate.parse(endDate));
        }
        return R.ok(scheduleMapper.selectList(qw));
    }

    @PostMapping("/{id}/schedules")
    @Audit(action = "新增排班")
    public R<Schedule> addSchedule(@PathVariable Long id, @RequestBody Schedule schedule) {
        schedule.setTechnicianId(id);
        scheduleMapper.insert(schedule);
        return R.ok(schedule);
    }

    @PutMapping("/{id}/schedules/{scheduleId}")
    @Audit(action = "修改排班")
    public R<Schedule> updateSchedule(@PathVariable Long id, @PathVariable Long scheduleId, @RequestBody Schedule schedule) {
        schedule.setId(scheduleId);
        schedule.setTechnicianId(id);
        scheduleMapper.updateById(schedule);
        return R.ok(schedule);
    }

    @DeleteMapping("/{id}/schedules/{scheduleId}")
    @Audit(action = "删除排班")
    public R<Void> deleteSchedule(@PathVariable Long id, @PathVariable Long scheduleId) {
        scheduleMapper.deleteById(scheduleId);
        return R.ok();
    }

    @GetMapping("/rotation")
    public R<List<TechRotation>> rotation(@RequestParam(required = false) Long storeId,
                                          @RequestParam(required = false) String date,
                                          @RequestParam(required = false) String bizType) {
        LambdaQueryWrapper<TechRotation> qw = new LambdaQueryWrapper<TechRotation>()
                .eq(TechRotation::getRotationDate, date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date))
                .orderByAsc(TechRotation::getSeq);
        if (storeId != null) {
            qw.eq(TechRotation::getStoreId, storeId);
        }
        if (bizType != null && !bizType.isBlank()) {
            qw.eq(TechRotation::getBizType, bizType);
        }
        return R.ok(rotationMapper.selectList(qw));
    }

    @PostMapping("/rotation")
    @Audit(action = "维护轮牌")
    public R<TechRotation> saveRotation(@RequestBody TechRotation rotation) {
        if (rotation.getRotationDate() == null) {
            rotation.setRotationDate(LocalDate.now());
        }
        rotationMapper.insert(rotation);
        return R.ok(rotation);
    }

    @PostMapping("/rotation/{id}/status")
    @Audit(action = "更新轮牌状态")
    public R<Void> rotationStatus(@PathVariable Long id, @RequestParam String status) {
        TechRotation r = rotationMapper.selectById(id);
        if (r == null) {
            throw BizException.notFound("轮牌不存在");
        }
        r.setStatus(status);
        rotationMapper.updateById(r);
        return R.ok();
    }

    @GetMapping("/{id}/stats")
    public R<Map<String, Object>> stats(@PathVariable Long id) {
        Technician t = technicianMapper.selectById(id);
        if (t == null) {
            throw BizException.notFound("技师不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("technician", t);
        m.put("certs", certMapper.selectList(new LambdaQueryWrapper<TechnicianCert>()
                .eq(TechnicianCert::getTechnicianId, id)));
        m.put("todayAppointments", appointmentMapper.selectCount(new LambdaQueryWrapper<Appointment>()
                .eq(Appointment::getTechnicianId, id).ge(Appointment::getStartTime, LocalDate.now().atStartOfDay())
                .lt(Appointment::getStartTime, LocalDate.now().plusDays(1).atStartOfDay())));
        m.put("reviews", reviewMapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getTechnicianId, id).orderByDesc(Review::getId).last("limit 10")));
        m.put("logs", serviceLogMapper.selectList(new LambdaQueryWrapper<TechServiceLog>()
                .eq(TechServiceLog::getTechnicianId, id).orderByDesc(TechServiceLog::getId).last("limit 20")));
        m.put("schedules", scheduleMapper.selectList(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getTechnicianId, id).ge(Schedule::getWorkDate, LocalDate.now())
                .orderByAsc(Schedule::getWorkDate).last("limit 14")));
        return R.ok(m);
    }

    @GetMapping("/{id}/logs")
    public R<List<TechServiceLog>> logs(@PathVariable Long id) {
        return R.ok(serviceLogMapper.selectList(new LambdaQueryWrapper<TechServiceLog>()
                .eq(TechServiceLog::getTechnicianId, id).orderByDesc(TechServiceLog::getId)));
    }

    @PostMapping("/{id}/logs")
    @Audit(action = "新增服务日志")
    public R<TechServiceLog> addLog(@PathVariable Long id, @RequestBody TechServiceLog log) {
        log.setTechnicianId(id);
        serviceLogMapper.insert(log);
        return R.ok(log);
    }

    @GetMapping("/export")
    @Audit(action = "导出技师")
    public void export(HttpServletResponse response) {
        List<Technician> list = technicianMapper.selectList(new LambdaQueryWrapper<Technician>().orderByDesc(Technician::getId));
        List<List<Object>> rows = new ArrayList<>();
        for (Technician t : list) {
            rows.add(Arrays.asList(t.getTechNo(), t.getName(), t.getPhone(), t.getPosition(), t.getSkills(),
                    t.getCertStatus(), t.getRating(), t.getServiceCount(), t.getTotalRevenue(), t.getStatus() == 1 ? "在职" : "停用"));
        }
        ExcelUtil.write(response, "technicians.xlsx", "技师档案",
                List.of("人员编号", "姓名", "手机号", "岗位", "技能标签", "证书状态", "评分", "服务次数", "累计业绩", "状态"), rows);
    }

    @PostMapping("/import")
    @Audit(action = "导入技师")
    public R<Map<String, Object>> importTech(@RequestParam("file") MultipartFile file) {
        List<Map<String, String>> rows = ExcelUtil.read(file);
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                String name = row.getOrDefault("姓名", row.get("name"));
                String idCard = row.getOrDefault("身份证号", row.get("idCard"));
                String phone = row.getOrDefault("手机号", row.get("phone"));
                if (name == null || name.isBlank() || idCard == null || idCard.isBlank()) {
                    throw new BizException(400, "姓名/身份证号不能为空");
                }
                checkIdCard(idCard, null);
                Technician t = new Technician();
                t.setName(name);
                t.setIdCard(idCard);
                t.setPhone(phone);
                t.setPosition(row.get("岗位"));
                t.setSkills(row.get("技能标签"));
                t.setTechNo(numberGenerator.next("TECH"));
                t.setCertStatus("OK");
                t.setRotationStatus("IDLE");
                t.setRating(new BigDecimal("5.00"));
                t.setServiceCount(0);
                t.setTotalRevenue(BigDecimal.ZERO);
                t.setStatus(1);
                t.setPassword(passwordEncoder.encode("123456"));
                technicianMapper.insert(t);
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 2) + "行：" + e.getMessage());
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("success", success);
        r.put("failed", errors.size());
        r.put("errors", errors.stream().limit(20).toList());
        return R.ok(r);
    }

    public void refreshCertStatus(Long techId) {
        List<TechnicianCert> certs = certMapper.selectList(new LambdaQueryWrapper<TechnicianCert>()
                .eq(TechnicianCert::getTechnicianId, techId));
        Technician t = technicianMapper.selectById(techId);
        if (t == null) {
            return;
        }
        if (certs.isEmpty()) {
            t.setCertStatus("OK");
        } else {
            boolean expired = false;
            boolean warn = false;
            LocalDate warnLine = LocalDate.now().plusDays(30);
            for (TechnicianCert c : certs) {
                if (c.getExpireAt() == null) {
                    continue;
                }
                if (c.getExpireAt().isBefore(LocalDate.now())) {
                    expired = true;
                } else if (c.getExpireAt().isBefore(warnLine)) {
                    warn = true;
                }
            }
            t.setCertStatus(expired ? "EXPIRED" : warn ? "WARN" : "OK");
        }
        technicianMapper.updateById(t);
    }

    private void validate(Technician body) {
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BizException(400, "技师姓名不能为空");
        }
        if (body.getIdCard() == null || body.getIdCard().length() < 15) {
            throw new BizException(400, "身份证号格式不正确");
        }
    }

    private void checkIdCard(String idCard, Long excludeId) {
        LambdaQueryWrapper<Technician> qw = new LambdaQueryWrapper<Technician>()
                .and(w -> w.eq(Technician::getIdCard, AesKeyHolder.encrypt(idCard)).or().eq(Technician::getIdCard, idCard));
        if (excludeId != null) {
            qw.ne(Technician::getId, excludeId);
        }
        if (technicianMapper.selectCount(qw) > 0) {
            throw new BizException(400, "身份证号已存在（租户内唯一）");
        }
    }

    private void applyEq(QueryWrapper<Technician> qw, Map<String, Object> p, String key, String col) {
        if (p.get(key) != null && !String.valueOf(p.get(key)).isBlank()) {
            qw.eq(col, p.get(key));
        }
    }
}
