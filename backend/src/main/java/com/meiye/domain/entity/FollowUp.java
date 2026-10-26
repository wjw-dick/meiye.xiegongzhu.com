package com.meiye.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.baomidou.mybatisplus.annotation.TableField;
import com.meiye.common.crypto.AesTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_follow_up", autoResultMap = true)
public class FollowUp extends TenantEntity {
    private Long customerId;
    private Long orderId;
    private String type;
    private LocalDateTime dueAt;
    private String status;
    private String content;
    private String result;
    private Long handlerId;
}
