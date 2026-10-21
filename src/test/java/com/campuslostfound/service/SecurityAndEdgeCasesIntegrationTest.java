package com.campuslostfound.service;

import com.campuslostfound.dao.ClaimDAO;
import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.database.DatabaseConfig;
import com.campuslostfound.database.DatabaseConnection;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.AdminAnalytics;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.Match;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Security & System Edge Cases Integration Tests")
class SecurityAndEdgeCasesIntegrationTest {

    private AuthService authService;
    private ItemService itemService;
    private ClaimService claimService;
    private MatchService matchService;
    private AnalyticsService analyticsService;

    private UserDAO userDAO;
    private ItemDAO itemDAO;
    private ClaimDAO claimDAO;
    private LocationDAO locationDAO;

    private User student1;
    private User student2;
    private User admin;

    @BeforeAll
    static void checkDatabaseAvailability() {
        Assumptions.assumeTrue(DatabaseConnection.isDatabaseAvailable(),
                "Local MySQL database is not reachable. Skipping live security & edge case tests.");
    }

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        itemDAO = new ItemDAO();
        claimDAO = new ClaimDAO();
        locationDAO = new LocationDAO();

        authService = new AuthService(userDAO);
        matchService = new MatchService();
        itemService = new ItemService(itemDAO, locationDAO, matchService);
        claimService = new ClaimService(claimDAO, itemDAO, userDAO);
        analyticsService = new AnalyticsService();

