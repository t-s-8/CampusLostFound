package com.campuslostfound.service;

import com.campuslostfound.dao.AnalyticsDAO;
import com.campuslostfound.dao.ClaimDAO;
import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.AdminAnalytics;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.ClaimStatus;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 5: Claim Transaction & Admin Analytics Integration Tests")
class ClaimTransactionIntegrationTest {

    private ClaimService claimService;
    private AnalyticsService analyticsService;
    private ItemDAO itemDAO;
    private ClaimDAO claimDAO;
    private UserDAO userDAO;

    private User studentFinder;
    private User studentClaimant;
    private User adminUser;

    @BeforeAll
    static void checkDatabaseAvailability() {
        boolean available = DatabaseConnection.isDatabaseAvailable();
        Assumptions.assumeTrue(available,
                "Local MySQL database is not reachable. Skipping live transaction integration tests.");
    }

    @BeforeEach
    void setUp() {
        itemDAO = new ItemDAO();
        claimDAO = new ClaimDAO();
        userDAO = new UserDAO();
        claimService = new ClaimService(claimDAO, itemDAO, userDAO);
        analyticsService = new AnalyticsService(new AnalyticsDAO());

        String ts = String.valueOf(System.currentTimeMillis() % 100000);
        studentFinder = userDAO.create(new User(0, "Finder_" + ts, "finder_" + ts + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));
        studentClaimant = userDAO.create(new User(0, "Claimant_" + ts, "claimant_" + ts + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));
        adminUser = userDAO.create(new Admin(0, "Admin_" + ts, "admin_" + ts + "@test.edu",
                AuthService.hashPassword("Pass123"), "Campus Safety", LocalDateTime.now()));
    }

    @Test
    @DisplayName("Transactional Claim Approval: Commits APPROVED claim and RECOVERED item")
    void testSuccessfulClaimApprovalTransaction() {
        FoundItem foundItem = (FoundItem) itemDAO.create(new FoundItem(
                studentFinder.getUserId(), 1, "Scientific Calculator Casio",
                "Electronics", "Casio FX-991EX with name sticker", LocalDate.now()
        ));
        assertEquals(ItemStatus.ACTIVE, foundItem.getStatus());

        Claim claim = claimService.submitClaim(
                foundItem.getItemId(), studentClaimant.getUserId(), "Has my initials engraved inside battery hatch"
        );
        assertEquals(ClaimStatus.PENDING, claim.getStatus());

        boolean approved = claimService.approveClaim(claim.getClaimId(), adminUser.getUserId());
        assertTrue(approved, "Transactional approval must return true on commit");

        Claim reloadedClaim = claimDAO.findById(claim.getClaimId()).orElseThrow();
        assertEquals(ClaimStatus.APPROVED, reloadedClaim.getStatus(), "Claim status in DB must be APPROVED");
        assertEquals(adminUser.getUserId(), reloadedClaim.getReviewedBy(), "Reviewer ID must match admin");
        assertNotNull(reloadedClaim.getReviewedAt(), "Reviewed timestamp must be recorded");

        Item reloadedItem = itemDAO.findById(foundItem.getItemId()).orElseThrow();
        assertEquals(ItemStatus.RECOVERED, reloadedItem.getStatus(), "Target item in DB must transition to RECOVERED");
    }

    @Test
    @DisplayName("Transaction Rollback: Failure mid-transaction rolls back all partial changes")
    void testTransactionRollbackOnFailure() {
        FoundItem foundItem = (FoundItem) itemDAO.create(new FoundItem(
                studentFinder.getUserId(), 1, "Sony Noise-Canceling Headphones",
                "Electronics", "Black over-ear headphones in hard case", LocalDate.now()
        ));

        Claim claim = claimService.submitClaim(
                foundItem.getItemId(), studentClaimant.getUserId(), "Bluetooth address ends with 7A:B2"
        );
        assertEquals(ClaimStatus.PENDING, claim.getStatus());

        assertThrows(CampusLostFoundException.class, () ->
                claimService.approveClaimWithSimulatedFailure(claim.getClaimId(), adminUser.getUserId()),
                "Simulated transaction failure must cause an exception and roll back"
        );

        Claim reloadedClaim = claimDAO.findById(claim.getClaimId()).orElseThrow();
        assertEquals(ClaimStatus.PENDING, reloadedClaim.getStatus(),
                "Rollback must ensure claim remains PENDING after failure");
        assertNull(reloadedClaim.getReviewedBy(), "Reviewer ID must remain NULL after rollback");

        Item reloadedItem = itemDAO.findById(foundItem.getItemId()).orElseThrow();
        assertEquals(ItemStatus.ACTIVE, reloadedItem.getStatus(),
                "Rollback must ensure item remains ACTIVE after failure");
    }

