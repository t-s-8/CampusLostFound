package com.campuslostfound.api;

import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.model.*;
import com.campuslostfound.service.ItemService;
import com.campuslostfound.service.SearchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemService items;
    private final SearchService search;
    private final LocationDAO locations;
    private final SessionAuth sessions;

    public ItemController(ItemService items, SearchService search, LocationDAO locations, SessionAuth sessions) {
        this.items=items; this.search=search; this.locations=locations; this.sessions=sessions;
    }

    @GetMapping
    public List<ItemRecord> list(@RequestParam(required=false) String keyword,
                                 @RequestParam(required=false) String category,
                                 @RequestParam(required=false) Integer locationId,
                                 @RequestParam(required=false) String type,
                                 @RequestParam(required=false) String status,
                                 @RequestParam(defaultValue="false") boolean mine,
                                 HttpSession session) {
        User user = optionalUser(session);
        if (mine) {
            if (user == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in required.");
            return items.getItemsByReporter(user.getUserId()).stream()
                    .map(item -> ItemRecord.from(item, locations.findById(item.getLocationId()).orElse(null), user.getUserId())).toList();
        }
        boolean admin = user != null && user.isAdmin();
        ItemStatus safeStatus = admin && status != null ? parseStatus(status) : ItemStatus.ACTIVE;
        ItemType itemType = type == null || type.isBlank() ? null : parseType(type);
        return search.search(keyword, category, locationId, itemType, safeStatus).stream()
                .map(item -> ItemRecord.from(item, locations.findById(item.getLocationId()).orElse(null), user == null ? null : user.getUserId())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRecord create(@RequestBody ReportRequest request, HttpSession session) {
        User user = sessions.requireUser(session);
        if (request.name()==null || request.category()==null || request.itemDate()==null || request.locationId()<=0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, category, date, and campus location are required.");
        }
        ItemType type = parseType(request.itemType());
        Item saved = type == ItemType.LOST
                ? items.reportLostItem(user.getUserId(), request.locationId(), request.name(), request.category(), request.description(), request.itemDate())
                : items.reportFoundItem(user.getUserId(), request.locationId(), request.name(), request.category(), request.description(), request.itemDate());
        return ItemRecord.from(saved, locations.findById(saved.getLocationId()).orElse(null), user.getUserId());
    }

    private User optionalUser(HttpSession session) {
        try { return sessions.requireUser(session); }
        catch (ResponseStatusException ex) { return null; }
    }
    private ItemType parseType(String value) {
        try { return ItemType.valueOf(value.trim().toUpperCase()); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type must be LOST or FOUND."); }
    }
    private ItemStatus parseStatus(String value) {
        try { return ItemStatus.valueOf(value.trim().toUpperCase()); }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown item status."); }
    }
    public record ReportRequest(String itemType, String name, String category, String description,
                                LocalDate itemDate, int locationId) { }
}
