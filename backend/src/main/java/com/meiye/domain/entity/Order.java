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
@TableName(value = "t_order", autoResultMap = true)
public class Order extends TenantEntity {
    private String orderNo;
    private String bizType;
    private Long customerId;
    private Long storeId;
    private Long technicianId;
    private Long appointmentId;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal paidAmount;
    private String payType;
    private String status;
    private Long cashierId;
    private LocalDateTime paidAt;
    private String remark;
}
