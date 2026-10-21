package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.exception.InvalidItemException;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.Match;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service managing lost and found item reports and triggering candidate matching.
 */
public class ItemService {

    private final ItemDAO itemDAO;
    private final LocationDAO locationDAO;
    private final MatchService matchService;

    public ItemService() {
        this(new ItemDAO(), new LocationDAO(), new MatchService());
    }

    public ItemService(ItemDAO itemDAO, LocationDAO locationDAO, MatchService matchService) {
        this.itemDAO = itemDAO;
        this.locationDAO = locationDAO;
        this.matchService = matchService;
    }

    /**
     * Reports a new lost item, saves it to the database, and initiates matching.
     */
    public LostItem reportLostItem(int userId, int locationId, String name, String category,
                                   String description, LocalDate date) {
        validateItemAttributes(name, category, date, userId, locationId);
        LostItem lostItem = new LostItem(userId, locationId, name, category, description, date);
        Item saved = itemDAO.create(lostItem);

        // Run matching against existing active found items
        coordinateMatching(saved);

        return (LostItem) saved;
    }

    /**
     * Reports a new found item, saves it to the database, and initiates matching.
     */
    public FoundItem reportFoundItem(int userId, int locationId, String name, String category,
                                     String description, LocalDate date) {
        validateItemAttributes(name, category, date, userId, locationId);
        FoundItem foundItem = new FoundItem(userId, locationId, name, category, description, date);
        Item saved = itemDAO.create(foundItem);

        // Run matching against existing active lost items
        coordinateMatching(saved);

        return (FoundItem) saved;
    }

    /**
     * Finds an item by ID.
     */
    public Optional<Item> getItemById(int itemId) {
        return itemDAO.findById(itemId);
    }

    /**
     * Retrieves all active lost items.
     */
    public List<Item> getActiveLostItems() {
        return itemDAO.findByTypeAndStatus(ItemType.LOST, ItemStatus.ACTIVE);
    }

    /**
     * Retrieves all active found items.
     */
    public List<Item> getActiveFoundItems() {
        return itemDAO.findByTypeAndStatus(ItemType.FOUND, ItemStatus.ACTIVE);
    }

    /**
     * Retrieves all items reported by a specific user.
     */
    public List<Item> getItemsByReporter(int userId) {
        return itemDAO.findByUserId(userId);
    }

    /**
     * Retrieves all items reported across the campus.
     */
    public List<Item> getAllItems() {
        return itemDAO.findAll();
    }

    /**
     * Updates the status of an item.
     */
    public boolean updateItemStatus(int itemId, ItemStatus status) {
        return itemDAO.updateStatus(itemId, status);
    }

    private void coordinateMatching(Item item) {
        try {
            List<Match> matches = matchService.findAndRecordMatchesForItem(item);
            boolean hasHighMatch = matches.stream().anyMatch(Match::isPossibleMatch);
            if (hasHighMatch) {
                // Update item status to MATCHED (Note: NEVER marks item RECOVERED automatically)
                itemDAO.updateStatus(item.getItemId(), ItemStatus.MATCHED);
                item.setStatus(ItemStatus.MATCHED);
            }
        } catch (Exception e) {
            System.err.println("[ItemService] Warning: Failed to run automatic matching for item " + item.getItemId() + ": " + e.getMessage());
        }
    }

    private void validateItemAttributes(String name, String category, LocalDate date, int userId, int locationId) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidItemException("Item name cannot be empty.");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidItemException("Item category cannot be empty.");
        }
        if (date == null) {
            throw new InvalidItemException("Item date is required.");
        }
        if (userId <= 0) {
            throw new InvalidItemException("Valid user ID required.");
        }
        if (locationId <= 0) {
            throw new InvalidItemException("Valid location ID required.");
        }
        if (locationDAO.findById(locationId).isEmpty()) {
            throw new InvalidItemException("Location with ID " + locationId + " does not exist.");
        }
    }
}
