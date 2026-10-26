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
@TableName(value = "t_coupon", autoResultMap = true)
public class Coupon extends TenantEntity {
    private Long customerId;
    private String name;
    private String type;
    private BigDecimal amount;
    private BigDecimal discount;
    private BigDecimal minAmount;
    private LocalDateTime startAt;
    private LocalDateTime expireAt;
    private String status;
    private String source;
}
