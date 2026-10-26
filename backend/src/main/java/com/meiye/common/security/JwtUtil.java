package com.meiye.common.security;

import com.meiye.common.context.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMillis;

    public JwtUtil(@Value("${meiye.jwt.secret}") String secret,
                   @Value("${meiye.jwt.expire-hours:24}") long expireHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireMillis = expireHours * 3600_000L;
    }

    public String create(LoginUser u) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uid", u.getUserId());
        claims.put("username", u.getUsername());
        claims.put("name", u.getName());
        claims.put("ut", u.getUserType());
        claims.put("role", u.getRole());
        claims.put("tid", u.getTenantId());
        claims.put("tcode", u.getTenantCode());
        claims.put("storeId", u.getStoreId());
        claims.put("techId", u.getTechnicianId());
        claims.put("cusId", u.getCustomerId());
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(u.getUserId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMillis))
                .signWith(key)
                .compact();
    }

    public LoginUser parse(String token) {
        Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        LoginUser u = new LoginUser();
        u.setUserId(asLong(c.get("uid")));
        u.setUsername((String) c.get("username"));
        u.setName((String) c.get("name"));
        u.setUserType((String) c.get("ut"));
        u.setRole((String) c.get("role"));
        u.setTenantId(asLong(c.get("tid")));
        u.setTenantCode((String) c.get("tcode"));
        u.setStoreId(asLong(c.get("storeId")));
        u.setTechnicianId(asLong(c.get("techId")));
        u.setCustomerId(asLong(c.get("cusId")));
        return u;
    }

    private Long asLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }
}