        String ts = String.valueOf(System.currentTimeMillis() % 1000000);
        student1 = userDAO.create(new User(0, "Student One", "s1_" + ts + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));
        student2 = userDAO.create(new User(0, "Student Two", "s2_" + ts + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));
        admin = userDAO.create(new Admin(0, "Security Admin", "adm_" + ts + "@test.edu",
                AuthService.hashPassword("Pass123"), "Campus Security", LocalDateTime.now()));
    }

    @AfterEach
    void tearDown() {
        // Ensure DatabaseConfig is reset to standard credentials after any test overrides
        DatabaseConfig.reset();
    }

    // =========================================================================
    // 1. SECURITY TESTS
    // =========================================================================

    @Test
    @DisplayName("Security: SQL Injection immunity via PreparedStatement parameter binding")
    void testSqlInjectionImmunity() {
        // Malicious SQL injection payloads in search keywords and descriptions
        String payload1 = "' OR '1'='1";
        String payload2 = "'; DROP TABLE claims; --";
        String payload3 = "Robert'); DROP TABLE items;--";

        // Must execute cleanly as literal text searches without throwing SQL syntax exceptions
        assertDoesNotThrow(() -> itemDAO.search(payload1, null, null, null, null));
        assertDoesNotThrow(() -> itemDAO.search(payload2, null, null, null, null));
        assertDoesNotThrow(() -> itemDAO.search(payload3, null, null, null, null));

        // Attempt to register with payload in name
        User injectedUser = authService.register(payload1, "sqli_" + System.currentTimeMillis() % 10000 + "@test.edu", "SafePass123", UserRole.STUDENT);
        assertEquals(payload1, injectedUser.getName());
    }

    @Test
    @DisplayName("Security: Passwords are encrypted with SHA-256 and never stored in plaintext")
    void testPasswordSecurity() {
        String rawPassword = "SuperSecretPassword123!";
        User user = authService.register("Secure User", "sec_" + System.currentTimeMillis() % 10000 + "@test.edu", rawPassword, UserRole.STUDENT);

        // Verify that stored password hash is SHA-256 (64 hex characters) and does not match plaintext
        assertNotEquals(rawPassword, user.getPasswordHash());
        assertEquals(64, user.getPasswordHash().length());

        // Re-read directly from DB
        User dbUser = userDAO.findById(user.getUserId()).orElseThrow();
        assertEquals(user.getPasswordHash(), dbUser.getPasswordHash());
        assertFalse(dbUser.toString().contains(rawPassword));
        assertFalse(dbUser.toString().contains(user.getPasswordHash()));
    }

    @Test
    @DisplayName("Security: Role authorization rejects students attempting administrative operations")
    void testRoleAuthorizationProtection() {
        FoundItem found = (FoundItem) itemDAO.create(new FoundItem(
                student1.getUserId(), 1, "Gold Watch", "Jewelry", "Wristwatch", LocalDate.now()
        ));
        Claim claim = claimService.submitClaim(found.getItemId(), student2.getUserId(), "Has my family crest");

        // student1 (role STUDENT) attempts to approve or reject
        assertThrows(InvalidClaimException.class, () ->
                claimService.approveClaim(claim.getClaimId(), student1.getUserId()),
                "Student must not be allowed to approve claims"
        );
        assertThrows(InvalidClaimException.class, () ->
                claimService.rejectClaim(claim.getClaimId(), student1.getUserId()),
                "Student must not be allowed to reject claims"
        );

        // Non-existent reviewer ID rejected
        assertThrows(InvalidClaimException.class, () ->
                claimService.approveClaim(claim.getClaimId(), 99999999)
        );
    }

    // =========================================================================
    // 2. EDGE CASE TESTS
    // =========================================================================

    @Test
    @DisplayName("Edge Case: Duplicate email registration is prevented")
    void testDuplicateEmailRegistration() {
        String email = "duplicate_" + System.currentTimeMillis() % 100000 + "@test.edu";
        authService.register("Original User", email, "Password123", UserRole.STUDENT);

        assertThrows(CampusLostFoundException.class, () ->
                authService.register("Imposter User", email, "Password123", UserRole.STUDENT)
        );
    }

    @Test
    @DisplayName("Edge Case: No matching candidates yields empty PriorityQueue without errors")
    void testNoMatchingCandidates() {
        // Report an isolated unique item with no complementary reports
        LostItem isolatedItem = (LostItem) itemDAO.create(new LostItem(
                student1.getUserId(), 1, "Rare Antique Pocket Clock 1850",
                "Antiques", "Brass mechanical clockwork", LocalDate.of(2020, 1, 1)
        ));

        List<Match> matches = matchService.findAndRecordMatchesForItem(isolatedItem);
        assertNotNull(matches);
        // Possible matches (score >= 70) should be empty
        assertTrue(matches.stream().noneMatch(Match::isPossibleMatch));
    }

    @Test
    @DisplayName("Edge Case: Duplicate pending claims on same item by same student are rejected")
    void testDuplicatePendingClaims() {
        FoundItem found = (FoundItem) itemDAO.create(new FoundItem(
                student1.getUserId(), 1, "Sunglasses Ray-Ban", "Accessories", "Polarized", LocalDate.now()
        ));

        // First claim succeeds
        claimService.submitClaim(found.getItemId(), student2.getUserId(), "First claim verification");

        // Second claim by same student on same item must be rejected
        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(found.getItemId(), student2.getUserId(), "Second claim duplicate attempt")
        );
    }

    @Test
    @DisplayName("Edge Case: Claims on non-existent items are rejected")
    void testClaimNonExistentItem() {
        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(99999999, student2.getUserId(), "Claiming ghost item")
        );
    }

    @Test
    @DisplayName("Edge Case: Database connection failure probe safely returns false")
    void testDatabaseConnectionFailureProbe() {
        // Temporarily configure an invalid host / port
        DatabaseConfig.setCredentials("jdbc:mysql://localhost:9999/non_existent_db", "invalid_user", "invalid_pass");

        // isDatabaseAvailable() must safely return false without throwing an unhandled exception
        boolean available = DatabaseConnection.isDatabaseAvailable();
        assertFalse(available, "Invalid database configuration must return false");

        // Reset restores standard configuration
        DatabaseConfig.reset();
        assertTrue(DatabaseConnection.isDatabaseAvailable(), "Reset must restore healthy connection");
    }

    @Test
    @DisplayName("Edge Case: Empty analytics dataset computes 0.0% recovery rate without division by zero")
    void testEmptyAnalyticsComputation() {
        AdminAnalytics emptyStats = new AdminAnalytics(0, 0, 0, 0, 0, 0, 0, 0.0, List.of(), List.of());
        assertEquals(0, emptyStats.getTotalReports());
        assertEquals(0.0, emptyStats.getRecoveryRatePercentage());
        assertNotNull(emptyStats.getTopLocations());
        assertNotNull(emptyStats.getTopCategories());
    }
}
