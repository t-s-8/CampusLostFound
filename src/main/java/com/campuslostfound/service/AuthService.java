package com.campuslostfound.service;

import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Service managing user registration, authentication, password hashing, and role authorization.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new student or admin user.
     * Validates input, prevents duplicate emails, and hashes the password with SHA-256.
     */
    public User register(String name, String email, String password, UserRole role) {
        if (name == null || name.trim().isEmpty()) {
            throw new CampusLostFoundException("Name cannot be empty.");
        }
        if (email == null || !email.contains("@")) {
            throw new CampusLostFoundException("A valid email address is required.");
        }
        if (password == null || password.length() < 6) {
            throw new CampusLostFoundException("Password must be at least 6 characters long.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        Optional<User> existing = userDAO.findByEmail(normalizedEmail);
        if (existing.isPresent()) {
            throw new CampusLostFoundException("An account with email " + normalizedEmail + " already exists.");
        }

        String hashedPassword = hashPassword(password);
        User newUser;
        if (role == UserRole.ADMIN) {
            newUser = new Admin(name.trim(), normalizedEmail, hashedPassword, "Administration");
        } else {
            newUser = new User(name.trim(), normalizedEmail, hashedPassword, UserRole.STUDENT);
        }

        return userDAO.create(newUser);
    }

    /**
     * Authenticates a user using email and plaintext password.
     * Compares the SHA-256 digest with the stored password_hash.
     */
    public User authenticate(String email, String password) {
        if (email == null || password == null) {
            throw new CampusLostFoundException("Email and password must not be null.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        User user = userDAO.findByEmail(normalizedEmail)
                .orElseThrow(() -> new CampusLostFoundException("Invalid email or password."));

        String inputHash = hashPassword(password);
        if (!inputHash.equalsIgnoreCase(user.getPasswordHash())) {
            throw new CampusLostFoundException("Invalid email or password.");
        }

        return user;
    }

    /**
     * Computes a deterministic SHA-256 hexadecimal hash string.
     * Exactly matches the MySQL native SHA2(password, 256) function used in database seeds.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CampusLostFoundException("Error computing password hash: " + e.getMessage(), e);
        }
    }

    /**
     * Looks up a user by ID.
     */
    public Optional<User> getUserById(int userId) {
        return userDAO.findById(userId);
    }
}
