-- ============================================================================
-- CAMPUS LOST AND FOUND SYSTEM
-- Demonstration & Application SQL Queries
-- Target Database: campus_lost_found
--
-- This script contains:
-- Part 1: Required SQL Clause Demonstrations (CRUD, JOINs, Aggregates, Subqueries)
-- Part 2: Core Application Workflow Queries (Reports, Claims, Analytics)
-- ============================================================================

USE campus_lost_found;

-- ============================================================================
-- PART 1: REQUIRED SQL LANGUAGE CONSTRUCTS DEMONSTRATION
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. INSERT DEMONSTRATION
-- Registers a new lost item reported by a student.
-- ----------------------------------------------------------------------------
INSERT INTO items (user_id, location_id, name, category, description, item_type, item_date, status)
VALUES (3, 2, 'Graphing Calculator TI-84', 'Electronics', 'Black TI-84 Plus with battery cover tape', 'LOST', '2026-10-15', 'ACTIVE');

-- ----------------------------------------------------------------------------
-- 2. SELECT & WHERE DEMONSTRATION
-- Retrieves all electronics reported lost within the last 7 days.
-- ----------------------------------------------------------------------------
SELECT item_id, name, category, item_date, status
FROM items
WHERE item_type = 'LOST'
  AND category = 'Electronics'
  AND item_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY);

-- ----------------------------------------------------------------------------
-- 3. ORDER BY DEMONSTRATION
-- Retrieves all found items ordered from newest report to oldest.
-- ----------------------------------------------------------------------------
SELECT item_id, name, category, item_date, created_at
FROM items
WHERE item_type = 'FOUND'
ORDER BY item_date DESC, created_at DESC;

-- ----------------------------------------------------------------------------
-- 4. UPDATE DEMONSTRATION
-- Updates the status of an item after administrative review.
-- ----------------------------------------------------------------------------
UPDATE items
SET status = 'MATCHED'
WHERE item_id = 2;

-- ----------------------------------------------------------------------------
-- 5. DELETE DEMONSTRATION
-- Deletes dismissed match records older than 30 days (cleanup query).
-- ----------------------------------------------------------------------------
DELETE FROM matches
WHERE status = 'DISMISSED'
  AND created_at < DATE_SUB(NOW(), INTERVAL 30 DAY);

-- ----------------------------------------------------------------------------
-- 6. INNER JOIN DEMONSTRATION
-- Retrieves items along with the reporter's name and location details.
-- ----------------------------------------------------------------------------
SELECT
    i.item_id,
    i.name AS item_name,
    i.category,
    i.item_type,
    i.status,
    u.name AS reported_by,
    u.email AS contact_email,
    l.location_name,
    l.building
FROM items i
INNER JOIN users u ON i.user_id = u.user_id
INNER JOIN locations l ON i.location_id = l.location_id
ORDER BY i.item_id;

-- ----------------------------------------------------------------------------
-- 7. LEFT JOIN DEMONSTRATION
-- Lists all campus locations and any items reported there (including locations with 0 items).
-- ----------------------------------------------------------------------------
SELECT
    l.location_id,
    l.location_name,
    l.building,
    COUNT(i.item_id) AS total_items_reported
FROM locations l
LEFT JOIN items i ON l.location_id = i.location_id
GROUP BY l.location_id, l.location_name, l.building
ORDER BY total_items_reported DESC;

-- ----------------------------------------------------------------------------
-- 8. AGGREGATES: COUNT, AVG, MIN, MAX
-- Calculates overall matching statistics across candidate matches.
-- ----------------------------------------------------------------------------
SELECT
    COUNT(*) AS total_matches,
    AVG(score) AS average_match_score,
    MIN(score) AS lowest_match_score,
    MAX(score) AS highest_match_score
FROM matches;

-- ----------------------------------------------------------------------------
-- 9. GROUP BY & HAVING DEMONSTRATION
-- Identifies categories having more than 1 active item report.
-- ----------------------------------------------------------------------------
SELECT
    category,
    item_type,
    COUNT(*) AS active_item_count
FROM items
WHERE status = 'ACTIVE'
GROUP BY category, item_type
HAVING COUNT(*) > 1
ORDER BY active_item_count DESC;

-- ----------------------------------------------------------------------------
-- 10. SUBQUERIES DEMONSTRATION
-- Subquery 1: IN subquery to find all students who have submitted claims.
-- ----------------------------------------------------------------------------
SELECT user_id, name, email
FROM users
WHERE user_id IN (
    SELECT DISTINCT user_id
    FROM claims
);

-- Subquery 2: Scalar subquery to find match pairs with above-average match scores.
SELECT m.match_id, m.lost_item_id, m.found_item_id, m.score
FROM matches m
WHERE m.score > (
    SELECT AVG(score) FROM matches
);

-- Subquery 3: Correlated EXISTS subquery to find locations having active lost item reports.
SELECT l.location_id, l.location_name, l.building
FROM locations l
WHERE EXISTS (
    SELECT 1
    FROM items i
    WHERE i.location_id = l.location_id
      AND i.item_type = 'LOST'
      AND i.status = 'ACTIVE'
);


