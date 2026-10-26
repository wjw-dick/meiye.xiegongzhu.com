package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.NumberGenerator;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.report.StatsService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/tenants")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "租户管理")
@RequiredArgsConstructor
public class PlatformTenantController extends BaseCrudController<Tenant> {

    private final TenantMapper tenantMapper;
    private final TenantConfigMapper tenantConfigMapper;
    private final TenantBizTypeMapper tenantBizTypeMapper;
    private final TenantUserMapper tenantUserMapper;
    private final BizTypePluginMapper bizTypePluginMapper;
    private final StatsService statsService;
    private final PasswordEncoder passwordEncoder;
    private final TenantExpiryJob tenantExpiryJob;

    @Override
    protected BaseMapper<Tenant> mapper() {
        return tenantMapper;
    }

    @GetMapping("/options")
    public R<List<Map<String, Object>>> options() {
        List<Map<String, Object>> list = tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                        .orderByAsc(Tenant::getId)).stream()
                .map(t -> Map.<String, Object>of("id", t.getId(), "code", t.getCode(), "name", t.getName(), "status", t.getStatus()))
                .toList();
        return R.ok(list);
    }

    @PostMapping("/create")
    @Transactional
    @Audit(action = "开通租户")
    public R<Tenant> create(@RequestBody TenantCreateReq req) {
        if (tenantMapper.selectCount(new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, req.getCode())) > 0) {
            throw new BizException(400, "租户编码已存在");
        }
        Tenant t = new Tenant();
        t.setCode(req.getCode().toUpperCase());
        t.setName(req.getName());
        t.setContact(req.getContact());
        t.setPhone(req.getPhone());
        t.setStoreLimit(req.getStoreLimit() == null ? 1 : req.getStoreLimit());
        t.setCustomerLimit(req.getCustomerLimit() == null ? 1000 : req.getCustomerLimit());
        t.setTechnicianLimit(req.getTechnicianLimit() == null ? 50 : req.getTechnicianLimit());
        t.setStatus("TRIAL");
        t.setOpenAt(LocalDateTime.now());
        t.setExpireAt(req.getExpireAt() == null ? LocalDateTime.now().plusDays(30) : req.getExpireAt());
        t.setRemark(req.getRemark());
        tenantMapper.insert(t);

        TenantConfig cfg = new TenantConfig();
        cfg.setTenantId(t.getId());
        cfg.setName(t.getName());
        cfg.setConfigJson("{\"commissionRate\":5,\"crossStore\":false}");
        tenantConfigMapper.insert(cfg);

        TenantUser admin = new TenantUser();
        admin.setTenantId(t.getId());
        admin.setUsername(req.getAdminUsername() == null ? "admin" : req.getAdminUsername());
        admin.setPassword(passwordEncoder.encode(req.getAdminPassword() == null ? "admin123" : req.getAdminPassword()));
        admin.setName(t.getName() + "管理员");
        admin.setRoleCode("TENANT_ADMIN");
        admin.setStatus(1);
        tenantUserMapper.insert(admin);

        List<String> bizTypes = req.getBizTypes();
        if (bizTypes == null || bizTypes.isEmpty()) {
            bizTypes = bizTypePluginMapper.selectList(new LambdaQueryWrapper<BizTypePlugin>()
                            .eq(BizTypePlugin::getDefaultEnabled, 1)).stream()
                    .map(BizTypePlugin::getCode).toList();
        }
        for (String code : bizTypes) {
            TenantBizType tbt = new TenantBizType();
            tbt.setTenantId(t.getId());
            tbt.setBizType(code);
            tbt.setEnabled(1);
            tbt.setEnabledAt(LocalDateTime.now());
            tbt.setInventoryEnabled(0);
            tenantBizTypeMapper.insert(tbt);
        }
        return R.ok(t);
    }

    @PostMapping("/{id}/status")
    @Audit(action = "变更租户状态")
    public R<Void> status(@PathVariable Long id, @RequestParam String status) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) {
            throw BizException.notFound("租户不存在");
        }
        if (!List.of("TRIAL", "NORMAL", "DISABLED", "EXPIRED").contains(status)) {
            throw new BizException(400, "非法状态");
        }
        t.setStatus(status);
        tenantMapper.updateById(t);
        return R.ok();
    }

    @PostMapping("/{id}/renew")
    @Audit(action = "租户续期")
    public R<Tenant> renew(@PathVariable Long id, @RequestParam(defaultValue = "365") int days) {
        Tenant t = tenantMapper.selectById(id);
        if (t == null) {
            throw BizException.notFound("租户不存在");
        }
        LocalDateTime base = t.getExpireAt() == null || t.getExpireAt().isBefore(LocalDateTime.now())
                ? LocalDateTime.now() : t.getExpireAt();
        t.setExpireAt(base.plusDays(days));
        t.setStatus("NORMAL");
        tenantMapper.updateById(t);
        return R.ok(t);
    }

    @GetMapping("/{id}/overview")
    public R<Map<String, Object>> overview(@PathVariable Long id) {
        return R.ok(statsService.overview(id));
    }

    @GetMapping("/{id}/biz-types")
    public R<List<TenantBizType>> bizTypes(@PathVariable Long id) {
        return R.ok(TenantContext.runAs(id, false, () -> tenantBizTypeMapper.selectList(
                new LambdaQueryWrapper<TenantBizType>().eq(TenantBizType::getTenantId, id))));
    }

    /** 租户到期预警：返回 N 天内到期且仍在运营的租户，供平台看板/租户管理红黄灯提示。 */
    @GetMapping("/expiring")
    public R<List<Tenant>> expiring(@RequestParam(defaultValue = "30") int days) {
        LocalDateTime now = LocalDateTime.now();
        return R.ok(tenantMapper.selectList(new LambdaQueryWrapper<Tenant>()
                .in(Tenant::getStatus, List.of("TRIAL", "NORMAL"))
                .isNotNull(Tenant::getExpireAt)
                .between(Tenant::getExpireAt, now, now.plusDays(days))
                .orderByAsc(Tenant::getExpireAt)));
    }

    /** 手动触发到期检查（运维/联调用），与每日 00:10 定时任务共用同一逻辑。 */
    @PostMapping("/expire-check")
    @Audit(action = "执行租户到期检查")
    public R<Map<String, Object>> expireCheck() {
        int count = tenantExpiryJob.autoExpire();
        return R.ok(Map.of("expired", count));
    }

    @Data
    public static class TenantCreateReq {
        private String code;
        private String name;
        private String contact;
        private String phone;
        private Integer storeLimit;
        private Integer customerLimit;
        private Integer technicianLimit;
        private LocalDateTime expireAt;
        private String remark;
        private String adminUsername;
        private String adminPassword;
        private List<String> bizTypes;
    }
}
