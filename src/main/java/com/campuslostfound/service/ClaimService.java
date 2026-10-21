package com.campuslostfound.service;

import com.campuslostfound.dao.ClaimDAO;
import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.ClaimStatus;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing ownership claims on found items and administrative claim verification.
 */
public class ClaimService {

    private final ClaimDAO claimDAO;
    private final ItemDAO itemDAO;
    private final UserDAO userDAO;

    public ClaimService() {
        this(new ClaimDAO(), new ItemDAO(), new UserDAO());
    }

    public ClaimService(ClaimDAO claimDAO, ItemDAO itemDAO, UserDAO userDAO) {
        this.claimDAO = claimDAO;
        this.itemDAO = itemDAO;
        this.userDAO = userDAO;
    }

    /**
     * Submits a new claim on a found item.
     * Enforces business rules:
     * - Target item must exist.
     * - Target item must be a FOUND item (students cannot claim a lost report).
     * - Target item must be claimable (status ACTIVE or MATCHED).
     * - Claimant cannot claim an item they turned in themselves.
     * - Prevents duplicate pending claims by the same claimant for the same item.
     */
    public Claim submitClaim(int itemId, int claimantUserId, String verificationDetails) {
        if (verificationDetails == null || verificationDetails.trim().isEmpty()) {
            throw new InvalidClaimException("Verification details/proof must be provided with the claim.");
        }

        Item item = itemDAO.findById(itemId)
                .orElseThrow(() -> new InvalidClaimException("Item with ID " + itemId + " not found."));

        if (item.getItemType() != ItemType.FOUND) {
            throw new InvalidClaimException("Cannot file a claim on a LOST report. Claims can only be made on FOUND items.");
        }

        if (!item.isClaimable()) {
            throw new InvalidClaimException("Item is not claimable. Current status: " + item.getStatus());
        }

        User claimant = userDAO.findById(claimantUserId)
                .orElseThrow(() -> new InvalidClaimException("Claimant user with ID " + claimantUserId + " not found."));

        if (item.getUserId() == claimantUserId) {
            throw new InvalidClaimException("You cannot submit a claim on an item you reported yourself.");
        }

        // Check for existing pending claims by this user on this item
        List<Claim> existingClaims = claimDAO.findByItemId(itemId);
        boolean alreadyPending = existingClaims.stream()
                .anyMatch(c -> c.getUserId() == claimantUserId && c.getStatus() == ClaimStatus.PENDING);

        if (alreadyPending) {
            throw new InvalidClaimException("You already have an active pending claim for this item.");
        }

        Claim newClaim = new Claim(itemId, claimantUserId, verificationDetails.trim());
        return claimDAO.create(newClaim);
    }

    /**
     * Approves a claim after admin verification using an atomic database transaction.
     * Transitions claim status to APPROVED, item status to RECOVERED, and related matches to CONFIRMED.
     */
    public boolean approveClaim(int claimId, int adminUserId) {
        verifyAdminReviewer(adminUserId);
        return claimDAO.approveClaimTransaction(claimId, adminUserId, false);
    }

    /**
     * Approves a claim with simulated mid-transaction failure for rollback testing.
     */
    public boolean approveClaimWithSimulatedFailure(int claimId, int adminUserId) {
        verifyAdminReviewer(adminUserId);
        return claimDAO.approveClaimTransaction(claimId, adminUserId, true);
    }

    /**
     * Rejects a claim with reviewer record.
     */
    public boolean rejectClaim(int claimId, int adminUserId) {
        verifyAdminReviewer(adminUserId);

        Claim claim = claimDAO.findById(claimId)
                .orElseThrow(() -> new InvalidClaimException("Claim with ID " + claimId + " not found."));

        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new InvalidClaimException("Only PENDING claims can be rejected.");
        }

        return claimDAO.updateStatus(claimId, ClaimStatus.REJECTED, adminUserId, LocalDateTime.now());
    }

    /**
     * Retrieves all pending claims for admin review queue.
     */
    public List<Claim> getPendingClaims() {
        return claimDAO.findPendingClaims();
    }

    /**
     * Retrieves all claims submitted by a specific user.
     */
    public List<Claim> getClaimsForUser(int userId) {
        return claimDAO.findByUserId(userId);
    }

    /**
     * Retrieves all claims across the campus.
     */
    public List<Claim> getAllClaims() {
        return claimDAO.findAll();
    }

    private void verifyAdminReviewer(int adminUserId) {
        User reviewer = userDAO.findById(adminUserId)
                .orElseThrow(() -> new InvalidClaimException("Reviewer user ID " + adminUserId + " not found."));

        if (!reviewer.isAdmin()) {
            throw new InvalidClaimException("User ID " + adminUserId + " is not authorized as an administrator to review claims.");
        }
    }
}
