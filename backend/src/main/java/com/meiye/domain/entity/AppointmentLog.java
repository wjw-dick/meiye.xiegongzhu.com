package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_appointment_log", autoResultMap = true)
public class AppointmentLog extends TenantEntity {
    private Long appointmentId;
    private String action;
    private String beforeJson;
    private String afterJson;
    private String operator;
    private String remark;
}
