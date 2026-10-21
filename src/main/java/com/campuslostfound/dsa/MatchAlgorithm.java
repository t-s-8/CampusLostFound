package com.campuslostfound.dsa;

import com.campuslostfound.exception.InvalidMatchException;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemType;
import com.campuslostfound.model.Match;
import com.campuslostfound.model.MatchStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Deterministic, local scoring algorithm for evaluating similarity between LOST and FOUND items.
 *
 * Fixed Weights:
 * - Category: 25 points
 * - Location: 25 points
 * - Date proximity: 20 points
 * - Name similarity: 20 points
 * - Description similarity: 10 points
 *
 * Maximum Score: 100.0 points.
 * Possible Match Threshold: >= 70.0 points.
 *
 * NOTE: A possible match score (>= 70) NEVER automatically marks an item as recovered.
 * Recovery requires administrative verification and the appropriate claim workflow.
 */
public class MatchAlgorithm {

    public static final double CATEGORY_WEIGHT = 25.0;
    public static final double LOCATION_WEIGHT = 25.0;
    public static final double DATE_WEIGHT = 20.0;
    public static final double NAME_WEIGHT = 20.0;
    public static final double DESCRIPTION_WEIGHT = 10.0;

    public static final double MAX_SCORE = 100.0;
    public static final double MATCH_THRESHOLD = 70.0;

    /**
     * Compares a LOST item with a FOUND item candidate, calculating a similarity score from 0 to 100.
     *
     * @param item1 First item (either LOST or FOUND)
     * @param item2 Second item (must be opposing type)
     * @return Generated Match object containing the calculated score and PENDING status
     * @throws InvalidMatchException if items are null, identical, or share the same ItemType
     */
    public Match compare(Item item1, Item item2) {
        if (item1 == null || item2 == null) {
            throw new InvalidMatchException("Both items must be non-null for comparison.");
        }

        // Rule 1: Reject comparisons between the same item
        if (item1.getItemId() > 0 && item1.getItemId() == item2.getItemId()) {
            throw new InvalidMatchException("Cannot compare an item with itself (ID: " + item1.getItemId() + ").");
        }

        // Rule 2: Reject comparisons between items of the same type (e.g. LOST with LOST, or FOUND with FOUND)
        if (item1.getItemType() == item2.getItemType()) {
            throw new InvalidMatchException("Invalid comparison: Both items have type " + item1.getItemType() +
                    ". A match must connect exactly one LOST item and one FOUND item.");
        }

        // Identify lost vs found item
        Item lostItem = item1.getItemType() == ItemType.LOST ? item1 : item2;
        Item foundItem = item1.getItemType() == ItemType.FOUND ? item1 : item2;

        double categoryScore = calculateCategoryScore(lostItem, foundItem);
        double locationScore = calculateLocationScore(lostItem, foundItem);
        double dateScore = calculateDateScore(lostItem, foundItem);
        double nameScore = calculateNameScore(lostItem, foundItem);
        double descriptionScore = calculateDescriptionScore(lostItem, foundItem);

        double totalScore = categoryScore + locationScore + dateScore + nameScore + descriptionScore;
        totalScore = Math.min(MAX_SCORE, Math.max(0.0, totalScore));
        totalScore = Math.round(totalScore * 100.0) / 100.0; // round to 2 decimal places

        return new Match(0, lostItem.getItemId(), foundItem.getItemId(), totalScore,
                MatchStatus.PENDING, LocalDateTime.now());
    }

    /**
     * Category similarity: 25 points if exact match (case-insensitive), else 0.
     */
    public double calculateCategoryScore(Item lost, Item found) {
        if (lost.getCategory() != null && found.getCategory() != null) {
            if (lost.getCategory().trim().equalsIgnoreCase(found.getCategory().trim())) {
                return CATEGORY_WEIGHT;
            }
        }
        return 0.0;
    }

    /**
     * Location similarity: 25 points if same location ID, else 0.
     */
    public double calculateLocationScore(Item lost, Item found) {
        if (lost.getLocationId() > 0 && lost.getLocationId() == found.getLocationId()) {
            return LOCATION_WEIGHT;
        }
        return 0.0;
    }

    /**
     * Date proximity scoring: 20 points max, decaying with the time difference.
     * - Same day (0 days diff): 20 pts
     * - 1 to 2 days: 16 pts
     * - 3 to 5 days: 12 pts
     * - 6 to 7 days: 8 pts
     * - 8 to 14 days: 4 pts
     * - > 14 days: 0 pts
     */
    public double calculateDateScore(Item lost, Item found) {
        if (lost.getItemDate() == null || found.getItemDate() == null) {
            return 0.0;
        }
        long daysDiff = Math.abs(ChronoUnit.DAYS.between(lost.getItemDate(), found.getItemDate()));
        if (daysDiff == 0) {
            return DATE_WEIGHT;
        } else if (daysDiff <= 2) {
            return 16.0;
        } else if (daysDiff <= 5) {
            return 12.0;
        } else if (daysDiff <= 7) {
            return 8.0;
        } else if (daysDiff <= 14) {
            return 4.0;
        } else {
            return 0.0;
        }
    }

    /**
     * Name similarity: 20 points max based on tokenized Jaccard similarity.
     */
    public double calculateNameScore(Item lost, Item found) {
        double similarity = calculateTokenJaccardSimilarity(lost.getName(), found.getName());
        return Math.round((similarity * NAME_WEIGHT) * 100.0) / 100.0;
    }

    /**
     * Description similarity: 10 points max based on tokenized Jaccard similarity.
     */
    public double calculateDescriptionScore(Item lost, Item found) {
        if (lost.getDescription() == null || found.getDescription() == null) {
            return 0.0;
        }
        double similarity = calculateTokenJaccardSimilarity(lost.getDescription(), found.getDescription());
        return Math.round((similarity * DESCRIPTION_WEIGHT) * 100.0) / 100.0;
    }

    /**
     * Deterministic, local text similarity based on tokenized Jaccard coefficient:
     * J(A, B) = |A ∩ B| / |A ∪ B|
     *
     * Time Complexity: O(L1 + L2) where L1, L2 are string lengths.
     * Space Complexity: O(W1 + W2) for word sets.
     */
    public static double calculateTokenJaccardSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null) {
            return 0.0;
        }

        Set<String> tokens1 = tokenize(text1);
        Set<String> tokens2 = tokenize(text2);

        if (tokens1.isEmpty() && tokens2.isEmpty()) {
            return 0.0;
        }
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(tokens1);
        intersection.retainAll(tokens2);

        Set<String> union = new HashSet<>(tokens1);
        union.addAll(tokens2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "in", "on", "at", "to", "for",
            "with", "by", "of", "from", "is", "it", "this", "that"
    );

    private static Set<String> tokenize(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        // Normalize: lowercase and strip non-alphanumeric punctuation
        String cleaned = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] words = cleaned.split("\\s+");
        Set<String> set = new HashSet<>();
        for (String word : words) {
            String trimmed = word.trim();
            if (!trimmed.isEmpty() && !STOP_WORDS.contains(trimmed)) {
                set.add(trimmed);
            }
        }
        return set;
    }
}
