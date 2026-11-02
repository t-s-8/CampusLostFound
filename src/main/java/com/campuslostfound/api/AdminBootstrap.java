package com.campuslostfound.api;

import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.User;
import com.campuslostfound.service.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserDAO users;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(UserDAO users,
                          @Value("${WEB_BOOTSTRAP_ADMIN_EMAIL:}") String email,
                          @Value("${WEB_BOOTSTRAP_ADMIN_PASSWORD:}") String password,
                          @Value("${WEB_BOOTSTRAP_ADMIN_NAME:Campus Administrator}") String name) {
        this.users=users; this.email=email; this.password=password; this.name=name;
    }

    @Override public void run(ApplicationArguments args) {
        if ((email == null || email.isBlank()) && (password == null || password.isBlank())) return;
        if (email == null || email.isBlank() || password == null || password.length() < 16) {
            throw new IllegalStateException("Set both WEB_BOOTSTRAP_ADMIN_EMAIL and WEB_BOOTSTRAP_ADMIN_PASSWORD; password must be at least 16 characters.");
        }
        String normalized=email.trim().toLowerCase();
        String hash=PasswordHasher.encode(password);
        User existing=users.findByEmail(normalized).orElse(null);
        if (existing == null) {
            users.create(new Admin(name, normalized, hash, "Campus Administration"));
        } else if (!users.configureBootstrapAdmin(existing.getUserId(), hash)) {
            throw new IllegalStateException("Could not configure the bootstrap administrator.");
        }
    }
}
