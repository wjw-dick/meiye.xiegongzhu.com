package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "audit_log", autoResultMap = true)
public class AuditLog extends TenantEntity {
    private Long operatorId;
    private String operatorName;
    private String userType;
    private String module;
    private String action;
    private String target;
    private String detail;
    private String ip;
}
