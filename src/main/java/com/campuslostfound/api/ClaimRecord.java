package com.campuslostfound.api;

import com.campuslostfound.model.Claim;
import java.time.LocalDateTime;

public record ClaimRecord(int claimId, int itemId, int userId, LocalDateTime claimDate, String status,
                          String verificationDetails, Integer reviewedBy, LocalDateTime reviewedAt) {
    public static ClaimRecord from(Claim c) {
        return new ClaimRecord(c.getClaimId(),c.getItemId(),c.getUserId(),c.getClaimDate(),c.getStatus().name(),
                c.getVerificationDetails(),c.getReviewedBy(),c.getReviewedAt());
    }
}
