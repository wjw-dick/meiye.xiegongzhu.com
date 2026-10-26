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
@TableName(value = "t_after_sale", autoResultMap = true)
public class AfterSale extends TenantEntity {
    private Long orderId;
    private Long customerId;
    private String type;
    private String reason;
    private String status;
    private String result;
    private Long handleBy;
    private BigDecimal compensation;
}
