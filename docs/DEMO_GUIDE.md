# Campus Lost & Found System — Live Demonstration Guide

A practical presentation guide for demonstrating the **Campus Lost & Found System** to **Object-Oriented Programming (OOP)** and **Relational Database Management Systems (RDBMS)** faculty evaluators.

---

## Pre-Demonstration Checklist

Before inviting evaluators to observe the system:
1. **Local MySQL Service Running**: Ensure MySQL Server is active on `localhost:3306`.
2. **Database Initialized**: Ensure `campus_lost_found` is created via `database/schema.sql` and populated with the 50 seeded item records via `database/seed.sql`.
3. **Database Configuration**: Ensure `src/main/resources/db.properties` contains your valid local MySQL credentials.
4. **Tools Open**:
   * **Visual Studio Code / IntelliJ IDEA**: Project workspace loaded with files ready to open.
   * **MySQL Workbench / Terminal Client**: Connected to `campus_lost_found`.
   * **Terminal / Command Prompt**: Positioned at `/Users/t_s_8/Desktop/CampusLostFound`.

> [!CAUTION]
> **DO NOT EXECUTE `database/seed.sql` DURING THE LIVE DEMONSTRATION.**
> `seed.sql` executes `TRUNCATE TABLE` on all five tables before inserting records. Running it during your live demo will erase live claims and items created during your presentation.

---

## Verified Demo Accounts

Use these pre-seeded accounts during the live presentation:

| User ID | Full Name | Email / Login ID | Role | Password | Demonstration Purpose |
| :---: | :--- | :--- | :---: | :---: | :--- |
| **1** | Campus Administrator | `admin@campus.edu` | **ADMIN** | `DevPass@123` | Log in to review claims, execute approval transactions, view analytics |
| **2** | Alice Johnson | `alice@campus.edu` | **STUDENT** | `DevPass@123` | Log in to report items, search campus reports, submit claims |
| **3** | Bob Smith | `bob@campus.edu` | **STUDENT** | `DevPass@123` | Secondary student account with pre-seeded reports |
| **4** | Charlie Davis | `charlie@campus.edu` | **STUDENT** | `DevPass@123` | Student account associated with lost ID card seed data |
| **5** | Diana Prince | `diana@campus.edu` | **STUDENT** | `DevPass@123` | Student account with pre-seeded claims |

> [!WARNING]
> **Security Notice**: These credentials are for the local college-project demonstration environment only. They are shared development accounts and must not be reused in production.

---

## Part A: OOP Demonstration (10 Minutes)

Focus: Show how modern Object-Oriented principles are genuinely applied in the Java codebase.

### Step 1: Explain Problem & Objectives (1 Min)
* **What to say**:
  > "Campus Lost & Found solves the problem of scattered, informal lost-and-found tracking on campus. Our objective was to design a clean, layered desktop application in Java 26 using JavaFX and MySQL, strictly demonstrating OOP concepts like inheritance, polymorphism, encapsulation, and custom exception handling, while enforcing relational database integrity via JDBC."

### Step 2: Show the Model Class Hierarchy (Inheritance) (2 Mins)
* **File to Open**: [`src/main/java/com/campuslostfound/model/Item.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/Item.java)
* **What to show**:
  * Show lines 9–40: `public abstract class Item`
  * Explain common properties: `itemId`, `userId`, `locationId`, `name`, `category`, `description`, `itemType`, `itemDate`, `status`, and `createdAt`.
* **Files to Open next**:
  * [`src/main/java/com/campuslostfound/model/LostItem.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/LostItem.java): Show `public class LostItem extends Item`.
  * [`src/main/java/com/campuslostfound/model/FoundItem.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/FoundItem.java): Show `public class FoundItem extends Item`.
  * [`src/main/java/com/campuslostfound/model/Admin.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/Admin.java): Show `public class Admin extends User`.
* **What to explain**:
  * Base class `Item` encapsulates shared attributes and common logic across all reported belongings.
  * Child classes `LostItem` and `FoundItem` inherit those fields and specialize behavior.
  * Code reuse: we do not duplicate common setters, getters, or field declarations.

