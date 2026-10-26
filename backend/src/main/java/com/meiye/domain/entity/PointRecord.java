package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_point_record", autoResultMap = true)
public class PointRecord extends TenantEntity {
    private Long customerId;
    private Integer points;
    private String type;
    private Long orderId;
    private String remark;
}