-- ============================================================================
-- PART 2: CORE APPLICATION USE-CASE QUERIES
-- ============================================================================

-- ----------------------------------------------------------------------------
-- App Query 1: Active Lost Reports with Reporter & Location Details
-- Used by: Student search view and Admin lost item dashboard.
-- ----------------------------------------------------------------------------
SELECT
    i.item_id,
    i.name AS lost_item,
    i.category,
    i.description,
    i.item_date AS date_lost,
    l.location_name,
    l.building,
    u.name AS student_name,
    u.email AS student_email
FROM items i
JOIN users u ON i.user_id = u.user_id
JOIN locations l ON i.location_id = l.location_id
WHERE i.item_type = 'LOST'
  AND i.status = 'ACTIVE'
ORDER BY i.item_date DESC;

-- ----------------------------------------------------------------------------
-- App Query 2: Active Found Reports
-- Used by: Students searching for their lost belongings.
-- ----------------------------------------------------------------------------
SELECT
    i.item_id,
    i.name AS found_item,
    i.category,
    i.description,
    i.item_date AS date_found,
    l.location_name,
    l.building,
    i.status
FROM items i
JOIN locations l ON i.location_id = l.location_id
WHERE i.item_type = 'FOUND'
  AND i.status = 'ACTIVE'
ORDER BY i.item_date DESC;

-- ----------------------------------------------------------------------------
-- App Query 3: Item Reports Summarized by Category
-- Used by: Analytics and breakdown panels.
-- ----------------------------------------------------------------------------
SELECT
    category,
    SUM(CASE WHEN item_type = 'LOST' THEN 1 ELSE 0 END) AS lost_count,
    SUM(CASE WHEN item_type = 'FOUND' THEN 1 ELSE 0 END) AS found_count,
    COUNT(*) AS total_reports
FROM items
GROUP BY category
ORDER BY total_reports DESC;

-- ----------------------------------------------------------------------------
-- App Query 4: Item Reports Summarized by Location (Campus Hotspots)
-- Used by: Campus safety & admin reporting.
-- ----------------------------------------------------------------------------
SELECT
    l.location_name,
    l.building,
    COUNT(i.item_id) AS total_incidents,
    SUM(CASE WHEN i.item_type = 'LOST' THEN 1 ELSE 0 END) AS lost_items,
    SUM(CASE WHEN i.item_type = 'FOUND' THEN 1 ELSE 0 END) AS found_items
FROM locations l
JOIN items i ON l.location_id = i.location_id
GROUP BY l.location_id, l.location_name, l.building
ORDER BY total_incidents DESC;

-- ----------------------------------------------------------------------------
-- App Query 5: Claims Awaiting Administrative Review
-- Used by: Admin claims management screen.
-- ----------------------------------------------------------------------------
SELECT
    c.claim_id,
    c.claim_date,
    c.status AS claim_status,
    c.verification_details,
    u.name AS claimant_name,
    u.email AS claimant_email,
    i.item_id,
    i.name AS item_name,
    i.category,
    i.item_date AS found_date,
    l.location_name
FROM claims c
JOIN users u ON c.user_id = u.user_id
JOIN items i ON c.item_id = i.item_id
JOIN locations l ON i.location_id = l.location_id
WHERE c.status = 'PENDING'
ORDER BY c.claim_date ASC;

-- ----------------------------------------------------------------------------
-- App Query 6: Total Recovered Items Count & Recovery Rate
-- Used by: Executive summary / stats cards on admin dashboard.
-- ----------------------------------------------------------------------------
SELECT
    COUNT(*) AS total_items,
    SUM(CASE WHEN status = 'RECOVERED' THEN 1 ELSE 0 END) AS recovered_items,
    ROUND(
        (SUM(CASE WHEN status = 'RECOVERED' THEN 1 ELSE 0 END) * 100.0) / COUNT(*),
        2
    ) AS recovery_rate_percentage
FROM items;

-- ----------------------------------------------------------------------------
-- App Query 7: Detailed Match Pair Inspection
-- Used by: Match review screen to display side-by-side details of potential matches.
-- ----------------------------------------------------------------------------
SELECT
    m.match_id,
    m.score,
    m.status AS match_status,
    m.created_at AS matched_at,
    -- Lost item details
    lost.item_id AS lost_id,
    lost.name AS lost_item_name,
    lost.category AS lost_category,
    lost_loc.location_name AS lost_location,
    lost_user.name AS lost_owner,
    -- Found item details
    found.item_id AS found_id,
    found.name AS found_item_name,
    found.category AS found_category,
    found_loc.location_name AS found_location,
    found_user.name AS found_finder
FROM matches m
JOIN items lost ON m.lost_item_id = lost.item_id
JOIN locations lost_loc ON lost.location_id = lost_loc.location_id
JOIN users lost_user ON lost.user_id = lost_user.user_id
JOIN items found ON m.found_item_id = found.item_id
JOIN locations found_loc ON found.location_id = found_loc.location_id
JOIN users found_user ON found.user_id = found_user.user_id
ORDER BY m.score DESC;
