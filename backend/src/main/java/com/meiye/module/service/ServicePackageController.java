package com.meiye.module.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.ServicePackage;
import com.meiye.domain.mapper.ServicePackageMapper;
import com.meiye.module.tenant.BizTypeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/service-packages")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER')")
@Audit(module = "套餐管理")
@RequiredArgsConstructor
public class ServicePackageController extends BaseCrudController<ServicePackage> {

    private final ServicePackageMapper mapper;
    private final BizTypeGuard guard;

    @Override
    protected BaseMapper<ServicePackage> mapper() {
        return mapper;
    }

    @Override
    @PostMapping
    @Audit(action = "新增套餐")
    public R<ServicePackage> save(@RequestBody ServicePackage body) {
        guard.assertEnabled(body.getBizType());
        mapper.insert(body);
        return R.ok(body);
    }
}
