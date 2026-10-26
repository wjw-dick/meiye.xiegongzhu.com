package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_service_flow_template", autoResultMap = true)
public class ServiceFlowTemplate extends TenantEntity {
    private String bizType;
    private Long itemId;
    private String name;
    private String stepsJson;
    private Integer status;
}
