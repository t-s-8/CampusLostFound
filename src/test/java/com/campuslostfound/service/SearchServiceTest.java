package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dsa.ItemSearch;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.LostItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchService Unit Tests")
class SearchServiceTest {

    private SearchService searchService;
    private InMemoryItemDAO itemDAO;

    private static class InMemoryItemDAO extends ItemDAO {
        private final List<Item> items = new ArrayList<>();

        @Override
        public List<Item> findAll() {
            return new ArrayList<>(items);
        }

        @Override
        public List<Item> search(String keyword, String category, Integer locationId, ItemType type, ItemStatus status) {
            List<Item> res = new ArrayList<>();
            for (Item item : items) {
                boolean match = true;
                if (keyword != null && !keyword.isEmpty()) {
                    boolean inName = item.getName() != null && item.getName().toLowerCase().contains(keyword.toLowerCase());
                    boolean inDesc = item.getDescription() != null && item.getDescription().toLowerCase().contains(keyword.toLowerCase());
                    if (!inName && !inDesc) match = false;
                }
                if (category != null && !category.equalsIgnoreCase(item.getCategory())) match = false;
                if (locationId != null && locationId != item.getLocationId()) match = false;
                if (type != null && type != item.getItemType()) match = false;
                if (status != null && status != item.getStatus()) match = false;
                if (match) res.add(item);
            }
            return res;
        }

        void add(Item item) {
            items.add(item);
        }
    }

    @BeforeEach
    void setUp() {
        itemDAO = new InMemoryItemDAO();
        searchService = new SearchService(itemDAO);

        itemDAO.add(new LostItem(1, 10, 1, "Scientific Calculator", "Electronics", "Casio fx-991EX", LocalDate.now()));
        itemDAO.add(new FoundItem(2, 20, 2, "Casio Calculator", "Electronics", "Found in lab", LocalDate.now()));
        itemDAO.add(new LostItem(3, 30, 1, "Gym Backpack", "Bags", "Nike black bag", LocalDate.now()));
    }

    @Test
    @DisplayName("Quick search returns active matching items")
    void testQuickSearch() {
        List<Item> results = searchService.quickSearch("Calculator");
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("Search in-memory index loaded from DAO")
    void testInMemorySearchIndex() {
        ItemSearch index = searchService.createInMemorySearchIndex();
        assertEquals(3, index.size());

        List<Item> bags = searchService.searchInMemory(index, "Backpack", "Bags", null, null);
        assertEquals(1, bags.size());
        assertEquals(3, bags.get(0).getItemId());
    }
}
