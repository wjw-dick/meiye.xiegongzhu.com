package com.meiye.module.portal;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.CmsArticle;
import com.meiye.domain.mapper.CmsArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 门户内容管理：公告/资讯/轮播/品牌故事，支持中英双语与 SEO 字段。
 */
@RestController
@RequestMapping("/api/tenant/cms/articles")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','MARKETING')")
@Audit(module = "门户内容")
@RequiredArgsConstructor
public class CmsArticleController extends BaseCrudController<CmsArticle> {

    private final CmsArticleMapper mapper;

    @Override
    protected BaseMapper<CmsArticle> mapper() {
        return mapper;
    }

    @PostMapping("/publish/{id}")
    @Audit(action = "发布内容")
    public R<CmsArticle> publish(@PathVariable Long id) {
        CmsArticle a = mapper.selectById(id);
        if (a == null) {
            return R.fail(404, "内容不存在");
        }
        a.setStatus(1);
        a.setPublishedAt(LocalDateTime.now());
        mapper.updateById(a);
        return R.ok(a);
    }

    @PostMapping("/offline/{id}")
    @Audit(action = "下线内容")
    public R<CmsArticle> offline(@PathVariable Long id) {
        CmsArticle a = mapper.selectById(id);
        if (a == null) {
            return R.fail(404, "内容不存在");
        }
        a.setStatus(0);
        mapper.updateById(a);
        return R.ok(a);
    }
}
