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
@TableName(value = "wechat_fan", autoResultMap = true)
public class WechatFan extends TenantEntity {
    private String openid;
    private String unionId;
    private String nickname;
    private String avatar;
    private String tags;
    private Integer subscribed;
    private LocalDateTime subscribedAt;
}
