package com.meiye.module.settlement;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.PlatformParam;
import com.meiye.domain.entity.Withdrawal;
import com.meiye.domain.mapper.PlatformParamMapper;
import com.meiye.domain.mapper.WithdrawalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/withdrawals")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','FINANCE','TECHNICIAN')")
@Audit(module = "技师提现")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalMapper mapper;
    private final SettlementService settlementService;
    private final PlatformParamMapper platformParamMapper;

    @GetMapping("/page")
    public R<PageResult<Withdrawal>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long techId) {
        LambdaQueryWrapper<Withdrawal> qw = new LambdaQueryWrapper<Withdrawal>()
                .eq(status != null && !status.isBlank(), Withdrawal::getStatus, status)
                .eq(techId != null, Withdrawal::getTechId, techId)
                .orderByDesc(Withdrawal::getId);
        if ("TECHNICIAN".equals(TenantContext.userType()) && TenantContext.user() != null) {
            qw.eq(Withdrawal::getTechId, TenantContext.user().getTechnicianId());
        }
        return R.ok(PageResult.of(mapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), qw)));
    }

    @GetMapping("/my")
    public R<Map<String, Object>> my() {
        Long techId = TenantContext.user() == null ? null : TenantContext.user().getTechnicianId();
        return R.ok(settlementService.techIncome(techId));
    }

    @PostMapping("/apply")
    @Audit(action = "申请提现")
    public R<Withdrawal> apply(@RequestBody Map<String, Object> body) {
        Long techId = body.get("techId") == null ? null : Long.valueOf(String.valueOf(body.get("techId")));
        if ("TECHNICIAN".equals(TenantContext.userType())) {
            techId = TenantContext.user().getTechnicianId();
        }
        if (techId == null) {
            throw new BizException(400, "缺少技师");
        }
        BigDecimal amount = new BigDecimal(String.valueOf(body.getOrDefault("amount", "0")));
        if (amount.signum() <= 0) {
            throw new BizException(400, "提现金额必须大于 0");
        }
        Map<String, Object> income = settlementService.techIncome(techId);
        BigDecimal available = (BigDecimal) income.get("available");
        if (amount.compareTo(available) > 0) {
            throw new BizException(400, "可提现余额不足，当前可提现 " + available);
        }
        BigDecimal feeRate = feeRate();
        BigDecimal fee = amount.multiply(feeRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        Withdrawal w = new Withdrawal();
        w.setTechId(techId);
        w.setAmount(amount);
        w.setFee(fee);
        w.setActualAmount(amount.subtract(fee));
        w.setStatus("APPLIED");
        w.setRemark(body.get("remark") == null ? null : String.valueOf(body.get("remark")));
        mapper.insert(w);
        return R.ok(w);
    }

    @PostMapping("/{id}/approve")
    @Audit(action = "审核提现")
    public R<Withdrawal> approve(@PathVariable Long id) {
        Withdrawal w = require(id);
        if (!"APPLIED".equals(w.getStatus())) {
            throw new BizException(400, "仅待审核提现可审核");
        }
        w.setStatus("APPROVED");
        w.setReviewedBy(TenantContext.user() == null ? null : TenantContext.user().getUserId());
        mapper.updateById(w);
        return R.ok(w);
    }

    @PostMapping("/{id}/reject")
    @Audit(action = "驳回提现")
    public R<Withdrawal> reject(@PathVariable Long id, @RequestParam(required = false) String remark) {
        Withdrawal w = require(id);
        if (!"APPLIED".equals(w.getStatus())) {
            throw new BizException(400, "仅待审核提现可驳回");
        }
        w.setStatus("REJECTED");
        w.setRemark(remark);
        w.setReviewedBy(TenantContext.user() == null ? null : TenantContext.user().getUserId());
        mapper.updateById(w);
        return R.ok(w);
    }

    @PostMapping("/{id}/pay")
    @Audit(action = "提现打款")
    public R<Withdrawal> pay(@PathVariable Long id) {
        Withdrawal w = require(id);
        if (!"APPROVED".equals(w.getStatus())) {
            throw new BizException(400, "仅已审核提现可打款");
        }
        w.setStatus("PAID");
        w.setPaidAt(LocalDateTime.now());
        mapper.updateById(w);
        return R.ok(w);
    }

    private Withdrawal require(Long id) {
        Withdrawal w = mapper.selectById(id);
        if (w == null) {
            throw BizException.notFound("提现记录不存在");
        }
        return w;
    }

    private BigDecimal feeRate() {
        try {
            PlatformParam p = TenantContext.runAs(null, true,
                    () -> platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                            .eq(PlatformParam::getParamKey, "withdrawal_fee_rate").last("limit 1")));
            if (p != null && p.getParamValue() != null && !p.getParamValue().isBlank()) {
                return new BigDecimal(p.getParamValue());
            }
        } catch (Exception ignored) {
        }
        return new BigDecimal("1");
    }
}
