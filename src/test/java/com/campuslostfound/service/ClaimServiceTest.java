package com.campuslostfound.service;

import com.campuslostfound.dao.ClaimDAO;
import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.exception.CampusLostFoundException;
import com.campuslostfound.exception.InvalidClaimException;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.ClaimStatus;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ClaimService Unit Tests")
class ClaimServiceTest {

    private ClaimService claimService;
    private Item foundFlask;
    private Item lostWallet;
    private User studentFinder;
    private User studentClaimant;
    private User adminUser;

    // In-memory test doubles
    private static class InMemoryItemDAO extends ItemDAO {
        private final Map<Integer, Item> map = new HashMap<>();

        @Override
        public Optional<Item> findById(int itemId) {
            return Optional.ofNullable(map.get(itemId));
        }

        @Override
        public boolean updateStatus(int itemId, ItemStatus status) {
            Item item = map.get(itemId);
            if (item != null) {
                item.setStatus(status);
                return true;
            }
            return false;
        }

        void put(Item item) {
            map.put(item.getItemId(), item);
        }
    }

    private static class InMemoryUserDAO extends UserDAO {
        private final Map<Integer, User> map = new HashMap<>();

        @Override
        public Optional<User> findById(int userId) {
            return Optional.ofNullable(map.get(userId));
        }

        void put(User user) {
            map.put(user.getUserId(), user);
        }
    }

    private static class InMemoryClaimDAO extends ClaimDAO {
        private final Map<Integer, Claim> map = new HashMap<>();
        private int sequence = 1;
        private final ItemDAO itemDAO;

        InMemoryClaimDAO(ItemDAO itemDAO) {
            this.itemDAO = itemDAO;
        }

        @Override
        public Claim create(Claim claim) {
            claim.setClaimId(sequence++);
            map.put(claim.getClaimId(), claim);
            return claim;
        }

        @Override
        public Optional<Claim> findById(int claimId) {
            return Optional.ofNullable(map.get(claimId));
        }

        @Override
        public List<Claim> findByItemId(int itemId) {
            List<Claim> results = new ArrayList<>();
            for (Claim c : map.values()) {
                if (c.getItemId() == itemId) {
                    results.add(c);
                }
            }
            return results;
        }

        @Override
        public List<Claim> findPendingClaims() {
            List<Claim> results = new ArrayList<>();
            for (Claim c : map.values()) {
                if (c.getStatus() == ClaimStatus.PENDING) {
                    results.add(c);
                }
            }
            return results;
        }

        @Override
        public boolean updateStatus(int claimId, ClaimStatus status, int reviewedBy, LocalDateTime reviewedAt) {
            Claim claim = map.get(claimId);
            if (claim != null) {
                claim.setStatus(status);
                return true;
            }
            return false;
        }

        @Override
        public boolean approveClaimTransaction(int claimId, int adminUserId, boolean simulateFailureForTesting) {
            if (simulateFailureForTesting) {
                throw new CampusLostFoundException("Simulated transaction failure");
            }
            Claim claim = map.get(claimId);
            if (claim == null) {
                throw new InvalidClaimException("Claim with ID " + claimId + " not found.");
            }
            if (claim.getStatus() != ClaimStatus.PENDING) {
                throw new InvalidClaimException("Only PENDING claims can be approved. Current status: " + claim.getStatus());
            }
            claim.setStatus(ClaimStatus.APPROVED);
            if (itemDAO != null) {
                itemDAO.updateStatus(claim.getItemId(), ItemStatus.RECOVERED);
            }
            for (Claim other : map.values()) {
                if (other.getItemId() == claim.getItemId() && other.getClaimId() != claimId && other.getStatus() == ClaimStatus.PENDING) {
                    other.setStatus(ClaimStatus.REJECTED);
                }
            }
            return true;
        }
    }

    private InMemoryItemDAO itemDAO;
    private InMemoryUserDAO userDAO;
    private InMemoryClaimDAO claimDAO;

