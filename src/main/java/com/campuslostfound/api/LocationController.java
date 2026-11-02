package com.campuslostfound.api;

import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.model.Location;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final LocationDAO locations;
    public LocationController(LocationDAO locations) { this.locations = locations; }
    @GetMapping public List<Location> list() { return locations.findAll(); }
}
