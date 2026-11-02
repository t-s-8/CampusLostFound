package com.campuslostfound.ui;

import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;

public class SessionManager {

    private static User currentUser;

    private SessionManager() {
    }

    public static synchronized void login(User user) {
        currentUser = user;
    }

    public static synchronized void logout() {
        currentUser = null;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public static synchronized boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == UserRole.ADMIN;
    }
}
