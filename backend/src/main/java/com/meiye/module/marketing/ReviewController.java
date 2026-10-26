package com.meiye.module.marketing;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.context.TenantContext;
import com.meiye.common.exception.BizException;
import com.meiye.common.web.BaseCrudController;
import com.meiye.common.web.R;
import com.meiye.domain.entity.Review;
import com.meiye.domain.entity.Technician;
import com.meiye.domain.entity.FollowUp;
import com.meiye.domain.mapper.FollowUpMapper;
import com.meiye.domain.mapper.ReviewMapper;
import com.meiye.domain.mapper.TechnicianMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tenant/reviews")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR','TENANT_ADMIN','STORE_MANAGER','CASHIER','TECHNICIAN','CUSTOMER')")
@Audit(module = "评价管理")
@RequiredArgsConstructor
public class ReviewController extends BaseCrudController<Review> {

    private final ReviewMapper mapper;
    private final TechnicianMapper technicianMapper;
    private final FollowUpMapper followUpMapper;

    @Override
    protected BaseMapper<Review> mapper() {
        return mapper;
    }

    @PostMapping("/submit")
    @Audit(action = "提交评价")
    public R<Review> submit(@RequestBody Review body) {
        if (body.getCustomerId() == null) {
            throw new BizException(400, "缺少会员");
        }
        if (body.getScore() == null || body.getScore() < 1 || body.getScore() > 5) {
            throw new BizException(400, "评分需在 1~5 之间");
        }
        body.setStatus(body.getStatus() == null ? "PUBLISHED" : body.getStatus());
        mapper.insert(body);
        refreshTechRating(body.getTechnicianId());
        if (body.getScore() != null && body.getScore() <= 2) {
            FollowUp f = new FollowUp();
            f.setCustomerId(body.getCustomerId());
            f.setOrderId(body.getOrderId());
            f.setType("COMPLAINT");
            f.setDueAt(LocalDateTime.now().plusHours(2));
            f.setStatus("PENDING");
            f.setContent("差评自动介入：评分 " + body.getScore() + " 分"
                    + (body.getContent() == null || body.getContent().isBlank() ? "" : "，客户反馈：" + body.getContent()));
            followUpMapper.insert(f);
        }
        return R.ok(body);
    }

    @PostMapping("/{id}/reply")
    @Audit(action = "回复评价")
    public R<Review> reply(@PathVariable Long id, @RequestParam String reply) {
        Review r = mapper.selectById(id);
        if (r == null) {
            throw BizException.notFound("评价不存在");
        }
        r.setReply(reply);
        r.setRepliedAt(LocalDateTime.now());
        mapper.updateById(r);
        return R.ok(r);
    }

    @GetMapping("/stats")
    public R<Map<String, Object>> stats(@RequestParam(required = false) Long technicianId) {
        Long tid = TenantContext.requireTenantId();
        LambdaQueryWrapper<Review> qw = new LambdaQueryWrapper<Review>().eq(Review::getTenantId, tid);
        if (technicianId != null) {
            qw.eq(Review::getTechnicianId, technicianId);
        }
        List<Review> list = mapper.selectList(qw);
        double avg = list.stream().mapToInt(r -> r.getScore() == null ? 5 : r.getScore()).average().orElse(0);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", list.size());
        m.put("avgScore", BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        m.put("fiveStar", list.stream().filter(r -> r.getScore() != null && r.getScore() == 5).count());
        m.put("lowScore", list.stream().filter(r -> r.getScore() != null && r.getScore() <= 3).count());
        m.put("unreplied", list.stream().filter(r -> r.getReply() == null || r.getReply().isBlank()).count());
        return R.ok(m);
    }

    private void refreshTechRating(Long techId) {
        if (techId == null) {
            return;
        }
        Technician t = technicianMapper.selectById(techId);
        if (t == null) {
            return;
        }
        List<Review> list = mapper.selectList(new LambdaQueryWrapper<Review>()
                .eq(Review::getTechnicianId, techId).eq(Review::getStatus, "PUBLISHED"));
        double avg = list.stream().mapToInt(r -> r.getScore() == null ? 5 : r.getScore()).average().orElse(0);
        t.setRating(BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP));
        technicianMapper.updateById(t);
    }
}
