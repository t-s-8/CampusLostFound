package com.campuslostfound.api;

import com.campuslostfound.dao.LocationDAO;
import com.campuslostfound.model.*;
import com.campuslostfound.service.ItemService;
import com.campuslostfound.service.MatchService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchService matches;
    private final ItemService items;
    private final LocationDAO locations;
    private final SessionAuth sessions;
    public MatchController(MatchService matches, ItemService items, LocationDAO locations, SessionAuth sessions) {
        this.matches=matches; this.items=items; this.locations=locations; this.sessions=sessions;
    }
    @GetMapping
    public List<MatchRecord> list(HttpSession session) {
        User user=sessions.requireUser(session);
        Map<Integer,Match> visible=new LinkedHashMap<>();
        if (user.isAdmin()) matches.getAllMatches().forEach(m -> visible.put(m.getMatchId(),m));
        else for (Item item : items.getItemsByReporter(user.getUserId())) {
            List<Match> found = item.getItemType()==ItemType.LOST
                    ? matches.getMatchesForLostItem(item.getItemId()) : matches.getMatchesForFoundItem(item.getItemId());
            found.forEach(m -> visible.put(m.getMatchId(),m));
        }
        return visible.values().stream().sorted(Comparator.comparingDouble(Match::getScore).reversed())
                .map(m -> MatchRecord.from(m, itemRecord(m.getLostItemId(), user.getUserId()), itemRecord(m.getFoundItemId(), user.getUserId()))).toList();
    }
    @PutMapping("/{id}/status")
    public Map<String,Boolean> updateStatus(@PathVariable int id, @RequestBody StatusRequest request, HttpSession session) {
        sessions.requireAdmin(session);
        try { return Map.of("updated",matches.updateMatchStatus(id,MatchStatus.valueOf(request.status().toUpperCase()))); }
        catch(Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown match status."); }
    }
    private ItemRecord itemRecord(int id, int viewerUserId) {
        Item item=items.getItemById(id).orElse(null);
        return item==null ? null : ItemRecord.from(item,locations.findById(item.getLocationId()).orElse(null),viewerUserId);
    }
    public record StatusRequest(String status) { }
}
