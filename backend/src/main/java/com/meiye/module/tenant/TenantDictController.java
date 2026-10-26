package com.meiye.module.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.TenantDict;
import com.meiye.domain.mapper.TenantDictMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant/dicts")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER')")
@Audit(module = "租户自定义字典")
@RequiredArgsConstructor
public class TenantDictController extends BaseCrudController<TenantDict> {

    private final TenantDictMapper mapper;

    @Override
    protected BaseMapper<TenantDict> mapper() {
        return mapper;
    }

    @GetMapping("/type/{type}")
    public R<List<TenantDict>> byType(@PathVariable String type) {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<TenantDict>()
                .eq(TenantDict::getDictType, type).eq(TenantDict::getStatus, 1)
                .orderByAsc(TenantDict::getSort)));
    }

    @Override
    @PostMapping
    @Audit(action = "新增自定义字典")
    public R<TenantDict> save(@RequestBody TenantDict body) {
        if (body.getDictType() == null || body.getLabel() == null) {
            throw new BizException(400, "字典类型与标签不能为空");
        }
        mapper.insert(body);
        return R.ok(body);
    }
}
