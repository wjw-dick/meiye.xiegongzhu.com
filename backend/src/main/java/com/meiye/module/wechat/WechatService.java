package com.meiye.module.wechat;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.domain.entity.Customer;
import com.meiye.domain.entity.WechatConfig;
import com.meiye.domain.mapper.CustomerMapper;
import com.meiye.domain.mapper.WechatConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WechatService {

    private final WechatConfigMapper wechatConfigMapper;
    private final CustomerMapper customerMapper;

    @Value("${meiye.wechat.mock:true}")
    private boolean mock;

    public record WxSession(String openid, String unionId) {
    }

    public WechatConfig config(Long tenantId) {
        return TenantContext.runAs(tenantId, false, () -> wechatConfigMapper.selectOne(
                new LambdaQueryWrapper<WechatConfig>().eq(WechatConfig::getTenantId, tenantId)));
    }

    public WxSession code2Session(Long tenantId, String code, String channel) {
        WechatConfig cfg = config(tenantId);
        boolean appChannel = "APP".equals(channel);
        if (mock || cfg == null
                || (appChannel && (cfg.getAppAppId() == null || cfg.getAppSecret() == null))
                || (!appChannel && (cfg.getMiniAppId() == null || cfg.getMiniSecret() == null))) {
            String seed = tenantId + ":" + code + ":" + channel;
            return new WxSession("openid-" + DigestUtil.md5Hex(seed).substring(0, 16),
                    "union-" + DigestUtil.md5Hex(seed).substring(0, 24));
        }
        try {
            // 小程序走 jscode2session；公众号网页授权与开放平台移动应用（APP）走 oauth2/access_token
            String appId = "MINI".equals(channel) ? cfg.getMiniAppId()
                    : (appChannel ? cfg.getAppAppId() : cfg.getMpAppId());
            String secret = "MINI".equals(channel) ? cfg.getMiniSecret()
                    : (appChannel ? cfg.getAppSecret() : cfg.getMpSecret());
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + secret
                    + "&js_code=" + code + "&grant_type=authorization_code";
            if (!"MINI".equals(channel)) {
                url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=" + appId + "&secret=" + secret
                        + "&code=" + code + "&grant_type=authorization_code";
            }
            JSONObject json = JSONUtil.parseObj(HttpUtil.get(url));
            String openid = json.getStr("openid");
            String unionId = json.getStr("unionid");
            if (openid == null) {
                throw new IllegalStateException("微信登录失败：" + json.getStr("errmsg"));
            }
            return new WxSession(openid, unionId == null ? openid : unionId);
        } catch (Exception e) {
            log.error("微信 code2Session 失败", e);
            throw new com.meiye.common.exception.BizException(502, "微信登录失败：" + e.getMessage());
        }
    }

    public String webAuthUrl(Long tenantId, String redirect) {
        WechatConfig cfg = config(tenantId);
        if (cfg == null || cfg.getMpAppId() == null) {
            return "";
        }
        String appId = cfg.getMpAppId();
        String encoded = URLEncoder.encode(redirect, StandardCharsets.UTF_8);
        return "https://open.weixin.qq.com/connect/oauth2/authorize?appid=" + appId
                + "&redirect_uri=" + encoded + "&response_type=code&scope=snsapi_userinfo&state=" + tenantId + "#wechat_redirect";
    }

    public String accessToken(Long tenantId) {
        WechatConfig cfg = config(tenantId);
        if (mock || cfg == null || cfg.getMpAppId() == null || cfg.getMpSecret() == null) {
            return "mock-token";
        }
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + cfg.getMpAppId() + "&secret=" + cfg.getMpSecret();
        JSONObject json = JSONUtil.parseObj(HttpUtil.get(url));
        return json.getStr("access_token");
    }

    public boolean sendTemplate(Long tenantId, Long customerId, String templateId, String title, String content) {
        Customer c = TenantContext.runAs(tenantId, false, () -> customerMapper.selectById(customerId));
        if (c == null || c.getUnionId() == null || templateId == null || templateId.isBlank()) {
            log.info("[模板消息-模拟] tenant={} template={} title={} content={}", tenantId, templateId, title, content);
            return true;
        }
        if (mock) {
            log.info("[模板消息-Mock] unionId={} template={} {}", c.getUnionId(), templateId, content);
            return true;
        }
        try {
            String token = accessToken(tenantId);
            Map<String, Object> body = new HashMap<>();
            body.put("touser", c.getUnionId());
            body.put("template_id", templateId);
            body.put("data", Map.of("first", Map.of("value", title), "remark", Map.of("value", content)));
            String resp = HttpUtil.post("https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token,
                    JSONUtil.toJsonStr(body));
            JSONObject json = JSONUtil.parseObj(resp);
            return json.getInt("errcode", -1) == 0;
        } catch (Exception e) {
            log.error("模板消息发送失败", e);
            return false;
        }
    }

    public boolean syncMenu(Long tenantId, String menuJson) {
        if (mock) {
            log.info("[自定义菜单-Mock] tenant={} menu={}", tenantId, menuJson);
            return true;
        }
        try {
            String token = accessToken(tenantId);
            String resp = HttpUtil.post("https://api.weixin.qq.com/cgi-bin/menu/create?access_token=" + token, menuJson);
            return JSONUtil.parseObj(resp).getInt("errcode", -1) == 0;
        } catch (Exception e) {
            log.error("自定义菜单同步失败", e);
            return false;
        }
    }
}
