package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_marketing_activity", autoResultMap = true)
public class MarketingActivity extends TenantEntity {
    private String bizType;
    private String type;
    private String name;
    private BigDecimal price;
    private BigDecimal originPrice;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String configJson;
    private Integer status;
}
