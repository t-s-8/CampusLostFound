package com.campuslostfound.dsa;

import com.campuslostfound.exception.InvalidItemException;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.LostItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages in-memory item collections, providing fast indexed lookups and keyword/predicate searches.
 *
 * DSA Concepts Demonstrated:
 * 1. HashMap<Integer, Item>: Provides O(1) average time complexity for primary key (ID) lookups.
 * 2. ArrayList<Item>: Provides contiguous storage for sequential linear scanning O(N) during keyword
 *    searches and multi-attribute filtering.
 * 3. Method Overloading: Provides multiple overloaded search variants with progressive filtering criteria.
 */
public class ItemSearch {

    private final List<Item> itemList;
    private final Map<Integer, Item> itemMap;

    public ItemSearch() {
        this.itemList = new ArrayList<>();
        this.itemMap = new HashMap<>();
    }

    /**
     * Adds an item to both internal collections (List and HashMap).
     * Time Complexity: O(1) amortized.
     */
    public void addItem(Item item) {
        if (item == null) {
            throw new InvalidItemException("Cannot add a null item.");
        }
        itemList.add(item);
        if (item.getItemId() > 0) {
            itemMap.put(item.getItemId(), item);
        }
    }

    /**
     * Adds multiple items to both internal collections.
     */
    public void addAll(Iterable<? extends Item> items) {
        if (items != null) {
            for (Item item : items) {
                addItem(item);
            }
        }
    }

    /**
     * Retrieves an item by its primary key ID using the HashMap index.
     * Time Complexity: O(1) average.
     */
    public Item findById(int itemId) {
        return itemMap.get(itemId);
    }

    /**
     * Removes an item by ID from both collections.
     * Time Complexity: O(N) due to ArrayList element removal.
     */
    public boolean removeItem(int itemId) {
        Item removed = itemMap.remove(itemId);
        if (removed != null) {
            return itemList.remove(removed);
        }
        return false;
    }

    /**
     * Returns an unmodifiable view of all managed items.
     */
    public List<Item> getAllItems() {
        return Collections.unmodifiableList(itemList);
    }

    public int size() {
        return itemList.size();
    }

    public boolean isEmpty() {
        return itemList.isEmpty();
    }

    public void clear() {
        itemList.clear();
        itemMap.clear();
    }

    /**
     * Search Variant 1: Search by text keyword across name and description.
     * Time Complexity: O(N).
     */
    public List<Item> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(itemList);
        }
        String term = keyword.trim().toLowerCase();
        List<Item> results = new ArrayList<>();
        for (Item item : itemList) {
            if (matchesKeyword(item, term)) {
                results.add(item);
            }
        }
        return results;
    }

    /**
     * Search Variant 2: Search by keyword AND category filter.
     * Time Complexity: O(N).
     */
    public List<Item> search(String keyword, String category) {
        List<Item> baseResults = search(keyword);
        if (category == null || category.trim().isEmpty()) {
            return baseResults;
        }
        String cat = category.trim().toLowerCase();
        List<Item> filtered = new ArrayList<>();
        for (Item item : baseResults) {
            if (item.getCategory().toLowerCase().equals(cat)) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Search Variant 3: Search by keyword, category, and physical location ID.
     * Time Complexity: O(N).
     */
    public List<Item> search(String keyword, String category, int locationId) {
        List<Item> baseResults = search(keyword, category);
        if (locationId <= 0) {
            return baseResults;
        }
        List<Item> filtered = new ArrayList<>();
        for (Item item : baseResults) {
            if (item.getLocationId() == locationId) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Search Variant 4: Search by keyword, category, and date range filter.
     * Time Complexity: O(N).
     */
    public List<Item> search(String keyword, String category, LocalDate startDate, LocalDate endDate) {
        List<Item> baseResults = search(keyword, category);
        List<Item> filtered = new ArrayList<>();
        for (Item item : baseResults) {
            boolean matches = true;
            if (startDate != null && item.getItemDate().isBefore(startDate)) {
                matches = false;
            }
            if (endDate != null && item.getItemDate().isAfter(endDate)) {
                matches = false;
            }
            if (matches) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Returns only reports of LOST items.
     */
    public List<LostItem> getLostItems() {
        List<LostItem> lost = new ArrayList<>();
        for (Item item : itemList) {
            if (item instanceof LostItem lostItem) {
                lost.add(lostItem);
            } else if (item.getItemType() == ItemType.LOST) {
                // Defensive fallback if generic Item typed as LOST
                lost.add(new LostItem(item.getItemId(), item.getUserId(), item.getLocationId(),
                        item.getName(), item.getCategory(), item.getDescription(),
                        item.getItemDate(), item.getStatus(), item.getCreatedAt()));
            }
        }
        return lost;
    }

    /**
     * Returns only reports of FOUND items.
     */
    public List<FoundItem> getFoundItems() {
        List<FoundItem> found = new ArrayList<>();
        for (Item item : itemList) {
            if (item instanceof FoundItem foundItem) {
                found.add(foundItem);
            } else if (item.getItemType() == ItemType.FOUND) {
                found.add(new FoundItem(item.getItemId(), item.getUserId(), item.getLocationId(),
                        item.getName(), item.getCategory(), item.getDescription(),
                        item.getItemDate(), item.getStatus(), item.getCreatedAt()));
            }
        }
        return found;
    }

    /**
     * Filters items by status (e.g. ACTIVE, RECOVERED).
     */
    public List<Item> filterByStatus(ItemStatus status) {
        if (status == null) {
            return new ArrayList<>(itemList);
        }
        List<Item> filtered = new ArrayList<>();
        for (Item item : itemList) {
            if (item.getStatus() == status) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    /**
     * Filters items by location.
     */
    public List<Item> filterByLocation(int locationId) {
        List<Item> filtered = new ArrayList<>();
        for (Item item : itemList) {
            if (item.getLocationId() == locationId) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    private boolean matchesKeyword(Item item, String lowerKeyword) {
        if (item.getName() != null && item.getName().toLowerCase().contains(lowerKeyword)) {
            return true;
        }
        return item.getDescription() != null && item.getDescription().toLowerCase().contains(lowerKeyword);
    }
}
