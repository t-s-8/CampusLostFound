package com.campuslostfound.dao;

import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.AdminAnalytics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsDAO {

    public AdminAnalytics fetchSystemAnalytics() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String itemsSql = "SELECT " +
                    "COUNT(*) AS total_items, " +
                    "SUM(CASE WHEN item_type = 'LOST' THEN 1 ELSE 0 END) AS total_lost, " +
                    "SUM(CASE WHEN item_type = 'FOUND' THEN 1 ELSE 0 END) AS total_found, " +
                    "SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) AS total_active, " +
                    "SUM(CASE WHEN status = 'RECOVERED' THEN 1 ELSE 0 END) AS total_recovered " +
                    "FROM items";

            long totalLost = 0;
            long totalFound = 0;
            long activeReports = 0;
            long recoveredItems = 0;
            long totalItems = 0;

            try (PreparedStatement stmt = conn.prepareStatement(itemsSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    totalItems = rs.getLong("total_items");
                    totalLost = rs.getLong("total_lost");
                    totalFound = rs.getLong("total_found");
                    activeReports = rs.getLong("total_active");
                    recoveredItems = rs.getLong("total_recovered");
                }
            }

            double recoveryRate = totalItems > 0 ? ((double) recoveredItems * 100.0) / totalItems : 0.0;

            String claimsSql = "SELECT " +
                    "SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_claims, " +
                    "SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS approved_claims, " +
                    "SUM(CASE WHEN status = 'REJECTED' THEN 1 ELSE 0 END) AS rejected_claims " +
                    "FROM claims";

            long pendingClaims = 0;
            long approvedClaims = 0;
            long rejectedClaims = 0;

            try (PreparedStatement stmt = conn.prepareStatement(claimsSql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    pendingClaims = rs.getLong("pending_claims");
                    approvedClaims = rs.getLong("approved_claims");
                    rejectedClaims = rs.getLong("rejected_claims");
                }
            }

            String locationsSql = "SELECT " +
                    "l.location_name, " +
                    "COALESCE(l.building, '') AS building, " +
                    "COUNT(i.item_id) AS total_reports, " +
                    "SUM(CASE WHEN i.item_type = 'LOST' THEN 1 ELSE 0 END) AS lost_count, " +
                    "SUM(CASE WHEN i.item_type = 'FOUND' THEN 1 ELSE 0 END) AS found_count " +
                    "FROM locations l " +
                    "JOIN items i ON l.location_id = i.location_id " +
                    "GROUP BY l.location_id, l.location_name, l.building " +
                    "ORDER BY total_reports DESC, l.location_name ASC " +
                    "LIMIT 5";

            List<AdminAnalytics.LocationStat> topLocations = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(locationsSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topLocations.add(new AdminAnalytics.LocationStat(
                            rs.getString("location_name"),
                            rs.getString("building"),
                            rs.getInt("total_reports"),
                            rs.getInt("lost_count"),
                            rs.getInt("found_count")
                    ));
                }
            }

            String categoriesSql = "SELECT " +
                    "category, " +
                    "COUNT(*) AS total_reports, " +
                    "SUM(CASE WHEN item_type = 'LOST' THEN 1 ELSE 0 END) AS lost_count, " +
                    "SUM(CASE WHEN item_type = 'FOUND' THEN 1 ELSE 0 END) AS found_count " +
                    "FROM items " +
                    "GROUP BY category " +
                    "ORDER BY total_reports DESC, category ASC " +
                    "LIMIT 5";

            List<AdminAnalytics.CategoryStat> topCategories = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(categoriesSql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    topCategories.add(new AdminAnalytics.CategoryStat(
                            rs.getString("category"),
                            rs.getInt("total_reports"),
                            rs.getInt("lost_count"),
                            rs.getInt("found_count")
                    ));
                }
            }

            return new AdminAnalytics(
                    totalLost,
                    totalFound,
                    activeReports,
                    recoveredItems,
                    pendingClaims,
                    approvedClaims,
                    rejectedClaims,
                    recoveryRate,
                    topLocations,
                    topCategories
            );

        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error computing system analytics: " + e.getMessage(), e);
        }
    }
}
