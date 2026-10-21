package com.campuslostfound.model;

import com.campuslostfound.exception.InvalidItemException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class representing a generic item reported on campus.
 * Demonstrates abstraction, encapsulation, and serves as the polymorphic base for LostItem and FoundItem.
 */
public abstract class Item {

    private int itemId;
    private int userId;
    private int locationId;
    private String name;
    private String category;
    private String description;
    private final ItemType itemType;
    private LocalDate itemDate;
    private ItemStatus status;
    private LocalDateTime createdAt;

    /**
     * Protected constructor for subclasses creating a new item report (without ID).
     */
    protected Item(int userId, int locationId, String name, String category,
                   String description, ItemType itemType, LocalDate itemDate) {
        this(0, userId, locationId, name, category, description, itemType, itemDate, ItemStatus.ACTIVE, LocalDateTime.now());
    }

    /**
     * Protected overloaded constructor with item ID and default ACTIVE status.
     */
    protected Item(int itemId, int userId, int locationId, String name, String category,
                   String description, ItemType itemType, LocalDate itemDate) {
        this(itemId, userId, locationId, name, category, description, itemType, itemDate, ItemStatus.ACTIVE, LocalDateTime.now());
    }

    /**
     * Protected full constructor for instantiating items loaded from database.
     */
    protected Item(int itemId, int userId, int locationId, String name, String category,
                   String description, ItemType itemType, LocalDate itemDate,
                   ItemStatus status, LocalDateTime createdAt) {
        if (itemType == null) {
            throw new InvalidItemException("Item type (LOST or FOUND) must be specified.");
        }
        this.itemType = itemType;
        setItemId(itemId);
        setUserId(userId);
        setLocationId(locationId);
        setName(name);
        setCategory(category);
        setDescription(description);
        setItemDate(itemDate);
        setStatus(status != null ? status : ItemStatus.ACTIVE);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    /**
     * Polymorphic method to return a formatted display summary specific to the item type.
     */
    public abstract String getDisplayDetails();

    /**
     * Polymorphic method determining whether this item is currently claimable by a claimant.
     */
    public abstract boolean isClaimable();

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        if (itemId < 0) {
            throw new InvalidItemException("Item ID cannot be negative.");
        }
        this.itemId = itemId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        if (userId <= 0) {
            throw new InvalidItemException("A valid user ID is required.");
        }
        this.userId = userId;
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        if (locationId <= 0) {
            throw new InvalidItemException("A valid location ID is required.");
        }
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidItemException("Item name cannot be empty.");
        }
        this.name = name.trim();
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            throw new InvalidItemException("Item category cannot be empty.");
        }
        this.category = category.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description.trim() : "";
    }

    public ItemType getItemType() {
        return itemType;
    }

    public LocalDate getItemDate() {
        return itemDate;
    }

    public void setItemDate(LocalDate itemDate) {
        if (itemDate == null) {
            throw new InvalidItemException("Item date cannot be null.");
        }
        this.itemDate = itemDate;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        if (status == null) {
            throw new InvalidItemException("Item status must be specified.");
        }
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return this.status == ItemStatus.ACTIVE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item item)) return false;
        return itemId == item.itemId && itemId != 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemId=" + itemId +
                ", name='" + name + '\'' +
                ", category='" + category + '\'' +
                ", itemType=" + itemType +
                ", status=" + status +
                ", itemDate=" + itemDate +
                '}';
    }
}
