package com.meiye.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meiye.domain.entity.TenantUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantUserMapper extends BaseMapper<TenantUser> {
}
