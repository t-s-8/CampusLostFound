package com.campuslostfound.api;

import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import com.campuslostfound.service.AuthService;
import com.campuslostfound.service.PasswordHasher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserDAO users;
    private final AuthService auth;
    private final SessionAuth sessions;
    private final SecureRandom random = new SecureRandom();

    public AuthController(UserDAO users, AuthService auth, SessionAuth sessions) {
        this.users = users;
        this.auth = auth;
        this.sessions = sessions;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpSession session) {
        String token = (String) session.getAttribute(CsrfTokenFilter.TOKEN);
        if (token == null) {
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute(CsrfTokenFilter.TOKEN, token);
        }
        return Map.of("token", token);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiUser register(@RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        HttpSession session = servletRequest.getSession(true);
        if (request.name() == null || request.name().isBlank() || request.name().length() > 100
                || request.email() == null || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || request.password() == null || request.password().length() < 10 || request.password().length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a name, valid email, and password of at least 10 characters.");
        }
        try {
            User user = users.create(new User(request.name().trim(), request.email().trim().toLowerCase(),
                    PasswordHasher.encode(request.password()), UserRole.STUDENT));
            servletRequest.changeSessionId();
            session.setAttribute(SessionAuth.USER_ID, user.getUserId());
            rotateCsrf(session);
            return ApiUser.from(user);
        } catch (RuntimeException ex) {
            if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists.");
            }
            throw ex;
        }
    }

    @PostMapping("/login")
    public ApiUser login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (request.email() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email and password are required.");
        }
        User user;
        try {
            user = auth.authenticate(request.email(), request.password());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        if (!PasswordHasher.isEncoded(user.getPasswordHash())) {
            users.updatePasswordHash(user.getUserId(), PasswordHasher.encode(request.password()));
        }
        HttpSession session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(SessionAuth.USER_ID, user.getUserId());
        rotateCsrf(session);
        user.setPasswordHash("web-session");
        return ApiUser.from(user);
    }

    @GetMapping("/me")
    public ApiUser me(HttpSession session) { return ApiUser.from(sessions.requireUser(session)); }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession session) { session.invalidate(); }

    private void rotateCsrf(HttpSession session) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        session.setAttribute(CsrfTokenFilter.TOKEN, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public record RegisterRequest(String name, String email, String password) { }
    public record LoginRequest(String email, String password) { }
}
