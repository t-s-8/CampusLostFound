package com.campuslostfound.dao;

import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.model.Match;
import com.campuslostfound.model.MatchStatus;

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

public class MatchDAO {

    public Match create(Match match) {
        String sql = "INSERT INTO matches (lost_item_id, found_item_id, score, status, created_at) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE score = VALUES(score), status = VALUES(status)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, match.getLostItemId());
            stmt.setInt(2, match.getFoundItemId());
            stmt.setDouble(3, match.getScore());
            stmt.setString(4, match.getStatus().name());
            stmt.setTimestamp(5, Timestamp.valueOf(match.getCreatedAt() != null ? match.getCreatedAt() : LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    match.setMatchId(generatedKeys.getInt(1));
                }
            }
            return match;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error saving match: " + e.getMessage(), e);
        }
    }

    public Optional<Match> findByLostAndFound(int lostItemId, int foundItemId) {
        String sql = "SELECT match_id, lost_item_id, found_item_id, score, status, created_at " +
                "FROM matches WHERE lost_item_id = ? AND found_item_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, lostItemId);
            stmt.setInt(2, foundItemId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToMatch(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding match by pair: " + e.getMessage(), e);
        }
    }

    public List<Match> findByLostItemId(int lostItemId) {
        String sql = "SELECT match_id, lost_item_id, found_item_id, score, status, created_at " +
                "FROM matches WHERE lost_item_id = ? ORDER BY score DESC";
        return queryMatches(sql, lostItemId);
    }

    public List<Match> findByFoundItemId(int foundItemId) {
        String sql = "SELECT match_id, lost_item_id, found_item_id, score, status, created_at " +
                "FROM matches WHERE found_item_id = ? ORDER BY score DESC";
        return queryMatches(sql, foundItemId);
    }

    public List<Match> findAll() {
        String sql = "SELECT match_id, lost_item_id, found_item_id, score, status, created_at " +
                "FROM matches ORDER BY score DESC";
        List<Match> matches = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                matches.add(mapResultSetToMatch(rs));
            }
            return matches;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error retrieving matches: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int matchId, MatchStatus status) {
        String sql = "UPDATE matches SET status = ? WHERE match_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            stmt.setInt(2, matchId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error updating match status: " + e.getMessage(), e);
        }
    }

    private List<Match> queryMatches(String sql, int itemId) {
        List<Match> matches = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, itemId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    matches.add(mapResultSetToMatch(rs));
                }
            }
            return matches;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error querying matches for item: " + e.getMessage(), e);
        }
    }

    private Match mapResultSetToMatch(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        return new Match(
                rs.getInt("match_id"),
                rs.getInt("lost_item_id"),
                rs.getInt("found_item_id"),
                rs.getDouble("score"),
                MatchStatus.valueOf(rs.getString("status")),
                ts != null ? ts.toLocalDateTime() : LocalDateTime.now()
        );
    }
}
