package com.campuslostfound.dsa;

import com.campuslostfound.model.Match;
import com.campuslostfound.model.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatchPriorityQueue (Binary Max-Heap) Tests")
class MatchPriorityQueueTest {

    private MatchPriorityQueue pq;

    @BeforeEach
    void setUp() {
        pq = new MatchPriorityQueue();
    }

    @Test
    @DisplayName("Empty queue operations are safe and do not throw exceptions")
    void testEmptyQueueHandling() {
        assertTrue(pq.isEmpty());
        assertEquals(0, pq.size());
        assertNull(pq.peek());
        assertNull(pq.poll());
        assertEquals(Optional.empty(), pq.pollOptional());
        assertTrue(pq.getPossibleMatches().isEmpty());
        assertTrue(pq.pollAll().isEmpty());
    }

    @Test
    @DisplayName("Max-Heap ordering: Highest scoring matches are polled first")
    void testMaxHeapOrdering() {
        Match m1 = new Match(1, 10, 20, 55.0, MatchStatus.PENDING, LocalDateTime.now());
        Match m2 = new Match(2, 11, 21, 92.5, MatchStatus.PENDING, LocalDateTime.now());
        Match m3 = new Match(3, 12, 22, 74.0, MatchStatus.PENDING, LocalDateTime.now());
        Match m4 = new Match(4, 13, 23, 30.0, MatchStatus.PENDING, LocalDateTime.now());

        pq.add(m1);
        pq.add(m2);
        pq.add(m3);
        pq.add(m4);

        assertEquals(4, pq.size());
        assertFalse(pq.isEmpty());

        // Peek should be the highest (92.5) without removing it
        assertEquals(m2, pq.peek());
        assertEquals(4, pq.size());

        // Poll order must be strictly descending: 92.5 -> 74.0 -> 55.0 -> 30.0
        assertEquals(m2, pq.poll());
        assertEquals(m3, pq.poll());
        assertEquals(m1, pq.poll());
        assertEquals(m4, pq.poll());

        assertTrue(pq.isEmpty());
    }

    @Test
    @DisplayName("Equal scores resolve deterministically via secondary and tertiary tie-breakers")
    void testEqualScoresTieBreaking() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 10, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 10, 12, 0); // newer timestamp

        // Same score 80.0, but mNewer has newer timestamp
        Match mOlder = new Match(1, 10, 20, 80.0, MatchStatus.PENDING, t1);
        Match mNewer = new Match(2, 11, 21, 80.0, MatchStatus.PENDING, t2);

        pq.add(mOlder);
        pq.add(mNewer);

        // Newer timestamp should be polled first
        assertEquals(mNewer, pq.poll(), "Newer timestamp should break tie before older timestamp");
        assertEquals(mOlder, pq.poll());

        // Same score and same timestamp -> lower matchId wins
        Match mId10 = new Match(10, 10, 20, 80.0, MatchStatus.PENDING, t1);
        Match mId20 = new Match(20, 11, 21, 80.0, MatchStatus.PENDING, t1);

        pq.add(mId20);
        pq.add(mId10);

        assertEquals(mId10, pq.poll(), "Lower matchId should break tie deterministically");
        assertEquals(mId20, pq.poll());
    }

    @Test
    @DisplayName("getPossibleMatches returns only matches with score >= 70.0 in descending order")
    void testGetPossibleMatchesNonDestructive() {
        Match mHigh = new Match(1, 1, 10, 95.0, MatchStatus.PENDING, LocalDateTime.now());
        Match mThreshold = new Match(2, 2, 20, 70.0, MatchStatus.PENDING, LocalDateTime.now());
        Match mLow = new Match(3, 3, 30, 69.5, MatchStatus.PENDING, LocalDateTime.now());
        Match mVeryLow = new Match(4, 4, 40, 25.0, MatchStatus.PENDING, LocalDateTime.now());

        pq.add(mVeryLow);
        pq.add(mThreshold);
        pq.add(mHigh);
        pq.add(mLow);

        List<Match> possible = pq.getPossibleMatches();
        assertEquals(2, possible.size());
        assertEquals(mHigh, possible.get(0));
        assertEquals(mThreshold, possible.get(1));

        // Original priority queue must NOT be mutated by getPossibleMatches()
        assertEquals(4, pq.size());
        assertEquals(mHigh, pq.peek());
    }

    @Test
    @DisplayName("pollAll drains all items in descending order")
    void testPollAll() {
        pq.add(new Match(1, 1, 10, 40.0, MatchStatus.PENDING, LocalDateTime.now()));
        pq.add(new Match(2, 2, 20, 80.0, MatchStatus.PENDING, LocalDateTime.now()));
        pq.add(new Match(3, 3, 30, 60.0, MatchStatus.PENDING, LocalDateTime.now()));

        List<Match> drained = pq.pollAll();
        assertEquals(3, drained.size());
        assertEquals(80.0, drained.get(0).getScore());
        assertEquals(60.0, drained.get(1).getScore());
        assertEquals(40.0, drained.get(2).getScore());

        assertTrue(pq.isEmpty(), "Queue should be completely empty after pollAll()");
    }
}
