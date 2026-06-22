package com.ssafy.prj.security.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ssafy.prj.model.dto.UserAccount;
import com.ssafy.prj.security.dto.CustomUserDetails;

public final class AuthViewHelper {

    private AuthViewHelper() {
    }

    public static UserAccount currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails details)) {
            return null;
        }
        return details.getUser();
    }

    public static boolean isAuthenticated() {
        return currentUser() != null;
    }

    public static boolean isAdmin() {
        UserAccount user = currentUser();
        return user != null && user.isAdmin();
    }
}
