package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.R;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.report.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tenant")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','MARKETING','FINANCE')")
@Audit(module = "租户配置")
@RequiredArgsConstructor
public class TenantProfileController {

    private final TenantMapper tenantMapper;
    private final TenantConfigMapper tenantConfigMapper;
    private final TenantBizTypeMapper tenantBizTypeMapper;
    private final BizTypePluginMapper bizTypePluginMapper;
    private final BizTypeGuard bizTypeGuard;
    private final StatsService statsService;
    private final ObjectMapper objectMapper;

    @GetMapping("/profile")
    public R<Map<String, Object>> profile() {
        Long tid = TenantContext.requireTenantId();
        Tenant tenant = TenantContext.runAs(tid, true, () -> tenantMapper.selectById(tid));
        TenantConfig cfg = tenantConfigMapper.selectOne(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tid).last("limit 1"));
        List<Map<String, Object>> bizTypes = bizTypeList();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tenant", tenant);
        m.put("config", cfg);
        m.put("configMap", parse(cfg == null ? null : cfg.getConfigJson()));
        m.put("bizTypes", bizTypes);
        return R.ok(m);
    }

    @GetMapping("/dashboard")
    public R<Map<String, Object>> dashboard() {
        return R.ok(statsService.tenantDashboard());
    }

    @PutMapping("/profile")
    @Audit(action = "保存租户配置")
    public R<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        Long tid = TenantContext.requireTenantId();
        TenantConfig cfg = tenantConfigMapper.selectOne(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tid).last("limit 1"));
        if (cfg == null) {
            cfg = new TenantConfig();
            cfg.setTenantId(tid);
        }
        if (body.get("name") != null) {
            cfg.setName(String.valueOf(body.get("name")));
        }
        if (body.get("logo") != null) {
            cfg.setLogo(String.valueOf(body.get("logo")));
        }
        if (body.get("servicePhone") != null) {
            cfg.setServicePhone(String.valueOf(body.get("servicePhone")));
        }
        Map<String, Object> existing = parse(cfg.getConfigJson());
        if (body.get("config") instanceof Map<?, ?> incoming) {
            incoming.forEach((k, v) -> existing.put(String.valueOf(k), v));
        }
        try {
            cfg.setConfigJson(objectMapper.writeValueAsString(existing));
        } catch (Exception e) {
            throw new BizException(400, "配置格式错误");
        }
        if (cfg.getId() == null) {
            tenantConfigMapper.insert(cfg);
        } else {
            tenantConfigMapper.updateById(cfg);
        }
        return R.ok(Map.of("config", cfg, "configMap", existing));
    }

    @GetMapping("/biz-types")
    public R<List<Map<String, Object>>> bizTypes() {
        return R.ok(bizTypeList());
    }

    @PostMapping("/biz-types/{bizType}/toggle")
    @Audit(action = "品类开关")
    public R<Map<String, Object>> toggle(@PathVariable String bizType, @RequestParam Integer enabled) {
        Long tid = TenantContext.requireTenantId();
        BizTypePlugin plugin = bizTypePluginMapper.selectOne(new LambdaQueryWrapper<BizTypePlugin>()
                .eq(BizTypePlugin::getCode, bizType).last("limit 1"));
        if (plugin == null || plugin.getStatus() == null || plugin.getStatus() != 1) {
            throw new BizException(404, "品类插件不存在或已停用");
        }
        TenantBizType tbt = tenantBizTypeMapper.selectOne(new LambdaQueryWrapper<TenantBizType>()
                .eq(TenantBizType::getBizType, bizType).last("limit 1"));
        if (tbt == null) {
            tbt = new TenantBizType();
            tbt.setTenantId(tid);
            tbt.setBizType(bizType);
            tbt.setEnabled(enabled);
            tbt.setEnabledAt(enabled == 1 ? LocalDateTime.now() : null);
            tbt.setInventoryEnabled(0);
            tenantBizTypeMapper.insert(tbt);
        } else {
            tbt.setEnabled(enabled);
            tbt.setEnabledAt(enabled == 1 ? LocalDateTime.now() : tbt.getEnabledAt());
            tenantBizTypeMapper.updateById(tbt);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("bizType", bizType);
        resp.put("enabled", enabled);
        resp.put("medical", plugin.getMedical());
        resp.put("warning", plugin.getMedical() == 1 ? "医疗类目，请确保已取得医疗机构执业许可等资质" : null);
        return R.ok(resp);
    }

    @PutMapping("/biz-types/{bizType}")
    @Audit(action = "保存品类配置")
    public R<TenantBizType> config(@PathVariable String bizType, @RequestBody TenantBizType body) {
        TenantBizType tbt = tenantBizTypeMapper.selectOne(new LambdaQueryWrapper<TenantBizType>()
                .eq(TenantBizType::getBizType, bizType).last("limit 1"));
        if (tbt == null) {
            throw new BizException(404, "请先启用品类");
        }
        tbt.setInventoryEnabled(body.getInventoryEnabled());
        tbt.setCommissionOverride(body.getCommissionOverride());
        tenantBizTypeMapper.updateById(tbt);
        return R.ok(tbt);
    }

    private List<Map<String, Object>> bizTypeList() {
        List<BizTypePlugin> plugins = bizTypePluginMapper.selectList(new LambdaQueryWrapper<BizTypePlugin>()
                .eq(BizTypePlugin::getStatus, 1).orderByAsc(BizTypePlugin::getSort));
        List<TenantBizType> enabled = tenantBizTypeMapper.selectList(null);
        Map<String, TenantBizType> map = new HashMap<>();
        enabled.forEach(e -> map.put(e.getBizType(), e));
        List<Map<String, Object>> result = new ArrayList<>();
        for (BizTypePlugin p : plugins) {
            TenantBizType t = map.get(p.getCode());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", p.getCode());
            m.put("name", p.getName());
            m.put("icon", p.getIcon());
            m.put("medical", p.getMedical());
            m.put("defaultEnabled", p.getDefaultEnabled());
            m.put("subModules", p.getSubModules());
            m.put("fieldTemplate", p.getFieldTemplate());
            m.put("defaultCommission", p.getDefaultCommission());
            m.put("enabled", t != null && t.getEnabled() == 1);
            m.put("enabledAt", t == null ? null : t.getEnabledAt());
            m.put("inventoryEnabled", t != null && t.getInventoryEnabled() == 1);
            m.put("commissionOverride", t == null ? null : t.getCommissionOverride());
            result.add(m);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
