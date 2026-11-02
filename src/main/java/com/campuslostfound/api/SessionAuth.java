package com.campuslostfound.api;

import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class SessionAuth {
    public static final String USER_ID = "campusUserId";
    private final UserDAO users;

    public SessionAuth(UserDAO users) { this.users = users; }

    public User requireUser(HttpSession session) {
        Object id = session.getAttribute(USER_ID);
        if (!(id instanceof Integer userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required.");
        return users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session user no longer exists."));
    }

    public User requireAdmin(HttpSession session) {
        User user = requireUser(session);
        if (!user.isAdmin()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required.");
        return user;
    }
}
