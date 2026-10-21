package com.campuslostfound.dsa;

import com.campuslostfound.exception.InvalidMatchException;
import com.campuslostfound.model.FoundItem;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.LostItem;
import com.campuslostfound.model.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MatchAlgorithm (5-Criteria Scoring) Tests")
class MatchAlgorithmTest {

    private MatchAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new MatchAlgorithm();
    }

    @Test
    @DisplayName("Verify fixed weights constants")
    void testFixedWeights() {
        assertEquals(25.0, MatchAlgorithm.CATEGORY_WEIGHT);
        assertEquals(25.0, MatchAlgorithm.LOCATION_WEIGHT);
        assertEquals(20.0, MatchAlgorithm.DATE_WEIGHT);
        assertEquals(20.0, MatchAlgorithm.NAME_WEIGHT);
        assertEquals(10.0, MatchAlgorithm.DESCRIPTION_WEIGHT);
        assertEquals(100.0, MatchAlgorithm.MAX_SCORE);
        assertEquals(70.0, MatchAlgorithm.MATCH_THRESHOLD);
    }

    @Test
    @DisplayName("Perfect match reaches maximum score of 100.0")
    void testPerfectMatchScores100() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        Item lost = new LostItem(1, 10, 1, "Blue Hydro Flask", "Accessories",
                "32oz bottle with stickers", date);
        Item found = new FoundItem(2, 20, 1, "Blue Hydro Flask", "Accessories",
                "32oz bottle with stickers", date);

        Match match = algorithm.compare(lost, found);
        assertEquals(100.0, match.getScore(), 0.01);
        assertTrue(match.isPossibleMatch());
    }

    @Test
    @DisplayName("Completely disjoint items score 0.0")
    void testCompletelyDisjointItemsScoreZero() {
        Item lost = new LostItem(1, 10, 1, "Graphing Calculator TI-84", "Electronics",
                "Black plastic casing with keyboard", LocalDate.of(2026, 1, 1));
        Item found = new FoundItem(2, 20, 2, "Woolen Winter Scarf", "Clothing",
                "Red knit scarf with fringes", LocalDate.of(2026, 6, 1));

        Match match = algorithm.compare(lost, found);
        assertEquals(0.0, match.getScore(), 0.01);
        assertFalse(match.isPossibleMatch());
    }

    @Test
    @DisplayName("Category weight calculation: 25 points for match, 0 for mismatch")
    void testCategoryScoring() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        Item lost = new LostItem(1, 10, 1, "Water Bottle", "Accessories", "Bottle", date);
        Item foundSameCat = new FoundItem(2, 20, 2, "Umbrella", "accessories", "Rain umbrella", date);
        Item foundDiffCat = new FoundItem(3, 20, 2, "Umbrella", "Clothing", "Rain umbrella", date);

        assertEquals(25.0, algorithm.calculateCategoryScore(lost, foundSameCat));
        assertEquals(0.0, algorithm.calculateCategoryScore(lost, foundDiffCat));
    }

    @Test
    @DisplayName("Location weight calculation: 25 points for match, 0 for mismatch")
    void testLocationScoring() {
        LocalDate date = LocalDate.of(2026, 10, 15);
        Item lost = new LostItem(1, 10, 5, "Keys", "Keys", "Dorm keys", date);
        Item foundSameLoc = new FoundItem(2, 20, 5, "Keychain", "Keys", "Set of keys", date);
        Item foundDiffLoc = new FoundItem(3, 20, 6, "Keychain", "Keys", "Set of keys", date);

        assertEquals(25.0, algorithm.calculateLocationScore(lost, foundSameLoc));
        assertEquals(0.0, algorithm.calculateLocationScore(lost, foundDiffLoc));
    }

    @Test
    @DisplayName("Date proximity scoring decay: 20 pts (0 days) down to 0 pts (>14 days)")
    void testDateScoringDecay() {
        LocalDate baseDate = LocalDate.of(2026, 10, 10);
        Item lost = new LostItem(1, 10, 1, "Item", "Cat", "Desc", baseDate);

        // Same day (0 days) -> 20.0
        Item found0Days = new FoundItem(2, 20, 1, "Item", "Cat", "Desc", baseDate);
        assertEquals(20.0, algorithm.calculateDateScore(lost, found0Days));

        // 1 to 2 days -> 16.0
        Item found2Days = new FoundItem(3, 20, 1, "Item", "Cat", "Desc", baseDate.plusDays(2));
        assertEquals(16.0, algorithm.calculateDateScore(lost, found2Days));

        // 3 to 5 days -> 12.0
        Item found4Days = new FoundItem(4, 20, 1, "Item", "Cat", "Desc", baseDate.plusDays(4));
        assertEquals(12.0, algorithm.calculateDateScore(lost, found4Days));

        // 6 to 7 days -> 8.0
        Item found7Days = new FoundItem(5, 20, 1, "Item", "Cat", "Desc", baseDate.plusDays(7));
        assertEquals(8.0, algorithm.calculateDateScore(lost, found7Days));

        // 8 to 14 days -> 4.0
        Item found10Days = new FoundItem(6, 20, 1, "Item", "Cat", "Desc", baseDate.plusDays(10));
        assertEquals(4.0, algorithm.calculateDateScore(lost, found10Days));

        // > 14 days -> 0.0
        Item found30Days = new FoundItem(7, 20, 1, "Item", "Cat", "Desc", baseDate.plusDays(30));
        assertEquals(0.0, algorithm.calculateDateScore(lost, found30Days));
    }

    @Test
    @DisplayName("Text token Jaccard similarity and partial description handling")
    void testTextSimilarityAndNullHandling() {
        Item lost = new LostItem(1, 10, 1, "Apple MacBook Pro Charger", "Electronics", null, LocalDate.now());
        Item found = new FoundItem(2, 20, 1, "Apple Charger 85W MacBook", "Electronics", "", LocalDate.now());

        // Name shares "apple", "macbook", "charger"
        double nameScore = algorithm.calculateNameScore(lost, found);
        assertTrue(nameScore > 10.0, "Substantial token overlap should give high name score");

        // Null/empty descriptions safely return 0.0 without NullPointerException
        assertEquals(0.0, algorithm.calculateDescriptionScore(lost, found));

        // Both populated descriptions with partial overlap
        lost.setDescription("White rectangular power brick with MagSafe 3 cable");
        found.setDescription("White MagSafe charger found in study room");
        double descScore = algorithm.calculateDescriptionScore(lost, found);
        assertTrue(descScore > 0.0 && descScore <= 10.0);
    }

    @Test
    @DisplayName("Realistic possible match exceeds 70.0 threshold")
    void testPossibleMatchThreshold() {
        LocalDate date = LocalDate.of(2026, 10, 12);
        // Lost Dell charger in Auditorium
        Item lost = new LostItem(1, 10, 3, "Dell 65W USB-C Laptop Charger", "Electronics",
                "Black oval AC adapter with USB-C connector", date);

        // Found Dell adapter in Auditorium, same day
        Item found = new FoundItem(2, 20, 3, "Dell Laptop Power Adapter", "Electronics",
                "Black Dell USB-C adapter found plugged into socket", date);

        Match match = algorithm.compare(lost, found);
        // Category match: 25
        // Location match: 25
        // Date match: 20
        // Name overlap: ~10
        // Desc overlap: ~3
        // Total: ~83
        assertTrue(match.getScore() >= 70.0, "Expected score >= 70, got: " + match.getScore());
        assertTrue(match.isPossibleMatch());
    }

    @Test
    @DisplayName("Comparison symmetry: compare(lost, found) equals compare(found, lost)")
    void testComparisonSymmetry() {
        LocalDate date = LocalDate.of(2026, 10, 12);
        Item lost = new LostItem(1, 10, 3, "Dell 65W Laptop Charger", "Electronics", "Black charger", date);
        Item found = new FoundItem(2, 20, 3, "Dell Laptop Adapter", "Electronics", "Black power unit", date);

        Match m1 = algorithm.compare(lost, found);
        Match m2 = algorithm.compare(found, lost);

        assertEquals(m1.getScore(), m2.getScore(), 0.01);
        assertEquals(m1.getLostItemId(), m2.getLostItemId());
        assertEquals(m1.getFoundItemId(), m2.getFoundItemId());
    }

    @Test
    @DisplayName("Rejects comparisons between same items and items of identical type")
    void testInvalidComparisons() {
        LocalDate date = LocalDate.now();
        Item lost1 = new LostItem(1, 10, 1, "Keys", "Keys", "Keys", date);
        Item lost2 = new LostItem(2, 11, 1, "Bag", "Bags", "Bag", date);
        Item found1 = new FoundItem(3, 20, 1, "Keys", "Keys", "Keys", date);
        Item sameAsLost1 = new FoundItem(1, 20, 1, "Keys", "Keys", "Keys", date);

        // Rule 1: Same item ID comparison
        assertThrows(InvalidMatchException.class, () -> algorithm.compare(lost1, sameAsLost1));

        // Rule 2: Comparing two LOST items
        assertThrows(InvalidMatchException.class, () -> algorithm.compare(lost1, lost2));

        // Rule 3: Null item comparison
        assertThrows(InvalidMatchException.class, () -> algorithm.compare(lost1, null));
        assertThrows(InvalidMatchException.class, () -> algorithm.compare(null, found1));
    }
}
