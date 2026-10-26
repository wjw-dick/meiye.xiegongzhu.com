package com.meiye.common.security;

import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Slf4j
@Aspect
@Component
@Order(20)
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String key = rateLimit.key().isEmpty() ? ((MethodSignature) pjp.getSignature()).getMethod().getName() : rateLimit.key();
        String user = TenantContext.user() == null ? "anon" : String.valueOf(TenantContext.user().getUserId());
        String redisKey = "rate:" + key + ":" + user + ":" + path();
        try {
            Long count = redis.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redis.expire(redisKey, Duration.ofSeconds(rateLimit.seconds()));
            }
            if (count != null && count > rateLimit.limit()) {
                throw new BizException(429, "操作过于频繁，请稍后再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("限流组件不可用，放行请求: {}", e.getMessage());
        }
        return pjp.proceed();
    }

    private String path() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs == null ? "" : attrs.getRequest().getRequestURI();
        } catch (Exception e) {
            return "";
        }
    }
}
