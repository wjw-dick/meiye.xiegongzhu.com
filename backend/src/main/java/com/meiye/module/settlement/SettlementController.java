package com.meiye.module.settlement;

import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Settlement;
import com.meiye.domain.entity.SettlementItem;
import com.meiye.domain.mapper.SettlementItemMapper;
import com.meiye.domain.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/settlements")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','FINANCE')")
@Audit(module = "结算分账")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final SettlementMapper settlementMapper;
    private final SettlementItemMapper settlementItemMapper;

    @GetMapping("/page")
    public R<PageResult<Settlement>> page(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Long storeId) {
        var qw = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Settlement>()
                .eq(status != null && !status.isBlank(), Settlement::getStatus, status)
                .eq(storeId != null, Settlement::getStoreId, storeId)
                .orderByDesc(Settlement::getId);
        return R.ok(PageResult.of(settlementMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), qw)));
    }

    @PostMapping("/generate")
    @Audit(action = "生成结算单")
    public R<Map<String, Object>> generate(@RequestParam(required = false) String periodType,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
                                           @RequestParam(required = false) Long storeId) {
        return R.ok(settlementService.generate(periodType, start, end, storeId));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(settlementService.detail(id));
    }

    @GetMapping("/{id}/items")
    public R<List<SettlementItem>> items(@PathVariable Long id) {
        return R.ok(settlementItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SettlementItem>()
                        .eq(SettlementItem::getSettlementId, id)));
    }

    @PostMapping("/{id}/confirm")
    @Audit(action = "确认结算单")
    public R<Settlement> confirm(@PathVariable Long id) {
        return R.ok(settlementService.confirm(id));
    }

    @PostMapping("/{id}/pay")
    @Audit(action = "结算单打款")
    public R<Settlement> pay(@PathVariable Long id) {
        return R.ok(settlementService.pay(id));
    }

    @GetMapping("/tech-income")
    public R<Map<String, Object>> techIncome(@RequestParam(required = false) Long techId) {
        return R.ok(settlementService.techIncome(techId));
    }

    @GetMapping("/report")
    public R<Map<String, Object>> report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return R.ok(settlementService.tenantSettlementReport(start, end));
    }

    @DeleteMapping("/{id}")
    @Audit(action = "删除结算单")
    public R<Void> delete(@PathVariable Long id) {
        Settlement s = settlementMapper.selectById(id);
        if (s != null && !"DRAFT".equals(s.getStatus())) {
            return R.fail(400, "仅草稿结算单可删除");
        }
        settlementItemMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SettlementItem>()
                .eq(SettlementItem::getSettlementId, id));
        settlementMapper.deleteById(id);
        return R.ok();
    }
}
