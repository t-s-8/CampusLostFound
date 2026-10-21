-- ============================================================================
-- CAMPUS LOST AND FOUND SYSTEM
-- Seed Data Script
-- Target Database: campus_lost_found
--
-- EXECUTION NOTICE:
-- Safe for repeated development use.
-- To perform a clean reset, this script truncates existing tables in proper
-- dependency order before inserting consistent, realistic sample records.
--
-- SECURITY NOTE:
-- All password_hash values use standard SHA-256 digests generated via SHA2().
-- These credentials are for LOCAL DEVELOPMENT AND VIVA DEMONSTRATION ONLY.
-- Development Password for all sample accounts: "DevPass@123"
-- Production admin and user accounts must NEVER use hardcoded or sample passwords.
-- ============================================================================

USE campus_lost_found;

-- Disable foreign key constraints during table refresh
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE claims;
TRUNCATE TABLE matches;
TRUNCATE TABLE items;
TRUNCATE TABLE locations;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. SEED USERS
-- Password for all seed users is 'DevPass@123'
-- SHA-256 hash: SHA2('DevPass@123', 256)
-- ----------------------------------------------------------------------------
INSERT INTO users (user_id, name, email, password_hash, role, created_at) VALUES
(1, 'Campus Administrator', 'admin@campus.edu', SHA2('DevPass@123', 256), 'ADMIN', '2026-09-01 09:00:00'),
(2, 'Alice Johnson',        'alice@campus.edu', SHA2('DevPass@123', 256), 'STUDENT', '2026-09-10 10:15:00'),
(3, 'Bob Smith',            'bob@campus.edu',   SHA2('DevPass@123', 256), 'STUDENT', '2026-09-11 11:30:00'),
(4, 'Charlie Davis',        'charlie@campus.edu', SHA2('DevPass@123', 256), 'STUDENT', '2026-09-12 14:00:00'),
(5, 'Diana Prince',          'diana@campus.edu', SHA2('DevPass@123', 256), 'STUDENT', '2026-09-15 16:45:00');

-- ----------------------------------------------------------------------------
-- 2. SEED LOCATIONS
-- Physical campus zones with optional building/room identifiers
-- ----------------------------------------------------------------------------
INSERT INTO locations (location_id, location_name, building) VALUES
(1, 'Central Library 2nd Floor Reading Room', 'Main Library'),
(2, 'Science Block Chemistry Lab 201',         'Science Wing B'),
(3, 'Engineering Auditorium Main Hall',       'Tech Complex'),
(4, 'Student Cafeteria Ground Floor',          'Student Activity Center'),
(5, 'Sports Complex Gymnasium',                'Athletic Arena'),
(6, 'Computer Science Lab 3',                  'IT & Software Block'),
(7, 'Administrative Office Lobby',             'Central Admin Building');

-- ----------------------------------------------------------------------------
-- 3. SEED ITEMS
-- Items 1 to 5 are LOST items reported by students.
-- Items 6 to 10 are FOUND items reported by students/staff.
-- ----------------------------------------------------------------------------
INSERT INTO items (item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at) VALUES
-- Lost items
(1, 2, 1, 'Blue Hydro Flask Water Bottle', 'Accessories', '32oz blue bottle with college coding club sticker on the side.', 'LOST', '2026-10-10', 'MATCHED', '2026-10-10 10:30:00'),
(2, 3, 3, 'Dell 65W USB-C Laptop Charger',  'Electronics', 'Black oval AC adapter with USB-C connector for Dell XPS.', 'LOST', '2026-10-11', 'ACTIVE',  '2026-10-11 11:00:00'),
(3, 4, 4, 'Campus Student ID Card',         'ID & Cards',  'Official university student ID card under name Charlie Davis.', 'LOST', '2026-10-12', 'ACTIVE',  '2026-10-12 12:15:00'),
(4, 5, 6, 'Calculus Early Transcendentals', 'Books & Stationery', 'Hardcover 9th edition math textbook with yellow highlighting.', 'LOST', '2026-10-13', 'ACTIVE',  '2026-10-13 15:00:00'),
(5, 2, 5, 'Black Leather Bifold Wallet',    'Personal Belongings', 'Contains student pass, gym card, and some cash.', 'LOST', '2026-10-14', 'RECOVERED', '2026-10-14 09:45:00'),

