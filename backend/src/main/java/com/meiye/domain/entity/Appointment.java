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
@TableName(value = "t_appointment", autoResultMap = true)
public class Appointment extends TenantEntity {
    private String appointmentNo;
    private String bizType;
    private Long customerId;
    private Long storeId;
    private Long technicianId;
    private Long itemId;
    private Long packageId;
    private Long workstationId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private String source;
    private BigDecimal amount;
    private String remark;
    private Long createdBy;
    private Integer released;
}
