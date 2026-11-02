package com.campuslostfound.dao;

import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.LostItem;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemDAO {

    public Item create(Item item) {
        String sql = "INSERT INTO items (user_id, location_id, name, category, description, item_type, item_date, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, item.getUserId());
            stmt.setInt(2, item.getLocationId());
            stmt.setString(3, item.getName());
            stmt.setString(4, item.getCategory());
            stmt.setString(5, item.getDescription());
            stmt.setString(6, item.getItemType().name());
            stmt.setDate(7, Date.valueOf(item.getItemDate()));
            stmt.setString(8, item.getStatus().name());
            stmt.setTimestamp(9, Timestamp.valueOf(item.getCreatedAt() != null ? item.getCreatedAt() : LocalDateTime.now()));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new CampusLostFoundException("Creating item report failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    item.setItemId(generatedKeys.getInt(1));
                }
            }
            return item;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error creating item report: " + e.getMessage(), e);
        }
    }

    public Optional<Item> findById(int itemId) {
        String sql = "SELECT item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at " +
                "FROM items WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding item by ID: " + e.getMessage(), e);
        }
    }

    public List<Item> findAll() {
        String sql = "SELECT item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at " +
                "FROM items ORDER BY item_date DESC, item_id DESC";
        return executeQueryForList(sql);
    }

    public List<Item> findByTypeAndStatus(ItemType type, ItemStatus status) {
        String sql = "SELECT item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at " +
                "FROM items WHERE item_type = ? AND status = ? ORDER BY item_date DESC";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, type.name());
            stmt.setString(2, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding items by type and status: " + e.getMessage(), e);
        }
    }

    public List<Item> findByUserId(int userId) {
        String sql = "SELECT item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at " +
                "FROM items WHERE user_id = ? ORDER BY item_date DESC";
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding items by user ID: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int itemId, ItemStatus status) {
        String sql = "UPDATE items SET status = ? WHERE item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, itemId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error updating item status: " + e.getMessage(), e);
        }
    }

    public List<Item> search(String keyword, String category, Integer locationId, ItemType type, ItemStatus status) {
        StringBuilder sql = new StringBuilder("SELECT item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at FROM items WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(description) LIKE ?)");
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (category != null && !category.trim().isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category.trim());
        }
        if (locationId != null && locationId > 0) {
            sql.append(" AND location_id = ?");
            params.add(locationId);
        }
        if (type != null) {
            sql.append(" AND item_type = ?");
            params.add(type.name());
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }

        sql.append(" ORDER BY item_date DESC, item_id DESC");

        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
            return items;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error executing dynamic item search: " + e.getMessage(), e);
        }
    }

    private List<Item> executeQueryForList(String sql) {
        List<Item> items = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
            return items;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error querying items: " + e.getMessage(), e);
        }
    }

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("item_id");
        int userId = rs.getInt("user_id");
        int locId = rs.getInt("location_id");
        String name = rs.getString("name");
        String category = rs.getString("category");
        String desc = rs.getString("description");
        ItemType type = ItemType.valueOf(rs.getString("item_type"));
        LocalDate date = rs.getDate("item_date").toLocalDate();
        ItemStatus status = ItemStatus.valueOf(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        if (type == ItemType.LOST) {
            return new LostItem(id, userId, locId, name, category, desc, date, status, createdAt);
        } else {
            return new FoundItem(id, userId, locId, name, category, desc, date, status, createdAt);
        }
    }
}
