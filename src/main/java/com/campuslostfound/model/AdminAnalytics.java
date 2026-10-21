package com.campuslostfound.model;

import java.util.List;

/**
 * Data Transfer Object (DTO) encapsulating aggregated system analytics and KPI metrics
 * computed directly from the MySQL database.
 */
public class AdminAnalytics {

    private final long totalLostItems;
    private final long totalFoundItems;
    private final long activeReports;
    private final long recoveredItems;
    private final long pendingClaims;
    private final long approvedClaims;
    private final long rejectedClaims;
    private final double recoveryRatePercentage;
    private final List<LocationStat> topLocations;
    private final List<CategoryStat> topCategories;

    /**
     * Immutable projection of campus incident count by location.
     */
    public record LocationStat(String locationName, String building, int totalReports, int lostCount, int foundCount) {}

    /**
     * Immutable projection of campus incident count by item category.
     */
    public record CategoryStat(String category, int totalReports, int lostCount, int foundCount) {}

    public AdminAnalytics(long totalLostItems, long totalFoundItems, long activeReports,
                          long recoveredItems, long pendingClaims, long approvedClaims,
                          long rejectedClaims, double recoveryRatePercentage,
                          List<LocationStat> topLocations, List<CategoryStat> topCategories) {
        this.totalLostItems = totalLostItems;
        this.totalFoundItems = totalFoundItems;
        this.activeReports = activeReports;
        this.recoveredItems = recoveredItems;
        this.pendingClaims = pendingClaims;
        this.approvedClaims = approvedClaims;
        this.rejectedClaims = rejectedClaims;
        this.recoveryRatePercentage = recoveryRatePercentage;
        this.topLocations = topLocations != null ? topLocations : List.of();
        this.topCategories = topCategories != null ? topCategories : List.of();
    }

    public long getTotalLostItems() {
        return totalLostItems;
    }

    public long getTotalFoundItems() {
        return totalFoundItems;
    }

    public long getTotalReports() {
        return totalLostItems + totalFoundItems;
    }

    public long getActiveReports() {
        return activeReports;
    }

    public long getRecoveredItems() {
        return recoveredItems;
    }

    public long getPendingClaims() {
        return pendingClaims;
    }

    public long getApprovedClaims() {
        return approvedClaims;
    }

    public long getRejectedClaims() {
        return rejectedClaims;
    }

    public double getRecoveryRatePercentage() {
        return recoveryRatePercentage;
    }

    public List<LocationStat> getTopLocations() {
        return topLocations;
    }

    public List<CategoryStat> getTopCategories() {
        return topCategories;
    }

    @Override
    public String toString() {
        return "AdminAnalytics{" +
                "totalLost=" + totalLostItems +
                ", totalFound=" + totalFoundItems +
                ", activeReports=" + activeReports +
                ", recovered=" + recoveredItems +
                ", pendingClaims=" + pendingClaims +
                ", approvedClaims=" + approvedClaims +
                ", recoveryRate=" + String.format("%.2f%%", recoveryRatePercentage) +
                ", topLocations=" + topLocations.size() +
                ", topCategories=" + topCategories.size() +
                '}';
    }
}
