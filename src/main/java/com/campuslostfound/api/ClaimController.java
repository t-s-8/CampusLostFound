package com.campuslostfound.api;

import com.campuslostfound.model.Claim;
import com.campuslostfound.model.User;
import com.campuslostfound.service.ClaimService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService claims;
    private final SessionAuth sessions;
    public ClaimController(ClaimService claims, SessionAuth sessions) { this.claims=claims; this.sessions=sessions; }

    @GetMapping("/mine")
    public List<ClaimRecord> mine(HttpSession session) {
        User user=sessions.requireUser(session);
        return claims.getClaimsForUser(user.getUserId()).stream().map(ClaimRecord::from).toList();
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimRecord submit(@RequestBody SubmitRequest request, HttpSession session) {
        User user=sessions.requireUser(session);
        if (request.itemId()<=0 || request.verificationDetails()==null || request.verificationDetails().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Item and ownership verification details are required.");
        return ClaimRecord.from(claims.submitClaim(request.itemId(),user.getUserId(),request.verificationDetails()));
    }
    @GetMapping("/admin/pending")
    public List<ClaimRecord> pending(HttpSession session) {
        sessions.requireAdmin(session);
        return claims.getPendingClaims().stream().map(ClaimRecord::from).toList();
    }
    @GetMapping("/admin/all")
    public List<ClaimRecord> all(HttpSession session) {
        sessions.requireAdmin(session);
        return claims.getAllClaims().stream().map(ClaimRecord::from).toList();
    }
    @PostMapping("/{id}/approve")
    public Map<String,Boolean> approve(@PathVariable int id,HttpSession session) {
        User admin=sessions.requireAdmin(session);
        return Map.of("updated",claims.approveClaim(id,admin.getUserId()));
    }
    @PostMapping("/{id}/reject")
    public Map<String,Boolean> reject(@PathVariable int id,HttpSession session) {
        User admin=sessions.requireAdmin(session);
        return Map.of("updated",claims.rejectClaim(id,admin.getUserId()));
    }
    public record SubmitRequest(int itemId, String verificationDetails) { }
}
