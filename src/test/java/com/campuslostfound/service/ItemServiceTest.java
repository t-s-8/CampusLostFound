package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.exception.InvalidItemException;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.Location;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemService Unit & Validation Tests")
class ItemServiceTest {

    private ItemService itemService;
    private InMemoryItemDAO itemDAO;
    private InMemoryLocationDAO locationDAO;
    private TestMatchService matchService;

    // In-memory test doubles
    private static class InMemoryItemDAO extends ItemDAO {
        private final Map<Integer, Item> items = new HashMap<>();
        private int sequence = 1;

        @Override
        public Item create(Item item) {
            item.setItemId(sequence++);
            items.put(item.getItemId(), item);
            return item;
        }

        @Override
        public Optional<Item> findById(int itemId) {
            return Optional.ofNullable(items.get(itemId));
        }

        @Override
        public List<Item> findAll() {
            return new ArrayList<>(items.values());
        }

        @Override
        public List<Item> findByTypeAndStatus(ItemType type, ItemStatus status) {
            List<Item> res = new ArrayList<>();
            for (Item item : items.values()) {
                if (item.getItemType() == type && item.getStatus() == status) {
                    res.add(item);
                }
            }
            return res;
        }

        @Override
        public List<Item> findByUserId(int userId) {
            List<Item> res = new ArrayList<>();
            for (Item item : items.values()) {
                if (item.getUserId() == userId) {
                    res.add(item);
                }
            }
            return res;
        }

        @Override
        public boolean updateStatus(int itemId, ItemStatus status) {
            Item item = items.get(itemId);
            if (item != null) {
                item.setStatus(status);
                return true;
            }
            return false;
        }
    }

    private static class InMemoryLocationDAO extends LocationDAO {
        private final Map<Integer, Location> locations = new HashMap<>();

        @Override
        public Optional<Location> findById(int locationId) {
            return Optional.ofNullable(locations.get(locationId));
        }

        void put(Location loc) {
            locations.put(loc.getLocationId(), loc);
        }
    }

    private static class TestMatchService extends MatchService {
        private boolean matchCalled = false;

        @Override
        public List<Match> findAndRecordMatchesForItem(Item item) {
            matchCalled = true;
            return new ArrayList<>();
        }

        boolean wasMatchCalled() {
            return matchCalled;
        }
    }

    @BeforeEach
    void setUp() {
        itemDAO = new InMemoryItemDAO();
        locationDAO = new InMemoryLocationDAO();
        matchService = new TestMatchService();

        locationDAO.put(new Location(1, "Main Library", "Floor 2"));
        locationDAO.put(new Location(2, "Student Center", "Lobby"));

        itemService = new ItemService(itemDAO, locationDAO, matchService);
    }

    @Test
    @DisplayName("Report lost item successfully persists and triggers matching")
    void testReportLostItemSuccess() {
        LostItem lost = itemService.reportLostItem(10, 1, "Graphing Calculator", "Electronics", "TI-84 Plus", LocalDate.now());
        assertNotNull(lost);
        assertEquals(1, lost.getItemId());
        assertEquals(ItemType.LOST, lost.getItemType());
        assertEquals(ItemStatus.ACTIVE, lost.getStatus());
        assertTrue(matchService.wasMatchCalled(), "Reporting lost item must coordinate matching");
    }

    @Test
    @DisplayName("Report found item successfully persists and triggers matching")
    void testReportFoundItemSuccess() {
        FoundItem found = itemService.reportFoundItem(20, 2, "Blue Water Flask", "Accessories", "Hydro Flask", LocalDate.now());
        assertNotNull(found);
        assertEquals(1, found.getItemId());
        assertEquals(ItemType.FOUND, found.getItemType());
        assertEquals(ItemStatus.ACTIVE, found.getStatus());
        assertTrue(matchService.wasMatchCalled(), "Reporting found item must coordinate matching");
    }

    @Test
    @DisplayName("Validation failure: Blank name rejected")
    void testValidationFailureBlankName() {
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(10, 1, "   ", "Electronics", "Desc", LocalDate.now())
        );
        assertThrows(InvalidItemException.class, () ->
                itemService.reportFoundItem(10, 1, null, "Electronics", "Desc", LocalDate.now())
        );
    }

    @Test
    @DisplayName("Validation failure: Blank category rejected")
    void testValidationFailureBlankCategory() {
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(10, 1, "Item", "  ", "Desc", LocalDate.now())
        );
    }

    @Test
    @DisplayName("Validation failure: Null date rejected")
    void testValidationFailureNullDate() {
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(10, 1, "Item", "Electronics", "Desc", null)
        );
    }

    @Test
    @DisplayName("Validation failure: Non-positive user ID rejected")
    void testValidationFailureInvalidUserId() {
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(0, 1, "Item", "Electronics", "Desc", LocalDate.now())
        );
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(-5, 1, "Item", "Electronics", "Desc", LocalDate.now())
        );
    }

    @Test
    @DisplayName("Validation failure: Non-positive location ID rejected")
    void testValidationFailureInvalidLocationId() {
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(10, 0, "Item", "Electronics", "Desc", LocalDate.now())
        );
    }

    @Test
    @DisplayName("Validation failure: Non-existent location ID rejected")
    void testValidationFailureNonExistentLocation() {
        assertThrows(InvalidItemException.class, () ->
                itemService.reportLostItem(10, 999, "Item", "Electronics", "Desc", LocalDate.now())
        );
    }

    @Test
    @DisplayName("Find item by ID and status update lifecycle")
    void testFindByIdAndUpdateStatus() {
        LostItem lost = itemService.reportLostItem(10, 1, "Umbrella", "Accessories", "Black umbrella", LocalDate.now());
        Optional<Item> foundOpt = itemService.getItemById(lost.getItemId());
        assertTrue(foundOpt.isPresent());
        assertEquals("Umbrella", foundOpt.get().getName());

        assertTrue(itemService.updateItemStatus(lost.getItemId(), ItemStatus.RECOVERED));
        assertEquals(ItemStatus.RECOVERED, itemService.getItemById(lost.getItemId()).get().getStatus());

        assertFalse(itemService.getItemById(999).isPresent());
    }

    @Test
    @DisplayName("Retrieval filters: active lost, active found, and items by reporter")
    void testRetrievalFilters() {
        itemService.reportLostItem(10, 1, "Lost Book", "Books", "Math", LocalDate.now());
        itemService.reportFoundItem(10, 1, "Found Book", "Books", "Physics", LocalDate.now());
        itemService.reportLostItem(20, 2, "Lost Keys", "Keys", "Car", LocalDate.now());

        assertEquals(2, itemService.getActiveLostItems().size());
        assertEquals(1, itemService.getActiveFoundItems().size());
        assertEquals(2, itemService.getItemsByReporter(10).size());
        assertEquals(1, itemService.getItemsByReporter(20).size());
        assertEquals(3, itemService.getAllItems().size());
    }
}
