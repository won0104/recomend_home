package com.ssafy.prj.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.ssafy.prj.model.dto.UserAccount;

@Mapper
public interface UserAccountMapper {

    List<UserAccount> findAll();

    UserAccount findByUserId(String userId);

    int insert(UserAccount user);

    int update(UserAccount user);

    int updateRole(@Param("userId") String userId, @Param("role") String role);

    int delete(String userId);
}
