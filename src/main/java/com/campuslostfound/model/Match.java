package com.campuslostfound.model;

import com.campuslostfound.exception.InvalidMatchException;

import java.time.LocalDateTime;
import java.util.Objects;

public class Match {

    public static final double MATCH_THRESHOLD = 70.0;

    private int matchId;
    private int lostItemId;
    private int foundItemId;
    private double score;
    private MatchStatus status;
    private LocalDateTime createdAt;

    public Match(int lostItemId, int foundItemId, double score) {
        this(0, lostItemId, foundItemId, score, MatchStatus.PENDING, LocalDateTime.now());
    }

    public Match(int lostItemId, int foundItemId, double score, MatchStatus status, LocalDateTime createdAt) {
        this(0, lostItemId, foundItemId, score, status, createdAt);
    }

    public Match(int matchId, int lostItemId, int foundItemId, double score,
                 MatchStatus status, LocalDateTime createdAt) {
        setMatchId(matchId);
        setLostItemId(lostItemId);
        setFoundItemId(foundItemId);
        setScore(score);
        setStatus(status != null ? status : MatchStatus.PENDING);
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        if (matchId < 0) {
            throw new InvalidMatchException("Match ID cannot be negative.");
        }
        this.matchId = matchId;
    }

    public int getLostItemId() {
        return lostItemId;
    }

    public void setLostItemId(int lostItemId) {
        if (lostItemId <= 0) {
            throw new InvalidMatchException("Valid lost item ID required.");
        }
        this.lostItemId = lostItemId;
    }

    public int getFoundItemId() {
        return foundItemId;
    }

    public void setFoundItemId(int foundItemId) {
        if (foundItemId <= 0) {
            throw new InvalidMatchException("Valid found item ID required.");
        }
        this.foundItemId = foundItemId;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        if (score < 0.0 || score > 100.0) {
            throw new InvalidMatchException("Match score must be between 0.0 and 100.0.");
        }
        this.score = score;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        if (status == null) {
            throw new InvalidMatchException("Match status cannot be null.");
        }
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isPossibleMatch() {
        return this.score >= MATCH_THRESHOLD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Match match)) return false;
        return matchId == match.matchId &&
                lostItemId == match.lostItemId &&
                foundItemId == match.foundItemId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, lostItemId, foundItemId);
    }

    @Override
    public String toString() {
        return "Match{" +
                "matchId=" + matchId +
                ", lostItemId=" + lostItemId +
                ", foundItemId=" + foundItemId +
                ", score=" + score +
                ", status=" + status +
                '}';
    }
}
