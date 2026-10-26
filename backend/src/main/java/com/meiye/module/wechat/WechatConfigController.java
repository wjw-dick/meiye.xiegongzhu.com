package com.meiye.module.wechat;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.util.MaskUtil;
import com.meiye.common.web.R;
import com.meiye.domain.entity.WechatConfig;
import com.meiye.domain.mapper.WechatConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信配置：小程序/公众号/开放平台/App 密钥 AES 加密存储，页面回显掩码，保存实时生效并审计。
 */
@RestController
@RequestMapping("/api/tenant/wechat/config")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN')")
@Audit(module = "微信配置")
@RequiredArgsConstructor
public class WechatConfigController {

    private final WechatConfigMapper mapper;
    private final WechatService wechatService;

    @Value("${meiye.wechat.mock:true}")
    private boolean mock;

    @GetMapping
    public R<Map<String, Object>> get() {
        Long tid = TenantContext.requireTenantId();
        WechatConfig cfg = mapper.selectOne(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, tid).last("limit 1"));
        Map<String, Object> m = new LinkedHashMap<>();
        if (cfg == null) {
            m.put("configured", false);
            return R.ok(m);
        }
        m.put("configured", true);
        m.put("id", cfg.getId());
        m.put("miniAppId", cfg.getMiniAppId());
        m.put("miniSecret", MaskUtil.secret(cfg.getMiniSecret()));
        m.put("mpAppId", cfg.getMpAppId());
        m.put("mpSecret", MaskUtil.secret(cfg.getMpSecret()));
        m.put("mpToken", cfg.getMpToken());
        m.put("mpAesKey", MaskUtil.secret(cfg.getMpAesKey()));
        m.put("mpUrl", cfg.getMpUrl());
        m.put("unionAppId", cfg.getUnionAppId());
        m.put("appAppId", cfg.getAppAppId());
        m.put("appSecret", MaskUtil.secret(cfg.getAppSecret()));
        m.put("subscribeIds", cfg.getSubscribeIds());
        m.put("pushChannel", cfg.getPushChannel());
        return R.ok(m);
    }

    @PutMapping
    @Audit(action = "保存微信配置")
    public R<Map<String, Object>> save(@RequestBody Map<String, String> body) {
        Long tid = TenantContext.requireTenantId();
        WechatConfig cfg = mapper.selectOne(new LambdaQueryWrapper<WechatConfig>()
                .eq(WechatConfig::getTenantId, tid).last("limit 1"));
        boolean create = cfg == null;
        if (create) {
            cfg = new WechatConfig();
            cfg.setTenantId(tid);
        }
        apply(body, "miniAppId", cfg::setMiniAppId);
        applySecret(body, "miniSecret", cfg::setMiniSecret);
        apply(body, "mpAppId", cfg::setMpAppId);
        applySecret(body, "mpSecret", cfg::setMpSecret);
        apply(body, "mpToken", cfg::setMpToken);
        applySecret(body, "mpAesKey", cfg::setMpAesKey);
        apply(body, "mpUrl", cfg::setMpUrl);
        apply(body, "unionAppId", cfg::setUnionAppId);
        apply(body, "appAppId", cfg::setAppAppId);
        applySecret(body, "appSecret", cfg::setAppSecret);
        apply(body, "subscribeIds", cfg::setSubscribeIds);
        apply(body, "pushChannel", cfg::setPushChannel);
        if (create) {
            mapper.insert(cfg);
        } else {
            mapper.updateById(cfg);
        }
        return get();
    }

    @PostMapping("/test")
    @Audit(action = "测试微信连接")
    public R<Map<String, Object>> test() {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        if (mock) {
            m.put("ok", true);
            m.put("mode", "MOCK");
            m.put("message", "当前为模拟模式，配置已保存；正式环境需配置真实 AppID/Secret 并关闭 mock");
            return R.ok(m);
        }
        try {
            String token = wechatService.accessToken(tid);
            m.put("ok", token != null && !token.isBlank());
            m.put("message", m.get("ok").equals(true) ? "连接成功" : "获取 access_token 失败");
        } catch (Exception e) {
            m.put("ok", false);
            m.put("message", e.getMessage());
        }
        return R.ok(m);
    }

    @GetMapping("/auth-url")
    public R<Map<String, Object>> authUrl(@RequestParam String redirect) {
        Long tid = TenantContext.requireTenantId();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("url", wechatService.webAuthUrl(tid, redirect));
        return R.ok(m);
    }

    @PostMapping("/menu/sync")
    @Audit(action = "同步公众号菜单")
    public R<Map<String, Object>> syncMenu(@RequestBody(required = false) String menuJson) {
        Long tid = TenantContext.requireTenantId();
        String json = menuJson;
        if (json == null || json.isBlank()) {
            var menu = menuConfigMapper.selectOne(new LambdaQueryWrapper<com.meiye.domain.entity.MenuConfig>()
                    .eq(com.meiye.domain.entity.MenuConfig::getTenantId, tid)
                    .eq(com.meiye.domain.entity.MenuConfig::getTarget, "WECHAT_MENU").last("limit 1"));
            json = menu == null ? null : menu.getContent();
        }
        if (json == null || json.isBlank()) {
            return R.fail(400, "请先配置公众号菜单");
        }
        boolean ok = wechatService.syncMenu(tid, json);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ok", ok);
        m.put("message", ok ? "菜单同步成功" : "菜单同步失败");
        return R.ok(m);
    }

    @GetMapping("/signature")
    public R<Map<String, Object>> signature(@RequestParam String token, @RequestParam String timestamp,
                                            @RequestParam String nonce, @RequestParam String echostr) {
        String raw = java.util.Arrays.stream(new String[]{token, timestamp, nonce}).sorted()
                .reduce("", String::concat);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("signature", DigestUtil.sha1Hex(raw));
        m.put("echostr", echostr);
        return R.ok(m);
    }

    private final com.meiye.domain.mapper.MenuConfigMapper menuConfigMapper;

    private interface Setter {
        void set(String v);
    }

    private void apply(Map<String, String> body, String key, Setter setter) {
        if (body.containsKey(key)) {
            setter.set(body.get(key));
        }
    }

    private void applySecret(Map<String, String> body, String key, Setter setter) {
        String v = body.get(key);
        if (v != null && !v.isBlank() && !v.contains("****")) {
            setter.set(v);
        }
    }
}
