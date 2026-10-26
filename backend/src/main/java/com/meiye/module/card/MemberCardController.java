package com.meiye.module.card;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.security.RepeatSubmit;
import com.meiye.common.web.R;
import com.meiye.domain.entity.CardFlow;
import com.meiye.domain.entity.MemberCard;
import com.meiye.domain.mapper.CardFlowMapper;
import com.meiye.domain.mapper.MemberCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tenant/member-cards")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','FINANCE')")
@Audit(module = "会员卡")
@RequiredArgsConstructor
public class MemberCardController {

    private final MemberCardMapper cardMapper;
    private final CardFlowMapper flowMapper;
    private final CardService cardService;
    private final JdbcTemplate jdbc;

    @GetMapping("/page")
    public R<Map<String, Object>> page(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long customerId,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String kw) {
        Long tid = TenantContext.requireTenantId();
        StringBuilder where = new StringBuilder(" where m.tenant_id=? and m.deleted=0");
        List<Object> args = new ArrayList<>();
        args.add(tid);
        if (status != null && !status.isBlank()) {
            where.append(" and m.status=?");
            args.add(status);
        }
        if (customerId != null) {
            where.append(" and m.customer_id=?");
            args.add(customerId);
        }
        if (type != null && !type.isBlank()) {
            where.append(" and m.type=?");
            args.add(type);
        }
        if (kw != null && !kw.isBlank()) {
            where.append(" and (m.card_no like ? or c.name like ?)");
            args.add("%" + kw + "%");
            args.add("%" + kw + "%");
        }
        String base = " from t_member_card m left join t_customer c on c.id=m.customer_id " + where;
        Long total = jdbc.queryForObject("select count(*)" + base, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add((page - 1) * size);
        List<Map<String, Object>> records = jdbc.queryForList(
                "select m.*, c.name customer_name" + base + " order by m.id desc limit ? offset ?", listArgs.toArray());
        return R.ok(Map.of("records", records, "total", total == null ? 0 : total, "page", page, "size", size));
    }

    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        MemberCard card = cardMapper.selectById(id);
        if (card == null) {
            throw BizException.notFound("会员卡不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("card", card);
        m.put("flows", flowMapper.selectList(new LambdaQueryWrapper<CardFlow>()
                .eq(CardFlow::getCardId, id).orderByDesc(CardFlow::getId)));
        return R.ok(m);
    }

    @PostMapping("/issue")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "开卡")
    public R<MemberCard> issue(@RequestBody Map<String, Object> body) {
        Long customerId = Long.valueOf(String.valueOf(body.get("customerId")));
        Long cardTypeId = Long.valueOf(String.valueOf(body.get("cardTypeId")));
        Long storeId = body.get("storeId") == null ? null : Long.valueOf(String.valueOf(body.get("storeId")));
        BigDecimal paid = body.get("paidAmount") == null ? null : new BigDecimal(String.valueOf(body.get("paidAmount")));
        Long parentId = body.get("parentId") == null ? null : Long.valueOf(String.valueOf(body.get("parentId")));
        return R.ok(cardService.issue(customerId, cardTypeId, storeId, paid, parentId));
    }

    @PostMapping("/{id}/recharge")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "储值充值")
    public R<MemberCard> recharge(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = new BigDecimal(String.valueOf(body.get("amount")));
        BigDecimal bonus = body.get("bonus") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("bonus")));
        return R.ok(cardService.recharge(id, amount, bonus));
    }

    @PostMapping("/{id}/consume")
    @RepeatSubmit(seconds = 3)
    @Audit(action = "会员卡核销")
    public R<MemberCard> consume(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal amount = body.get("amount") == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(body.get("amount")));
        int times = body.get("times") == null ? 0 : Integer.parseInt(String.valueOf(body.get("times")));
        Long orderId = body.get("orderId") == null ? null : Long.valueOf(String.valueOf(body.get("orderId")));
        return R.ok(cardService.consume(id, amount, times, orderId));
    }

    @GetMapping("/{id}/flows")
    public R<List<CardFlow>> flows(@PathVariable Long id) {
        return R.ok(flowMapper.selectList(new LambdaQueryWrapper<CardFlow>()
                .eq(CardFlow::getCardId, id).orderByDesc(CardFlow::getId)));
    }

    @GetMapping("/expiring")
    public R<List<MemberCard>> expiring(@RequestParam(defaultValue = "30") int days) {
        return R.ok(cardMapper.selectList(new LambdaQueryWrapper<MemberCard>()
                .eq(MemberCard::getStatus, "ACTIVE")
                .isNotNull(MemberCard::getExpireAt)
                .le(MemberCard::getExpireAt, LocalDateTime.now().plusDays(days))
                .orderByAsc(MemberCard::getExpireAt)));
    }

    @GetMapping("/low-balance")
    public R<List<MemberCard>> lowBalance(@RequestParam(defaultValue = "100") BigDecimal threshold) {
        return R.ok(cardMapper.selectList(new LambdaQueryWrapper<MemberCard>()
                .eq(MemberCard::getStatus, "ACTIVE").eq(MemberCard::getType, "STORED")
                .lt(MemberCard::getBalance, threshold).orderByAsc(MemberCard::getBalance)));
    }
}
