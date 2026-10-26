package com.meiye.module.service;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.ServiceItem;
import com.meiye.domain.mapper.ServiceItemMapper;
import com.meiye.module.tenant.BizTypeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/service-items")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER')")
@Audit(module = "服务项目")
@RequiredArgsConstructor
public class ServiceItemController extends BaseCrudController<ServiceItem> {

    private final ServiceItemMapper mapper;
    private final BizTypeGuard guard;

    @Override
    protected BaseMapper<ServiceItem> mapper() {
        return mapper;
    }

    @Override
    @PostMapping
    @Audit(action = "新增服务项目")
    public R<ServiceItem> save(@RequestBody ServiceItem body) {
        guard.assertEnabled(body.getBizType());
        mapper.insert(body);
        return R.ok(body);
    }

    @Override
    @PutMapping
    @Audit(action = "修改服务项目")
    public R<ServiceItem> update(@RequestBody ServiceItem body) {
        guard.assertEnabled(body.getBizType());
        mapper.updateById(body);
        return R.ok(body);
    }

    @PostMapping("/{id}/status")
    @Audit(action = "上下架服务项目")
    public R<Void> status(@PathVariable Long id, @RequestParam Integer status) {
        ServiceItem item = mapper.selectById(id);
        item.setStatus(status);
        mapper.updateById(item);
        return R.ok();
    }
}
