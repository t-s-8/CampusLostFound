package com.campuslostfound.api;

import com.campuslostfound.model.Item;
import com.campuslostfound.model.Location;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ItemRecord(int itemId, boolean mine, int locationId, String location, String name, String category,
                         String description, String itemType, LocalDate itemDate, String status, LocalDateTime createdAt) {
    public static ItemRecord from(Item item, Location location) { return from(item, location, null); }
    public static ItemRecord from(Item item, Location location, Integer viewerUserId) {
        return new ItemRecord(item.getItemId(), viewerUserId != null && item.getUserId() == viewerUserId,
                item.getLocationId(), location == null ? "" : location.getFullLocation(), item.getName(),
                item.getCategory(), item.getDescription(), item.getItemType().name(), item.getItemDate(),
                item.getStatus().name(), item.getCreatedAt());
    }
}
