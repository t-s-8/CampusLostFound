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

    public List<Match> findAndRecordMatchesForItem(Item item) {
        if (item == null || item.getItemId() <= 0) {
            return new ArrayList<>();
        }

        ItemType candidateType = item.getItemType() == ItemType.LOST ? ItemType.FOUND : ItemType.LOST;
        List<Item> candidates = itemDAO.findByTypeAndStatus(candidateType, ItemStatus.ACTIVE);

        MatchPriorityQueue pq = new MatchPriorityQueue();

        for (Item candidate : candidates) {
            if (candidate.getItemId() != item.getItemId()) {
                try {
                    Match match = matchAlgorithm.compare(item, candidate);
                    pq.add(match);

                    if (match.isPossibleMatch()) {
                        matchDAO.create(match);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return pq.pollAll();
    }

    public List<Match> getMatchesForLostItem(int lostItemId) {
        return matchDAO.findByLostItemId(lostItemId);
    }

    public List<Match> getMatchesForFoundItem(int foundItemId) {
        return matchDAO.findByFoundItemId(foundItemId);
    }

    public boolean updateMatchStatus(int matchId, MatchStatus status) {
        return matchDAO.updateStatus(matchId, status);
    }

    public List<Match> getAllMatches() {
        return matchDAO.findAll();
    }
}
