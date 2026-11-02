package com.campuslostfound.database;

import com.campuslostfound.dao.ClaimDAO;
import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.dao.MatchDAO;
import com.campuslostfound.dao.UserDAO;
import com.campuslostfound.model.Admin;
import com.campuslostfound.model.Claim;
import com.campuslostfound.model.ClaimStatus;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.Location;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.Match;
import com.campuslostfound.model.MatchStatus;
import com.campuslostfound.model.User;
import com.campuslostfound.model.UserRole;
import com.campuslostfound.service.AuthService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DAO Layer Live MySQL CRUD Integration Tests")
class DaoCrudIntegrationTest {

    private UserDAO userDAO;
    private LocationDAO locationDAO;
    private ItemDAO itemDAO;
    private MatchDAO matchDAO;
    private ClaimDAO claimDAO;

    @BeforeAll
    static void checkDatabaseAvailability() {
        Assumptions.assumeTrue(DatabaseConnection.isDatabaseAvailable(),
                "Local MySQL database is not reachable. Skipping live DAO CRUD integration tests.");
    }

    @BeforeEach
    void setUp() {
        userDAO = new UserDAO();
        locationDAO = new LocationDAO();
        itemDAO = new ItemDAO();
        matchDAO = new MatchDAO();
        claimDAO = new ClaimDAO();
    }

