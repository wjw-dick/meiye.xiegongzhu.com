package com.meiye.module.platform;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.common.audit.Audit;
import com.meiye.common.web.BaseCrudController;
import com.meiye.domain.entity.Language;
import com.meiye.domain.mapper.LanguageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform/languages")
@PreAuthorize("hasAnyRole('SYSTEM','OPERATOR')")
@Audit(module = "多语言")
@RequiredArgsConstructor
public class LanguageController extends BaseCrudController<Language> {

    private final LanguageMapper mapper;

    @Override
    protected BaseMapper<Language> mapper() {
        return mapper;
    }
}
