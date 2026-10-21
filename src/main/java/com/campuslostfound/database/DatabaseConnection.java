package com.campuslostfound.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Factory and connection manager for standard JDBC connections.
 *
 * Concepts Demonstrated:
 * 1. JDBC Driver Registration and Connection Factory.
 * 2. Separation of Connection Management from Business Logic.
 * 3. Graceful connectivity probe (isDatabaseAvailable).
 */
public class DatabaseConnection {

    static {
        try {
            // Explicitly load MySQL JDBC driver class
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] MySQL JDBC Driver not found on classpath: " + e.getMessage());
        }
    }

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Creates and returns a new JDBC Connection using the configured credentials.
     * The caller is responsible for closing this connection via try-with-resources.
     *
     * @return Open java.sql.Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUser(),
                DatabaseConfig.getPassword()
        );
    }

    /**
     * Probes if the target MySQL database is reachable without throwing unhandled errors.
     * Useful for diagnostics and test assumptions.
     *
     * @return true if database responds within 1 second, false otherwise
     */
    public static boolean isDatabaseAvailable() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(1);
        } catch (Exception e) {
            return false;
        }
    }
}
