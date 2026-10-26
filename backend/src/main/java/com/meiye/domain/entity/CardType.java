package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_card_type", autoResultMap = true)
public class CardType extends TenantEntity {
    private String bizType;
    private String name;
    private String type;
    private BigDecimal price;
    private BigDecimal faceValue;
    private Integer times;
    private Integer validDays;
    private BigDecimal bonus;
    private BigDecimal discount;
    private BigDecimal commissionRate;
    private Integer status;
    private String description;
}
