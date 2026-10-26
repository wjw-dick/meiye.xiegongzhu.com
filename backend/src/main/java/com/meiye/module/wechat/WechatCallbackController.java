package com.meiye.module.wechat;

import cn.hutool.core.util.XmlUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Customer;
import com.meiye.domain.entity.Tenant;
import com.meiye.domain.entity.WechatFan;
import com.meiye.domain.mapper.CustomerMapper;
import com.meiye.domain.mapper.TenantMapper;
import com.meiye.domain.mapper.WechatFanMapper;
import com.meiye.module.message.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公众号服务器回调：签名校验、关注/取关事件、关键词自动回复。
 */
@Slf4j
@RestController
@RequestMapping("/api/public/wechat")
@RequiredArgsConstructor
public class WechatCallbackController {

    private final TenantMapper tenantMapper;
    private final WechatFanMapper fanMapper;
    private final CustomerMapper customerMapper;
    private final MessageService messageService;
    private final WechatService wechatService;

    private static final Map<String, Long> REPLAY_CACHE = new ConcurrentHashMap<>();

    @GetMapping("/{tenantCode}/callback")
    public String verify(@PathVariable String tenantCode,
                         @RequestParam(required = false) String signature,
                         @RequestParam(required = false) String timestamp,
                         @RequestParam(required = false) String nonce,
                         @RequestParam(required = false) String echostr) {
        Tenant tenant = tenant(tenantCode);
        var cfg = wechatService.config(tenant.getId());
        if (cfg == null || cfg.getMpToken() == null || cfg.getMpToken().isBlank()) {
            return echostr == null ? "" : echostr;
        }
        // 微信服务器会同时带上 signature/timestamp/nonce；缺少任一参数无法校验，按非法请求处理
        if (signature != null) {
            if (timestamp == null || nonce == null || !signature.equals(sign(cfg.getMpToken(), timestamp, nonce))) {
                log.warn("微信回调签名不匹配 tenant={}", tenantCode);
                return "invalid signature";
            }
        }
        return echostr == null ? "ok" : echostr;
    }

