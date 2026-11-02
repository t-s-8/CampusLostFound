package com.campuslostfound.model;

import java.time.LocalDateTime;

public class Admin extends User {

    private String department;

    public Admin(String name, String email, String passwordHash, String department) {
        super(name, email, passwordHash, UserRole.ADMIN);
        this.department = department != null ? department.trim() : "Campus Security & Lost/Found";
    }

    public Admin(int userId, String name, String email, String passwordHash, String department, LocalDateTime createdAt) {
        super(userId, name, email, passwordHash, UserRole.ADMIN, createdAt);
        this.department = department != null ? department.trim() : "Campus Security & Lost/Found";
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department != null ? department.trim() : "General Administration";
    }

    public boolean canReviewClaims() {
        return true;
    }

    @Override
    public String toString() {
        return "Admin{" +
                "userId=" + getUserId() +
                ", name='" + getName() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", department='" + department + '\'' +
                '}';
    }
}
