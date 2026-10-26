package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wechat_config", autoResultMap = true)
public class WechatConfig extends TenantEntity {
    private String miniAppId;
    @TableField(typeHandler = AesTypeHandler.class)
    private String miniSecret;
    private String mpAppId;
    @TableField(typeHandler = AesTypeHandler.class)
    private String mpSecret;
    private String mpToken;
    @TableField(typeHandler = AesTypeHandler.class)
    private String mpAesKey;
    private String mpUrl;
    private String unionAppId;
    private String appAppId;
    @TableField(typeHandler = AesTypeHandler.class)
    private String appSecret;
    private String subscribeIds;
    private String pushChannel;
}
