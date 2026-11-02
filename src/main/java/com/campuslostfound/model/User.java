package com.campuslostfound.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    private int userId;
    private String name;
    private String email;
    private String passwordHash;
    private UserRole role;
    private LocalDateTime createdAt;

    public User(String name, String email, String passwordHash, UserRole role) {
        this(0, name, email, passwordHash, role, LocalDateTime.now());
    }

    public User(String name, String email, String passwordHash) {
        this(0, name, email, passwordHash, UserRole.STUDENT, LocalDateTime.now());
    }

    public User(int userId, String name, String email, String passwordHash, UserRole role, LocalDateTime createdAt) {
        setUserId(userId);
        setName(name);
        setEmail(email);
        setPasswordHash(passwordHash);
        setRole(role);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        if (userId < 0) {
            throw new IllegalArgumentException("User ID cannot be negative.");
        }
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Password hash cannot be empty.");
        }
        this.passwordHash = passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        if (role == null) {
            throw new IllegalArgumentException("User role must be specified.");
        }
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return userId == user.userId && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}
