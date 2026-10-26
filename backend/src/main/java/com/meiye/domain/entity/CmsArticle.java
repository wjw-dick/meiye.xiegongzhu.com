package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "cms_article", autoResultMap = true)
public class CmsArticle extends TenantEntity {
    private String type;
    private String title;
    private String titleEn;
    private String summary;
    private String content;
    private String contentEn;
    private String cover;
    private String lang;
    private String seoKeywords;
    private String seoDescription;
    private Integer status;
    private Integer sort;
    private LocalDateTime publishedAt;
}
