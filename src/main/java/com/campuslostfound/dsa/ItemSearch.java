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

public class ItemSearch {

    private final List<Item> itemList;
    private final Map<Integer, Item> itemMap;

    public ItemSearch() {
        this.itemList = new ArrayList<>();
        this.itemMap = new HashMap<>();
    }

    public void addItem(Item item) {
        if (item == null) {
            throw new InvalidItemException("Cannot add a null item.");
        }
        itemList.add(item);
        if (item.getItemId() > 0) {
            itemMap.put(item.getItemId(), item);
        }
    }

    public void addAll(Iterable<? extends Item> items) {
        if (items != null) {
            for (Item item : items) {
                addItem(item);
            }
        }
    }

    public Item findById(int itemId) {
        return itemMap.get(itemId);
    }

    public boolean removeItem(int itemId) {
        Item removed = itemMap.remove(itemId);
        if (removed != null) {
            return itemList.remove(removed);
        }
        return false;
    }

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

    public List<LostItem> getLostItems() {
        List<LostItem> lost = new ArrayList<>();
        for (Item item : itemList) {
            if (item instanceof LostItem lostItem) {
                lost.add(lostItem);
            } else if (item.getItemType() == ItemType.LOST) {
                lost.add(new LostItem(item.getItemId(), item.getUserId(), item.getLocationId(),
                        item.getName(), item.getCategory(), item.getDescription(),
                        item.getItemDate(), item.getStatus(), item.getCreatedAt()));
            }
        }
        return lost;
    }

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
