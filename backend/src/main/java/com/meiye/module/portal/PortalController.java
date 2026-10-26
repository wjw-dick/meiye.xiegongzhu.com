package com.meiye.module.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.MaskUtil;
import com.meiye.common.web.R;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.appointment.AppointmentService;
import com.meiye.module.tenant.BizTypeGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 公开门户：品牌/门店/项目/套餐/资讯/技师/时段，无需登录。
 */
@RestController
@RequestMapping("/api/public/portal/{tenantCode}")
@RequiredArgsConstructor
public class PortalController {

    private final TenantMapper tenantMapper;
    private final TenantConfigMapper tenantConfigMapper;
    private final TenantBizTypeMapper tenantBizTypeMapper;
    private final BizTypePluginMapper bizTypePluginMapper;
    private final StoreMapper storeMapper;
    private final ServiceItemMapper serviceItemMapper;
    private final ServicePackageMapper servicePackageMapper;
    private final CmsArticleMapper cmsArticleMapper;
    private final TechnicianMapper technicianMapper;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final AppointmentService appointmentService;

    @GetMapping("/home")
    public R<Map<String, Object>> home(@PathVariable String tenantCode) {
        Long tid = tenantId(tenantCode);
        Tenant tenant = tenantMapper.selectById(tid);
        TenantConfig cfg = tenantConfigMapper.selectOne(new LambdaQueryWrapper<TenantConfig>()
                .eq(TenantConfig::getTenantId, tid).last("limit 1"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tenant", publicTenant(tenant));
        m.put("config", cfg);
        m.put("bizTypes", bizTypes(tid));
        m.put("stores", stores(tid));
        m.put("items", items(tid, null).stream().limit(8).toList());
        m.put("packages", packages(tid).stream().limit(6).toList());
        m.put("articles", articles(tid, "NOTICE").stream().limit(5).toList());
        m.put("banners", articles(tid, "BANNER").stream().limit(5).toList());
        return R.ok(m);
    }

    @GetMapping("/items")
    public R<List<ServiceItem>> items(@PathVariable String tenantCode,
                                      @RequestParam(required = false) String bizType) {
        return R.ok(items(tenantId(tenantCode), bizType));
    }

    @GetMapping("/items/{id}")
    public R<Map<String, Object>> itemDetail(@PathVariable String tenantCode, @PathVariable Long id) {
        Long tid = tenantId(tenantCode);
        ServiceItem item = serviceItemMapper.selectById(id);
        if (item == null || !tid.equals(item.getTenantId())) {
            throw BizException.notFound("项目不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("item", item);
        m.put("stores", stores(tid));
        List<Technician> techs = technicianMapper.selectList(new LambdaQueryWrapper<Technician>()
                .eq(Technician::getTenantId, tid).eq(Technician::getStatus, 1)
                .like(item.getBizType() != null, Technician::getSkills, item.getBizType())
                .orderByDesc(Technician::getRating));
        m.put("technicians", techs);
        return R.ok(m);
    }

    @GetMapping("/packages")
    public R<List<ServicePackage>> packages(@PathVariable String tenantCode) {
        return R.ok(packages(tenantId(tenantCode)));
    }

    @GetMapping("/packages/{id}")
    public R<ServicePackage> packageDetail(@PathVariable String tenantCode, @PathVariable Long id) {
        ServicePackage p = servicePackageMapper.selectById(id);
        if (p == null) {
            throw BizException.notFound("套餐不存在");
        }
        return R.ok(p);
    }

    @GetMapping("/stores")
    public R<List<Map<String, Object>>> storeList(@PathVariable String tenantCode) {
        return R.ok(stores(tenantId(tenantCode)));
    }

    @GetMapping("/stores/{id}")
    public R<Map<String, Object>> storeDetail(@PathVariable String tenantCode, @PathVariable Long id) {
        Long tid = tenantId(tenantCode);
        Store s = storeMapper.selectById(id);
        if (s == null || !tid.equals(s.getTenantId())) {
            throw BizException.notFound("门店不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("store", s);
        List<Technician> techs = technicianMapper.selectList(new LambdaQueryWrapper<Technician>()
                .eq(Technician::getTenantId, tid).eq(Technician::getStoreId, id)
                .eq(Technician::getStatus, 1).orderByDesc(Technician::getRating));
        m.put("technicians", techs);
        m.put("items", items(tid, null));
        return R.ok(m);
    }

    @GetMapping("/articles")
    public R<List<CmsArticle>> articles(@PathVariable String tenantCode,
                                        @RequestParam(defaultValue = "NOTICE") String type) {
        return R.ok(articles(tenantId(tenantCode), type));
    }

    @GetMapping("/articles/{id}")
    public R<CmsArticle> article(@PathVariable String tenantCode, @PathVariable Long id) {
        CmsArticle a = cmsArticleMapper.selectById(id);
        if (a == null || a.getStatus() == null || a.getStatus() != 1) {
            throw BizException.notFound("文章不存在");
        }
        return R.ok(a);
    }

    @GetMapping("/technicians")
    public R<List<Technician>> technicians(@PathVariable String tenantCode,
                                           @RequestParam(required = false) Long storeId,
                                           @RequestParam(required = false) String bizType) {
        Long tid = tenantId(tenantCode);
        LambdaQueryWrapper<Technician> qw = new LambdaQueryWrapper<Technician>()
                .eq(Technician::getTenantId, tid).eq(Technician::getStatus, 1)
                .eq(storeId != null, Technician::getStoreId, storeId)
                .like(bizType != null && !bizType.isBlank(), Technician::getSkills, bizType)
                .orderByDesc(Technician::getRating);
        return R.ok(technicianMapper.selectList(qw));
    }

    @GetMapping("/slots")
    public R<List<Map<String, Object>>> slots(@PathVariable String tenantCode,
                                              @RequestParam Long storeId,
                                              @RequestParam(required = false) Long itemId,
                                              @RequestParam String date,
                                              @RequestParam(required = false) Long techId) {
        Long tid = tenantId(tenantCode);
        return R.ok(TenantContext.runAs(tid, false,
                () -> appointmentService.slots(storeId, itemId, date, techId)));
    }

    /** 分享查询：订单号 + 手机后四位，仅返回脱敏进度信息。 */
    @GetMapping("/share/order")
    public R<Map<String, Object>> shareOrder(@PathVariable String tenantCode,
                                             @RequestParam String orderNo,
                                             @RequestParam String phoneLast4) {
        Long tid = tenantId(tenantCode);
        Order o = TenantContext.runAs(tid, false,
                () -> orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderNo, orderNo).last("limit 1")));
        if (o == null) {
            throw BizException.notFound("订单不存在");
        }
        Customer c = customerMapper.selectById(o.getCustomerId());
        String phone = c == null ? null : c.getPhone();
        if (phone == null || !phone.endsWith(phoneLast4)) {
            throw BizException.forbidden("手机号校验失败");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderNo", o.getOrderNo());
        m.put("customerName", c == null ? "" : c.getName());
        m.put("phone", MaskUtil.phone(phone));
        m.put("amount", o.getPaidAmount());
        m.put("status", o.getStatus());
        m.put("paidAt", o.getPaidAt());
        m.put("bizType", o.getBizType());
        return R.ok(m);
    }

    private Long tenantId(String code) {
        Tenant t = TenantContext.runAs(null, true, () -> tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code).last("limit 1")));
        if (t == null) {
            throw BizException.notFound("租户不存在");
        }
        if (!"NORMAL".equals(t.getStatus()) && !"TRIAL".equals(t.getStatus())) {
            throw BizException.forbidden("租户已停用");
        }
        return t.getId();
    }

    private Map<String, Object> publicTenant(Tenant t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", t.getCode());
        m.put("name", t.getName());
        m.put("logo", t.getLogo());
        m.put("contact", t.getContact());
        m.put("phone", MaskUtil.phone(t.getPhone()));
        return m;
    }

    private List<Map<String, Object>> bizTypes(Long tid) {
        List<TenantBizType> enabled = TenantContext.runAs(tid, false,
                () -> tenantBizTypeMapper.selectList(new LambdaQueryWrapper<TenantBizType>()
                        .eq(TenantBizType::getTenantId, tid).eq(TenantBizType::getEnabled, 1)));
        List<Map<String, Object>> list = new ArrayList<>();
        for (TenantBizType t : enabled) {
            BizTypePlugin p = TenantContext.runAs(null, true,
                    () -> bizTypePluginMapper.selectOne(new LambdaQueryWrapper<BizTypePlugin>()
                            .eq(BizTypePlugin::getCode, t.getBizType()).last("limit 1")));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bizType", t.getBizType());
            m.put("name", p == null ? t.getBizType() : p.getName());
            m.put("icon", p == null ? null : p.getIcon());
            list.add(m);
        }
        return list;
    }

    private List<ServiceItem> items(Long tid, String bizType) {
        return TenantContext.runAs(tid, false, () -> serviceItemMapper.selectList(
                new LambdaQueryWrapper<ServiceItem>()
                        .eq(ServiceItem::getTenantId, tid).eq(ServiceItem::getStatus, 1)
                        .eq(bizType != null && !bizType.isBlank(), ServiceItem::getBizType, bizType)
                        .orderByAsc(ServiceItem::getId)));
    }

    private List<ServicePackage> packages(Long tid) {
        return TenantContext.runAs(tid, false, () -> servicePackageMapper.selectList(
                new LambdaQueryWrapper<ServicePackage>()
                        .eq(ServicePackage::getTenantId, tid).eq(ServicePackage::getStatus, 1)
                        .orderByAsc(ServicePackage::getId)));
    }

    private List<CmsArticle> articles(Long tid, String type) {
        return TenantContext.runAs(tid, false, () -> cmsArticleMapper.selectList(
                new LambdaQueryWrapper<CmsArticle>()
                        .eq(CmsArticle::getTenantId, tid).eq(CmsArticle::getStatus, 1)
                        .eq(CmsArticle::getType, type)
                        .orderByDesc(CmsArticle::getSort).orderByDesc(CmsArticle::getId)));
    }

    private List<Map<String, Object>> stores(Long tid) {
        List<Store> list = TenantContext.runAs(tid, false, () -> storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getTenantId, tid).eq(Store::getStatus, 1)));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Store s : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("code", s.getCode());
            m.put("name", s.getName());
            m.put("phone", MaskUtil.phone(s.getPhone()));
            m.put("address", s.getAddress());
            m.put("bizTypes", s.getBizTypes());
            m.put("description", s.getDescription());
            out.add(m);
        }
        return out;
    }
}
