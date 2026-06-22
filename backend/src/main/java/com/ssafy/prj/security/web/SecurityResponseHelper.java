package com.ssafy.prj.security.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.prj.model.dto.ApiResponse;
import com.ssafy.prj.model.dto.UserAccount;

import jakarta.servlet.http.HttpServletResponse;

public final class SecurityResponseHelper {

    private SecurityResponseHelper() {
    }

    public static void writeJson(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        new ObjectMapper().writeValue(response.getWriter(), body);
    }

    public static Map<String, Object> sanitize(UserAccount user) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("userId", user.getUserId());
        data.put("name", user.getName());
        data.put("email", user.getEmail());
        data.put("phone", user.getPhone());
        data.put("role", user.getRole());
        data.put("admin", user.isAdmin());
        data.put("createdAt", user.getCreatedAt());
        data.put("updatedAt", user.getUpdatedAt());
        return data;
    }

    public static ApiResponse<Map<String, Object>> ok(String message, UserAccount user) {
        return ApiResponse.ok(message, sanitize(user));
    }
}
