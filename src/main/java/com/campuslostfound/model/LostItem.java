package com.campuslostfound.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Concrete model representing a lost item reported by a student.
 * Demonstrates inheritance and polymorphism.
 */
public class LostItem extends Item {

    /**
     * Overloaded constructor for reporting a new lost item.
     */
    public LostItem(int userId, int locationId, String name, String category,
                    String description, LocalDate itemDate) {
        super(userId, locationId, name, category, description, ItemType.LOST, itemDate);
    }

    /**
     * Overloaded constructor with item ID and default ACTIVE status.
     */
    public LostItem(int itemId, int userId, int locationId, String name, String category,
                    String description, LocalDate itemDate) {
        super(itemId, userId, locationId, name, category, description, ItemType.LOST, itemDate);
    }

    /**
     * Full constructor for instantiating lost items from database.
     */
    public LostItem(int itemId, int userId, int locationId, String name, String category,
                    String description, LocalDate itemDate, ItemStatus status, LocalDateTime createdAt) {
        super(itemId, userId, locationId, name, category, description, ItemType.LOST, itemDate, status, createdAt);
    }

    @Override
    public String getDisplayDetails() {
        return "[LOST ITEM] " + getName() + " (" + getCategory() + ") - Reported on " + getItemDate() +
                ". Status: " + getStatus();
    }

    @Override
    public boolean isClaimable() {
        // Lost item reports cannot be claimed by others; they represent a missing item search
        return false;
    }
}
