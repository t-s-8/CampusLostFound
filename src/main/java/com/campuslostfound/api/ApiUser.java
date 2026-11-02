package com.campuslostfound.api;

import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;

public record ApiUser(int userId, String name, String email, UserRole role) {
    public static ApiUser from(User user) {
        return new ApiUser(user.getUserId(), user.getName(), user.getEmail(), user.getRole());
    }
}
