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
@TableName(value = "t_payment", autoResultMap = true)
public class Payment extends TenantEntity {
    private Long orderId;
    private String payNo;
    private String channel;
    private BigDecimal amount;
    private String status;
    private String tradeNo;
    private LocalDateTime paidAt;
}
