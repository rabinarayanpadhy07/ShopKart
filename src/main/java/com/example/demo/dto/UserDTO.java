package com.example.demo.dto;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import java.time.LocalDateTime;

public class UserDTO {
    private Integer userId;
    private String username;
    private String email;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
    }

    // Getters and Setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
