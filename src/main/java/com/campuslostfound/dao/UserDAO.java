package com.campuslostfound.dao;

import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {

    public User create(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getRole().name());
            stmt.setTimestamp(5, Timestamp.valueOf(user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now()));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new CampusLostFoundException("Creating user failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    user.setUserId(generatedKeys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error creating user: " + e.getMessage(), e);
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT user_id, name, email, password_hash, role, created_at FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email != null ? email.trim().toLowerCase() : "");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding user by email: " + e.getMessage(), e);
        }
    }

    public Optional<User> findById(int userId) {
        String sql = "SELECT user_id, name, email, password_hash, role, created_at FROM users WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding user by ID: " + e.getMessage(), e);
        }
    }


    public boolean updatePasswordHash(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error updating password hash.", e);
        }
    }


    public boolean configureBootstrapAdmin(int userId, String passwordHash) {
        String sql = "UPDATE users SET role = 'ADMIN', password_hash = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error configuring bootstrap administrator.", e);
        }
    }

    public List<User> findAll() {
        String sql = "SELECT user_id, name, email, password_hash, role, created_at FROM users ORDER BY user_id";
        List<User> users = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error retrieving all users: " + e.getMessage(), e);
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String pwdHash = rs.getString("password_hash");
        UserRole role = UserRole.valueOf(rs.getString("role"));
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        if (role == UserRole.ADMIN) {
            return new Admin(id, name, email, pwdHash, "Administration", createdAt);
        } else {
            return new User(id, name, email, pwdHash, role, createdAt);
        }
    }
}
