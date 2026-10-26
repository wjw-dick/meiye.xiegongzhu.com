package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_tech_service_log", autoResultMap = true)
public class TechServiceLog extends TenantEntity {
    private Long technicianId;
    private Long appointmentId;
    private Long orderId;
    private Long customerId;
    private String content;
    private String images;
    private Integer duration;
}
