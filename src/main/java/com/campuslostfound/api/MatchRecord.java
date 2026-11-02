package com.campuslostfound.api;

import com.campuslostfound.model.Match;
import java.time.LocalDateTime;

public record MatchRecord(int matchId, double score, String status, LocalDateTime createdAt,
                          ItemRecord lostItem, ItemRecord foundItem) {
    public static MatchRecord from(Match m, ItemRecord lost, ItemRecord found) {
        return new MatchRecord(m.getMatchId(), m.getScore(), m.getStatus().name(), m.getCreatedAt(), lost, found);
    }
}
