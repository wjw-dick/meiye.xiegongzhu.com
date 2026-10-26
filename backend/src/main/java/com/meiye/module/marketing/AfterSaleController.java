package com.meiye.module.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.AfterSale;
import com.meiye.domain.mapper.AfterSaleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tenant/after-sales")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','TECHNICIAN','CUSTOMER')")
@Audit(module = "售后返工")
@RequiredArgsConstructor
public class AfterSaleController extends BaseCrudController<AfterSale> {

    private final AfterSaleMapper mapper;

    @Override
    protected BaseMapper<AfterSale> mapper() {
        return mapper;
    }

    @PostMapping("/apply")
    @Audit(action = "提交售后申请")
    public R<AfterSale> apply(@RequestBody AfterSale body) {
        if (body.getOrderId() == null) {
            throw new BizException(400, "请选择订单");
        }
        body.setStatus("PENDING");
        mapper.insert(body);
        return R.ok(body);
    }

    @PostMapping("/{id}/handle")
    @Audit(action = "处理售后")
    public R<AfterSale> handle(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(required = false) String result,
                               @RequestParam(required = false) BigDecimal compensation) {
        AfterSale a = mapper.selectById(id);
        if (a == null) {
            throw BizException.notFound("售后单不存在");
        }
        if (!"PENDING".equals(a.getStatus())) {
            throw new BizException(400, "售后单已处理");
        }
        a.setStatus(status);
        a.setResult(result);
        a.setCompensation(compensation);
        a.setHandleBy(TenantContext.user() == null ? null : TenantContext.user().getUserId());
        mapper.updateById(a);
        return R.ok(a);
    }
}
