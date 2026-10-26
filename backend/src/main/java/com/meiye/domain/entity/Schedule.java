package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_schedule", autoResultMap = true)
public class Schedule extends TenantEntity {
    private Long storeId;
    private Long technicianId;
    private LocalDate workDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String bizType;
}
