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
@TableName(value = "t_card_flow", autoResultMap = true)
public class CardFlow extends TenantEntity {
    private Long cardId;
    private Long customerId;
    private String type;
    private BigDecimal amount;
    private Integer times;
    private BigDecimal balanceAfter;
    private Integer timesAfter;
    private Long orderId;
    private String remark;
    private String operator;
}
