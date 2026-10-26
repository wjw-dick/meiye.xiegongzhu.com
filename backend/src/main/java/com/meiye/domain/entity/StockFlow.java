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
@TableName(value = "t_stock_flow", autoResultMap = true)
public class StockFlow extends TenantEntity {
    private Long itemId;
    private String type;
    private BigDecimal quantity;
    private BigDecimal cost;
    private Long orderId;
    private String operator;
    private String remark;
}
