-- Run this only against the selected, empty hosted database (for example Aiven's defaultdb).
-- Unlike schema.sql, this file does not DROP or truncate tables.
CREATE TABLE IF NOT EXISTS users (
  user_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL, email VARCHAR(150) NOT NULL, password_hash VARCHAR(255) NOT NULL,
  role ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS locations (
  location_id INT AUTO_INCREMENT PRIMARY KEY,
  location_name VARCHAR(100) NOT NULL, building VARCHAR(100) NULL,
  CONSTRAINT uk_location_building UNIQUE (location_name, building)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS items (
  item_id INT AUTO_INCREMENT PRIMARY KEY, user_id INT NOT NULL, location_id INT NOT NULL,
  name VARCHAR(150) NOT NULL, category VARCHAR(50) NOT NULL, description TEXT NULL,
  item_type ENUM('LOST','FOUND') NOT NULL, item_date DATE NOT NULL,
  status ENUM('ACTIVE','MATCHED','CLAIMED','RECOVERED') NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_items_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_items_location FOREIGN KEY (location_id) REFERENCES locations(location_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  INDEX idx_items_user (user_id), INDEX idx_items_location (location_id),
  INDEX idx_items_type_status (item_type,status), INDEX idx_items_category (category), INDEX idx_items_date (item_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS matches (
  match_id INT AUTO_INCREMENT PRIMARY KEY, lost_item_id INT NOT NULL, found_item_id INT NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  status ENUM('PENDING','NOTIFIED','CONFIRMED','DISMISSED') NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT chk_match_score_range CHECK (score >= 0.00 AND score <= 100.00),
  CONSTRAINT uk_match_pair UNIQUE (lost_item_id,found_item_id),
  CONSTRAINT fk_matches_lost_item FOREIGN KEY (lost_item_id) REFERENCES items(item_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_matches_found_item FOREIGN KEY (found_item_id) REFERENCES items(item_id) ON DELETE CASCADE ON UPDATE CASCADE,
  INDEX idx_matches_lost (lost_item_id), INDEX idx_matches_found (found_item_id), INDEX idx_matches_score (score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS claims (
  claim_id INT AUTO_INCREMENT PRIMARY KEY, item_id INT NOT NULL, user_id INT NOT NULL,
  claim_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status ENUM('PENDING','APPROVED','REJECTED') NOT NULL DEFAULT 'PENDING',
  verification_details TEXT NULL, reviewed_by INT NULL, reviewed_at TIMESTAMP NULL,
  CONSTRAINT fk_claims_item FOREIGN KEY (item_id) REFERENCES items(item_id) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT fk_claims_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT fk_claims_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(user_id) ON DELETE SET NULL ON UPDATE CASCADE,
  INDEX idx_claims_item (item_id), INDEX idx_claims_user (user_id),
  INDEX idx_claims_status (status), INDEX idx_claims_reviewer (reviewed_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
