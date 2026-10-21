# Campus Lost & Found System — Viva Preparation Guide

Comprehensive interview preparation guide designed for a first-year B.Tech Computer Science & Engineering student presenting before **Object-Oriented Programming (OOP)** and **Relational Database Management Systems (RDBMS)** faculty panels.

---

## Table of Contents
1. [One-Minute Oral Project Pitch](#1-one-minute-oral-project-pitch)
2. [Project Objectives and Scope](#2-project-objectives-and-scope)
3. [Architecture & Workflow Traces](#3-architecture--workflow-traces)
4. [OOP Viva Questions & Answers](#4-oop-viva-questions--answers)
5. [RDBMS Viva Questions & Answers](#5-rdbms-viva-questions--answers)
6. [ERD & Normalization Viva Questions](#6-erd--normalization-viva-questions)
7. [SQL Viva Questions & Answers](#7-sql-viva-questions--answers)
8. [JDBC & Transaction Viva Questions](#8-jdbc--transaction-viva-questions)
9. [Data Structures & Algorithms (DSA) Questions](#9-data-structures--algorithms-dsa-questions)
10. [Testing & Quality Assurance Questions](#10-testing--quality-assurance-questions)
11. [Verified Demonstration Credentials](#11-verified-demonstration-credentials)
12. [Design Trade-Offs & Limitations](#12-design-trade-offs--limitations)

---

## 1. One-Minute Oral Project Pitch

> **Practice saying this aloud:**
>
> *"Good morning, Professors. Our project is **Campus Lost & Found**, a centralized desktop management system developed in **Java 26** using **JavaFX** and a local **MySQL** relational database.*
>
> *The primary objective of this project is to eliminate fragmented lost-and-found communication across campus through a secure, structured platform. Technically, the project was engineered to demonstrate two foundational computer science pillars:*
>
> *First, **Object-Oriented Programming**: We implemented an abstract domain model using inheritance with `Item`, `LostItem`, and `FoundItem`, demonstrated runtime polymorphism through dynamic method dispatch with `isClaimable()` and `getDisplayDetails()`, enforced strict encapsulation with field validation, and structured the system into a decoupled 6-layer architecture.*
>
> *Second, **Relational Database Management**: We designed a normalized 3NF database schema comprising five tables with foreign key referential integrity, check constraints, composite unique keys, and indexed lookups. Crucially, we implemented true **ACID transactional integrity** using JDBC: claim approvals utilize pessimistic row-level locking (`SELECT ... FOR UPDATE`), atomic state updates, and guaranteed rollback protection.*
>
> *The database contains 50 pre-seeded item reports, and our automated JUnit 5 test suite passes all 92 tests with zero errors or failures. I am excited to demonstrate both the code and the database."*

---

## 2. Project Objectives and Scope

### Key Objectives
1. **Consolidate Campus Reports**: Eliminate noticeboards and informal chat groups by centralizing item reports in a relational schema.
2. **Prevent False Claims**: Require students to provide verification details (distinguishing marks, serial numbers, contents) before an item can be claimed.
3. **Automate Candidate Matching**: Provide candidate match suggestions using a weighted similarity algorithm (0–100 score) and priority queue ranking.
4. **Guarantee Data Integrity**: Ensure that once an administrator approves a claim, the item and claim states transition atomically without race conditions.

### System Scope & Boundaries
* **Target Audience**: University students, staff, and campus security/facility administrators.
* **Platform**: Desktop JavaFX application connecting via JDBC over TCP port 3306 to MySQL Server.
* **Dataset**: 5 pre-seeded user accounts, 7 campus locations, 50 item reports (25 LOST, 25 FOUND), 12 candidate matches, and 7 claims.

---

## 3. Architecture & Workflow Traces

### The 6-Layer Architecture
```text
JavaFX UI  -->  Service Layer  -->  Business Logic / DSA  -->  DAO Layer  -->  JDBC  -->  MySQL
```

### Complete Workflow Traces

#### Workflow 1: User Login
1. User enters email and password in [`LoginView`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/ui/LoginView.java).
2. `LoginView.handleLogin()` invokes `AuthService.authenticate(email, password)`.
3. `AuthService` queries `UserDAO.findByEmail(normalizedEmail)`.
4. `UserDAO` executes a parameterized SQL query: `SELECT * FROM users WHERE email = ?`.
5. `AuthService.hashPassword(password)` computes SHA-256 and compares it with the database `password_hash`.
6. On success, `ViewNavigator` routes the user to `StudentDashboardView` (if `role == STUDENT`) or `AdminDashboardView` (if `role == ADMIN`).

#### Workflow 2: Reporting an Item
1. Student fills out [`ReportItemView`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/ui/ReportItemView.java) (name, category, description, date, campus location, type).
2. `ReportItemView.handleSubmit()` validates non-empty fields and calls `ItemService.reportItem(item)`.
3. `ItemService` delegates persistence to `ItemDAO.create(item)`.
4. `ItemDAO` executes `INSERT INTO items (...) VALUES (?, ?, ?, ...)` via `PreparedStatement` with `RETURN_GENERATED_KEYS`.
5. `ItemService` invokes `MatchService.findAndRecordMatchesForItem(savedItem)` to automatically compute candidate matches against opposing items.

#### Workflow 3: Searching Items
1. User types in [`SearchView`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/ui/SearchView.java) and selects category or status filters.
2. `SearchView.executeSearch()` calls `SearchService.search(keyword, type, category, status)`.
3. `SearchService` delegates to in-memory `ItemSearch.search(keyword, type, category, status)` which scans the collection in $O(N)$ time using predicate filters.

#### Workflow 4: Calculating & Ranking Matches
1. `MatchService.findAndRecordMatchesForItem(item)` fetches all active opposing items from `ItemDAO`.
2. For each pair, `MatchAlgorithm.compare(item1, item2)` evaluates category (25), location (25), date (20), name (20), and description (10).
3. If score $\ge 70.0$, a `Match` object is created and inserted into `MatchPriorityQueue`.
4. `MatchPriorityQueue` orders candidate matches using a binary Max-Heap ($O(\log N)$ insertion).
5. Significant matches are persisted to MySQL via `MatchDAO.save(match)`.

#### Workflow 5: Submitting a Claim
1. Student enters item ID and verification details in [`ClaimsView`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/ui/ClaimsView.java).
2. `ClaimsView.handleSubmitClaim()` invokes `ClaimService.submitClaim(itemId, studentId, proof)`.
3. `ClaimService` verifies:
   * Target item exists and is a `FOUND` item (`item.getItemType() == ItemType.FOUND`).
   * Item is currently claimable (`item.isClaimable() == true`).
   * Student is not claiming their own reported item (`item.getUserId() != claimantUserId`).
   * Student does not already have a pending claim for this item.
4. `ClaimDAO.create(newClaim)` executes `INSERT INTO claims (item_id, user_id, verification_details, status) VALUES (?, ?, ?, 'PENDING')`.

#### Workflow 6: Approving a Claim (Transactional)
1. Administrator inspects pending claims in `AdminDashboardView` or `ClaimsView`.
2. Clicks **Approve Claim**, triggering `ClaimService.approveClaim(claimId, adminId)`.
3. `ClaimService` verifies that the reviewer has `ADMIN` privileges.
4. `ClaimDAO.approveClaimTransaction()` initiates a database transaction:
   * Sets `conn.setAutoCommit(false)`.
   * Acquires row-level write locks using `SELECT ... FOR UPDATE` on `claims` and `items`.
   * Validates claim is `PENDING` and item is `FOUND` and `ACTIVE`/`MATCHED`.
   * `UPDATE claims SET status = 'APPROVED', reviewed_by = ?, reviewed_at = ? WHERE claim_id = ?`.
   * `UPDATE items SET status = 'RECOVERED' WHERE item_id = ?`.
   * `UPDATE matches SET status = 'CONFIRMED' WHERE found_item_id = ?`.
   * `UPDATE claims SET status = 'REJECTED' WHERE item_id = ? AND claim_id <> ? AND status = 'PENDING'`.
   * Executes `conn.commit()`.
5. If any step throws an error, the `catch` block invokes `conn.rollback()`.
6. In `finally`, `conn.setAutoCommit(true)` restores the connection.

#### Workflow 7: Rejecting a Claim
1. Administrator selects claim and clicks **Reject Claim**.
2. `ClaimService.rejectClaim(claimId, adminId)` validates admin role and pending status.
3. `ClaimDAO.updateStatus(claimId, ClaimStatus.REJECTED, adminId, timestamp)` updates the row.

#### Workflow 8: Displaying Admin Analytics
1. Administrator opens [`AdminDashboardView`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/ui/AdminDashboardView.java).
2. `AdminDashboardView.loadAnalytics()` calls `AnalyticsService.getDashboardAnalytics()`.
3. `AnalyticsService` calls `AnalyticsDAO.fetchAnalytics()`.
4. `AnalyticsDAO` executes SQL aggregate queries: `COUNT(*)`, `SUM(CASE WHEN item_type = 'LOST' THEN 1 ELSE 0 END)`, and `GROUP BY location_id`/`category`.
5. Maps results into an [`AdminAnalytics`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/AdminAnalytics.java) object.
6. JavaFX UI renders KPI metric cards and breakdown tables.

---

## 4. OOP Viva Questions & Answers

### Q1: What is Encapsulation and where is it used in your project?
* **Answer**: Encapsulation is the bundling of data and the methods that operate on that data into a single unit (class), while restricting direct access to the internal state from outside the class. In our project, all entity fields in [`Item.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/Item.java) (`name`, `category`, `status`, etc.) are declared `private`. Access is granted exclusively through public getters and setters. Setters contain validation logic—for example, `setName(String name)` checks that the argument is not null or empty and throws `InvalidItemException`, preventing external code from corrupting the object's state.

### Q2: Why did you use Inheritance in your model layer?
* **Answer**: Inheritance allows a child class to inherit fields and methods from a parent class, promoting code reuse and establishing an *is-a* relationship. In our system, `Item` is an abstract base class containing common attributes like `itemId`, `name`, `category`, and `itemDate`. `LostItem` and `FoundItem` extend `Item`. Both child classes inherit all common attributes without code duplication while providing specialized behavior. Similarly, `Admin` extends `User`, adding administrative department tracking.

### Q3: Where is Polymorphism demonstrated in this project?
* **Answer**: Polymorphism is demonstrated through runtime dynamic method dispatch. In [`Item.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/model/Item.java), we declared two abstract methods:
  ```java
  public abstract String getDisplayDetails();
  public abstract boolean isClaimable();
  ```
  * In `LostItem`: `isClaimable()` returns `false` (lost reports cannot be claimed).
  * In `FoundItem`: `isClaimable()` returns `true` if status is `ACTIVE` or `MATCHED`.
  * When `ClaimService` verifies claim eligibility, it executes `item.isClaimable()`. The JVM resolves which implementation to execute at runtime based on the actual object instance, without requiring `instanceof` checks.

### Q4: What is the difference between an Abstract Class and an Interface, and why did you make `Item` abstract?
* **Answer**: An abstract class can have member fields, constructors, and implemented methods alongside abstract methods, whereas traditional interfaces primarily declare method contracts. `Item` was made an `abstract class` because lost and found items share substantial concrete state (`itemId`, `userId`, `locationId`, timestamps) and implemented getters/setters, but an "Item" by itself cannot exist in the real world—it must be either a `LostItem` or a `FoundItem`. Making `Item` abstract prevents direct instantiation (`new Item(...)` is illegal).

### Q5: What is the difference between Method Overloading and Method Overriding in your project?
* **Answer**:
  * **Overriding (Runtime Polymorphism)**: A child class provides a specific implementation of a method already declared in its parent class with the exact same signature. Example: `LostItem` overriding `getDisplayDetails()` from `Item`.
  * **Overloading (Compile-Time Polymorphism)**: Defining methods with the same name but different parameter lists within the same class. Example: In [`ItemSearch.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/dsa/ItemSearch.java), we have overloaded search methods:
    * `search(String keyword)`
    * `search(String keyword, ItemType type)`
    * `search(String keyword, ItemType type, String category, ItemStatus status)`

### Q6: Why did you create custom exceptions instead of using standard exceptions?
* **Answer**: Standard exceptions like `IllegalArgumentException` or `RuntimeException` are generic and do not convey domain context. We created [`CampusLostFoundException`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/exception/CampusLostFoundException.java) as a base domain exception, and specialized child exceptions like `InvalidClaimException` and `InvalidItemException`. This allows service layers to communicate specific validation failures (e.g. self-claiming, duplicate pending claims) that the UI catches to display targeted, meaningful error dialogs to the user.

### Q7: What are `static` and `final` keywords used for in your application?
* **Answer**:
  * `static`: Belongs to the class rather than instances. Example: `AuthService.hashPassword(String)` is a pure static utility method; `MatchAlgorithm.CATEGORY_WEIGHT` is a static constant shared across all calculations.
  * `final`: Means unchangeable. On variables, it creates constants (`public static final double MAX_SCORE = 100.0`); on references, it prevents reassignment (e.g. `private final UserDAO userDAO` in `AuthService` ensures the dependency reference is immutable after constructor injection).

### Q8: Which SOLID principles does your project follow?
* **Answer**:
  1. **Single Responsibility Principle (SRP)**: Each class has one purpose. `ItemDAO` only handles SQL mapping for items; `ItemSearch` only handles in-memory search; `ItemService` only enforces business logic.
  2. **Open/Closed Principle (OCP)**: New item categories or location types can be introduced without modifying the core `Item` class.
  3. **Liskov Substitution Principle (LSP)**: Any method accepting an `Item` reference can accept a `LostItem` or `FoundItem` without breaking program correctness.
  4. **Dependency Inversion Principle (DIP)**: Services receive DAOs through constructor injection, decoupling service logic from DAO instantiation.

---

## 5. RDBMS Viva Questions & Answers

### Q1: Why did you choose a Relational Database (MySQL) instead of NoSQL or file storage?
* **Answer**: Campus Lost & Found requires strict data consistency, relationships, and ACID transactional integrity. Belongings belong to users, items are found at specific campus locations, and claims link students to items. A relational DBMS enforces foreign key referential integrity, eliminates update/deletion anomalies, prevents duplicate claims via unique constraints, and supports ACID transactions (`conn.commit()` / `conn.rollback()`), which plain file storage or eventual-consistency NoSQL databases cannot guarantee without complex application-level locking.

### Q2: What are the Primary Keys and Foreign Keys in your schema?
* **Answer**:
  * **Primary Keys**: Every table uses an auto-incrementing integer surrogate key (`user_id`, `location_id`, `item_id`, `match_id`, `claim_id`).
  * **Foreign Keys**:
    * In `items`: `user_id` references `users(user_id)` and `location_id` references `locations(location_id)`.
    * In `matches`: `lost_item_id` references `items(item_id)` and `found_item_id` references `items(item_id)`.
    * In `claims`: `item_id` references `items(item_id)`, `user_id` references `users(user_id)`, and `reviewed_by` references `users(user_id)`.

### Q3: What happens if someone tries to insert a foreign key referencing a nonexistent ID?
* **Answer**: MySQL immediately rejects the insert statement and raises an `Error 1452: Cannot add or update a child row: a foreign key constraint fails`. The JDBC driver throws a `java.sql.SQLException`, which our DAO catches and wraps into a `CampusLostFoundException`. This guarantees referential integrity—an item can never point to a ghost user or nonexistent location.

### Q4: What is the difference between `ON DELETE CASCADE`, `ON DELETE RESTRICT`, and `ON DELETE SET NULL` in your database?
* **Answer**:
  * `ON DELETE RESTRICT`: Used on `items.user_id` and `items.location_id`. Prevents deleting a user or location if there are active item records associated with them, preserving audit history.
  * `ON DELETE CASCADE`: Used on `matches.lost_item_id` and `claims.item_id`. If an item report is purged from the database, all its candidate matches and claims are automatically cleaned up to prevent orphaned records.
  * `ON DELETE SET NULL`: Used on `claims.reviewed_by`. If an administrator account is deleted, the claim history is preserved, but the `reviewed_by` column is safely set to `NULL`.

### Q5: What is Candidate Key vs Primary Key in your `users` table?
* **Answer**: A candidate key is any attribute (or set of attributes) that uniquely identifies a row in a table. In our `users` table, both `user_id` and `email` are candidate keys. We chose `user_id` as the **Primary Key** because compact integer keys are faster for joins, indexing, and foreign key references. We placed a `UNIQUE` constraint on `email`, making it an alternate (secondary) candidate key.

---

## 6. ERD & Normalization Viva Questions

### Q1: Explain the ER Diagram and its relationships.
* **Answer**: Referring to [`docs/ERD.png`](file:///Users/t_s_8/Desktop/CampusLostFound/docs/ERD.png):
  * `users` to `items`: One-to-Many ($1:N$). One user can report many items; each item report has exactly one reporting user.
  * `locations` to `items`: One-to-Many ($1:N$). One campus location can have many reported items; each item is associated with one location.
  * `items` to `matches`: Two One-to-Many relationships. Exactly one lost item and one found item form a candidate match pair.
  * `items` to `claims`: One-to-Many ($1:N$). A found item can receive multiple claims from different students.
  * `users` to `claims`: Two One-to-Many relationships—one for the student filing the claim (`user_id`), and one optional relationship for the administrator reviewing the claim (`reviewed_by`).

### Q2: How does your schema satisfy First Normal Form (1NF)?
* **Answer**: A table is in 1NF if every attribute contains only atomic (indivisible) scalar values, every record has a unique primary key, and there are no repeating groups. In our database:
  * Each table has a defined single-column primary key (`item_id`, `user_id`, etc.).
  * No column contains comma-separated values, arrays, or nested objects.
  * Each column has a strictly typed definition (`VARCHAR`, `INT`, `DATE`, `ENUM`).

### Q3: How does your schema satisfy Second Normal Form (2NF)?
* **Answer**: A table is in 2NF if it is in 1NF and all non-key attributes are fully functionally dependent on the entire primary key (no partial dependencies). Partial dependencies only occur when a table has a composite primary key. Because every table in our schema uses a single-column synthetic primary key (`AUTO_INCREMENT`), partial dependency is mathematically impossible. Therefore, all five tables automatically satisfy 2NF.

### Q4: How does your schema satisfy Third Normal Form (3NF)?
* **Answer**: A table is in 3NF if it is in 2NF and contains no transitive dependencies (no non-prime attribute depends on another non-prime attribute). In our design:
  * We separated `locations` into its own table rather than embedding `location_name` and `building` inside `items`.
  * If `building` were stored in `items`, `building` would depend on `location_name`, which depends on `item_id` ($item\_id \rightarrow location \rightarrow building$). That is a transitive dependency!
  * By creating a separate `locations` table, we eliminate:
    1. **Insertion Anomaly**: We can register a new campus science lab even before any item is lost there.
    2. **Deletion Anomaly**: If the only lost item in the gymnasium is deleted, we do not lose record of the gymnasium location.
    3. **Update Anomaly**: If a campus building is renamed, we update exactly one row in `locations` instead of thousands of rows in `items`.

---

## 7. SQL Viva Questions & Answers

### Q1: What is the difference between `WHERE` and `HAVING`?
* **Answer**: `WHERE` filters individual rows *before* any grouping or aggregation takes place. `HAVING` filters aggregated groups *after* the `GROUP BY` clause has been evaluated.
  * Example from our project:
    ```sql
    SELECT category, COUNT(*) AS count
    FROM items
    WHERE status = 'ACTIVE'      -- filters rows before grouping
    GROUP BY category
    HAVING COUNT(*) > 1;         -- filters groups after aggregation
    ```

### Q2: What is the difference between `INNER JOIN` and `LEFT JOIN` in your queries?
* **Answer**:
  * `INNER JOIN`: Returns only rows where there is a matching record in both tables. When joining `items` with `users`, only items with a valid matching user are returned.
  * `LEFT JOIN`: Returns all rows from the left table, and matching rows from the right table (or `NULL` if no match exists). In our analytics query:
    ```sql
    SELECT l.location_name, COUNT(i.item_id) AS items_reported
    FROM locations l
    LEFT JOIN items i ON l.location_id = i.location_id
    GROUP BY l.location_id;
    ```
    This ensures that campus locations where zero items have been lost still appear in the report with a count of `0`.

### Q3: What is a Subquery and where is it used?
* **Answer**: A subquery is a `SELECT` statement nested inside another SQL statement. In [`ClaimDAO.approveClaimTransaction`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/dao/ClaimDAO.java#L193-L194), we use an `IN` subquery:
  ```sql
  UPDATE items SET status = 'RECOVERED'
  WHERE item_id IN (
      SELECT lost_item_id FROM matches WHERE found_item_id = ?
  ) AND user_id = ? AND status IN ('ACTIVE', 'MATCHED');
  ```
  This automatically identifies and updates the claimant's matching lost item report within the approval transaction.

### Q4: Why did you create Indexes and what are their costs?
* **Answer**: We added indexes on frequently searched and joined columns: `INDEX idx_items_type_status (item_type, status)` and `INDEX idx_items_category (category)`.
  * **Benefit**: B-Tree indexes speed up `SELECT` queries from $O(N)$ full table scans to $O(\log N)$ logarithmic index lookups.
  * **Trade-off / Cost**: Indexes consume additional disk space and slightly slow down `INSERT`, `UPDATE`, and `DELETE` operations because the DBMS must update the B-Tree index structures whenever data changes.

---

## 8. JDBC & Transaction Viva Questions

### Q1: What is the difference between `Statement` and `PreparedStatement`, and why is `PreparedStatement` superior?
* **Answer**:
  1. **SQL Injection Prevention**: `Statement` concatenates raw user strings directly into SQL syntax, allowing malicious inputs like `' OR '1'='1` to alter query execution. `PreparedStatement` uses parameterized placeholders (`?`); user input is transmitted separately as pure data values, making SQL injection impossible.
  2. **Pre-compilation & Performance**: The database compiles the SQL query structure once; subsequent executions with different parameters reuse the cached query execution plan.

### Q2: How does `try-with-resources` work in JDBC?
* **Answer**: `try-with-resources` was introduced in Java 7 for classes implementing `java.lang.AutoCloseable`. Database connections, statements, and result sets consume OS sockets and database memory buffers. By declaring them inside the `try (...)` parentheses:
  ```java
  try (Connection conn = DatabaseConnection.getConnection();
       PreparedStatement stmt = conn.prepareStatement(sql);
       ResultSet rs = stmt.executeQuery()) {
      // process results
  }
  ```
  Java guarantees that `close()` is automatically invoked on `rs`, `stmt`, and `conn` when the block exits, even if an exception or `return` occurs, completely preventing database connection leaks.

### Q3: Explain the ACID properties using your Claim Approval transaction.
* **Answer**:
  * **Atomicity**: In [`ClaimDAO.approveClaimTransaction`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/dao/ClaimDAO.java#L186-L308), the approval must update `claims` to `APPROVED`, update `items` to `RECOVERED`, update `matches` to `CONFIRMED`, and reject competing claims. Either all 4 updates succeed, or if any fails, `conn.rollback()` restores all records to their previous state. It is all-or-nothing.
  * **Consistency**: The database transitions only from one valid state to another. Constraints (such as item statuses, foreign keys, and check constraints) are preserved.
  * **Isolation**: The transaction sets `conn.setAutoCommit(false)` and uses `SELECT ... FOR UPDATE` row-level write locks. Concurrent transactions cannot read or modify the locked claim or item rows until the current transaction commits.
  * **Durability**: Once `conn.commit()` executes, MySQL writes the transaction to its persistent redo log on disk. Even if the server loses power a millisecond later, the approval is permanently saved.

### Q4: What does `conn.setAutoCommit(false)` do?
* **Answer**: By default, JDBC operates in auto-commit mode, where every individual SQL statement is treated as an independent transaction and committed immediately upon execution. Calling `conn.setAutoCommit(false)` disables this behavior, allowing multiple `INSERT` or `UPDATE` statements to be grouped into a single transactional boundary that is only committed when `conn.commit()` is explicitly called.

### Q5: What is pessimistic row locking (`SELECT ... FOR UPDATE`)?
* **Answer**: It is a concurrency control mechanism. In [`ClaimDAO.approveClaimTransaction`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/dao/ClaimDAO.java#L187-L188), we execute:
  ```sql
  SELECT claim_id, item_id, user_id, status FROM claims WHERE claim_id = ? FOR UPDATE;
  SELECT item_id, user_id, item_type, status FROM items WHERE item_id = ? FOR UPDATE;
  ```
  This tells MySQL to place exclusive row-level write locks on those records. If two administrators click "Approve" on different claims for the same item at the exact same millisecond, the second transaction is blocked until the first transaction finishes, preventing double-recoveries or race conditions.

---

## 9. Data Structures & Algorithms (DSA) Questions

### Q1: What data structures did you use and why?
* **Answer**:
  1. `ArrayList<Item>`: For storing and filtering dynamic lists of items retrieved from the database.
  2. `HashMap<Integer, Item>`: In `ItemSearch`, provides $O(1)$ constant-time lookup of item details by their unique primary key ID.
  3. `PriorityQueue<Match>`: A binary Max-Heap that automatically maintains candidate matches sorted by similarity score in descending order.

### Q2: How does your Weighted Matching Algorithm calculate similarity?
* **Answer**: In [`MatchAlgorithm.java`](file:///Users/t_s_8/Desktop/CampusLostFound/src/main/java/com/campuslostfound/dsa/MatchAlgorithm.java), similarity is computed on a 0–100 scale using five weighted factors:
  * **Category (25 points)**: 25 points if exact case-insensitive match, else 0.
  * **Location (25 points)**: 25 points if same campus location ID, else 0.
  * **Date Proximity (20 points)**: 20 points if same day; 15 if $\le 3$ days; 10 if $\le 7$ days; 5 if $\le 14$ days; 0 otherwise.
  * **Name Similarity (20 points)**: Word-level Jaccard token overlap between item names.
  * **Description Similarity (10 points)**: Word-level token overlap between item descriptions.
  * **Threshold**: $\ge 70.0$ points is classified as a potential candidate match.

---

## 10. Testing & Quality Assurance Questions

### Q1: How did you test your application?
* **Answer**: We implemented comprehensive automated testing using **JUnit 5**:
  * **Unit Tests**: Test models, custom exception handling, algorithm weights, heap ordering, and service validation logic in isolation without requiring a live database.
  * **Integration Tests**: Connect to our local MySQL database (`campus_lost_found`) to verify real JDBC CRUD operations, foreign key constraints, SQL injection resistance, and transactional rollback behavior.

### Q2: What are your latest test results?
* **Answer**: Running `mvn clean test` compiles all 47 production classes and 14 test classes, executing all **92 automated tests** with:
  * **Tests run**: 92
  * **Failures**: 0
  * **Errors**: 0
  * **Skipped**: 0
  * **Build Status**: SUCCESS

---

## 11. Verified Demonstration Credentials

| User ID | Name | Email / Login ID | Role | Password |
| :---: | :--- | :--- | :---: | :---: |
| **1** | Campus Administrator | `admin@campus.edu` | **ADMIN** | `DevPass@123` |
| **2** | Alice Johnson | `alice@campus.edu` | **STUDENT** | `DevPass@123` |
| **3** | Bob Smith | `bob@campus.edu` | **STUDENT** | `DevPass@123` |
| **4** | Charlie Davis | `charlie@campus.edu` | **STUDENT** | `DevPass@123` |
| **5** | Diana Prince | `diana@campus.edu` | **STUDENT** | `DevPass@123` |

> [!WARNING]
> These credentials are for the local college-project demonstration environment only. They are shared development accounts and must not be reused in production.

---

## 12. Design Trade-Offs & Limitations

### Honest Assessment of System Boundaries (Great for Viva Maturity)
1. **Desktop vs Web**: We chose JavaFX over a web framework (like Spring Boot/React) to keep the project self-contained and focused strictly on core Java OOP and JDBC principles without web framework abstractions hiding low-level details.
2. **Password Hashing**: We implemented standard unsalted SHA-256 for direct equivalence with MySQL's `SHA2()` seed functions. In a commercial production environment, we would upgrade to salted BCrypt or Argon2.
3. **Pessimistic vs Optimistic Locking**: We chose pessimistic locking (`SELECT ... FOR UPDATE`) because claim approvals involve high-value physical asset handovers where conflicting double-approvals are strictly intolerable.
4. **Binary Attachments**: We deliberately stored item descriptions as text rather than storing binary photo BLOBs in MySQL to prevent database bloat; production systems would store photos in S3/cloud storage and persist image URLs.
