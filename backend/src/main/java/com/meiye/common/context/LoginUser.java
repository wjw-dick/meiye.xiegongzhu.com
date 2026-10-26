package com.meiye.common.context;

import lombok.Data;

@Data
public class LoginUser {

    private Long userId;
    private String username;
    private String name;
    private String userType;
    private String role;
    private Long tenantId;
    private String tenantCode;
    private Long storeId;
    private Long technicianId;
    private Long customerId;
}
