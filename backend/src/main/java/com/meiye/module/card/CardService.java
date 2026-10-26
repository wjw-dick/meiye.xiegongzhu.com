package com.meiye.module.card;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.util.NumberGenerator;
import com.meiye.domain.entity.*;
import com.meiye.domain.mapper.*;
import com.meiye.module.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {

    private final MemberCardMapper cardMapper;
    private final CardTypeMapper cardTypeMapper;
    private final CardFlowMapper flowMapper;
    private final CustomerMapper customerMapper;
    private final StoreMapper storeMapper;
    private final TenantMapper tenantMapper;
    private final NumberGenerator numberGenerator;
    private final MessageService messageService;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;

    @Transactional
    public MemberCard issue(Long customerId, Long cardTypeId, Long storeId, BigDecimal paidAmount, Long parentId) {
        Long tid = TenantContext.requireTenantId();
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException(404, "客户不存在");
        }
        CardType type = cardTypeMapper.selectById(cardTypeId);
        if (type == null || type.getStatus() == null || type.getStatus() != 1) {
            throw new BizException(404, "卡种不存在或已停用");
        }
        if (parentId != null) {
            MemberCard parent = cardMapper.selectById(parentId);
            if (parent == null || !parent.getCustomerId().equals(customerId)) {
                throw new BizException(400, "附属卡主卡无效");
            }
        }
        MemberCard card = new MemberCard();
        card.setCardNo(numberGenerator.next("CARD"));
        card.setCustomerId(customerId);
        card.setCardTypeId(cardTypeId);
        card.setType(type.getType());
        card.setName(type.getName());
        card.setBizType(type.getBizType());
        card.setStoreId(storeId);
        card.setBalance("STORED".equals(type.getType())
                ? type.getFaceValue().add(type.getBonus() == null ? BigDecimal.ZERO : type.getBonus())
                : BigDecimal.ZERO);
        card.setTimes("COUNT".equals(type.getType()) || "COMBO".equals(type.getType()) ? type.getTimes() : 0);
        card.setTotalTimes(card.getTimes());
        card.setExpireAt(type.getValidDays() == null || type.getValidDays() <= 0 ? null
                : LocalDateTime.now().plusDays(type.getValidDays()));
        card.setStatus("ACTIVE");
        card.setParentId(parentId);
        card.setDiscount(type.getDiscount() == null ? new BigDecimal("100") : type.getDiscount());
        cardMapper.insert(card);
        BigDecimal paid = paidAmount == null ? type.getPrice() : paidAmount;
        flow(card, "OPEN", paid, card.getTimes(), null, "开卡：" + type.getName(), TenantContext.user() == null ? "system" : TenantContext.user().getName());
        customer.setTotalRecharge((customer.getTotalRecharge() == null ? BigDecimal.ZERO : customer.getTotalRecharge()).add(paid));
        customer.setGrowth((customer.getGrowth() == null ? 0 : customer.getGrowth()) + paid.intValue());
        customerMapper.updateById(customer);
        messageService.send(tid, customerId, "STORED", "开卡成功",
                "卡号 " + card.getCardNo() + "，余额 " + card.getBalance() + "，次数 " + card.getTimes());
        return card;
    }

    @Transactional
    public MemberCard recharge(Long cardId, BigDecimal amount, BigDecimal bonus) {
        MemberCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new BizException(404, "会员卡不存在");
        }
        if (!"STORED".equals(card.getType())) {
            throw new BizException(400, "仅储值卡支持充值");
        }
        checkUsable(card);
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(400, "充值金额必须大于0");
        }
        BigDecimal add = amount.add(bonus == null ? BigDecimal.ZERO : bonus);
        card.setBalance(card.getBalance().add(add));
        cardMapper.updateById(card);
        flow(card, "RECHARGE", amount, 0, null, "充值" + amount + (bonus != null && bonus.signum() > 0 ? "，赠送" + bonus : ""),
                TenantContext.user() == null ? "system" : TenantContext.user().getName());
        Customer c = customerMapper.selectById(card.getCustomerId());
        if (c != null) {
            c.setTotalRecharge((c.getTotalRecharge() == null ? BigDecimal.ZERO : c.getTotalRecharge()).add(amount));
            c.setGrowth((c.getGrowth() == null ? 0 : c.getGrowth()) + add.intValue());
            upgradeLevel(c);
            customerMapper.updateById(c);
        }
        messageService.send(TenantContext.requireTenantId(), card.getCustomerId(), "STORED",
                "储值到账", "充值 " + amount + " 元，当前余额 " + card.getBalance() + " 元");
        return card;
    }

    @Transactional
    public MemberCard consume(Long cardId, BigDecimal amount, int times, Long orderId) {
        MemberCard card = cardMapper.selectById(cardId);
        if (card == null) {
            throw new BizException(404, "会员卡不存在");
        }
        checkUsable(card);
        Long useStoreId = orderStoreId(orderId);
        if (card.getStoreId() != null && useStoreId != null && !useStoreId.equals(card.getStoreId())) {
            if (!tenantAllowsCrossStore(TenantContext.requireTenantId())) {
                throw new BizException(400, "租户未开启跨店通兑，该卡不可跨店使用");
            }
            Store store = storeMapper.selectById(card.getStoreId());
            if (store != null && store.getAllowCrossStore() != null && store.getAllowCrossStore() == 0) {
                throw new BizException(400, "该卡不支持跨店使用");
            }
        }
        if (amount != null && amount.signum() > 0) {
            int rows = jdbc.update("update t_member_card set balance=balance-? where id=? and tenant_id=? and balance>=? and status='ACTIVE'",
                    amount, cardId, TenantContext.requireTenantId(), amount);
            if (rows == 0) {
                throw new BizException(400, "会员卡余额不足");
            }
        }
        if (times > 0) {
            int rows = jdbc.update("update t_member_card set times=times-? where id=? and tenant_id=? and times>=? and status='ACTIVE'",
                    times, cardId, TenantContext.requireTenantId(), times);
            if (rows == 0) {
                throw new BizException(400, "会员卡剩余次数不足");
            }
        }
        MemberCard after = cardMapper.selectById(cardId);
        if (after.getTimes() != null && after.getTimes() <= 0 && "COUNT".equals(after.getType())) {
            after.setStatus("USED");
            cardMapper.updateById(after);
        }
        flow(after, "CONSUME", amount == null ? BigDecimal.ZERO : amount.negate(), -times, orderId, "消费核销",
                TenantContext.user() == null ? "system" : TenantContext.user().getName());
        return after;
    }

    @Transactional
    public void refund(Long cardId, BigDecimal amount, int times, Long orderId) {
        MemberCard card = cardMapper.selectById(cardId);
        if (card == null) {
            return;
        }
        if (amount != null && amount.signum() > 0) {
            card.setBalance(card.getBalance().add(amount));
        }
        if (times > 0) {
            card.setTimes(card.getTimes() + times);
        }
        cardMapper.updateById(card);
        flow(card, "REFUND", amount == null ? BigDecimal.ZERO : amount, times, orderId, "退款回滚", "system");
    }

    public List<MemberCard> activeCards(Long customerId) {
        expireCards(customerId);
        return cardMapper.selectList(new LambdaQueryWrapper<MemberCard>()
                .eq(MemberCard::getCustomerId, customerId)
                .eq(MemberCard::getStatus, "ACTIVE")
                .orderByDesc(MemberCard::getId));
    }

    public BigDecimal memberDiscount(Long customerId) {
        List<MemberCard> cards = activeCards(customerId);
        return cards.stream().map(MemberCard::getDiscount).filter(d -> d != null)
                .min(BigDecimal::compareTo).orElse(new BigDecimal("100"));
    }

    @Scheduled(fixedDelay = 3600000)
    public void scanExpiry() {
        try {
            List<Tenant> tenants = TenantContext.runAs(null, true, () -> tenantMapper.selectList(
                    new LambdaQueryWrapper<Tenant>().in(Tenant::getStatus, List.of("NORMAL", "TRIAL"))));
            for (Tenant t : tenants) {
                TenantContext.runAs(t.getId(), false, () -> {
                    List<MemberCard> cards = cardMapper.selectList(new LambdaQueryWrapper<MemberCard>()
                            .eq(MemberCard::getStatus, "ACTIVE")
                            .isNotNull(MemberCard::getExpireAt)
                            .lt(MemberCard::getExpireAt, LocalDateTime.now().plusDays(15)));
                    for (MemberCard c : cards) {
                        boolean expired = c.getExpireAt().isBefore(LocalDateTime.now());
                        if (expired) {
                            c.setStatus("EXPIRED");
                            cardMapper.updateById(c);
                        }
                        String key = "card:warn:" + t.getId() + ":" + c.getId() + ":" + c.getExpireAt().toLocalDate();
                        try {
                            Boolean first = redis.opsForValue().setIfAbsent(key, "1", Duration.ofDays(7));
                            if (Boolean.FALSE.equals(first)) {
                                continue;
                            }
                        } catch (Exception ignored) {
                        }
                        messageService.send(t.getId(), c.getCustomerId(), "EXPIRY",
                                expired ? "会员卡已到期" : "会员卡即将到期",
                                "卡号 " + c.getCardNo() + " 到期时间 " + c.getExpireAt().toLocalDate() + "，剩余次数 " + c.getTimes() + "，余额 " + c.getBalance());
                    }
                    return null;
                });
            }
        } catch (Exception e) {
            log.warn("会员卡到期扫描失败: {}", e.getMessage());
        }
    }

    private void expireCards(Long customerId) {
        List<MemberCard> cards = cardMapper.selectList(new LambdaQueryWrapper<MemberCard>()
                .eq(MemberCard::getCustomerId, customerId).eq(MemberCard::getStatus, "ACTIVE")
                .isNotNull(MemberCard::getExpireAt).lt(MemberCard::getExpireAt, LocalDateTime.now()));
        for (MemberCard c : cards) {
            c.setStatus("EXPIRED");
            cardMapper.updateById(c);
        }
    }

    private void checkUsable(MemberCard card) {
        if (!"ACTIVE".equals(card.getStatus())) {
            throw new BizException(400, "会员卡状态不可用：" + card.getStatus());
        }
        if (card.getExpireAt() != null && card.getExpireAt().isBefore(LocalDateTime.now())) {
            card.setStatus("EXPIRED");
            cardMapper.updateById(card);
            throw new BizException(400, "会员卡已过期");
        }
        if (card.getParentId() != null) {
            MemberCard parent = cardMapper.selectById(card.getParentId());
            if (parent != null && ("ACTIVE".equals(parent.getStatus()))) {
                return;
            }
        }
    }

    private Long orderStoreId(Long orderId) {
        if (orderId == null) {
            return null;
        }
        try {
            return jdbc.queryForObject("select store_id from t_order where id=? and tenant_id=?",
                    Long.class, orderId, TenantContext.requireTenantId());
        } catch (Exception e) {
            return null;
        }
    }

    private void flow(MemberCard card, String type, BigDecimal amount, int times, Long orderId, String remark, String operator) {
        CardFlow f = new CardFlow();
        f.setCardId(card.getId());
        f.setCustomerId(card.getCustomerId());
        f.setType(type);
        f.setAmount(amount == null ? BigDecimal.ZERO : amount);
        f.setTimes(times);
        f.setBalanceAfter(card.getBalance());
        f.setTimesAfter(card.getTimes());
        f.setOrderId(orderId);
        f.setRemark(remark);
        f.setOperator(operator);
        flowMapper.insert(f);
    }

    /** 读取租户级跨店通兑开关（tenant_config.config_json.crossStore），无配置时默认关闭。 */
    private boolean tenantAllowsCrossStore(Long tenantId) {
        try {
            String v = jdbc.queryForObject("select json_unquote(json_extract(config_json,'$.crossStore')) " +
                    "from tenant_config where tenant_id=? and deleted=0 limit 1", String.class, tenantId);
            return "true".equalsIgnoreCase(v) || "1".equals(v);
        } catch (Exception e) {
            return false;
        }
    }

    private void upgradeLevel(Customer c) {        BigDecimal total = Optional.ofNullable(c.getTotalConsume()).orElse(BigDecimal.ZERO);
        if (total.compareTo(new BigDecimal("50000")) >= 0) {
            c.setLevel("黑金会员");
        } else if (total.compareTo(new BigDecimal("20000")) >= 0) {
            c.setLevel("金卡会员");
        } else if (total.compareTo(new BigDecimal("5000")) >= 0) {
            c.setLevel("银卡会员");
        } else {
            c.setLevel("普通会员");
        }
    }
}
