package com.meiye.module.platform;

import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.R;
import com.meiye.domain.entity.MenuConfig;
import com.meiye.domain.mapper.MenuConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/menus")
@Audit(module = "菜单可视化")
@RequiredArgsConstructor
public class MenuConfigController {

    private final MenuConfigMapper mapper;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<MenuConfig> get(@RequestParam String target,
                             @RequestParam(required = false) Long tenantId) {
        Long tid = tenantId != null ? tenantId : defaultTenant();
        MenuConfig mc = TenantContext.runAs(tid, tid == 0, () -> mapper.selectOne(
                new LambdaQueryWrapper<MenuConfig>()
                        .eq(MenuConfig::getTenantId, tid)
                        .eq(MenuConfig::getTarget, target)
                        .last("limit 1")));
        return R.ok(mc);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN')")
    @Audit(action = "保存菜单配置")
    public R<MenuConfig> save(@RequestBody Map<String, Object> body) {
        String target = String.valueOf(body.get("target"));
        Long tid = body.get("tenantId") == null ? defaultTenant() : Long.valueOf(String.valueOf(body.get("tenantId")));
        String content = String.valueOf(body.get("content"));
        MenuConfig mc = TenantContext.runAs(tid, tid == 0, () -> mapper.selectOne(
                new LambdaQueryWrapper<MenuConfig>()
                        .eq(MenuConfig::getTenantId, tid)
                        .eq(MenuConfig::getTarget, target).last("limit 1")));
        if (mc == null) {
            mc = new MenuConfig();
            mc.setTenantId(tid);
            mc.setTarget(target);
            mc.setContent(content);
            mapper.insert(mc);
        } else {
            mc.setContent(content);
            mapper.updateById(mc);
        }
        return R.ok(mc);
    }

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
    public R<List<MenuConfig>> list() {
        return R.ok(TenantContext.runAs(null, true, () -> mapper.selectList(null)));
    }

    private Long defaultTenant() {
        if (TenantContext.isPlatform()) {
            return 0L;
        }
        return TenantContext.requireTenantId();
    }
}
