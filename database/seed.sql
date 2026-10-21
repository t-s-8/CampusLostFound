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
(1, 'Campus Administrator', 'admin@campus.edu',   SHA2('DevPass@123', 256), 'ADMIN',   '2026-09-01 09:00:00'),
(2, 'Alice Johnson',        'alice@campus.edu',   SHA2('DevPass@123', 256), 'STUDENT', '2026-09-10 10:15:00'),
(3, 'Bob Smith',            'bob@campus.edu',     SHA2('DevPass@123', 256), 'STUDENT', '2026-09-11 11:30:00'),
(4, 'Charlie Davis',        'charlie@campus.edu', SHA2('DevPass@123', 256), 'STUDENT', '2026-09-12 14:00:00'),
(5, 'Diana Prince',          'diana@campus.edu',   SHA2('DevPass@123', 256), 'STUDENT', '2026-09-15 16:45:00');

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
-- Items 1 to 5: Original LOST items (preserved for backwards-compatibility).
-- Items 6 to 10: Original FOUND items (preserved for backwards-compatibility).
-- Items 11 to 26: Curated matching pairs (Strong, Partial, and Weak candidates).
-- Items 27 to 50: Diverse standalone LOST and FOUND items across all categories.
-- Total: Exactly 50 items (25 LOST, 25 FOUND).
-- ----------------------------------------------------------------------------
INSERT INTO items (item_id, user_id, location_id, name, category, description, item_type, item_date, status, created_at) VALUES
-- --- Foundational 10 Items (Preserved) ---
(1, 2, 1, 'Blue Hydro Flask Water Bottle', 'Accessories', '32oz blue bottle with college coding club sticker on the side.', 'LOST', '2026-10-10', 'MATCHED', '2026-10-10 10:30:00'),
(2, 3, 3, 'Dell 65W USB-C Laptop Charger',  'Electronics', 'Black oval AC adapter with USB-C connector for Dell XPS.', 'LOST', '2026-10-11', 'ACTIVE',  '2026-10-11 11:00:00'),
(3, 4, 4, 'Campus Student ID Card',         'ID & Cards',  'Official university student ID card under name Charlie Davis.', 'LOST', '2026-10-12', 'ACTIVE',  '2026-10-12 12:15:00'),
(4, 5, 6, 'Calculus Early Transcendentals', 'Books & Stationery', 'Hardcover 9th edition math textbook with yellow highlighting.', 'LOST', '2026-10-13', 'ACTIVE',  '2026-10-13 15:00:00'),
(5, 2, 5, 'Black Leather Bifold Wallet',    'Personal Belongings', 'Contains student pass, gym card, and some cash.', 'LOST', '2026-10-14', 'RECOVERED', '2026-10-14 09:45:00'),

(6, 3, 1, 'Blue Insulated Water Flask',     'Accessories', 'Metal blue water flask found under study desk 14 on 2nd floor.', 'FOUND', '2026-10-10', 'CLAIMED', '2026-10-10 14:00:00'),
(7, 4, 3, 'Dell Laptop Power Adapter',       'Electronics', 'Black Dell USB-C adapter left plugged into rear auditorium socket.', 'FOUND', '2026-10-11', 'ACTIVE',  '2026-10-11 16:30:00'),
(8, 5, 4, 'Student ID Card (Charlie Davis)', 'ID & Cards', 'University ID card found on table near cafeteria juice stall.', 'FOUND', '2026-10-12', 'ACTIVE',  '2026-10-12 13:00:00'),
(9, 3, 5, 'Leather Wallet (Black)',         'Personal Belongings', 'Turned in to sports reception desk after evening basketball.', 'FOUND', '2026-10-14', 'RECOVERED', '2026-10-14 18:00:00'),
(10, 2, 2, 'Casio fx-991EX Calculator',     'Electronics', 'Scientific calculator with slide-on case left in chemistry lab.', 'FOUND', '2026-10-09', 'ACTIVE',  '2026-10-09 17:15:00'),

-- --- Strong Match Candidates (High similarity: category + location + date + name) ---
(11, 2, 1, 'Apple AirPods Pro 2nd Gen', 'Electronics', 'White charging case with small scratch on the front lid.', 'LOST', '2026-10-15', 'MATCHED', '2026-10-15 10:00:00'),
(12, 3, 1, 'Apple AirPods Pro Wireless Earbuds', 'Electronics', 'White wireless earbuds in charging case found on 2nd floor library table.', 'FOUND', '2026-10-15', 'CLAIMED', '2026-10-15 14:30:00'),