    @BeforeEach
    void setUp() {
        itemDAO = new InMemoryItemDAO();
        userDAO = new InMemoryUserDAO();
        claimDAO = new InMemoryClaimDAO(itemDAO);

        studentFinder = new User(1, "Bob Finder", "bob@campus.edu", "hash", UserRole.STUDENT, LocalDateTime.now());
        studentClaimant = new User(2, "Alice Owner", "alice@campus.edu", "hash", UserRole.STUDENT, LocalDateTime.now());
        adminUser = new Admin(3, "Security Admin", "admin@campus.edu", "hash", "Campus Safety", LocalDateTime.now());

        userDAO.put(studentFinder);
        userDAO.put(studentClaimant);
        userDAO.put(adminUser);

        foundFlask = new FoundItem(10, 1, 1, "Blue Water Flask", "Accessories", "Metal flask", LocalDate.now());
        lostWallet = new LostItem(20, 2, 1, "Leather Wallet", "Personal", "Bifold wallet", LocalDate.now());

        itemDAO.put(foundFlask);
        itemDAO.put(lostWallet);

        claimService = new ClaimService(claimDAO, itemDAO, userDAO);
    }

    @Test
    @DisplayName("Submit claim on active found item succeeds")
    void testSubmitClaimSuccess() {
        Claim claim = claimService.submitClaim(10, 2, "Has my initials AJ scratched on bottom");
        assertNotNull(claim);
        assertEquals(ClaimStatus.PENDING, claim.getStatus());
        assertEquals(10, claim.getItemId());
        assertEquals(2, claim.getUserId());
    }

    @Test
    @DisplayName("Cannot file a claim on a LOST report")
    void testCannotClaimLostReport() {
        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(20, 2, "This is mine")
        );
    }

    @Test
    @DisplayName("Claimant cannot claim an item they turned in themselves")
    void testFinderCannotClaimOwnTurnIn() {
        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(10, 1, "I want it back now")
        );
    }

    @Test
    @DisplayName("Prevent duplicate pending claims by same student on same item")
    void testPreventDuplicatePendingClaims() {
        claimService.submitClaim(10, 2, "First claim proof");

        assertThrows(InvalidClaimException.class, () ->
                claimService.submitClaim(10, 2, "Second duplicate claim proof")
        );
    }

    @Test
    @DisplayName("Approve claim marks claim APPROVED and item RECOVERED")
    void testApproveClaimLifecycle() {
        Claim claim = claimService.submitClaim(10, 2, "Valid proof");
        assertEquals(ItemStatus.ACTIVE, foundFlask.getStatus());

        boolean approved = claimService.approveClaim(claim.getClaimId(), 3);
        assertTrue(approved);

        assertEquals(ClaimStatus.APPROVED, claim.getStatus());
        assertEquals(ItemStatus.RECOVERED, foundFlask.getStatus(), "Approved claim must mark item RECOVERED");
    }

    @Test
    @DisplayName("Reject claim marks claim REJECTED without modifying item status")
    void testRejectClaim() {
        Claim claim = claimService.submitClaim(10, 2, "Weak proof");
        boolean rejected = claimService.rejectClaim(claim.getClaimId(), 3);
        assertTrue(rejected);

        assertEquals(ClaimStatus.REJECTED, claim.getStatus());
        assertEquals(ItemStatus.ACTIVE, foundFlask.getStatus(), "Rejected claim leaves item ACTIVE");
    }

    @Test
    @DisplayName("Non-admin user cannot approve or reject claims")
    void testNonAdminCannotReviewClaims() {
        Claim claim = claimService.submitClaim(10, 2, "Valid proof");

        // studentFinder (user ID 1) is not an Admin
        assertThrows(InvalidClaimException.class, () ->
                claimService.approveClaim(claim.getClaimId(), 1)
        );

        assertThrows(InvalidClaimException.class, () ->
                claimService.rejectClaim(claim.getClaimId(), 1)
        );
    }

    @Test
    @DisplayName("Simulated failure in approveClaim triggers exception")
    void testApproveClaimWithSimulatedFailureThrowsException() {
        Claim claim = claimService.submitClaim(10, 2, "Valid proof");
        assertThrows(CampusLostFoundException.class, () ->
                claimService.approveClaimWithSimulatedFailure(claim.getClaimId(), 3)
        );
    }
}
