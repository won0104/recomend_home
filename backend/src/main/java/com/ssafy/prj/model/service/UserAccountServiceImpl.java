package com.ssafy.prj.model.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.model.mapper.UserAccountMapper;
import com.ssafy.prj.util.TimeUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UserAccount> findAll() throws Exception {
        return userAccountMapper.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount findByUserId(String userId) throws Exception {
        return userAccountMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public UserAccount register(String userId, String name, String email, String phone, String rawPassword)
            throws Exception {
        if (isBlank(userId) || isBlank(name) || isBlank(email) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("필수 회원 정보가 누락되었습니다.");
        }

        if (userAccountMapper.findByUserId(userId) != null) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        String now = TimeUtil.now();
        UserAccount user = new UserAccount(userId, name, email, phone == null ? "" : phone,
                passwordEncoder.encode(rawPassword), UserAccount.ROLE_USER, now, now);
        userAccountMapper.insert(user);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public UserAccount login(String userId, String rawPassword) throws Exception {
        if (isBlank(userId) || rawPassword == null) {
            return null;
        }

        UserAccount user = userAccountMapper.findByUserId(userId);
        if (user == null) {
            return null;
        }
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return null;
        }
        return user;
    }

    @Override
    @Transactional
    public UserAccount update(String userId, String name, String email, String phone, String rawPassword)
            throws Exception {
        if (isBlank(name) || isBlank(email)) {
            throw new IllegalArgumentException("이름과 이메일은 필수입니다.");
        }

        UserAccount user = userAccountMapper.findByUserId(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }

        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone == null ? "" : phone);
        if (!isBlank(rawPassword)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
        }
        user.setUpdatedAt(TimeUtil.now());
        userAccountMapper.update(user);
        return userAccountMapper.findByUserId(userId);
    }

    @Override
    @Transactional
    public UserAccount changeRole(String actorUserId, String targetUserId, String newRole) throws Exception {
        if (isBlank(targetUserId)) {
            throw new IllegalArgumentException("대상 사용자 ID가 없습니다.");
        }
        if (!UserAccount.ROLE_USER.equals(newRole) && !UserAccount.ROLE_ADMIN.equals(newRole)) {
            throw new IllegalArgumentException("권한 값은 USER 또는 ADMIN 이어야 합니다.");
        }
        if (actorUserId != null && actorUserId.equals(targetUserId)
                && UserAccount.ROLE_USER.equals(newRole)) {
            throw new IllegalArgumentException("본인의 관리자 권한은 스스로 해제할 수 없습니다.");
        }

        UserAccount target = userAccountMapper.findByUserId(targetUserId);
        if (target == null) {
            throw new IllegalArgumentException("대상 사용자를 찾을 수 없습니다.");
        }
        if (!newRole.equals(target.getRole())) {
            userAccountMapper.updateRole(targetUserId, newRole);
        }
        return userAccountMapper.findByUserId(targetUserId);
    }

    @Override
    @Transactional
    public boolean delete(String userId) throws Exception {
        return userAccountMapper.delete(userId) == 1;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
