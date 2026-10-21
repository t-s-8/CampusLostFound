package com.campuslostfound.database;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.dao.MatchDAO;
import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.Location;
import com.campuslostfound.model.Match;
import com.campuslostfound.model.User;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MySQL Live Database Integration Tests")
class DatabaseIntegrationTest {

    @BeforeAll
    static void checkDatabaseAvailability() {
        // Enforce that tests only run if local MySQL is accessible.
        // If credentials are not configured or MySQL is offline, the test is honestly skipped.
        boolean available = DatabaseConnection.isDatabaseAvailable();
        Assumptions.assumeTrue(available,
                "Local MySQL database is not reachable or credentials not configured. Skipping live database integration tests.");
    }

    @Test
    @DisplayName("Verify valid JDBC connection to campus_lost_found")
    void testJdbcConnection() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn, "Connection should not be null");
            assertTrue(conn.isValid(2), "Connection should be valid within 2 seconds");
            assertEquals("campus_lost_found", conn.getCatalog(), "Target catalog must be campus_lost_found");
        }
    }

    @Test
    @DisplayName("Verify UserDAO queries against seeded users")
    void testUserDAOIntegration() {
        UserDAO userDAO = new UserDAO();
        Optional<User> admin = userDAO.findByEmail("admin@campus.edu");
        assertTrue(admin.isPresent(), "Seeded admin@campus.edu should be found in database");
        assertTrue(admin.get().isAdmin(), "Seeded admin must have ADMIN role");

        List<User> users = userDAO.findAll();
        assertFalse(users.isEmpty(), "Users table should have seeded records");
    }

    @Test
    @DisplayName("Verify LocationDAO queries against seeded campus locations")
    void testLocationDAOIntegration() {
        LocationDAO locationDAO = new LocationDAO();
        List<Location> locations = locationDAO.findAll();
        assertFalse(locations.isEmpty(), "Locations table should contain seeded locations");

        Optional<Location> loc1 = locationDAO.findById(1);
        assertTrue(loc1.isPresent(), "Location ID 1 should exist");
        assertNotNull(loc1.get().getLocationName());
    }

    @Test
    @DisplayName("Verify ItemDAO queries against seeded items")
    void testItemDAOIntegration() {
        ItemDAO itemDAO = new ItemDAO();
        List<Item> items = itemDAO.findAll();
        assertFalse(items.isEmpty(), "Items table should contain seeded reports");

        Optional<Item> item1 = itemDAO.findById(1);
        assertTrue(item1.isPresent(), "Item ID 1 should exist");
    }

    @Test
    @DisplayName("Verify MatchDAO queries against seeded matches")
    void testMatchDAOIntegration() {
        MatchDAO matchDAO = new MatchDAO();
        List<Match> matches = matchDAO.findAll();
        assertFalse(matches.isEmpty(), "Matches table should contain seeded matches");
    }
}
