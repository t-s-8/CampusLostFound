package com.campuslostfound.dao;

import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) for Location persistence operations.
 */
public class LocationDAO {

    /**
     * Persists a new campus location.
     */
    public Location create(Location location) {
        String sql = "INSERT INTO locations (location_name, building) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, location.getLocationName());
            stmt.setString(2, location.getBuilding());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new CampusLostFoundException("Creating location failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    location.setLocationId(generatedKeys.getInt(1));
                }
            }
            return location;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error creating location: " + e.getMessage(), e);
        }
    }

    /**
     * Finds a location by its ID.
     */
    public Optional<Location> findById(int locationId) {
        String sql = "SELECT location_id, location_name, building FROM locations WHERE location_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, locationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToLocation(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding location by ID: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieves all campus locations ordered alphabetically.
     */
    public List<Location> findAll() {
        String sql = "SELECT location_id, location_name, building FROM locations ORDER BY location_name";
        List<Location> locations = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                locations.add(mapResultSetToLocation(rs));
            }
            return locations;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error retrieving all locations: " + e.getMessage(), e);
        }
    }

    private Location mapResultSetToLocation(ResultSet rs) throws SQLException {
        return new Location(
                rs.getInt("location_id"),
                rs.getString("location_name"),
                rs.getString("building")
        );
    }
}