### Step 3: Demonstrate Encapsulation & Data Protection (2 Mins)
* **File to Open**: [`src/main/java/com/campuslostfound/model/Item.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/Item.java#L80-L125)
* **What to show**:
  * Point out that all class variables (`name`, `category`, `status`, etc.) are declared `private`.
  * Show the validation logic inside `setName(String name)`:
    ```java
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidItemException("Item name cannot be empty.");
        }
        this.name = name.trim();
    }
    ```
* **What to explain**:
  * Encapsulation is not just writing getters and setters; it is safeguarding internal object invariants. External code cannot set an item's name to `null` or a blank string, because the setter enforces business rules and throws an exception immediately.

### Step 4: Demonstrate Polymorphism (Dynamic Method Dispatch) (2 Mins)
* **File to Open**: [`src/main/java/com/campuslostfound/model/Item.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/Item.java#L63-L72)
* **What to show**:
  * Show the two abstract method definitions in `Item.java`:
    ```java
    public abstract String getDisplayDetails();
    public abstract boolean isClaimable();
    ```
  * Open `LostItem.java`:
    ```java
    @Override
    public boolean isClaimable() {
        return false;
    }
    @Override
    public String getDisplayDetails() {
        return "[LOST] " + getName() + " on " + getItemDate();
    }
    ```
  * Open `FoundItem.java`:
    ```java
    @Override
    public boolean isClaimable() {
        return getStatus() == ItemStatus.ACTIVE || getStatus() == ItemStatus.MATCHED;
    }
    @Override
    public String getDisplayDetails() {
        return "[FOUND] " + getName() + " on " + getItemDate();
    }
    ```
* **What to explain**:
  * In `ClaimService.java`, when checking if an item can be claimed, the code simply calls `item.isClaimable()`.
  * The Java Virtual Machine uses dynamic method dispatch at runtime to invoke either `LostItem.isClaimable()` or `FoundItem.isClaimable()` based on the actual object in memory.

### Step 5: Demonstrate Exception Handling (1.5 Mins)
* **Files to Open**:
  * [`src/main/java/com/campuslostfound/exception/CampusLostFoundException.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/exception/CampusLostFoundException.java)
  * [`src/main/java/com/campuslostfound/exception/InvalidClaimException.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/exception/InvalidClaimException.java)
* **What to explain**:
  * Custom unchecked exceptions (`extends RuntimeException`) represent domain errors cleanly.
  * In [`ClaimService.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/service/ClaimService.java#L63-L65): If a student attempts to file a claim on an item they reported themselves, the system throws `InvalidClaimException("You cannot submit a claim on an item you reported yourself.")`.
  * The JavaFX UI catches domain exceptions and displays a user-friendly alert via [`AlertHelper.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/ui/AlertHelper.java).

### Step 6: Explain Layered Architecture & Separation of Concerns (1.5 Mins)
* **What to show**:
  * Point out the package structure in the IDE project tree:
    * `model`: Entities and state.
    * `dao`: SQL queries and ResultSet mapping.
    * `service`: Business logic, validations, transactions.
    * `ui`: JavaFX controls and layout.
* **What to explain**:
  * The UI never calls JDBC directly. If the database engine changes from MySQL to PostgreSQL, only the DAO and database config packages change; zero lines of UI code are touched.

---

## Part B: RDBMS Demonstration (15 Minutes)

Focus: Show relational database design, schema constraints, normalization, SQL queries, and ACID transactional integrity.

### Step 1: Open MySQL Workbench & Show Schema (2 Mins)
* In MySQL Workbench or CLI, run:
  ```sql
  USE campus_lost_found;
  SHOW TABLES;
  ```