-- Found items
(6, 3, 1, 'Blue Insulated Water Flask',     'Accessories', 'Metal blue water flask found under study desk 14 on 2nd floor.', 'FOUND', '2026-10-10', 'CLAIMED', '2026-10-10 14:00:00'),
(7, 4, 3, 'Dell Laptop Power Adapter',       'Electronics', 'Black Dell USB-C adapter left plugged into rear auditorium socket.', 'FOUND', '2026-10-11', 'ACTIVE',  '2026-10-11 16:30:00'),
(8, 5, 4, 'Student ID Card (Charlie Davis)', 'ID & Cards', 'University ID card found on table near cafeteria juice stall.', 'FOUND', '2026-10-12', 'ACTIVE',  '2026-10-12 13:00:00'),
(9, 3, 5, 'Leather Wallet (Black)',         'Personal Belongings', 'Turned in to sports reception desk after evening basketball.', 'FOUND', '2026-10-14', 'RECOVERED', '2026-10-14 18:00:00'),
(10, 2, 2, 'Casio fx-991EX Calculator',     'Electronics', 'Scientific calculator with slide-on case left in chemistry lab.', 'FOUND', '2026-10-09', 'ACTIVE',  '2026-10-09 17:15:00');

-- ----------------------------------------------------------------------------
-- 4. SEED MATCHES
-- Links lost and found item pairs based on scoring criteria:
-- Category (25), Location (25), Date (20), Name (20), Description (10)
-- ----------------------------------------------------------------------------
INSERT INTO matches (match_id, lost_item_id, found_item_id, score, status, created_at) VALUES
-- Item 1 (Lost Hydro Flask) & Item 6 (Found Blue Flask) -> High Score (85.00)
(1, 1, 6, 85.00, 'CONFIRMED', '2026-10-10 14:30:00'),

-- Item 2 (Lost Dell Charger) & Item 7 (Found Dell Adapter) -> High Score (82.50)
(2, 2, 7, 82.50, 'PENDING',   '2026-10-11 17:00:00'),

-- Item 3 (Lost Student ID) & Item 8 (Found Student ID) -> Very High Score (92.00)
(3, 3, 8, 92.00, 'NOTIFIED',  '2026-10-12 13:30:00'),

-- Item 5 (Lost Wallet) & Item 9 (Found Wallet) -> High Score (88.00)
(4, 5, 9, 88.00, 'CONFIRMED', '2026-10-14 18:30:00');

-- ----------------------------------------------------------------------------
-- 5. SEED CLAIMS
-- Demonstrates full lifecycle of student claims on found items
-- ----------------------------------------------------------------------------
INSERT INTO claims (claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at) VALUES
-- Claim 1: Pending claim by Alice for Item 6 (Found Hydro Flask)
(1, 6, 2, '2026-10-11 09:00:00', 'PENDING',
 'It has a circular sticker of the Campus Coding Club on the lower side and a small dent on the cap.',
 NULL, NULL),

-- Claim 2: Approved claim by Alice for Item 9 (Found Wallet)
(2, 9, 2, '2026-10-14 19:00:00', 'APPROVED',
 'Contains student ID card for Alice Johnson with ID number 2024-CS-042 and blue gym membership card.',
 1, '2026-10-14 19:30:00'),

-- Claim 3: Rejected claim by Charlie for Item 10 (Found Calculator)
(3, 10, 4, '2026-10-12 10:00:00', 'REJECTED',
 'I lost a calculator in the Science Block last week. Mine has a silver sticker on the back cover.',
 1, '2026-10-12 11:30:00');
