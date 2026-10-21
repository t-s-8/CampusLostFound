package com.campuslostfound.model;

import java.util.Objects;

/**
 * Represents a physical campus area or building where items are lost or found.
 */
public class Location {

    private int locationId;
    private String locationName;
    private String building;

    /**
     * Overloaded constructor without building.
     */
    public Location(String locationName) {
        this(0, locationName, null);
    }

    /**
     * Overloaded constructor without ID.
     */
    public Location(String locationName, String building) {
        this(0, locationName, building);
    }

    /**
     * Full constructor for instantiating locations from database.
     */
    public Location(int locationId, String locationName, String building) {
        setLocationId(locationId);
        setLocationName(locationName);
        setBuilding(building);
    }

    public int getLocationId() {
        return locationId;
    }

    public void setLocationId(int locationId) {
        if (locationId < 0) {
            throw new IllegalArgumentException("Location ID cannot be negative.");
        }
        this.locationId = locationId;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        if (locationName == null || locationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Location name cannot be empty.");
        }
        this.locationName = locationName.trim();
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = (building != null && !building.trim().isEmpty()) ? building.trim() : null;
    }

    /**
     * Returns a human-readable composite location descriptor.
     */
    public String getFullLocation() {
        if (building == null || building.isEmpty()) {
            return locationName;
        }
        return locationName + " (" + building + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location location)) return false;
        return locationId == location.locationId &&
                Objects.equals(locationName, location.locationName) &&
                Objects.equals(building, location.building);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, locationName, building);
    }

    @Override
    public String toString() {
        return getFullLocation();
    }
}