    @PostMapping(value = "/{tenantCode}/callback", produces = "application/xml;charset=UTF-8")
    public String event(@PathVariable String tenantCode, @RequestBody String xml,
                        @RequestParam(required = false) String signature,
                        @RequestParam(required = false) String timestamp,
                        @RequestParam(required = false) String nonce) {
        Tenant tenant = tenant(tenantCode);
        var cfg = wechatService.config(tenant.getId());
        if (cfg != null && cfg.getMpToken() != null && !cfg.getMpToken().isBlank()
                && signature != null && timestamp != null && nonce != null) {
            if (!signature.equals(sign(cfg.getMpToken(), timestamp, nonce))) {
                log.warn("微信事件签名不匹配 tenant={}", tenantCode);
                return "invalid signature";
            }
        }
        try {
            var root = XmlUtil.parseXml(xml).getDocumentElement();
            String openid = XmlUtil.getElement(root, "FromUserName") == null ? null
                    : XmlUtil.getElement(root, "FromUserName").getTextContent();
            String msgType = XmlUtil.getElement(root, "MsgType") == null ? null
                    : XmlUtil.getElement(root, "MsgType").getTextContent();
            String event = XmlUtil.getElement(root, "Event") == null ? null
                    : XmlUtil.getElement(root, "Event").getTextContent();
            String content = XmlUtil.getElement(root, "Content") == null ? null
                    : XmlUtil.getElement(root, "Content").getTextContent();
            String toUser = XmlUtil.getElement(root, "ToUserName") == null ? null
                    : XmlUtil.getElement(root, "ToUserName").getTextContent();
            String unionId = null;
            if (openid != null) {
                WechatFan fan = TenantContext.runAs(tenant.getId(), false, () -> fanMapper.selectOne(
                        new LambdaQueryWrapper<WechatFan>().eq(WechatFan::getOpenid, openid).last("limit 1")));
                if (fan == null) {
                    fan = new WechatFan();
                    fan.setOpenid(openid);
                    fan.setSubscribedAt(LocalDateTime.now());
                    fan.setSubscribed(1);
                    WechatFan f = fan;
                    final String oid = openid;
                    TenantContext.runAs(tenant.getId(), false, () -> {
                        f.setTenantId(tenant.getId());
                        fanMapper.insert(f);
                        return null;
                    });
                }
                if ("unsubscribe".equalsIgnoreCase(event)) {
                    fan.setSubscribed(0);
                    WechatFan f = fan;
                    TenantContext.runAs(tenant.getId(), false, () -> {
                        fanMapper.updateById(f);
                        return null;
                    });
                } else if ("subscribe".equalsIgnoreCase(event)) {
                    fan.setSubscribed(1);
                    fan.setSubscribedAt(LocalDateTime.now());
                    WechatFan f = fan;
                    TenantContext.runAs(tenant.getId(), false, () -> {
                        fanMapper.updateById(f);
                        return null;
                    });
                    Customer c = customerMapper.selectOne(new LambdaQueryWrapper<Customer>()
                            .eq(Customer::getUnionId, fan.getUnionId()).last("limit 1"));
                    if (c != null) {
                        messageService.send(tenant.getId(), c.getId(), "WECHAT_SUBSCRIBE",
                                "欢迎关注", "感谢关注，回复【预约】即可在线预约，回复【门店】查看门店信息。");
                    }
                }
                unionId = fan.getUnionId();
            }
            String reply = autoReply(msgType, content);
            if (reply == null || openid == null || toUser == null) {
                return "";
            }
            String key = openid + ":" + reply;
            Long last = REPLAY_CACHE.get(key);
            long now = System.currentTimeMillis();
            if (last != null && now - last < 3000) {
                return "";
            }
            REPLAY_CACHE.put(key, now);
            return "<xml><ToUserName><![CDATA[" + openid + "]]></ToUserName>"
                    + "<FromUserName><![CDATA[" + toUser + "]]></FromUserName>"
                    + "<CreateTime>" + (now / 1000) + "</CreateTime>"
                    + "<MsgType><![CDATA[text]]></MsgType>"
                    + "<Content><![CDATA[" + reply + "]]></Content></xml>";
        } catch (Exception e) {
            log.error("微信事件处理失败", e);
            return "";
        }
    }

    private String sign(String token, String timestamp, String nonce) {
        return DigestUtil.sha1Hex(Arrays.stream(new String[]{token, timestamp, nonce})
                .sorted()
                .reduce("", String::concat));
    }

    private String autoReply(String msgType, String content) {
        if (!"text".equalsIgnoreCase(msgType) || content == null) {
            return null;
        }
        String kw = content.trim();
        if (kw.contains("预约")) {
            return "在线预约：请点击菜单【预约服务】选择门店、项目与时段，或直接回复门店名称。";
        }
        if (kw.contains("订单")) {
            return "订单查询：请进入【我的-我的订单】查看服务状态与核销码。";
        }
        if (kw.contains("卡包") || kw.contains("储值")) {
            return "卡包查询：请进入【我的-我的卡包】查看次数、余额与有效期。";
        }
        if (kw.contains("门店")) {
            return "门店信息：请进入【关于我们-门店导航】查看地址、营业时间与联系电话。";
        }
        if (kw.contains("项目") || kw.contains("套餐")) {
            return "项目/套餐：请点击菜单【服务项目】查看项目介绍、价格与适用门店。";
        }
        if (kw.contains("客服") || kw.contains("人工")) {
            return "客服：请拨打门店服务电话，或在【我的-联系客服】留言，我们会尽快回复。";
        }
        return "您好，回复【预约】【订单】【卡包】【门店】【项目】【客服】获取对应服务。";
    }

    private Tenant tenant(String code) {
        Tenant t = TenantContext.runAs(null, true, () -> tenantMapper.selectOne(
                new LambdaQueryWrapper<Tenant>().eq(Tenant::getCode, code).last("limit 1")));
        if (t == null) {
            throw com.meiye.common.exception.BizException.notFound("租户不存在");
        }
        return t;
    }
}
