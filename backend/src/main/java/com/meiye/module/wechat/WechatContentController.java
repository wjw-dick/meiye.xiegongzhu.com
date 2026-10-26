package com.meiye.module.wechat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.PageResult;
import com.meiye.common.web.R;
import com.meiye.domain.entity.WechatFan;
import com.meiye.domain.entity.WechatMaterial;
import com.meiye.domain.entity.WechatTag;
import com.meiye.domain.mapper.WechatFanMapper;
import com.meiye.domain.mapper.WechatMaterialMapper;
import com.meiye.domain.mapper.WechatTagMapper;
import com.meiye.module.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公众号内容运营：粉丝、标签、图文素材、定向群发（限流）。
 */
@RestController
@RequestMapping("/api/tenant/wechat")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','MARKETING')")
@Audit(module = "公众号运营")
@RequiredArgsConstructor
public class WechatContentController {

    private final WechatFanMapper fanMapper;
    private final WechatTagMapper tagMapper;
    private final WechatMaterialMapper materialMapper;
    private final MessageService messageService;

    @GetMapping("/fans/page")
    public R<PageResult<WechatFan>> fans(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) Integer subscribed) {
        return R.ok(PageResult.of(fanMapper.selectPage(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size),
                new LambdaQueryWrapper<WechatFan>()
                        .eq(subscribed != null, WechatFan::getSubscribed, subscribed)
                        .orderByDesc(WechatFan::getId))));
    }

    @PostMapping("/fans/{id}/tag")
    @Audit(action = "粉丝打标签")
    public R<WechatFan> tag(@PathVariable Long id, @RequestParam String tags) {
        WechatFan f = fanMapper.selectById(id);
        if (f == null) {
            return R.fail(404, "粉丝不存在");
        }
        f.setTags(tags);
        fanMapper.updateById(f);
        return R.ok(f);
    }

    @GetMapping("/tags")
    public R<List<WechatTag>> tags() {
        return R.ok(tagMapper.selectList(new LambdaQueryWrapper<WechatTag>().orderByDesc(WechatTag::getId)));
    }

    @PostMapping("/tags")
    @Audit(action = "新增粉丝标签")
    public R<WechatTag> createTag(@RequestBody WechatTag body) {
        tagMapper.insert(body);
        return R.ok(body);
    }

    @DeleteMapping("/tags/{id}")
    @Audit(action = "删除粉丝标签")
    public R<Void> deleteTag(@PathVariable Long id) {
        tagMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/materials")
    public R<List<WechatMaterial>> materials(@RequestParam(required = false) String type) {
        return R.ok(materialMapper.selectList(new LambdaQueryWrapper<WechatMaterial>()
                .eq(type != null && !type.isBlank(), WechatMaterial::getType, type)
                .orderByDesc(WechatMaterial::getId)));
    }

    @PostMapping("/materials")
    @Audit(action = "新增图文素材")
    public R<WechatMaterial> createMaterial(@RequestBody WechatMaterial body) {
        body.setStatus(body.getStatus() == null ? 1 : body.getStatus());
        materialMapper.insert(body);
        return R.ok(body);
    }

    @PutMapping("/materials")
    @Audit(action = "编辑图文素材")
    public R<WechatMaterial> updateMaterial(@RequestBody WechatMaterial body) {
        materialMapper.updateById(body);
        return R.ok(body);
    }

    @DeleteMapping("/materials/{id}")
    @Audit(action = "删除图文素材")
    public R<Void> deleteMaterial(@PathVariable Long id) {
        materialMapper.deleteById(id);
        return R.ok();
    }

    /**
     * 定向群发：按标签或全量粉丝生成站内消息并投递模板消息；单次最多 500 人，避免触发平台频控。
     */
    @PostMapping("/mass-send")
    @Audit(action = "公众号定向群发")
    public R<Map<String, Object>> massSend(@RequestParam String title,
                                           @RequestParam String content,
                                           @RequestParam(required = false) String tag,
                                           @RequestParam(required = false) String code) {
        Long tid = TenantContext.requireTenantId();
        LambdaQueryWrapper<WechatFan> qw = new LambdaQueryWrapper<WechatFan>()
                .eq(WechatFan::getSubscribed, 1);
        if (tag != null && !tag.isBlank()) {
            qw.like(WechatFan::getTags, tag);
        }
        List<WechatFan> fans = fanMapper.selectList(qw);
        int sent = 0;
        for (WechatFan f : fans) {
            if (sent >= 500) {
                break;
            }
            Long customerId = customerIdByUnion(f.getUnionId());
            if (customerId != null) {
                messageService.send(tid, customerId, code == null ? "WECHAT_MASS" : code, title, content);
                sent++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("targets", fans.size());
        m.put("sent", sent);
        m.put("limited", fans.size() > 500);
        return R.ok(m);
    }

    private final com.meiye.domain.mapper.CustomerMapper customerMapper;

    private Long customerIdByUnion(String unionId) {
        if (unionId == null) {
            return null;
        }
        var c = customerMapper.selectOne(new LambdaQueryWrapper<com.meiye.domain.entity.Customer>()
                .eq(com.meiye.domain.entity.Customer::getUnionId, unionId).last("limit 1"));
        return c == null ? null : c.getId();
    }
}
