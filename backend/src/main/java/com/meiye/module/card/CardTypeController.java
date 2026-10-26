package com.meiye.module.card;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.CardType;
import com.meiye.domain.mapper.CardTypeMapper;
import com.meiye.module.tenant.BizTypeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/card-types")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER')")
@Audit(module = "卡种管理")
@RequiredArgsConstructor
public class CardTypeController extends BaseCrudController<CardType> {

    private final CardTypeMapper mapper;
    private final BizTypeGuard guard;

    @Override
    protected BaseMapper<CardType> mapper() {
        return mapper;
    }

    @Override
    @PostMapping
    @Audit(action = "新增卡种")
    public R<CardType> save(@RequestBody CardType body) {
        if (body.getBizType() != null && !body.getBizType().isBlank()) {
            guard.assertEnabled(body.getBizType());
        }
        mapper.insert(body);
        return R.ok(body);
    }
}
