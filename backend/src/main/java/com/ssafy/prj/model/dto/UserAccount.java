package com.ssafy.prj.model.dto;

import java.io.Serializable;

public class UserAccount implements Serializable {

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    private String userId;
    private String name;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;
    private String createdAt;
    private String updatedAt;

    public UserAccount() {
    }

    public UserAccount(String userId, String name, String email, String phone, String passwordHash, String createdAt,
            String updatedAt) {
        this(userId, name, email, phone, passwordHash, ROLE_USER, createdAt, updatedAt);
    }

    public UserAccount(String userId, String name, String email, String phone, String passwordHash, String role,
            String createdAt, String updatedAt) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