    @Test
    @DisplayName("Claim Rejection: Marks claim REJECTED without modifying item status")
    void testClaimRejection() {
        FoundItem foundItem = (FoundItem) itemDAO.create(new FoundItem(
                studentFinder.getUserId(), 1, "Silver Keychain with 3 Keys",
                "Accessories", "Keyring with blue gym tag", LocalDate.now()
        ));

        Claim claim = claimService.submitClaim(
                foundItem.getItemId(), studentClaimant.getUserId(), "Keys for dorm room 101"
        );

        boolean rejected = claimService.rejectClaim(claim.getClaimId(), adminUser.getUserId());
        assertTrue(rejected);

        Claim reloadedClaim = claimDAO.findById(claim.getClaimId()).orElseThrow();
        assertEquals(ClaimStatus.REJECTED, reloadedClaim.getStatus());
        assertEquals(adminUser.getUserId(), reloadedClaim.getReviewedBy());

        Item reloadedItem = itemDAO.findById(foundItem.getItemId()).orElseThrow();
        assertEquals(ItemStatus.ACTIVE, reloadedItem.getStatus(), "Item must remain ACTIVE when claim is rejected");
    }

    @Test
    @DisplayName("Repeated Approval: Cannot re-approve an already APPROVED claim")
    void testRepeatedApprovalThrowsException() {
        FoundItem foundItem = (FoundItem) itemDAO.create(new FoundItem(
                studentFinder.getUserId(), 1, "Umbrella Black",
                "Accessories", "Windproof travel umbrella", LocalDate.now()
        ));

        Claim claim = claimService.submitClaim(
                foundItem.getItemId(), studentClaimant.getUserId(), "Has broken rib"
        );

        assertTrue(claimService.approveClaim(claim.getClaimId(), adminUser.getUserId()));

        assertThrows(InvalidClaimException.class, () ->
                claimService.approveClaim(claim.getClaimId(), adminUser.getUserId()),
                "Approving an already approved claim must throw InvalidClaimException"
        );
    }

    @Test
    @DisplayName("Invalid Item States: Cannot claim or approve already RECOVERED items")
    void testInvalidItemStateClaiming() {
        FoundItem recoveredItem = (FoundItem) itemDAO.create(new FoundItem(
                studentFinder.getUserId(), 1, "Archived Recovered Laptop",
                "Electronics", "Old report", LocalDate.now()
        ));
        itemDAO.updateStatus(recoveredItem.getItemId(), ItemStatus.RECOVERED);

        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(recoveredItem.getItemId(), studentClaimant.getUserId(), "I want to claim it"),
                "Cannot submit a claim on a RECOVERED item"
        );
    }

    @Test
    @DisplayName("Invalid Claims: Cannot claim a LOST report or file claim without proof")
    void testInvalidClaimSubmissions() {
        LostItem lostItem = (LostItem) itemDAO.create(new LostItem(
                studentFinder.getUserId(), 1, "Lost Gym Bag",
                "Personal", "Red duffel bag", LocalDate.now()
        ));

        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(lostItem.getItemId(), studentClaimant.getUserId(), "Proof text"),
                "Cannot submit a claim on a LOST report"
        );

        FoundItem foundItem = (FoundItem) itemDAO.create(new FoundItem(
                studentFinder.getUserId(), 1, "Found Thermos",
                "Accessories", "Steel thermos", LocalDate.now()
        ));
        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(foundItem.getItemId(), studentClaimant.getUserId(), "   "),
                "Cannot submit claim with blank verification details"
        );

        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(foundItem.getItemId(), studentFinder.getUserId(), "My own found item"),
                "Finder cannot claim the item they turned in"
        );
    }

    @Test
    @DisplayName("Admin Analytics: Computes non-null, coherent database aggregations")
    void testAnalyticsQueryResults() {
        AdminAnalytics stats = analyticsService.getSystemAnalytics();
        assertNotNull(stats, "Analytics DTO must not be null");

        assertTrue(stats.getTotalReports() >= 0, "Total reports count must be non-negative");
        assertTrue(stats.getTotalLostItems() >= 0, "Total lost items must be non-negative");
        assertTrue(stats.getTotalFoundItems() >= 0, "Total found items must be non-negative");
        assertTrue(stats.getActiveReports() >= 0, "Active reports must be non-negative");
        assertTrue(stats.getRecoveredItems() >= 0, "Recovered items count must be non-negative");
        assertTrue(stats.getPendingClaims() >= 0, "Pending claims count must be non-negative");
        assertTrue(stats.getApprovedClaims() >= 0, "Approved claims count must be non-negative");
        assertTrue(stats.getRecoveryRatePercentage() >= 0.0 && stats.getRecoveryRatePercentage() <= 100.0,
                "Recovery rate must be between 0.0% and 100.0%");

        assertNotNull(stats.getTopLocations(), "Top locations list must not be null");
        assertNotNull(stats.getTopCategories(), "Top categories list must not be null");

        for (AdminAnalytics.LocationStat locStat : stats.getTopLocations()) {
            assertNotNull(locStat.locationName(), "Location name must not be null");
            assertTrue(locStat.totalReports() >= 0, "Location total reports must be non-negative");
        }

        for (AdminAnalytics.CategoryStat catStat : stats.getTopCategories()) {
            assertNotNull(catStat.category(), "Category must not be null");
            assertTrue(catStat.totalReports() >= 0, "Category total reports must be non-negative");
        }
    }
}
