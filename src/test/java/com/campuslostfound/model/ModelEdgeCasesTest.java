package com.campuslostfound.model;

import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.exception.InvalidItemException;
import com.campuslostfound.exception.InvalidMatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Model Edge Cases & Custom Exception Tests")
class ModelEdgeCasesTest {

    @Test
    @DisplayName("User edge cases: null password hash, equals & hashCode")
    void testUserEdgeCases() {
        User u1 = new User(1, "Alice", "alice@test.edu", "hash1", UserRole.STUDENT, LocalDateTime.now());
        User u2 = new User(1, "Alice", "alice@test.edu", "hash2", UserRole.STUDENT, LocalDateTime.now());
        User u3 = new User(2, "Bob", "bob@test.edu", "hash1", UserRole.STUDENT, LocalDateTime.now());

        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
        assertNotEquals(u1, u3);
        assertNotEquals(u1, "NotAUser");
        assertNotEquals(u1, null);

        assertThrows(IllegalArgumentException.class, () -> u1.setPasswordHash(null));
        assertThrows(IllegalArgumentException.class, () -> u1.setPasswordHash("   "));
        assertThrows(IllegalArgumentException.class, () -> u1.setRole(null));
    }

    @Test
    @DisplayName("Admin edge cases: default department and toString exclusion")
    void testAdminEdgeCases() {
        Admin admin = new Admin("Admin", "admin@test.edu", "hash", null);
        assertEquals("Campus Security & Lost/Found", admin.getDepartment());

        admin.setDepartment(null);
        assertEquals("General Administration", admin.getDepartment());

        String str = admin.toString();
        assertFalse(str.contains("hash"), "Admin toString must not expose password hash");
    }

    @Test
    @DisplayName("Location edge cases: negative ID, blank name, equals & hashCode")
    void testLocationEdgeCases() {
        Location loc1 = new Location(1, "Library", "Floor 1");
        Location loc2 = new Location(1, "Library", "Floor 1");
        Location loc3 = new Location(2, "Library", "Floor 1");

        assertEquals(loc1, loc2);
        assertEquals(loc1.hashCode(), loc2.hashCode());
        assertNotEquals(loc1, loc3);
        assertNotEquals(loc1, null);

        assertThrows(IllegalArgumentException.class, () -> new Location(-1, "Library", "Bldg"));
        assertThrows(IllegalArgumentException.class, () -> loc1.setLocationName("  "));
        assertThrows(IllegalArgumentException.class, () -> loc1.setLocationId(-10));
    }

    @Test
    @DisplayName("Item edge cases: negative ID, null status, equals & hashCode")
    void testItemEdgeCases() {
        LocalDate date = LocalDate.now();
        LostItem l1 = new LostItem(1, 10, 2, "Keys", "Keys", "Dorm keys", date);
        LostItem l2 = new LostItem(1, 10, 2, "Keys", "Keys", "Dorm keys", date);
        FoundItem f1 = new FoundItem(2, 20, 2, "Keys", "Keys", "Dorm keys", date);

        assertEquals(l1, l2);
        assertEquals(l1.hashCode(), l2.hashCode());
        assertNotEquals(l1, f1);

        assertThrows(InvalidItemException.class, () -> l1.setItemId(-1));
        assertThrows(InvalidItemException.class, () -> l1.setStatus(null));
    }

    @Test
    @DisplayName("Match edge cases: score bounds (0.0 and 100.0 allowed), negative IDs, equals & hashCode")
    void testMatchEdgeCases() {
        Match m1 = new Match(1, 10, 20, 0.0, MatchStatus.PENDING, LocalDateTime.now());
        assertEquals(0.0, m1.getScore());

        Match m2 = new Match(2, 10, 20, 100.0, MatchStatus.PENDING, LocalDateTime.now());
        assertEquals(100.0, m2.getScore());

        Match m3 = new Match(1, 10, 20, 50.0, MatchStatus.PENDING, LocalDateTime.now());
        assertEquals(m1, m3, "Matches with same matchId, lostId, and foundId are equal");
        assertEquals(m1.hashCode(), m3.hashCode());
        assertNotEquals(m1, m2);

        assertThrows(InvalidMatchException.class, () -> m1.setMatchId(-1));
        assertThrows(InvalidMatchException.class, () -> m1.setLostItemId(0));
        assertThrows(InvalidMatchException.class, () -> m1.setFoundItemId(-5));
        assertThrows(InvalidMatchException.class, () -> m1.setStatus(null));

        m1.setScore(69.9);
        assertFalse(m1.isPossibleMatch());
        m1.setScore(70.0);
        assertTrue(m1.isPossibleMatch());
    }