(13, 3, 4, 'North Face Jester Backpack Black', 'Personal Belongings', 'Black backpack containing textbooks, water bottle, and student notebook.', 'LOST', '2026-10-16', 'MATCHED', '2026-10-16 11:15:00'),
(14, 4, 4, 'Black North Face Backpack', 'Personal Belongings', 'Black nylon backpack found on cafeteria booth seat.', 'FOUND', '2026-10-17', 'RECOVERED', '2026-10-17 09:45:00'),

(15, 4, 5, 'Casio G-Shock Watch Black', 'Accessories', 'Matte black digital sports watch with resin strap.', 'LOST', '2026-10-18', 'MATCHED', '2026-10-18 16:00:00'),
(16, 5, 5, 'Black Casio Digital Watch', 'Accessories', 'Water-resistant digital wristwatch found near gym bench press.', 'FOUND', '2026-10-18', 'ACTIVE', '2026-10-18 19:20:00'),

-- --- Partial Match Candidates (Moderate similarity: same category, different location/date) ---
(17, 5, 2, 'TI-84 Plus CE Graphing Calculator', 'Electronics', 'Color screen graphing calculator with slide cover.', 'LOST', '2026-10-19', 'MATCHED', '2026-10-19 13:00:00'),
(18, 2, 6, 'Texas Instruments Graphing Calculator', 'Electronics', 'Black graphing calculator left next to computer workstation 12.', 'FOUND', '2026-10-21', 'ACTIVE', '2026-10-21 17:40:00'),

(19, 2, 1, 'Stanley Quencher Tumbler Rose', 'Accessories', '40oz rose quartz tumbler with reusable clear straw.', 'LOST', '2026-10-20', 'MATCHED', '2026-10-20 12:30:00'),
(20, 3, 4, 'Pink Insulated Drink Tumbler', 'Accessories', 'Large pink insulated mug with handle left by cafeteria tray return.', 'FOUND', '2026-10-22', 'ACTIVE', '2026-10-22 15:10:00'),

(21, 3, 3, 'Sony WH-1000XM5 Noise Cancelling Headphones', 'Electronics', 'Silver/platinum over-ear Bluetooth headphones with gray zippered case.', 'LOST', '2026-10-22', 'MATCHED', '2026-10-22 14:00:00'),
(22, 5, 1, 'Sony Over-Ear Wireless Headphones', 'Electronics', 'Silver Sony headphones found in study pod 4.', 'FOUND', '2026-10-23', 'ACTIVE', '2026-10-23 18:15:00'),

-- --- Weak Match Candidates (Low similarity: category match with date or name variance) ---
(23, 4, 6, 'Blue Spiral Notebook Physics', 'Books & Stationery', 'Five-star spiral bound notebook with hand-written mechanics equations.', 'LOST', '2026-10-24', 'MATCHED', '2026-10-24 11:00:00'),
(24, 2, 3, 'Blue Physics Notes Folder', 'Books & Stationery', 'Cardstock folder containing handwritten physics problem sets.', 'FOUND', '2026-10-30', 'ACTIVE', '2026-10-30 16:30:00'),

(25, 5, 5, 'Car Key Fob with Red Lanyard', 'Personal Belongings', 'Toyota key fob attached to red university athletics lanyard.', 'LOST', '2026-10-25', 'MATCHED', '2026-10-25 17:30:00'),
(26, 3, 7, 'Brass Room Key with Keychain', 'Personal Belongings', 'Single brass key on miniature carabiner found on lobby bench.', 'FOUND', '2026-11-05', 'ACTIVE', '2026-11-05 10:15:00'),

