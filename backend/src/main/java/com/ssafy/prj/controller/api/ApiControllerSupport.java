package com.ssafy.prj.controller.api;

import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.security.dto.CustomUserDetails;
import com.ssafy.prj.security.web.SecurityResponseHelper;

final class ApiControllerSupport {

    static final String LOGIN_REQUIRED = "로그인이 필요합니다.";
    static final String ADMIN_REQUIRED = "관리자 권한이 필요합니다.";

    private ApiControllerSupport() {
    }

    static UserAccount requireLogin(CustomUserDetails details) {
        UserAccount user = details == null ? null : details.getUser();
        if (user == null) {
            throw new AuthenticationCredentialsNotFoundException(LOGIN_REQUIRED);
        }
        return user;
    }

    static UserAccount requireAdmin(CustomUserDetails details) {
        UserAccount user = requireLogin(details);
        if (!user.isAdmin()) {
            throw new AccessDeniedException(ADMIN_REQUIRED);
        }
        return user;
    }

    static Map<String, Object> sanitize(UserAccount user) {
        return SecurityResponseHelper.sanitize(user);
    }
}