    @Test
    @DisplayName("Claim edge cases: negative IDs, null status, null proof, equals & hashCode")
    void testClaimEdgeCases() {
        Claim c1 = new Claim(1, 10, 20, LocalDateTime.now(), ClaimStatus.PENDING, "Details", null, null);
        Claim c2 = new Claim(1, 10, 20, LocalDateTime.now(), ClaimStatus.PENDING, "Details", null, null);
        Claim c3 = new Claim(2, 10, 20, LocalDateTime.now(), ClaimStatus.PENDING, "Details", null, null);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        assertNotEquals(c1, c3);

        assertThrows(InvalidClaimException.class, () -> c1.setClaimId(-1));
        assertThrows(InvalidClaimException.class, () -> c1.setItemId(0));
        assertThrows(InvalidClaimException.class, () -> c1.setUserId(0));
        assertThrows(InvalidClaimException.class, () -> c1.setStatus(null));

        c1.setVerificationDetails(null);
        assertEquals("", c1.getVerificationDetails());
    }

    @Test
    @DisplayName("AdminAnalytics edge cases: empty list safety and recovery rate calculation")
    void testAdminAnalyticsEdgeCases() {
        AdminAnalytics emptyAnalytics = new AdminAnalytics(0, 0, 0, 0, 0, 0, 0, 0.0, null, null);
        assertEquals(0, emptyAnalytics.getTotalReports());
        assertEquals(0.0, emptyAnalytics.getRecoveryRatePercentage());
        assertTrue(emptyAnalytics.getTopLocations().isEmpty());
        assertTrue(emptyAnalytics.getTopCategories().isEmpty());
        assertNotNull(emptyAnalytics.toString());

        AdminAnalytics.LocationStat locStat = new AdminAnalytics.LocationStat("Library", "Main", 5, 3, 2);
        assertEquals("Library", locStat.locationName());
        assertEquals("Main", locStat.building());
        assertEquals(5, locStat.totalReports());
        assertEquals(3, locStat.lostCount());
        assertEquals(2, locStat.foundCount());

        AdminAnalytics.CategoryStat catStat = new AdminAnalytics.CategoryStat("Electronics", 10, 6, 4);
        assertEquals("Electronics", catStat.category());
        assertEquals(10, catStat.totalReports());
        assertEquals(6, catStat.lostCount());
        assertEquals(4, catStat.foundCount());
    }

    @Test
    @DisplayName("Custom exception hierarchy and chaining verification")
    void testCustomExceptionHierarchy() {
        Exception cause = new RuntimeException("Root database failure");
        CampusLostFoundException baseEx = new CampusLostFoundException("Base error", cause);
        assertEquals("Base error", baseEx.getMessage());
        assertEquals(cause, baseEx.getCause());

        InvalidItemException itemEx = new InvalidItemException("Item error", cause);
        assertTrue(itemEx instanceof CampusLostFoundException);
        assertEquals("Item error", itemEx.getMessage());
        assertEquals(cause, itemEx.getCause());

        InvalidClaimException claimEx = new InvalidClaimException("Claim error", cause);
        assertTrue(claimEx instanceof CampusLostFoundException);
        assertEquals("Claim error", claimEx.getMessage());

        InvalidMatchException matchEx = new InvalidMatchException("Match error", cause);
        assertTrue(matchEx instanceof CampusLostFoundException);
        assertEquals("Match error", matchEx.getMessage());
    }
}
