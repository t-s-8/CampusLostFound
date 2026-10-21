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

/**
 * Data Access Object (DAO) for Claim persistence operations.
 */
public class ClaimDAO {

    /**
     * Submits a new ownership claim.
     */
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

    /**
     * Finds a claim by its primary key claim_id.
     */
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

    /**
     * Retrieves all claims submitted for a particular item.
     */
    public List<Claim> findByItemId(int itemId) {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims WHERE item_id = ? ORDER BY claim_date DESC";
        return queryClaims(sql, itemId);
    }

    /**
     * Retrieves all claims filed by a specific student user.
     */
    public List<Claim> findByUserId(int userId) {
        String sql = "SELECT claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at " +
                "FROM claims WHERE user_id = ? ORDER BY claim_date DESC";
        return queryClaims(sql, userId);
    }

    /**
     * Retrieves all claims awaiting administrative review (status = 'PENDING').
     */
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

    /**
     * Retrieves all claims in the database.
     */
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

    /**
     * Updates claim review status along with reviewer ID and timestamp.
     */
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

    /**
     * Overload for standard transactional claim approval.
     */
    public boolean approveClaimTransaction(int claimId, int adminUserId) {
        return approveClaimTransaction(claimId, adminUserId, false);
    }

    /**
     * Executes atomic transactional claim approval within a single database transaction.
     *
     * Transaction Workflow:
     * 1. Acquires row-level write locks (FOR UPDATE) on both the claim and the target item,
     *    preventing race conditions, duplicate approvals, or conflicting modifications.
     * 2. Validates that the claim is in PENDING status.
     * 3. Validates that the target item is a FOUND item, is currently claimable (ACTIVE or MATCHED),
     *    and has not already been RECOVERED.
     * 4. Updates the claim to APPROVED with the reviewer admin ID and timestamp.
     * 5. Updates the target item status to RECOVERED.
     * 6. Updates any related candidate matches for this found item to CONFIRMED.
     * 7. If the claimant previously reported a corresponding lost item, updates that lost item to RECOVERED.
     * 8. Rejects all other competing PENDING claims for the same item.
     * 9. Commits the transaction if all operations succeed; rolls back completely on any failure.
     *
     * @param claimId ID of the claim to approve.
     * @param adminUserId ID of the reviewing administrator.
     * @param simulateFailureForTesting If true, throws an exception after updating the claim to verify rollback.
     * @return true if the approval transaction committed successfully.
     */
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

                // 1. Lock and validate claim state
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

                // 2. Lock and validate item state
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

                // 3. Update claim to APPROVED
                try (PreparedStatement stmt = conn.prepareStatement(updateClaimSql)) {
                    stmt.setInt(1, adminUserId);
                    stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setInt(3, claimId);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new InvalidClaimException("Conflicting approval detected: Claim was already modified by another transaction.");
                    }
                }

                // Simulated failure hook for testing transaction rollback behavior
                if (simulateFailureForTesting) {
                    throw new SQLException("Simulated transaction failure triggered for rollback verification.");
                }

                // 4. Update item status to RECOVERED
                try (PreparedStatement stmt = conn.prepareStatement(updateItemSql)) {
                    stmt.setInt(1, itemId);
                    int affected = stmt.executeUpdate();
                    if (affected == 0) {
                        throw new InvalidClaimException("Conflicting approval detected: Item state changed concurrently.");
                    }
                }

                // 5. Update related matches to CONFIRMED
                try (PreparedStatement stmt = conn.prepareStatement(updateMatchesSql)) {
                    stmt.setInt(1, itemId);
                    stmt.executeUpdate();
                }

                // 6. If claimant filed a matched lost report, update that lost item to RECOVERED as well
                try (PreparedStatement stmt = conn.prepareStatement(updateMatchedLostSql)) {
                    stmt.setInt(1, itemId);
                    stmt.setInt(2, claimantUserId);
                    stmt.executeUpdate();
                }

                // 7. Reject any other competing pending claims for this item
                try (PreparedStatement stmt = conn.prepareStatement(rejectOtherClaimsSql)) {
                    stmt.setInt(1, adminUserId);
                    stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setInt(3, itemId);
                    stmt.setInt(4, claimId);
                    stmt.executeUpdate();
                }

                // 8. Commit the entire transaction
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