* Explain the 5 tables: `users`, `locations`, `items`, `matches`, `claims`.
* Open the rendered ER diagram at [`docs/ERD.png`](file:///Users/t_s_8/Desktop/CampusLostFound/docs/ERD.png) and explain the 1:N cardinalities.

### Step 2: Prove 50 Pre-Seeded Item Reports (1 Min)
* Run this query in MySQL Workbench:
  ```sql
  SELECT item_type, COUNT(*) AS count
  FROM items
  GROUP BY item_type
  UNION ALL
  SELECT 'TOTAL ITEMS', COUNT(*)
  FROM items;
  ```
* **Expected Output**:
  * `LOST`: 25
  * `FOUND`: 25
  * `TOTAL ITEMS`: 50

### Step 3: Demonstrate Constraints & Referential Integrity (2 Mins)
* **Table to Inspect**: `database/schema.sql` (lines 30–127).
* **What to show**:
  * Primary Keys: `AUTO_INCREMENT PRIMARY KEY` on all 5 tables.
  * Unique Constraints: `CONSTRAINT uk_users_email UNIQUE (email)`.
  * Unique Composite Constraint: `CONSTRAINT uk_location_building UNIQUE (location_name, building)`.
  * Check Constraint: `CONSTRAINT chk_match_score_range CHECK (score >= 0.00 AND score <= 100.00)` on `matches`.
  * Foreign Key Referential Actions:
    * `items.user_id` uses `ON DELETE RESTRICT` (cannot delete a user who has active item history).
    * `matches.lost_item_id` uses `ON DELETE CASCADE` (deleting an item cleans up candidate matches).

### Step 4: Demonstrate SQL Queries (JOIN, GROUP BY, Aggregate) (3 Mins)
* **Query 1: INNER JOIN between Items, Users, and Locations**:
  ```sql
  SELECT
      i.item_id,
      i.name AS item_name,
      i.item_type,
      i.status,
      u.name AS reported_by,
      l.location_name
  FROM items i
  INNER JOIN users u ON i.user_id = u.user_id
  INNER JOIN locations l ON i.location_id = l.location_id
  LIMIT 5;
  ```
* **Query 2: GROUP BY with Category Breakdown**:
  ```sql
  SELECT
      category,
      COUNT(*) AS total_reports,
      SUM(CASE WHEN item_type = 'LOST' THEN 1 ELSE 0 END) AS lost_items,
      SUM(CASE WHEN item_type = 'FOUND' THEN 1 ELSE 0 END) AS found_items
  FROM items
  GROUP BY category
  ORDER BY total_reports DESC;
  ```

---

## Part C: Live End-to-End Application & Transaction Demo (10 Minutes)

Now demonstrate the running JavaFX desktop application and verify database state changes live.

### Step 1: Launch Application
In the terminal, run:
```bash
mvn javafx:run
```
The **Campus Lost & Found** Login screen appears.

### Step 2: Student Login
1. Enter:
   * **Email**: `alice@campus.edu`
   * **Password**: `DevPass@123`
2. Click **Sign In**.
3. Point out Alice's **Student Dashboard**:
   * Shows Alice's personal reported items (e.g., Item 1: Blue Hydro Flask, Item 5: Black Leather Wallet).
   * Shows navigation tiles: **Report Item**, **Search Items**, **Possible Matches**, **My Claims**.

### Step 3: Search & Candidate Matching
1. Click **Search Items**.
   * Type `"AirPods"` or `"Calculator"` in the live filter.
   * Filter by Category: `"Electronics"`.
   * Point out that searching is responsive and executes via `ItemSearch`.
2. Click **Possible Matches**.
   * Show ranked lost/found pairs sorted by similarity score using `MatchPriorityQueue`.
   * Explain the scoring weights: Category (25), Location (25), Date (20), Name (20), Description (10).

### Step 4: Submit a Claim Live
1. Click **My Claims** (or navigate to Claims).
2. Look up a pre-seeded claimable found item, such as **Item 10** (`Casio fx-991EX Calculator`, FOUND, ACTIVE):
   * Select or enter **Item ID**: `10`
   * Enter Verification Details:
     ```text
     "My calculator has a small scratch on the solar panel and my initials 'AJ' written inside the slide case."
     ```
3. Click **Submit Ownership Claim**.
4. A success dialog appears confirming submission.

### Step 5: Switch to Administrator & Review Claim
1. Click **Sign Out**.
2. Log in with the Administrator account:
   * **Email**: `admin@campus.edu`
   * **Password**: `DevPass@123`
3. Point out the **Admin Dashboard**:
   * Live statistics cards: Total Lost, Total Found, Active Reports, Recovered Items, Pending Claims.
   * Top loss locations table.
4. Click **Claims Management** (or Review Claims panel):
   * Alice's newly submitted claim appears at the top of the **PENDING** queue!

### Step 6: Execute the Atomic Approval Transaction
1. Select Alice's pending claim for Item 10.
2. Click **Approve Claim**.
3. The claim transitions to `APPROVED`.
4. Explain what happened under the hood:
   * `ClaimDAO.approveClaimTransaction()` opened an atomic transaction:
     * `conn.setAutoCommit(false)`
     * Acquired write locks on claim and item rows using `SELECT ... FOR UPDATE`
     * Updated claim status to `APPROVED`
     * Updated item status to `RECOVERED`
     * Confirmed associated matches
     * Rejected competing pending claims
     * `conn.commit()`

### Step 7: Live SQL Verification (Crucial for RDBMS Evaluators)
Switch back to MySQL Workbench and run this query live:
```sql
SELECT
    c.claim_id,
    c.status AS claim_status,
    c.reviewed_by,
    c.reviewed_at,
    i.item_id,
    i.name AS item_name,
    i.status AS item_status
FROM claims c
JOIN items i ON c.item_id = i.item_id
WHERE c.item_id = 10;
```
* **Evaluators can see live with their own eyes**:
  * `claim_status` is now `'APPROVED'`
  * `reviewed_by` is `1` (Admin ID)
  * `reviewed_at` has the live timestamp
  * `item_status` in the `items` table is now `'RECOVERED'`!

This conclusively proves end-to-end integration between JavaFX UI, Service logic, JDBC transactions, and the MySQL database engine.
