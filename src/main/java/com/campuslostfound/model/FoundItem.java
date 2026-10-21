package com.campuslostfound.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete model representing a found item turned in on campus.
 * Demonstrates inheritance and polymorphism.
 */
public class FoundItem extends Item {

    /**
     * Overloaded constructor for reporting a new found item.
     */
    public FoundItem(int userId, int locationId, String name, String category,
                     String description, LocalDate itemDate) {
        super(userId, locationId, name, category, description, ItemType.FOUND, itemDate);
    }

    /**
     * Overloaded constructor with item ID and default ACTIVE status.
     */
    public FoundItem(int itemId, int userId, int locationId, String name, String category,
                     String description, LocalDate itemDate) {
        super(itemId, userId, locationId, name, category, description, ItemType.FOUND, itemDate);
    }

    /**
     * Full constructor for instantiating found items from database.
     */
    public FoundItem(int itemId, int userId, int locationId, String name, String category,
                     String description, LocalDate itemDate, ItemStatus status, LocalDateTime createdAt) {
        super(itemId, userId, locationId, name, category, description, ItemType.FOUND, itemDate, status, createdAt);
    }

    @Override
    public String getDisplayDetails() {
        return "[FOUND ITEM] " + getName() + " (" + getCategory() + ") - Found on " + getItemDate() +
                ". Status: " + getStatus();
    }

    @Override
    public boolean isClaimable() {
        // A found item is claimable if it is ACTIVE or MATCHED (not yet claimed or recovered)
        return getStatus() == ItemStatus.ACTIVE || getStatus() == ItemStatus.MATCHED;
    }
}
