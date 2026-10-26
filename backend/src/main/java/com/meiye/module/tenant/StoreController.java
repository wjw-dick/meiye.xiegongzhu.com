package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Store;
import com.meiye.domain.entity.Tenant;
import com.meiye.domain.mapper.StoreMapper;
import com.meiye.domain.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/stores")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER')")
@Audit(module = "门店管理")
@RequiredArgsConstructor
public class StoreController extends BaseCrudController<Store> {

    private final StoreMapper mapper;
    private final TenantMapper tenantMapper;

    @Override
    protected BaseMapper<Store> mapper() {
        return mapper;
    }

    @Override
    @PostMapping
    @Audit(action = "新增门店")
    public R<Store> save(@RequestBody Store body) {
        Long tid = TenantContext.requireTenantId();
        Tenant tenant = TenantContext.runAs(tid, true, () -> tenantMapper.selectById(tid));
        long count = mapper.selectCount(null);
        if (tenant != null && tenant.getStoreLimit() != null && count >= tenant.getStoreLimit()) {
            throw BizException.forbidden("门店数已达租户配额上限（" + tenant.getStoreLimit() + "）");
        }
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BizException(400, "门店名称不能为空");
        }
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        mapper.insert(body);
        return R.ok(body);
    }
}
