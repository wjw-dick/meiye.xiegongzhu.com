package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_tech_rotation", autoResultMap = true)
public class TechRotation extends TenantEntity {
    private Long storeId;
    private Long technicianId;
    private String bizType;
    private LocalDate rotationDate;
    private Integer seq;
    private String status;
}
