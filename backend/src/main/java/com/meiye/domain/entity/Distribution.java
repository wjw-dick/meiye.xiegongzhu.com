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
@TableName(value = "t_distribution", autoResultMap = true)
public class Distribution extends TenantEntity {
    private Long customerId;
    private Long parentCustomerId;
    private Long orderId;
    private Integer level;
    private BigDecimal amount;
    private BigDecimal rate;
    private BigDecimal commission;
    private String status;
}
