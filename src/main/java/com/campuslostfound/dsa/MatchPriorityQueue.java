package com.campuslostfound.dsa;

import com.campuslostfound.model.Match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

public class MatchPriorityQueue {

    public static final Comparator<Match> MATCH_COMPARATOR = (m1, m2) -> {
        if (m1 == null && m2 == null) return 0;
        if (m1 == null) return 1;
        if (m2 == null) return -1;

        int scoreComparison = Double.compare(m2.getScore(), m1.getScore());
        if (scoreComparison != 0) {
            return scoreComparison;
        }

        if (m1.getCreatedAt() != null && m2.getCreatedAt() != null) {
            int timeComparison = m2.getCreatedAt().compareTo(m1.getCreatedAt());
            if (timeComparison != 0) {
                return timeComparison;
            }
        }

        return Integer.compare(m1.getMatchId(), m2.getMatchId());
    };

    private final PriorityQueue<Match> queue;

    public MatchPriorityQueue() {
        this.queue = new PriorityQueue<>(MATCH_COMPARATOR);
    }

    public MatchPriorityQueue(int initialCapacity) {
        this.queue = new PriorityQueue<>(initialCapacity, MATCH_COMPARATOR);
    }

    public void add(Match match) {
        if (match != null) {
            queue.offer(match);
        }
    }

    public void addAll(Collection<Match> matches) {
        if (matches != null) {
            for (Match match : matches) {
                add(match);
            }
        }
    }

    public Match poll() {
        return queue.poll();
    }

    public Optional<Match> pollOptional() {
        return Optional.ofNullable(queue.poll());
    }

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

    public List<Match> pollAll() {
        List<Match> ordered = new ArrayList<>(queue.size());
        while (!queue.isEmpty()) {
            ordered.add(queue.poll());
        }
        return ordered;
    }

    public List<Match> getPossibleMatches() {
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
