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
@TableName(value = "t_technician_cert", autoResultMap = true)
public class TechnicianCert extends TenantEntity {
    private Long technicianId;
    private String certType;
    private String certNo;
    private String issuer;
    private LocalDate expireAt;
    private String fileUrl;
    private String status;
}
