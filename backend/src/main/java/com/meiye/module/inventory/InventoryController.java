package com.meiye.module.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.InventoryItem;
import com.meiye.domain.entity.StockFlow;
import com.meiye.domain.mapper.InventoryItemMapper;
import com.meiye.domain.mapper.StockFlowMapper;
import com.meiye.module.tenant.BizTypeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/inventory")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER')")
@Audit(module = "耗材库存")
@RequiredArgsConstructor
public class InventoryController extends BaseCrudController<InventoryItem> {

    private final InventoryItemMapper mapper;
    private final StockFlowMapper flowMapper;
    private final BizTypeGuard bizTypeGuard;

    @Override
    protected BaseMapper<InventoryItem> mapper() {
        return mapper;
    }

    @Override
    @PostMapping
    @Audit(action = "新增库存品")
    public R<InventoryItem> save(@RequestBody InventoryItem body) {
        if (body.getBizType() != null) {
            bizTypeGuard.assertEnabled(body.getBizType());
        }
        mapper.insert(body);
        return R.ok(body);
    }

    @PostMapping("/{id}/in")
    @Transactional
    @Audit(action = "库存入库")
    public R<Map<String, Object>> stockIn(@PathVariable Long id,
                                          @RequestParam BigDecimal quantity,
                                          @RequestParam(required = false) BigDecimal cost,
                                          @RequestParam(required = false) String remark) {
        return change(id, quantity, "IN", cost, remark);
    }

    @PostMapping("/{id}/out")
    @Transactional
    @Audit(action = "库存出库")
    public R<Map<String, Object>> stockOut(@PathVariable Long id,
                                           @RequestParam BigDecimal quantity,
                                           @RequestParam(required = false) String remark) {
        return change(id, quantity.negate(), "OUT", null, remark);
    }

    @PostMapping("/{id}/use")
    @Transactional
    @Audit(action = "项目耗材领用")
    public R<Map<String, Object>> use(@PathVariable Long id,
                                      @RequestParam BigDecimal quantity,
                                      @RequestParam(required = false) Long orderId,
                                      @RequestParam(required = false) String remark) {
        R<Map<String, Object>> r = change(id, quantity.negate(), "USE", null, remark);
        Map<String, Object> data = r.getData();
        if (data != null && orderId != null) {
            StockFlow flow = (StockFlow) data.get("flow");
            flow.setOrderId(orderId);
            flowMapper.updateById(flow);
        }
        return r;
    }

    @GetMapping("/flows")
    public R<List<StockFlow>> flows(@RequestParam(required = false) Long itemId) {
        return R.ok(flowMapper.selectList(new LambdaQueryWrapper<StockFlow>()
                .eq(itemId != null, StockFlow::getItemId, itemId)
                .orderByDesc(StockFlow::getId)));
    }

    @GetMapping("/warnings")
    public R<List<InventoryItem>> warnings() {
        return R.ok(mapper.selectList(new LambdaQueryWrapper<InventoryItem>()
                .apply("stock <= warn_stock").orderByAsc(InventoryItem::getStock)));
    }

    @GetMapping("/summary")
    public R<Map<String, Object>> summary() {
        Long tid = TenantContext.requireTenantId();
        List<InventoryItem> items = mapper.selectList(new LambdaQueryWrapper<InventoryItem>()
                .eq(InventoryItem::getTenantId, tid));
        BigDecimal stockValue = items.stream()
                .map(i -> (i.getStock() == null ? BigDecimal.ZERO : i.getStock())
                        .multiply(i.getCostPrice() == null ? BigDecimal.ZERO : i.getCostPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("items", items.size());
        m.put("stockValue", stockValue);
        m.put("lowStock", items.stream().filter(i -> i.getWarnStock() != null && i.getStock() != null
                && i.getStock().compareTo(i.getWarnStock()) <= 0).count());
        return R.ok(m);
    }

    private R<Map<String, Object>> change(Long id, BigDecimal quantity, String type, BigDecimal cost, String remark) {
        if (quantity == null || quantity.signum() == 0) {
            throw new BizException(400, "数量不能为 0");
        }
        InventoryItem item = mapper.selectById(id);
        if (item == null) {
            throw BizException.notFound("库存品不存在");
        }
        BigDecimal stock = item.getStock() == null ? BigDecimal.ZERO : item.getStock();
        BigDecimal next = stock.add(quantity);
        if (next.signum() < 0) {
            throw new BizException(400, "库存不足，当前库存 " + stock);
        }
        item.setStock(next);
        if (cost != null && "IN".equals(type)) {
            item.setCostPrice(cost);
        }
        mapper.updateById(item);
        StockFlow flow = new StockFlow();
        flow.setItemId(id);
        flow.setType(type);
        flow.setQuantity(quantity.abs());
        flow.setCost(cost);
        flow.setRemark(remark);
        flow.setOperator(TenantContext.user() == null ? null : TenantContext.user().getName());
        flowMapper.insert(flow);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("item", item);
        m.put("flow", flow);
        return R.ok(m);
    }
}