    @Test
    @DisplayName("UserDAO CRUD: create student and admin, findByEmail, findById, findAll")
    void testUserDaoCrud() {
        String uid = String.valueOf(System.currentTimeMillis() % 1000000);
        User student = new User(0, "Student " + uid, "student_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now());
        User createdStudent = userDAO.create(student);
        assertTrue(createdStudent.getUserId() > 0);

        Admin admin = new Admin(0, "Admin " + uid, "admin_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), "IT Security", LocalDateTime.now());
        User createdAdmin = userDAO.create(admin);
        assertTrue(createdAdmin.getUserId() > 0);
        assertTrue(createdAdmin.isAdmin());

        Optional<User> foundByEmail = userDAO.findByEmail("STUDENT_" + uid + "@TEST.EDU");
        assertTrue(foundByEmail.isPresent());
        assertEquals(createdStudent.getUserId(), foundByEmail.get().getUserId());

        Optional<User> foundById = userDAO.findById(createdAdmin.getUserId());
        assertTrue(foundById.isPresent());
        assertEquals("Administration", ((Admin) foundById.get()).getDepartment());

        assertFalse(userDAO.findById(99999999).isPresent());
    }

    @Test
    @DisplayName("LocationDAO: findById and findAll")
    void testLocationDao() {
        List<Location> locations = locationDAO.findAll();
        assertFalse(locations.isEmpty(), "Locations table must contain seeded locations");

        Location first = locations.get(0);
        Optional<Location> byId = locationDAO.findById(first.getLocationId());
        assertTrue(byId.isPresent());
        assertEquals(first.getLocationName(), byId.get().getLocationName());

        assertFalse(locationDAO.findById(99999999).isPresent());
    }

    @Test
    @DisplayName("ItemDAO CRUD: create lost & found, findById, updateStatus, search")
    void testItemDaoCrud() {
        String uid = String.valueOf(System.currentTimeMillis() % 1000000);
        User user = userDAO.create(new User(0, "Reporter " + uid, "reporter_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));

        LostItem lost = (LostItem) itemDAO.create(new LostItem(
                user.getUserId(), 1, "Lenovo ThinkPad " + uid, "Electronics", "Black laptop", LocalDate.now()
        ));
        assertTrue(lost.getItemId() > 0);
        assertEquals(ItemType.LOST, lost.getItemType());

        FoundItem found = (FoundItem) itemDAO.create(new FoundItem(
                user.getUserId(), 1, "Wireless Mouse " + uid, "Electronics", "Logitech mouse", LocalDate.now()
        ));
        assertTrue(found.getItemId() > 0);
        assertEquals(ItemType.FOUND, found.getItemType());

        Optional<Item> loadedLost = itemDAO.findById(lost.getItemId());
        assertTrue(loadedLost.isPresent());
        assertEquals("Lenovo ThinkPad " + uid, loadedLost.get().getName());

        assertTrue(itemDAO.updateStatus(lost.getItemId(), ItemStatus.MATCHED));
        assertEquals(ItemStatus.MATCHED, itemDAO.findById(lost.getItemId()).get().getStatus());

        List<Item> matchedItems = itemDAO.findByTypeAndStatus(ItemType.LOST, ItemStatus.MATCHED);
        assertTrue(matchedItems.stream().anyMatch(i -> i.getItemId() == lost.getItemId()));

        List<Item> userItems = itemDAO.findByUserId(user.getUserId());
        assertEquals(2, userItems.size());

        List<Item> searchResults = itemDAO.search("ThinkPad " + uid, "Electronics", 1, ItemType.LOST, ItemStatus.MATCHED);
        assertEquals(1, searchResults.size());
        assertEquals(lost.getItemId(), searchResults.get(0).getItemId());

        List<Item> emptyResults = itemDAO.search("NonExistentStringXYZ123", null, null, null, null);
        assertTrue(emptyResults.isEmpty());
    }

    @Test
    @DisplayName("MatchDAO CRUD: create, upsert on duplicate key, find, updateStatus")
    void testMatchDaoCrud() {
        String uid = String.valueOf(System.currentTimeMillis() % 1000000);
        User user = userDAO.create(new User(0, "U_" + uid, "u_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));

        Item lost = itemDAO.create(new LostItem(user.getUserId(), 1, "Lost_" + uid, "Cat", "Desc", LocalDate.now()));
        Item found = itemDAO.create(new FoundItem(user.getUserId(), 1, "Found_" + uid, "Cat", "Desc", LocalDate.now()));

        Match match = new Match(lost.getItemId(), found.getItemId(), 82.5);
        Match saved = matchDAO.create(match);
        assertTrue(saved.getMatchId() > 0);

        Optional<Match> byPair = matchDAO.findByLostAndFound(lost.getItemId(), found.getItemId());
        assertTrue(byPair.isPresent());
        assertEquals(82.5, byPair.get().getScore(), 0.01);

        Match updatedMatch = new Match(lost.getItemId(), found.getItemId(), 95.0, MatchStatus.NOTIFIED, LocalDateTime.now());
        matchDAO.create(updatedMatch);

        Optional<Match> reloaded = matchDAO.findByLostAndFound(lost.getItemId(), found.getItemId());
        assertTrue(reloaded.isPresent());
        assertEquals(95.0, reloaded.get().getScore(), 0.01);
        assertEquals(MatchStatus.NOTIFIED, reloaded.get().getStatus());

        assertTrue(matchDAO.updateStatus(reloaded.get().getMatchId(), MatchStatus.CONFIRMED));
        assertEquals(MatchStatus.CONFIRMED, matchDAO.findByLostAndFound(lost.getItemId(), found.getItemId()).get().getStatus());

        assertFalse(matchDAO.findByLostItemId(lost.getItemId()).isEmpty());
        assertFalse(matchDAO.findByFoundItemId(found.getItemId()).isEmpty());
    }

    @Test
    @DisplayName("ClaimDAO CRUD: create, findById, findByItemId, findPendingClaims, updateStatus")
    void testClaimDaoCrud() {
        String uid = String.valueOf(System.currentTimeMillis() % 1000000);
        User finder = userDAO.create(new User(0, "F_" + uid, "f_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));
        User claimant = userDAO.create(new User(0, "C_" + uid, "c_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), UserRole.STUDENT, LocalDateTime.now()));
        User admin = userDAO.create(new Admin(0, "A_" + uid, "a_" + uid + "@test.edu",
                AuthService.hashPassword("Pass123"), "Admin", LocalDateTime.now()));

        FoundItem item = (FoundItem) itemDAO.create(new FoundItem(
                finder.getUserId(), 1, "Wallet_" + uid, "Personal", "Black leather", LocalDate.now()
        ));

        Claim claim = new Claim(item.getItemId(), claimant.getUserId(), "Contains student card");
        Claim created = claimDAO.create(claim);
        assertTrue(created.getClaimId() > 0);
        assertEquals(ClaimStatus.PENDING, created.getStatus());

        Optional<Claim> loaded = claimDAO.findById(created.getClaimId());
        assertTrue(loaded.isPresent());
        assertEquals("Contains student card", loaded.get().getVerificationDetails());

        List<Claim> byItem = claimDAO.findByItemId(item.getItemId());
        assertEquals(1, byItem.size());

        List<Claim> byUser = claimDAO.findByUserId(claimant.getUserId());
        assertFalse(byUser.isEmpty());

        List<Claim> pending = claimDAO.findPendingClaims();
        assertTrue(pending.stream().anyMatch(c -> c.getClaimId() == created.getClaimId()));

        assertTrue(claimDAO.updateStatus(created.getClaimId(), ClaimStatus.REJECTED, admin.getUserId(), LocalDateTime.now()));
        Claim rejected = claimDAO.findById(created.getClaimId()).orElseThrow();
        assertEquals(ClaimStatus.REJECTED, rejected.getStatus());
        assertEquals(admin.getUserId(), rejected.getReviewedBy());
        assertNotNull(rejected.getReviewedAt());
    }
}
