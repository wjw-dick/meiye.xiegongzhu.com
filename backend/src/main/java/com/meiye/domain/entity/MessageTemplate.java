package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "message_template", autoResultMap = true)
public class MessageTemplate extends TenantEntity {
    private String code;
    private String name;
    private String templateId;
    private String channel;
    private String fieldsJson;
    private Integer enabled;
}
