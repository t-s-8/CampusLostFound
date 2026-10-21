package com.campuslostfound.model;

import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.exception.InvalidItemException;
import com.campuslostfound.exception.InvalidMatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Model & OOP Behavior Tests")
class ModelTest {

    @Test
    @DisplayName("User encapsulation and validation")
    void testUserEncapsulationAndValidation() {
        User user = new User("Alice Johnson", "alice@campus.edu", "hashed_pwd", UserRole.STUDENT);
        assertEquals("Alice Johnson", user.getName());
        assertEquals("alice@campus.edu", user.getEmail());
        assertEquals(UserRole.STUDENT, user.getRole());
        assertFalse(user.isAdmin());

        // Name validation
        assertThrows(IllegalArgumentException.class, () -> user.setName("   "));
        assertThrows(IllegalArgumentException.class, () -> user.setName(null));

        // Email validation
        assertThrows(IllegalArgumentException.class, () -> user.setEmail("invalid-email"));
        assertThrows(IllegalArgumentException.class, () -> user.setEmail(null));

        // Negative ID validation
        assertThrows(IllegalArgumentException.class, () -> user.setUserId(-5));
    }

    @Test
    @DisplayName("Admin inheritance and polymorphic behavior")
    void testAdminInheritanceAndPolymorphism() {
        User adminUser = new Admin("Campus Admin", "admin@campus.edu", "secret_hash", "Security Dept");

        // Polymorphic check: adminUser is an instance of User and Admin
        assertTrue(adminUser instanceof User);
        assertTrue(adminUser instanceof Admin);
        assertEquals(UserRole.ADMIN, adminUser.getRole());
        assertTrue(adminUser.isAdmin());

        Admin specificAdmin = (Admin) adminUser;
        assertEquals("Security Dept", specificAdmin.getDepartment());
        assertTrue(specificAdmin.canReviewClaims());
    }

    @Test
    @DisplayName("Location creation and formatting")
    void testLocationFormatting() {
        Location locWithBuilding = new Location(1, "Lab 201", "Science Wing");
        assertEquals("Lab 201 (Science Wing)", locWithBuilding.getFullLocation());

        Location locWithoutBuilding = new Location(2, "Main Lawn", null);
        assertEquals("Main Lawn", locWithoutBuilding.getFullLocation());

        assertThrows(IllegalArgumentException.class, () -> new Location(""));
    }

    @Test
    @DisplayName("Item polymorphism: LostItem vs FoundItem behavior")
    void testItemPolymorphism() {
        LocalDate today = LocalDate.now();
        Item lost = new LostItem(1, 10, 101, "Blue Backpack", "Bags", "Nike bag", today, ItemStatus.ACTIVE, LocalDateTime.now());
        Item found = new FoundItem(2, 20, 101, "Blue Backpack", "Bags", "Nike bag", today, ItemStatus.ACTIVE, LocalDateTime.now());

        // Polymorphic itemType
        assertEquals(ItemType.LOST, lost.getItemType());
        assertEquals(ItemType.FOUND, found.getItemType());

        // Polymorphic isClaimable()
        assertFalse(lost.isClaimable(), "Lost items are missing item searches and cannot be claimed");
        assertTrue(found.isClaimable(), "Active found items can be claimed");

        // When found item becomes RECOVERED, it should no longer be claimable
        found.setStatus(ItemStatus.RECOVERED);
        assertFalse(found.isClaimable());

        // Polymorphic getDisplayDetails()
        assertTrue(lost.getDisplayDetails().startsWith("[LOST ITEM]"));
        assertTrue(found.getDisplayDetails().startsWith("[FOUND ITEM]"));
    }

    @Test
    @DisplayName("Item attribute validation")
    void testItemValidation() {
        LocalDate date = LocalDate.of(2026, 10, 10);
        assertThrows(InvalidItemException.class, () ->
                new LostItem(0, 1, "Keys", "Accessories", "Car keys", date)
        ); // user ID 0 is invalid

        assertThrows(InvalidItemException.class, () ->
                new FoundItem(1, 0, "Keys", "Accessories", "Car keys", date)
        ); // location ID 0 is invalid

        assertThrows(InvalidItemException.class, () ->
                new LostItem(1, 1, "", "Accessories", "Car keys", date)
        ); // empty name

        assertThrows(InvalidItemException.class, () ->
                new LostItem(1, 1, "Keys", "", "Car keys", date)
        ); // empty category

        assertThrows(InvalidItemException.class, () ->
                new LostItem(1, 1, "Keys", "Accessories", "Car keys", null)
        ); // null date
    }

    @Test
    @DisplayName("Match score validation and possible match threshold")
    void testMatchModel() {
        Match matchHigh = new Match(1, 10, 20, 85.0, MatchStatus.PENDING, LocalDateTime.now());
        assertTrue(matchHigh.isPossibleMatch(), "Score 85 >= 70 should be considered possible match");

        Match matchLow = new Match(2, 10, 30, 65.0, MatchStatus.PENDING, LocalDateTime.now());
        assertFalse(matchLow.isPossibleMatch(), "Score 65 < 70 should not be a possible match");

        // Score boundaries
        assertThrows(InvalidMatchException.class, () -> matchHigh.setScore(-1.0));
        assertThrows(InvalidMatchException.class, () -> matchHigh.setScore(100.5));
    }

    @Test
    @DisplayName("Claim lifecycle: submission, approval, and rejection")
    void testClaimLifecycle() {
        Claim claim = new Claim(100, 5, "I have the receipt and matching serial number");
        assertEquals(ClaimStatus.PENDING, claim.getStatus());
        assertTrue(claim.isPending());
        assertNull(claim.getReviewedBy());

        // Approval
        claim.approve(1);
        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
        assertEquals(1, claim.getReviewedBy());
        assertNotNull(claim.getReviewedAt());

        // Rejection validation
        Claim claim2 = new Claim(101, 6, "It belongs to my friend");
        assertThrows(InvalidClaimException.class, () -> claim2.reject(0)); // admin ID 0 is invalid
        claim2.reject(1);
        assertEquals(ClaimStatus.REJECTED, claim2.getStatus());
    }
}
