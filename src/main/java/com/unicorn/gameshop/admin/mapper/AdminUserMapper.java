package com.unicorn.gameshop.admin.mapper;

import com.unicorn.gameshop.admin.model.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminUserMapper {

    AdminUser findByUsername(@Param("username") String username);
}
