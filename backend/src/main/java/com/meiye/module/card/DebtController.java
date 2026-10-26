package com.meiye.module.card;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Customer;
import com.meiye.domain.entity.Debt;
import com.meiye.domain.mapper.CustomerMapper;
import com.meiye.domain.mapper.DebtMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/tenant/debts")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','FINANCE')")
@Audit(module = "欠款管理")
@RequiredArgsConstructor
public class DebtController extends BaseCrudController<Debt> {

    private final DebtMapper mapper;
    private final CustomerMapper customerMapper;

    @Override
    protected BaseMapper<Debt> mapper() {
        return mapper;
    }

    @PostMapping("/{id}/repay")
    @Audit(action = "欠款还款")
    public R<Debt> repay(@PathVariable Long id, @RequestParam BigDecimal amount) {
        Debt d = mapper.selectById(id);
        if (d == null) {
            throw BizException.notFound("欠款记录不存在");
        }
        BigDecimal paid = (d.getPaidAmount() == null ? BigDecimal.ZERO : d.getPaidAmount()).add(amount);
        if (paid.compareTo(d.getAmount()) > 0) {
            throw new BizException(400, "还款金额超过欠款");
        }
        d.setPaidAmount(paid);
        d.setStatus(paid.compareTo(d.getAmount()) == 0 ? "PAID" : "PARTIAL");
        mapper.updateById(d);
        Customer c = customerMapper.selectById(d.getCustomerId());
        if (c != null) {
            c.setDebt(c.getDebt().subtract(amount).max(BigDecimal.ZERO));
            customerMapper.updateById(c);
        }
        return R.ok(d);
    }

    @GetMapping("/customer/{customerId}")
    public R<java.util.List<Debt>> byCustomer(@PathVariable Long customerId) {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<Debt>().eq(Debt::getCustomerId, customerId)));
    }
}
