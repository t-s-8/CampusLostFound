package com.campuslostfound.dao;

import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.ClaimStatus;

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

public class ClaimDAO {

    public Claim create(Claim claim) {
        String sql = "INSERT INTO claims (item_id, user_id, claim_date, status, verification_details) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, claim.getItemId());
            stmt.setInt(2, claim.getUserId());
            stmt.setTimestamp(3, Timestamp.valueOf(claim.getClaimDate() != null ? claim.getClaimDate() : LocalDateTime.now()));
            stmt.setString(4, claim.getStatus().name());
            stmt.setString(5, claim.getVerificationDetails());

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                throw new CampusLostFoundException("Submitting claim failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    claim.setClaimId(generatedKeys.getInt(1));
                }
            }
            return claim;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error submitting claim: " + e.getMessage(), e);
        }
    }

    public Optional<Claim> findById(int claimId) {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims WHERE claim_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, claimId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToClaim(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding claim by ID: " + e.getMessage(), e);
        }
    }

    public List<Claim> findByItemId(int itemId) {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims WHERE item_id = ? ORDER BY claim_date DESC";
        return queryClaims(sql, itemId);
    }

    public List<Claim> findByUserId(int userId) {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims WHERE user_id = ? ORDER BY claim_date DESC";
        return queryClaims(sql, userId);
    }

    public List<Claim> findPendingClaims() {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims WHERE status = 'PENDING' ORDER BY claim_date ASC";
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                claims.add(mapResultSetToClaim(rs));
            }
            return claims;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding pending claims: " + e.getMessage(), e);
        }
    }

    public List<Claim> findAll() {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims ORDER BY claim_date DESC";
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                claims.add(mapResultSetToClaim(rs));
            }
            return claims;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error finding all claims: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int claimId, ClaimStatus status, int reviewedBy, LocalDateTime reviewedAt) {
        String sql = "UPDATE claims SET status = ?, reviewed_by = ?, reviewed_at = ? WHERE claim_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status.name());
            if (reviewedBy > 0) {
                stmt.setInt(2, reviewedBy);
            } else {
                stmt.setNull(2, java.sql.Types.INTEGER);
            }
            stmt.setTimestamp(3, reviewedAt != null ? Timestamp.valueOf(reviewedAt) : Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, claimId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error updating claim status: " + e.getMessage(), e);
        }
    }

    public boolean approveClaimTransaction(int claimId, int adminUserId) {
        return approveClaimTransaction(claimId, adminUserId, false);
    }

    public boolean approveClaimTransaction(int claimId, int adminUserId, boolean simulateFailureForTesting) {
        String claimLockSql = "SELECT claim_id, item_id, user_id, status FROM claims WHERE claim_id = ? FOR UPDATE";
        String itemLockSql = "SELECT item_id, user_id, item_type, status FROM items WHERE item_id = ? FOR UPDATE";
        String updateClaimSql = "UPDATE claims SET status = 'APPROVED', reviewed_by = ?, reviewed_at = ? " +
                "WHERE claim_id = ? AND status = 'PENDING'";
        String updateItemSql = "UPDATE items SET status = 'RECOVERED' WHERE item_id = ? AND status IN ('ACTIVE', 'MATCHED')";
        String updateMatchesSql = "UPDATE matches SET status = 'CONFIRMED' WHERE found_item_id = ? AND status IN ('PENDING', 'NOTIFIED')";
        String updateMatchedLostSql = "UPDATE items SET status = 'RECOVERED' WHERE item_id IN (" +
                "SELECT lost_item_id FROM matches WHERE found_item_id = ?) AND user_id = ? AND status IN ('ACTIVE', 'MATCHED')";
        String rejectOtherClaimsSql = "UPDATE claims SET status = 'REJECTED', reviewed_by = ?, reviewed_at = ? " +
                "WHERE item_id = ? AND claim_id <> ? AND status = 'PENDING'";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int itemId;
                int claimantUserId;

                try (PreparedStatement stmt = conn.prepareStatement(claimLockSql)) {
                    stmt.setInt(1, claimId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new InvalidClaimException("Claim with ID " + claimId + " not found.");
                        }
                        String claimStatus = rs.getString("status");
                        if (!"PENDING".equalsIgnoreCase(claimStatus)) {
                            throw new InvalidClaimException("Only PENDING claims can be approved. Current claim status: " + claimStatus);
                        }
                        itemId = rs.getInt("item_id");
                        claimantUserId = rs.getInt("user_id");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(itemLockSql)) {
                    stmt.setInt(1, itemId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new InvalidClaimException("Target item with ID " + itemId + " not found.");
                        }
                        String itemType = rs.getString("item_type");
                        String itemStatus = rs.getString("status");
                        int reporterId = rs.getInt("user_id");

                        if (!"FOUND".equalsIgnoreCase(itemType)) {
                            throw new InvalidClaimException("Cannot approve claim: Target item is not a FOUND item (Type: " + itemType + ").");
                        }
                        if ("RECOVERED".equalsIgnoreCase(itemStatus)) {
                            throw new InvalidClaimException("Item is already marked as RECOVERED.");
                        }
                        if (!"ACTIVE".equalsIgnoreCase(itemStatus) && !"MATCHED".equalsIgnoreCase(itemStatus)) {
                            throw new InvalidClaimException("Item is not in a claimable state. Current status: " + itemStatus);
                        }
                        if (reporterId == claimantUserId) {
                            throw new InvalidClaimException("Cannot approve claim: Claimant is the finder who reported the item.");
                        }
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(updateClaimSql)) {
                    stmt.setInt(1, adminUserId);
                    stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setInt(3, claimId);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new InvalidClaimException("Conflicting approval detected: Claim was already modified by another transaction.");
                    }
                }

                if (simulateFailureForTesting) {
                    throw new SQLException("Simulated transaction failure triggered for rollback verification.");
                }

                try (PreparedStatement stmt = conn.prepareStatement(updateItemSql)) {
                    stmt.setInt(1, itemId);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new InvalidClaimException("Conflicting approval detected: Item state changed concurrently.");
                    }
                }

                try (PreparedStatement stmt = conn.prepareStatement(updateMatchesSql)) {
                    stmt.setInt(1, itemId);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(updateMatchedLostSql)) {
                    stmt.setInt(1, itemId);
                    stmt.setInt(2, claimantUserId);
                    stmt.executeUpdate();
                }

                try (PreparedStatement stmt = conn.prepareStatement(rejectOtherClaimsSql)) {
                    stmt.setInt(1, adminUserId);
                    stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setInt(3, itemId);
                    stmt.setInt(4, claimId);
                    stmt.executeUpdate();
                }

                conn.commit();
                return true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof InvalidClaimException) {
                    throw (InvalidClaimException) e;
                }
                throw new CampusLostFoundException("Transaction failed and was rolled back: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error during transactional claim approval: " + e.getMessage(), e);
        }
    }

    private List<Claim> queryClaims(String sql, int id) {
        List<Claim> claims = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    claims.add(mapResultSetToClaim(rs));
                }
            }
            return claims;
        } catch (SQLException e) {
            throw new CampusLostFoundException("Database error querying claims: " + e.getMessage(), e);
        }
    }

    private Claim mapResultSetToClaim(ResultSet rs) throws SQLException {
        Timestamp claimTs = rs.getTimestamp("claim_date");
        Timestamp revTs = rs.getTimestamp("reviewed_at");
        int reviewer = rs.getInt("reviewed_by");
        Integer reviewedBy = rs.wasNull() ? null : reviewer;

        return new Claim(
                rs.getInt("claim_id"),
                rs.getInt("item_id"),
                rs.getInt("user_id"),
                claimTs != null ? claimTs.toLocalDateTime() : LocalDateTime.now(),
                ClaimStatus.valueOf(rs.getString("status")),
                rs.getString("verification_details"),
                reviewedBy,
                revTs != null ? revTs.toLocalDateTime() : null
        );
    }
}
