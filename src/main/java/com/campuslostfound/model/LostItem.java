package com.campuslostfound.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class LostItem extends Item {

    public LostItem(int userId, int locationId, String name, String category,
                    String description, LocalDate itemDate) {
        super(userId, locationId, name, category, description, ItemType.LOST, itemDate);
    }

    public LostItem(int itemId, int userId, int locationId, String name, String category,
                    String description, LocalDate itemDate) {
        super(itemId, userId, locationId, name, category, description, ItemType.LOST, itemDate);
    }

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
        return false;
    }
}
