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
@TableName(value = "t_review", autoResultMap = true)
public class Review extends TenantEntity {
    private Long customerId;
    private Long orderId;
    private Long technicianId;
    private Integer score;
    private String tags;
    private String content;
    private String images;
    private String reply;
    private String status;
    private LocalDateTime repliedAt;
}
