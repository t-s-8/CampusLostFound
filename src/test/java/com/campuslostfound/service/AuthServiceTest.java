package com.campuslostfound.service;

import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    private AuthService authService;
    private InMemoryUserDAO userDAO;

    private static class InMemoryUserDAO extends UserDAO {
        private final Map<Integer, User> userMap = new HashMap<>();
        private final Map<String, User> emailMap = new HashMap<>();
        private int idSequence = 1;

        @Override
        public User create(User user) {
            user.setUserId(idSequence++);
            userMap.put(user.getUserId(), user);
            emailMap.put(user.getEmail().toLowerCase(), user);
            return user;
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return Optional.ofNullable(emailMap.get(email.toLowerCase()));
        }

        @Override
        public Optional<User> findById(int userId) {
            return Optional.ofNullable(userMap.get(userId));
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(userMap.values());
        }
    }

    @BeforeEach
    void setUp() {
        userDAO = new InMemoryUserDAO();
        authService = new AuthService(userDAO);
    }

    @Test
    @DisplayName("Register a new student user successfully")
    void testRegisterStudentSuccess() {
        User user = authService.register("Alice Student", "alice@test.edu", "Password123", UserRole.STUDENT);
        assertNotNull(user);
        assertTrue(user.getUserId() > 0);
        assertEquals("Alice Student", user.getName());
        assertEquals("alice@test.edu", user.getEmail());
        assertEquals(UserRole.STUDENT, user.getRole());
        assertFalse(user.isAdmin());

        assertNotEquals("Password123", user.getPasswordHash());
        assertEquals(64, user.getPasswordHash().length());
    }

    @Test
    @DisplayName("Register an admin user successfully")
    void testRegisterAdminSuccess() {
        User admin = authService.register("Campus Admin", "admin@test.edu", "AdminSecret123", UserRole.ADMIN);
        assertTrue(admin instanceof Admin);
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.isAdmin());
    }

    @Test
    @DisplayName("Registration rejects duplicate email addresses")
    void testDuplicateEmailRegistration() {
        authService.register("Alice Student", "alice@test.edu", "Password123", UserRole.STUDENT);

        assertThrows(CampusLostFoundException.class, () ->
                authService.register("Alice Clone", "alice@test.edu", "AnotherPwd123", UserRole.STUDENT)
        );
    }

    @Test
    @DisplayName("Registration input validation checks")
    void testRegistrationValidation() {
        assertThrows(CampusLostFoundException.class, () ->
                authService.register("", "test@test.edu", "Password123", UserRole.STUDENT)
        );

        assertThrows(CampusLostFoundException.class, () ->
                authService.register("Test User", "invalid-email", "Password123", UserRole.STUDENT)
        );

        assertThrows(CampusLostFoundException.class, () ->
                authService.register("Test User", "test@test.edu", "12345", UserRole.STUDENT)
        );
    }

    @Test
    @DisplayName("Authentication succeeds with correct credentials")
    void testAuthenticationSuccess() {
        authService.register("Bob", "bob@test.edu", "SecretPass123", UserRole.STUDENT);

        User authenticated = authService.authenticate("bob@test.edu", "SecretPass123");
        assertNotNull(authenticated);
        assertEquals("Bob", authenticated.getName());
    }

    @Test
    @DisplayName("Authentication fails with incorrect password or non-existent email")
    void testAuthenticationFailure() {
        authService.register("Bob", "bob@test.edu", "SecretPass123", UserRole.STUDENT);

        assertThrows(CampusLostFoundException.class, () ->
                authService.authenticate("bob@test.edu", "WrongPassword")
        );

        assertThrows(CampusLostFoundException.class, () ->
                authService.authenticate("unknown@test.edu", "SecretPass123")
        );
    }

    @Test
    @DisplayName("Verify SHA-256 hash matches expected output")
    void testPasswordHashingAlgorithm() {
        String hash = AuthService.hashPassword("DevPass@123");
        assertNotNull(hash);
        assertEquals(64, hash.length());
        assertEquals(hash, AuthService.hashPassword("DevPass@123"));
    }
}
