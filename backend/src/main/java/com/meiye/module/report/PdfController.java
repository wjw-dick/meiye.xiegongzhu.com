package com.meiye.module.report;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.PdfUtil;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板打印：结算单、开卡单、服务报告、服务合同、经营报表 PDF，含电子签名位。
 */
@RestController
@RequestMapping("/api/tenant/pdf")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','FINANCE','TECHNICIAN')")
@Audit(module = "PDF 打印")
@RequiredArgsConstructor
public class PdfController {

    private final PdfUtil pdfUtil;
    private final SettlementMapper settlementMapper;
    private final SettlementItemMapper settlementItemMapper;
    private final TechnicianMapper technicianMapper;
    private final MemberCardMapper memberCardMapper;
    private final CardTypeMapper cardTypeMapper;
    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StoreMapper storeMapper;

    @GetMapping("/settlement/{id}")
    public void settlement(@PathVariable Long id, HttpServletResponse response) {
        Settlement s = settlementMapper.selectById(id);
        if (s == null) {
            throw BizException.notFound("结算单不存在");
        }
        List<SettlementItem> items = settlementItemMapper.selectList(
                new LambdaQueryWrapper<SettlementItem>().eq(SettlementItem::getSettlementId, id));
        List<List<Object>> rows = new ArrayList<>();
        for (SettlementItem it : items) {
            Technician t = technicianMapper.selectById(it.getTechnicianId());
            rows.add(List.of(t == null ? it.getTechnicianId() : t.getName(),
                    it.getOrderId() == null ? "-" : it.getOrderId(),
                    nz(it.getRevenue()), nz(it.getRate()) + "%", nz(it.getCommissionAmount())));
        }
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("结算单号", s.getSettlementNo());
        meta.put("结算周期", s.getStartDate() + " ~ " + s.getEndDate());
        meta.put("周期类型", "WEEK".equals(s.getPeriodType()) ? "按周" : "按月");
        meta.put("状态", s.getStatus());
        meta.put("订单收入", nz(s.getRevenue()).toPlainString());
        meta.put("技师提成", nz(s.getTechCommission()).toPlainString());
        meta.put("平台抽成", nz(s.getPlatformShare()).toPlainString());
        meta.put("门店分成", nz(s.getStoreShare()).toPlainString());
        meta.put("股东分红", nz(s.getDividend()).toPlainString());
        pdfUtil.write(response, "settlement-" + s.getSettlementNo() + ".pdf", "结 算 单",
                "结算单号：" + s.getSettlementNo(), meta,
                List.of("技师", "订单ID", "收入", "提成比例", "提成金额"), rows,
                "说明：本结算单经双方确认后生效，如有异议请在 3 个工作日内提出。", "门店负责人");
    }

