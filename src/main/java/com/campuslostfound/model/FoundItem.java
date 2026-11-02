package com.campuslostfound.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FoundItem extends Item {

    public FoundItem(int userId, int locationId, String name, String category,
                     String description, LocalDate itemDate) {
        super(userId, locationId, name, category, description, ItemType.FOUND, itemDate);
    }

    public FoundItem(int itemId, int userId, int locationId, String name, String category,
                     String description, LocalDate itemDate) {
        super(itemId, userId, locationId, name, category, description, ItemType.FOUND, itemDate);
    }

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
        return getStatus() == ItemStatus.ACTIVE || getStatus() == ItemStatus.MATCHED;
    }
}
