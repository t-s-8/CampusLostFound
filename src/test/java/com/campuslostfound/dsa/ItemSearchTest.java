package com.campuslostfound.dsa;

import com.campuslostfound.exception.InvalidItemException;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.LostItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ItemSearch (ArrayList & HashMap Collections) Tests")
class ItemSearchTest {

    private ItemSearch searchIndex;
    private Item lostLaptop;
    private Item foundCharger;
    private Item lostIdCard;
    private Item foundWallet;

    @BeforeEach
    void setUp() {
        searchIndex = new ItemSearch();

        lostLaptop = new LostItem(1, 10, 1, "Dell XPS 15 Laptop", "Electronics",
                "Silver aluminum laptop with carbon fiber palm rest", LocalDate.of(2026, 10, 10),
                ItemStatus.ACTIVE, LocalDateTime.now());

        foundCharger = new FoundItem(2, 20, 1, "Dell 65W Charger", "Electronics",
                "Black USB-C power adapter found near outlet", LocalDate.of(2026, 10, 10),
                ItemStatus.ACTIVE, LocalDateTime.now());

        lostIdCard = new LostItem(3, 11, 2, "Campus ID Card", "ID & Cards",
                "Student ID card for Bob Smith", LocalDate.of(2026, 10, 12),
                ItemStatus.MATCHED, LocalDateTime.now());

        foundWallet = new FoundItem(4, 21, 3, "Brown Leather Wallet", "Personal Belongings",
                "Contains cash and library card", LocalDate.of(2026, 10, 14),
                ItemStatus.RECOVERED, LocalDateTime.now());

        searchIndex.addItem(lostLaptop);
        searchIndex.addItem(foundCharger);
        searchIndex.addItem(lostIdCard);
        searchIndex.addItem(foundWallet);
    }

    @Test
    @DisplayName("HashMap O(1) fast ID lookup")
    void testHashMapFastLookup() {
        assertEquals(lostLaptop, searchIndex.findById(1));
        assertEquals(foundCharger, searchIndex.findById(2));
        assertEquals(lostIdCard, searchIndex.findById(3));
        assertEquals(foundWallet, searchIndex.findById(4));
        assertNull(searchIndex.findById(999), "Non-existent ID must return null");
    }

    @Test
    @DisplayName("ArrayList sequential scan: search by keyword across name and description")
    void testSearchByKeyword() {
        List<Item> dellItems = searchIndex.search("Dell");
        assertEquals(2, dellItems.size(), "Should find 2 Dell items");

        List<Item> carbonItems = searchIndex.search("carbon fiber");
        assertEquals(1, carbonItems.size());
        assertEquals(lostLaptop, carbonItems.get(0));

        List<Item> walletItems = searchIndex.search("wALLeT");
        assertEquals(1, walletItems.size());
        assertEquals(foundWallet, walletItems.get(0));

        assertEquals(4, searchIndex.search("").size());
        assertEquals(4, searchIndex.search(null).size());
    }

    @Test
    @DisplayName("Overloaded search: keyword AND category")
    void testSearchByKeywordAndCategory() {
        List<Item> electronics = searchIndex.search("Dell", "Electronics");
        assertEquals(2, electronics.size());

        List<Item> personal = searchIndex.search("Dell", "Personal Belongings");
        assertTrue(personal.isEmpty());
    }

    @Test
    @DisplayName("Overloaded search: keyword, category, and location ID")
    void testSearchByKeywordCategoryAndLocation() {
        List<Item> loc1 = searchIndex.search("Dell", "Electronics", 1);
        assertEquals(2, loc1.size());

        List<Item> loc2 = searchIndex.search("Dell", "Electronics", 2);
        assertTrue(loc2.isEmpty());
    }

    @Test
    @DisplayName("Overloaded search: keyword, category, and date range")
    void testSearchByDateRange() {
        LocalDate start = LocalDate.of(2026, 10, 9);
        LocalDate end = LocalDate.of(2026, 10, 11);

        List<Item> items = searchIndex.search("", "Electronics", start, end);
        assertEquals(2, items.size());

        List<Item> idCard = searchIndex.search("ID", "ID & Cards", LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 12));
        assertEquals(1, idCard.size());
        assertEquals(lostIdCard, idCard.get(0));
    }

    @Test
    @DisplayName("Polymorphic segregation of LOST and FOUND item collections")
    void testSegregateLostAndFound() {
        List<LostItem> lost = searchIndex.getLostItems();
        assertEquals(2, lost.size());
        assertTrue(lost.contains(lostLaptop));
        assertTrue(lost.contains(lostIdCard));

        List<FoundItem> found = searchIndex.getFoundItems();
        assertEquals(2, found.size());
        assertTrue(found.contains(foundCharger));
        assertTrue(found.contains(foundWallet));
    }

    @Test
    @DisplayName("Filtering by status and location")
    void testFilteringByStatusAndLocation() {
        List<Item> activeItems = searchIndex.filterByStatus(ItemStatus.ACTIVE);
        assertEquals(2, activeItems.size());

        List<Item> recoveredItems = searchIndex.filterByStatus(ItemStatus.RECOVERED);
        assertEquals(1, recoveredItems.size());
        assertEquals(foundWallet, recoveredItems.get(0));

        List<Item> loc1Items = searchIndex.filterByLocation(1);
        assertEquals(2, loc1Items.size());
    }

    @Test
    @DisplayName("Item removal and empty collection handling")
    void testRemovalAndClear() {
        assertTrue(searchIndex.removeItem(1));
        assertNull(searchIndex.findById(1));
        assertEquals(3, searchIndex.size());

        assertFalse(searchIndex.removeItem(999), "Removing non-existent ID returns false");

        searchIndex.clear();
        assertTrue(searchIndex.isEmpty());
        assertEquals(0, searchIndex.size());

        assertThrows(InvalidItemException.class, () -> searchIndex.addItem(null));
    }
}