-- --- Standalone Diverse Items (No Direct Match Pairs) ---
(27, 2, 6, 'Apple MacBook Air 13-inch M2', 'Electronics', 'Midnight blue MacBook Air with clear protective hard shell.', 'LOST', '2026-10-26', 'ACTIVE', '2026-10-26 10:45:00'),
(28, 4, 1, 'Logitech MX Master 3S Mouse', 'Electronics', 'Ergonomic dark gray wireless mouse found on reading table.', 'FOUND', '2026-10-27', 'ACTIVE', '2026-10-27 14:00:00'),
(29, 3, 4, 'Campus Dining Hall Meal Pass', 'ID & Cards', 'Laminated meal plan card under student name Bob Smith.', 'LOST', '2026-10-28', 'ACTIVE', '2026-10-28 12:00:00'),
(30, 5, 2, 'Safety Goggles & Lab Coat', 'Personal Belongings', 'Clear UV safety goggles and white cotton lab coat size M.', 'FOUND', '2026-10-28', 'ACTIVE', '2026-10-28 17:00:00'),
(31, 4, 1, 'Organic Chemistry as a Second Language', 'Books & Stationery', 'Paperback 5th edition study guide by David Klein.', 'LOST', '2026-10-29', 'ACTIVE', '2026-10-29 15:30:00'),
(32, 2, 7, 'Ray-Ban Aviator Sunglasses', 'Accessories', 'Gold frame aviator sunglasses with green polarized lenses in brown case.', 'FOUND', '2026-10-30', 'ACTIVE', '2026-10-30 11:20:00'),
(33, 5, 5, 'Nike Brasilia Training Duffel Bag', 'Personal Belongings', 'Navy blue gym duffel with workout clothes and lifting belt.', 'LOST', '2026-10-31', 'ACTIVE', '2026-10-31 09:00:00'),
(34, 3, 3, 'Anker 20000mAh Power Bank', 'Electronics', 'Heavy black portable charger with dual USB-A and USB-C output.', 'FOUND', '2026-11-01', 'ACTIVE', '2026-11-01 13:45:00'),
(35, 2, 4, 'State Driver License (Alice Johnson)', 'ID & Cards', 'Official driver license issued in California.', 'LOST', '2026-11-01', 'ACTIVE', '2026-11-01 18:30:00'),
(36, 4, 5, 'Silver Mailbox Key #14', 'Personal Belongings', 'Single mailbox key on blue leather loop found by gym locker 52.', 'FOUND', '2026-11-02', 'CLAIMED', '2026-11-02 12:00:00'),
(37, 3, 6, 'SanDisk 128GB Extreme USB Drive', 'Electronics', 'Red and black USB 3.2 flash drive containing computer science project repo.', 'LOST', '2026-11-02', 'ACTIVE', '2026-11-02 16:45:00'),
(38, 5, 1, 'Moleskine Classic Hardcover Journal', 'Books & Stationery', 'Black unruled notebook with black ribbon bookmark.', 'FOUND', '2026-11-03', 'ACTIVE', '2026-11-03 10:15:00'),
(39, 4, 2, 'Silver Claddagh Ring Size 7', 'Accessories', 'Irish Claddagh sterling silver ring with heart and crown motif.', 'LOST', '2026-11-03', 'ACTIVE', '2026-11-03 14:00:00'),
(40, 2, 3, 'Kindle Paperwhite 11th Gen', 'Electronics', '6.8-inch e-reader with green origami magnetic folding cover.', 'FOUND', '2026-11-04', 'ACTIVE', '2026-11-04 16:20:00'),
(41, 5, 7, 'Patagonia Better Sweater Fleece', 'Personal Belongings', 'Quarter-zip gray heather polyester sweater size Small.', 'LOST', '2026-11-04', 'ACTIVE', '2026-11-04 11:30:00'),
(42, 3, 6, 'USB-C Multiport Hub Adapter', 'Electronics', 'Space gray aluminum adapter with HDMI, SD card reader, and 3 USB ports.', 'FOUND', '2026-11-05', 'ACTIVE', '2026-11-05 15:00:00'),
(43, 2, 5, 'Apple Watch SE 40mm Starlight', 'Accessories', 'Smartwatch with sport band, Starlight aluminum finish.', 'LOST', '2026-11-05', 'ACTIVE', '2026-11-05 18:00:00'),
(44, 4, 4, 'Metro Transit TAP Card', 'ID & Cards', 'Blue regional public transit stored-value smart card.', 'FOUND', '2026-11-06', 'ACTIVE', '2026-11-06 12:45:00'),
(45, 3, 1, 'Rotring 600 Mechanical Pencil 0.5mm', 'Books & Stationery', 'Matte black all-metal drafting pencil with knurled grip.', 'LOST', '2026-11-06', 'ACTIVE', '2026-11-06 17:15:00'),
(46, 5, 2, 'Folding Rain Umbrella Black', 'Accessories', 'Compact automatic open/close travel umbrella with wooden handle.', 'FOUND', '2026-11-07', 'ACTIVE', '2026-11-07 09:30:00'),
(47, 4, 3, 'Linear Algebra and Its Applications', 'Books & Stationery', 'Hardcover 6th edition textbook by David Lay.', 'LOST', '2026-11-07', 'ACTIVE', '2026-11-07 14:40:00'),
(48, 2, 7, 'Tortoise Shell Prescription Eyeglasses', 'Accessories', 'Warby Parker optical glasses in brown tortoise shell case.', 'FOUND', '2026-11-08', 'ACTIVE', '2026-11-08 11:00:00'),
(49, 5, 4, 'Set of 3 Brass Dormitory Keys', 'Personal Belongings', 'Keys attached to blue university carabiner and mini LED flashlight.', 'LOST', '2026-11-08', 'ACTIVE', '2026-11-08 13:15:00'),
(50, 3, 6, 'iPad Air 5th Gen Space Gray', 'Electronics', '10.9-inch tablet with black magnetic folio cover and Apple Pencil attached.', 'FOUND', '2026-11-09', 'ACTIVE', '2026-11-09 17:50:00');

