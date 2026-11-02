package com.campuslostfound.model;

import com.campuslostfound.exception.InvalidClaimException;

import java.time.LocalDateTime;
import java.util.Objects;

public class Claim {

    private int claimId;
    private int itemId;
    private int userId;
    private LocalDateTime claimDate;
    private ClaimStatus status;
    private String verificationDetails;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;

    public Claim(int itemId, int userId, String verificationDetails) {
        this(0, itemId, userId, LocalDateTime.now(), ClaimStatus.PENDING, verificationDetails, null, null);
    }

    public Claim(int claimId, int itemId, int userId, LocalDateTime claimDate,
                 ClaimStatus status, String verificationDetails, Integer reviewedBy, LocalDateTime reviewedAt) {
        setClaimId(claimId);
        setItemId(itemId);
        setUserId(userId);
        this.claimDate = claimDate != null ? claimDate : LocalDateTime.now();
        setStatus(status != null ? status : ClaimStatus.PENDING);
        setVerificationDetails(verificationDetails);
        this.reviewedBy = reviewedBy;
        this.reviewedAt = reviewedAt;
    }

    public int getClaimId() {
        return claimId;
    }

    public void setClaimId(int claimId) {
        if (claimId < 0) {
            throw new InvalidClaimException("Claim ID cannot be negative.");
        }
        this.claimId = claimId;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        if (itemId <= 0) {
            throw new InvalidClaimException("A valid item ID is required for a claim.");
        }
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        if (userId <= 0) {
            throw new InvalidClaimException("A valid user ID is required for a claim.");
        }
        this.userId = userId;
    }

    public LocalDateTime getClaimDate() {
        return claimDate;
    }

    public ClaimStatus getStatus() {
        return status;
    }

    public void setStatus(ClaimStatus status) {
        if (status == null) {
            throw new InvalidClaimException("Claim status must be specified.");
        }
        this.status = status;
    }

    public String getVerificationDetails() {
        return verificationDetails;
    }

    public void setVerificationDetails(String verificationDetails) {
        this.verificationDetails = verificationDetails != null ? verificationDetails.trim() : "";
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public boolean isPending() {
        return this.status == ClaimStatus.PENDING;
    }

    public void approve(int adminUserId) {
        if (adminUserId <= 0) {
            throw new InvalidClaimException("A valid reviewer admin ID is required to approve a claim.");
        }
        this.status = ClaimStatus.APPROVED;
        this.reviewedBy = adminUserId;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(int adminUserId) {
        if (adminUserId <= 0) {
            throw new InvalidClaimException("A valid reviewer admin ID is required to reject a claim.");
        }
        this.status = ClaimStatus.REJECTED;
        this.reviewedBy = adminUserId;
        this.reviewedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim claim)) return false;
        return claimId == claim.claimId &&
                itemId == claim.itemId &&
                userId == claim.userId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(claimId, itemId, userId);
    }

    @Override
    public String toString() {
        return "Claim{" +
                "claimId=" + claimId +
                ", itemId=" + itemId +
                ", userId=" + userId +
                ", status=" + status +
                ", claimDate=" + claimDate +
                '}';
    }
}
