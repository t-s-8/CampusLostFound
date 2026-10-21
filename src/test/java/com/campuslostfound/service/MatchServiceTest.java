package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dao.MatchDAO;
import com.campuslostfound.dsa.MatchAlgorithm;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatchService Unit Tests")
class MatchServiceTest {

    private MatchService matchService;
    private InMemoryItemDAO itemDAO;
    private InMemoryMatchDAO matchDAO;

    private static class InMemoryItemDAO extends ItemDAO {
        private final List<Item> items = new ArrayList<>();

        @Override
        public List<Item> findByTypeAndStatus(ItemType type, ItemStatus status) {
            List<Item> res = new ArrayList<>();
            for (Item item : items) {
                if (item.getItemType() == type && item.getStatus() == status) {
                    res.add(item);
                }
            }
            return res;
        }

        void add(Item item) {
            items.add(item);
        }
    }

    private static class InMemoryMatchDAO extends MatchDAO {
        private final Map<Integer, Match> map = new HashMap<>();
        private int sequence = 1;

        @Override
        public Match create(Match match) {
            match.setMatchId(sequence++);
            map.put(match.getMatchId(), match);
            return match;
        }

        @Override
        public List<Match> findByLostItemId(int lostItemId) {
            List<Match> res = new ArrayList<>();
            for (Match m : map.values()) {
                if (m.getLostItemId() == lostItemId) {
                    res.add(m);
                }
            }
            return res;
        }

        int getCount() {
            return map.size();
        }
    }

    @BeforeEach
    void setUp() {
        itemDAO = new InMemoryItemDAO();
        matchDAO = new InMemoryMatchDAO();
        matchService = new MatchService(matchDAO, itemDAO, new MatchAlgorithm());
    }

    @Test
    @DisplayName("Matching new LostItem against active Found candidates ranks and persists possible matches")
    void testFindAndRecordMatchesRanksAndPersists() {
        LocalDate date = LocalDate.of(2026, 10, 10);

        // Found candidates in database
        Item foundSimilar = new FoundItem(1, 10, 1, "Blue Hydro Flask Bottle", "Accessories", "32oz blue bottle", date);
        Item foundDifferent = new FoundItem(2, 20, 5, "Winter Scarf", "Clothing", "Red woolen scarf", date);
        itemDAO.add(foundSimilar);
        itemDAO.add(foundDifferent);

        // Newly reported lost item
        Item newLostItem = new LostItem(100, 30, 1, "Blue Hydro Flask", "Accessories", "32oz bottle", date);

        List<Match> ranked = matchService.findAndRecordMatchesForItem(newLostItem);
        assertEquals(2, ranked.size());

        // Max-Heap priority queue should place foundSimilar first (Score near 100)
        Match topMatch = ranked.get(0);
        assertEquals(1, topMatch.getFoundItemId());
        assertTrue(topMatch.getScore() >= 70.0, "Score should exceed 70.0 possible match threshold");

        // Possible match (score >= 70) must have been persisted to matchDAO
        assertTrue(matchDAO.getCount() >= 1);
    }
}
