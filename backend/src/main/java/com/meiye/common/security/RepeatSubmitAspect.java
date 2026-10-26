package com.meiye.common.security;

import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RepeatSubmitAspect {

    private final StringRedisTemplate redis;

    @Around("@annotation(repeatSubmit)")
    public Object around(ProceedingJoinPoint pjp, RepeatSubmit repeatSubmit) throws Throwable {
        String user = TenantContext.user() == null ? "anon" : String.valueOf(TenantContext.user().getUserId());
        String uri = "";
        String requestId = "";
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                uri = attrs.getRequest().getRequestURI();
                requestId = attrs.getRequest().getHeader("X-Request-Id");
                if (requestId == null || requestId.isBlank()) {
                    requestId = attrs.getRequest().getHeader("X-Idempotency-Key");
                }
            }
        } catch (Exception ignored) {
        }
        String raw = user + ":" + uri + ":" + (requestId == null || requestId.isBlank()
                ? Arrays.deepToString(pjp.getArgs()) : "req:" + requestId);
        String hash = md5(raw);
        String key = "repeat:" + hash;
        try {
            Boolean ok = redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(repeatSubmit.seconds()));
            if (Boolean.FALSE.equals(ok)) {
                throw new BizException(429, "请勿重复提交");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("防重组件不可用，放行请求: {}", e.getMessage());
        }
        return pjp.proceed();
    }

    private String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] b = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) {
                sb.append(String.format("%02x", x));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }
}