    @GetMapping("/card/{id}")
    public void card(@PathVariable Long id, HttpServletResponse response) {
        MemberCard c = memberCardMapper.selectById(id);
        if (c == null) {
            throw BizException.notFound("会员卡不存在");
        }
        Customer cu = customerMapper.selectById(c.getCustomerId());
        CardType ct = cardTypeMapper.selectById(c.getCardTypeId());
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("卡号", c.getCardNo());
        meta.put("卡名称", c.getName() == null ? (ct == null ? "" : ct.getName()) : c.getName());
        meta.put("会员", cu == null ? "" : cu.getName());
        meta.put("卡类型", c.getType());
        meta.put("余额", nz(c.getBalance()).toPlainString());
        meta.put("剩余次数", String.valueOf(c.getTimes() == null ? 0 : c.getTimes()));
        meta.put("有效期至", c.getExpireAt() == null ? "长期有效" : c.getExpireAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        pdfUtil.write(response, "card-" + c.getCardNo() + ".pdf", "会 员 开 卡 单",
                "卡号：" + c.getCardNo(), meta, null, null,
                "本卡仅限本人使用，具体权益以门店公示为准。", "会员签字");
    }

    @GetMapping("/service-report/{orderId}")
    public void serviceReport(@PathVariable Long orderId, HttpServletResponse response) {
        Order o = orderMapper.selectById(orderId);
        if (o == null) {
            throw BizException.notFound("订单不存在");
        }
        Customer cu = customerMapper.selectById(o.getCustomerId());
        Technician t = o.getTechnicianId() == null ? null : technicianMapper.selectById(o.getTechnicianId());
        Store st = o.getStoreId() == null ? null : storeMapper.selectById(o.getStoreId());
        List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId));
        List<List<Object>> rows = new ArrayList<>();
        for (OrderItem it : items) {
            rows.add(List.of(it.getName(), it.getQuantity() == null ? 1 : it.getQuantity(),
                    nz(it.getPrice()), nz(it.getAmount())));
        }
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("订单号", o.getOrderNo());
        meta.put("会员", cu == null ? "" : cu.getName());
        meta.put("门店", st == null ? "" : st.getName());
        meta.put("技师", t == null ? "" : t.getName());
        meta.put("实付金额", nz(o.getPaidAmount()).toPlainString());
        meta.put("支付方式", o.getPayType() == null ? "" : o.getPayType());
        meta.put("服务时间", o.getPaidAt() == null ? "" : o.getPaidAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        pdfUtil.write(response, "service-report-" + o.getOrderNo() + ".pdf", "服 务 报 告",
                "订单号：" + o.getOrderNo(), meta,
                List.of("项目", "数量", "单价", "金额"), rows,
                "服务效果因个人体质存在差异，请按技师建议进行护理。", "技师签字");
    }

    @GetMapping("/contract/{customerId}")
    public void contract(@PathVariable Long customerId, HttpServletResponse response) {
        Customer cu = customerMapper.selectById(customerId);
        if (cu == null) {
            throw BizException.notFound("会员不存在");
        }
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("甲方（会员）", cu.getName());
        meta.put("会员编号", cu.getCustomerNo());
        meta.put("会员等级", cu.getLevel());
        meta.put("签订日期", java.time.LocalDate.now().toString());
        meta.put("合同期限", "自签订之日起一年");
        pdfUtil.write(response, "contract-" + cu.getCustomerNo() + ".pdf", "美 业 服 务 合 同",
                "合同编号：HT-" + cu.getCustomerNo(), meta, null, null,
                "甲乙双方本着平等自愿原则，就美业服务事项达成如下协议：\n" +
                        "一、甲方有权按照门店公示的项目、价格及会员权益接受服务；\n" +
                        "二、乙方应保证服务人员具备相应资质，并保护甲方个人隐私；\n" +
                        "三、预付费卡项按《单用途商业预付卡管理办法》执行，支持退卡与转让；\n" +
                        "四、争议解决：双方协商不成，可向乙方所在地人民法院提起诉讼。",
                "甲方签字");
    }

    @GetMapping("/report")
    public void report(HttpServletResponse response) {
        Long tid = TenantContext.requireTenantId();
        List<List<Object>> rows = new ArrayList<>();
        List<Map<String, Object>> list = jdbcList(tid);
        for (Map<String, Object> r : list) {
            rows.add(List.of(r.get("d"), r.get("orders"), r.get("amount")));
        }
        Map<String, String> meta = new LinkedHashMap<>();
        meta.put("租户", TenantContext.user() == null ? "" : String.valueOf(TenantContext.user().getTenantCode()));
        meta.put("统计时间", java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        pdfUtil.write(response, "business-report.pdf", "经 营 报 表",
                "近 30 天经营日报", meta, List.of("日期", "订单数", "营业额"), rows, null, null);
    }

    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    private List<Map<String, Object>> jdbcList(Long tid) {
        return jdbc.queryForList("select date(paid_at) d, count(*) orders, coalesce(sum(paid_amount),0) amount " +
                "from t_order where deleted=0 and tenant_id=? and paid_at>=date_sub(curdate(), interval 30 day) " +
                "group by date(paid_at) order by d", tid);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
