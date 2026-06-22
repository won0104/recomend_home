package com.ssafy.prj.model.service;

import java.util.List;

import com.ssafy.prj.model.dto.UserAccount;

public interface UserAccountService {

    List<UserAccount> findAll() throws Exception;

    UserAccount findByUserId(String userId) throws Exception;

    UserAccount register(String userId, String name, String email, String phone, String rawPassword) throws Exception;

    UserAccount login(String userId, String rawPassword) throws Exception;

    UserAccount update(String userId, String name, String email, String phone, String rawPassword) throws Exception;

    UserAccount changeRole(String actorUserId, String targetUserId, String newRole) throws Exception;

    boolean delete(String userId) throws Exception;
}