-- ----------------------------------------------------------------------------
-- 4. SEED MATCHES
-- Links lost and found item pairs based on scoring criteria:
-- Category (25), Location (20), Date (15), Name similarity (40).
-- ----------------------------------------------------------------------------
INSERT INTO matches (match_id, lost_item_id, found_item_id, score, status, created_at) VALUES
-- --- Original Matches (Preserved) ---
(1, 1, 6, 85.00, 'CONFIRMED', '2026-10-10 14:30:00'),
(2, 2, 7, 82.50, 'PENDING',   '2026-10-11 17:00:00'),
(3, 3, 8, 92.00, 'NOTIFIED',  '2026-10-12 13:30:00'),
(4, 5, 9, 88.00, 'CONFIRMED', '2026-10-14 18:30:00'),

-- --- Additional Matches ---
-- Item 11 (AirPods Pro Lost) & Item 12 (AirPods Pro Found) -> Strong Match (90.00)
(5, 11, 12, 90.00, 'PENDING',   '2026-10-15 15:00:00'),

-- Item 13 (North Face Lost) & Item 14 (North Face Found) -> Strong Match (87.50)
(6, 13, 14, 87.50, 'NOTIFIED',  '2026-10-17 10:30:00'),

-- Item 15 (G-Shock Lost) & Item 16 (Casio Found) -> Strong Match (85.00)
(7, 15, 16, 85.00, 'PENDING',   '2026-10-18 20:00:00'),

-- Item 17 (TI-84 Lost) & Item 18 (TI Found) -> Partial Match (57.50)
(8, 17, 18, 57.50, 'PENDING',   '2026-10-21 18:00:00'),

-- Item 19 (Stanley Tumbler) & Item 20 (Pink Tumbler) -> Partial Match (52.00)
(9, 19, 20, 52.00, 'DISMISSED', '2026-10-22 16:00:00'),

-- Item 21 (Sony Headphones) & Item 22 (Sony Headphones) -> Partial Match (65.00)
(10, 21, 22, 65.00, 'NOTIFIED', '2026-10-23 19:00:00'),

-- Item 23 (Physics Notebook) & Item 24 (Physics Folder) -> Weak Match (42.50)
(11, 23, 24, 42.50, 'PENDING',   '2026-10-30 17:00:00'),

-- Item 25 (Key Fob) & Item 26 (Brass Key) -> Weak Match (33.00)
(12, 25, 26, 33.00, 'DISMISSED', '2026-11-05 11:00:00');

-- ----------------------------------------------------------------------------
-- 5. SEED CLAIMS
-- Demonstrates full lifecycle of student claims on found items
-- ----------------------------------------------------------------------------
INSERT INTO claims (claim_id, item_id, user_id, claim_date, status, verification_details, reviewed_by, reviewed_at) VALUES
-- --- Original Claims (Preserved) ---
(1, 6, 2, '2026-10-11 09:00:00', 'PENDING',
 'It has a circular sticker of the Campus Coding Club on the lower side and a small dent on the cap.',
 NULL, NULL),

(2, 9, 2, '2026-10-14 19:00:00', 'APPROVED',
 'Contains student ID card for Alice Johnson with ID number 2024-CS-042 and blue gym membership card.',
 1, '2026-10-14 19:30:00'),

(3, 10, 4, '2026-10-12 10:00:00', 'REJECTED',
 'I lost a calculator in the Science Block last week. Mine has a silver sticker on the back cover.',
 1, '2026-10-12 11:30:00'),

-- --- Additional Claims ---
-- Claim 4: Pending claim by Alice for Item 12 (Found AirPods Pro)
(4, 12, 2, '2026-10-16 09:30:00', 'PENDING',
 'Has customized engraving "AJ-2026" on the charging case lid and black silicone tips.',
 NULL, NULL),

-- Claim 5: Approved claim by Bob for Item 14 (Found North Face Backpack)
(5, 14, 3, '2026-10-18 11:00:00', 'APPROVED',
 'Contains engineering notebook with Bob Smith written on page 1 and dorm room key 304B.',
 1, '2026-10-18 14:00:00'),

-- Claim 6: Rejected claim by Charlie for Item 22 (Found Sony Headphones)
(6, 22, 4, '2026-10-25 15:30:00', 'REJECTED',
 'Black Sony headphones left on desk. Serial number not known.',
 1, '2026-10-26 10:00:00'),

-- Claim 7: Pending claim by Diana for Item 36 (Found Mailbox Key)
(7, 36, 5, '2026-11-02 16:15:00', 'PENDING',
 'Single brass mailbox key stamped #14 on blue leather loop.',
 NULL, NULL);
