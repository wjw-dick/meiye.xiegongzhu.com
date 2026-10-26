package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wechat_material", autoResultMap = true)
public class WechatMaterial extends TenantEntity {
    private String type;
    private String title;
    private String content;
    private String mediaId;
    private String url;
    private Integer status;
}
