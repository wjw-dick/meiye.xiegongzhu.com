package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "biz_type_plugin", autoResultMap = true)
public class BizTypePlugin extends BaseEntity {
    private String code;
    private String name;
    private String icon;
    private Integer defaultEnabled;
    private Integer medical;
    private String subModules;
    private String fieldTemplate;
    private String defaultCommission;
    private Integer sort;
    private Integer status;
}
