-- ============================================================================
-- CAMPUS LOST AND FOUND SYSTEM
-- Database Schema Definition
-- Target DBMS: MySQL 8.0+ / 9.0+
-- Database: campus_lost_found
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. DATABASE CREATION AND CONTEXT
-- ----------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS campus_lost_found
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_lost_found;

-- Disable foreign key checks during drop operations for clean recreation
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS claims;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS locations;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 2. USERS TABLE
-- Stores credentials and roles for campus students and administrators.
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints & Indexes
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. LOCATIONS TABLE
-- Normalized repository of physical campus areas and buildings.
-- Eliminates redundant text storage and update anomalies in item records.
-- ----------------------------------------------------------------------------
CREATE TABLE locations (
    location_id INT AUTO_INCREMENT PRIMARY KEY,
    location_name VARCHAR(100) NOT NULL,
    building VARCHAR(100) NULL,

    -- Constraints: Prevent duplicate location + building entries
    CONSTRAINT uk_location_building UNIQUE (location_name, building)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. ITEMS TABLE
-- Central entity recording both LOST and FOUND items reported by users.
-- ----------------------------------------------------------------------------
CREATE TABLE items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    location_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT NULL,
    item_type ENUM('LOST', 'FOUND') NOT NULL,
    item_date DATE NOT NULL,
    status ENUM('ACTIVE', 'MATCHED', 'CLAIMED', 'RECOVERED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    -- RESTRICT prevents deleting active users or locations that have item history
    CONSTRAINT fk_items_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_items_location FOREIGN KEY (location_id)
        REFERENCES locations (location_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    -- Indexes for frequent searching, filtering, and matching
    INDEX idx_items_user (user_id),
    INDEX idx_items_location (location_id),
    INDEX idx_items_type_status (item_type, status),
    INDEX idx_items_category (category),
    INDEX idx_items_date (item_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. MATCHES TABLE
-- Stores similarity match candidates computed by the matching algorithm.
-- Links exactly one LOST item and one FOUND item with a calculated score (0-100).
-- ----------------------------------------------------------------------------
CREATE TABLE matches (
    match_id INT AUTO_INCREMENT PRIMARY KEY,
    lost_item_id INT NOT NULL,
    found_item_id INT NOT NULL,
    score DECIMAL(5, 2) NOT NULL,
    status ENUM('PENDING', 'NOTIFIED', 'CONFIRMED', 'DISMISSED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    -- Score must be between 0.00 and 100.00
    CONSTRAINT chk_match_score_range CHECK (score >= 0.00 AND score <= 100.00),

    -- Prevent duplicate match records for the same lost/found item pair
    CONSTRAINT uk_match_pair UNIQUE (lost_item_id, found_item_id),

    -- Foreign Keys: If an item is purged, remove its match associations
    CONSTRAINT fk_matches_lost_item FOREIGN KEY (lost_item_id)
        REFERENCES items (item_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_matches_found_item FOREIGN KEY (found_item_id)
        REFERENCES items (item_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- Indexes for ranking and lookup performance
    INDEX idx_matches_lost (lost_item_id),
    INDEX idx_matches_found (found_item_id),
    INDEX idx_matches_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Note on Item Matching Integrity & MySQL Error 3823:
-- 1. MySQL Error 3823 disallows columns with CASCADE referential actions from being
--    used in CHECK constraints (e.g., CHECK (lost_item_id <> found_item_id)).
-- 2. Furthermore, CHECK constraints cannot contain subqueries across other rows/tables.
-- 3. Therefore, ensuring lost_item_id <> found_item_id, and ensuring lost_item_id strictly
--    points to a LOST item and found_item_id strictly points to a FOUND item, is guaranteed
--    at the Service and DAO layer in Java before inserting candidate matches.

-- ----------------------------------------------------------------------------
-- 6. CLAIMS TABLE
-- Records ownership claims made by students on reported found items.
-- Supports administrative review and approval workflow.
-- ----------------------------------------------------------------------------
CREATE TABLE claims (
    claim_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    user_id INT NOT NULL,
    claim_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    verification_details TEXT NULL,
    reviewed_by INT NULL,
    reviewed_at TIMESTAMP NULL,

    -- Foreign Keys
    CONSTRAINT fk_claims_item FOREIGN KEY (item_id)
        REFERENCES items (item_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    CONSTRAINT fk_claims_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_claims_reviewer FOREIGN KEY (reviewed_by)
        REFERENCES users (user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    -- Indexes for claim lookup and administrative queue filtering
    INDEX idx_claims_item (item_id),
    INDEX idx_claims_user (user_id),
    INDEX idx_claims_status (status),
    INDEX idx_claims_reviewer (reviewed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. NORMALIZATION SUMMARY & DESIGN JUSTIFICATION (FOR VIVA & DOCUMENTATION)
-- ----------------------------------------------------------------------------
-- 1NF (First Normal Form):
--   - Every table has a primary key (`user_id`, `location_id`, `item_id`, etc.).
--   - All attributes contain atomic (indivisible) scalar values (no CSVs or JSON arrays).
--   - There are no repeating groups of columns.
--
-- 2NF (Second Normal Form):
--   - All tables are in 1NF.
--   - Every non-key attribute is fully functionally dependent on the primary key.
--   - No partial dependencies exist because all primary keys are single-column synthetic keys.
--
-- 3NF (Third Normal Form):
--   - All tables are in 2NF.
--   - No transitive dependencies exist (non-prime attributes do not depend on other non-prime attributes).
--   - Redundancy Elimination: The `items` table does NOT store `user_name`, `email`,
--     `location_name`, or `building`. Storing them in `items` would introduce:
--       * Insertion anomaly: Cannot register a new campus location without reporting an item.
--       * Deletion anomaly: Deleting the last item in a building loses all knowledge of that location.
--       * Update anomaly: Changing a user's name or building name would require updating thousands of item rows.
--     Separating `users` and `locations` resolves all these anomalies.
-- ============================================================================
