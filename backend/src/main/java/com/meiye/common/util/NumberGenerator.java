package com.meiye.common.util;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.domain.entity.Tenant;
import com.meiye.domain.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 编号生成器：租户编码前缀 + 业务前缀 + 年份 + 4位序号；Redis INCR 原子自增防并发重号。
 * 重启/缓存清空后序号会从 1 开始，这里再按唯一键回查数据库跳过已占用序号，确保租户内唯一。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NumberGenerator {

    private static final AtomicLong FALLBACK = new AtomicLong(System.currentTimeMillis() % 100000);

    /** 业务前缀 -> 表/列，用于唯一性回查（仅白名单，避免 SQL 注入）。 */
    private static final Map<String, String[]> UNIQUE_COLUMNS = Map.of(
            "CUS", new String[]{"t_customer", "customer_no"},
            "TECH", new String[]{"t_technician", "tech_no"},
            "APT", new String[]{"t_appointment", "appointment_no"},
            "ORD", new String[]{"t_order", "order_no"},
            "CARD", new String[]{"t_member_card", "card_no"},
            "PAY", new String[]{"t_payment", "pay_no"},
            "STL", new String[]{"t_settlement", "settlement_no"}
    );

    private final StringRedisTemplate redis;
    private final TenantMapper tenantMapper;
    private final JdbcTemplate jdbc;

    public String next(String bizPrefix) {
        Long tid = TenantContext.getTenantId();
        String tenantCode = tenantCode(tid);
        int year = LocalDate.now().getYear();
        String key = "seq:" + tid + ":" + bizPrefix + ":" + year;
        String[] tableCol = UNIQUE_COLUMNS.get(bizPrefix);
        for (int i = 0; i < 100; i++) {
            long seq = nextSeq(key);
            String no = tenantCode + "-" + bizPrefix + "-" + year + "-" + String.format("%04d", seq);
            if (tableCol == null || !exists(tableCol[0], tableCol[1], no, tid)) {
                return no;
            }
        }
        // 极端情况下 100 个序号都被占用，退化为时间戳序号仍保持格式
        String no = tenantCode + "-" + bizPrefix + "-" + year + "-" + (System.currentTimeMillis() % 10000);
        log.warn("编号连续冲突，降级生成: {}", no);
        return no;
    }

    private long nextSeq(String key) {
        try {
            Long v = redis.opsForValue().increment(key);
            if (v != null && v == 1L) {
                redis.expire(key, Duration.ofDays(370));
            }
            return v == null ? FALLBACK.incrementAndGet() : v;
        } catch (Exception e) {
            log.warn("Redis 编号生成降级: {}", e.getMessage());
            return FALLBACK.incrementAndGet();
        }
    }

    private boolean exists(String table, String column, String no, Long tid) {
        try {
            Integer c = jdbc.queryForObject("select count(1) from " + table + " where tenant_id=? and " + column + "=?",
                    Integer.class, tid, no);
            return c != null && c > 0;
        } catch (Exception e) {
            log.warn("编号唯一性回查失败: {}", e.getMessage());
            return false;
        }
    }

    private String tenantCode(Long tid) {
        if (TenantContext.user() != null && TenantContext.user().getTenantCode() != null) {
            return TenantContext.user().getTenantCode();
        }
        if (tid == null) {
            return "SYS";
        }
        try {
            Tenant t = TenantContext.runAs(tid, true,
                    () -> tenantMapper.selectOne(new LambdaQueryWrapper<Tenant>().eq(Tenant::getId, tid)));
            return t == null ? "T" + tid : t.getCode();
        } catch (Exception e) {
            return "T" + tid;
        }
    }
}
