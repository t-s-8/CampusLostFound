package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.MatchDAO;
import com.campuslostfound.dsa.MatchAlgorithm;
import com.campuslostfound.dsa.MatchPriorityQueue;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.Match;
import com.campuslostfound.model.MatchStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Service coordinating candidate matching, algorithm execution, ranking, and persistence.
 *
 * Architecture Integration:
 * - Fetches candidate reports from ItemDAO.
 * - Computes similarity scores via MatchAlgorithm.
 * - Ranks matches via MatchPriorityQueue (Max-Heap).
 * - Persists high-confidence matches via MatchDAO.
 */
public class MatchService {

    private final MatchDAO matchDAO;
    private final ItemDAO itemDAO;
    private final MatchAlgorithm matchAlgorithm;

    public MatchService() {
        this(new MatchDAO(), new ItemDAO(), new MatchAlgorithm());
    }

    public MatchService(MatchDAO matchDAO, ItemDAO itemDAO, MatchAlgorithm matchAlgorithm) {
        this.matchDAO = matchDAO;
        this.itemDAO = itemDAO;
        this.matchAlgorithm = matchAlgorithm;
    }

    /**
     * Finds and scores candidate items for a newly created or inspected report.
     * Enqueues candidates into MatchPriorityQueue and returns all matches ranked by score.
     * Persists possible matches (score >= 70.0) into the database.
     */
    public List<Match> findAndRecordMatchesForItem(Item item) {
        if (item == null || item.getItemId() <= 0) {
            return new ArrayList<>();
        }

        // Determine opposite item type to compare against
        ItemType candidateType = item.getItemType() == ItemType.LOST ? ItemType.FOUND : ItemType.LOST;
        List<Item> candidates = itemDAO.findByTypeAndStatus(candidateType, ItemStatus.ACTIVE);

        MatchPriorityQueue pq = new MatchPriorityQueue();

        for (Item candidate : candidates) {
            if (candidate.getItemId() != item.getItemId()) {
                try {
                    Match match = matchAlgorithm.compare(item, candidate);
                    pq.add(match);

                    // Persist possible matches (score >= 70) to database
                    if (match.isPossibleMatch()) {
                        matchDAO.create(match);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Return ranked list (highest scoring first)
        return pq.pollAll();
    }

    /**
     * Retrieves all recorded matches for a given lost item.
     */
    public List<Match> getMatchesForLostItem(int lostItemId) {
        return matchDAO.findByLostItemId(lostItemId);
    }

    /**
     * Retrieves all recorded matches for a given found item.
     */
    public List<Match> getMatchesForFoundItem(int foundItemId) {
        return matchDAO.findByFoundItemId(foundItemId);
    }

    /**
     * Updates the review status of a match.
     */
    public boolean updateMatchStatus(int matchId, MatchStatus status) {
        return matchDAO.updateStatus(matchId, status);
    }

    /**
     * Retrieves all recorded matches across the system.
     */
    public List<Match> getAllMatches() {
        return matchDAO.findAll();
    }
}
