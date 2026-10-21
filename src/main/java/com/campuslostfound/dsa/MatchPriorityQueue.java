package com.campuslostfound.dsa;

import com.campuslostfound.model.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * Priority queue wrapper utilizing a binary max-heap to serve the highest-scoring
 * item matches first.
 *
 * DSA Concepts Demonstrated:
 * 1. PriorityQueue (Heap): Implemented internally using a binary heap array representation.
 * 2. Custom Comparator with Deterministic Tie-Breaking:
 *    - Primary sorting: Match score descending (highest score first).
 *    - Secondary sorting (tie-breaker): Creation timestamp descending (newest first).
 *    - Tertiary sorting (tie-breaker): Unique matchId ascending.
 * 3. Time Complexities:
 *    - Insert (add/offer): O(log N) due to heap sift-up.
 *    - Remove maximum (poll): O(log N) due to heap sift-down.
 *    - Peek maximum: O(1).
 */
public class MatchPriorityQueue {

    /**
     * Comparator enforcing Max-Heap order with deterministic tie-breaking.
     */
    public static final Comparator<Match> MATCH_COMPARATOR = (m1, m2) -> {
        if (m1 == null && m2 == null) return 0;
        if (m1 == null) return 1;
        if (m2 == null) return -1;

        // Primary: Score descending (higher score comes first)
        int scoreComparison = Double.compare(m2.getScore(), m1.getScore());
        if (scoreComparison != 0) {
            return scoreComparison;
        }

        // Secondary tie-breaker: createdAt descending (newer matches come first)
        if (m1.getCreatedAt() != null && m2.getCreatedAt() != null) {
            int timeComparison = m2.getCreatedAt().compareTo(m1.getCreatedAt());
            if (timeComparison != 0) {
                return timeComparison;
            }
        }

        // Tertiary tie-breaker: matchId ascending for strict determinism
        return Integer.compare(m1.getMatchId(), m2.getMatchId());
    };

    private final PriorityQueue<Match> queue;

    public MatchPriorityQueue() {
        this.queue = new PriorityQueue<>(MATCH_COMPARATOR);
    }

    public MatchPriorityQueue(int initialCapacity) {
        this.queue = new PriorityQueue<>(initialCapacity, MATCH_COMPARATOR);
    }

    /**
     * Inserts a match into the priority queue.
     * Time Complexity: O(log N).
     */
    public void add(Match match) {
        if (match != null) {
            queue.offer(match);
        }
    }

    /**
     * Inserts multiple matches into the priority queue.
     */
    public void addAll(Collection<Match> matches) {
        if (matches != null) {
            for (Match match : matches) {
                add(match);
            }
        }
    }

    /**
     * Retrieves and removes the highest-scoring match, or returns null if empty.
     * Time Complexity: O(log N).
     */
    public Match poll() {
        return queue.poll();
    }

    /**
     * Null-safe Optional retrieval of the highest-scoring match.
     */
    public Optional<Match> pollOptional() {
        return Optional.ofNullable(queue.poll());
    }

    /**
     * Inspects the highest-scoring match without removing it, or returns null if empty.
     * Time Complexity: O(1).
     */
    public Match peek() {
        return queue.peek();
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
    }

    /**
     * Drains and returns all matches in descending score order.
     * Time Complexity: O(N log N).
     */
    public List<Match> pollAll() {
        List<Match> ordered = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }

    /**
     * Returns a snapshot list of matches meeting or exceeding the possible match threshold (>= 70.0)
     * in strictly descending order without destroying the original queue.
     */
    public List<Match> getPossibleMatches() {
        // Create a copy of the priority queue to avoid mutating original state
        PriorityQueue<Match> copy = new PriorityQueue<>(queue);
        List<Match> results = new ArrayList<>();
        while (!copy.isEmpty()) {
            Match m = copy.poll();
            if (m.isPossibleMatch()) {
                results.add(m);
            }
        }
        return results;
    }
}
