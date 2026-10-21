# Campus Lost and Found

A desktop application for managing lost and found items on campus, built with Java 26, JavaFX, JDBC, and a local MySQL database.

## Architecture

The project adheres to a strict layered architecture:
```text
JavaFX UI
    ->
Service Layer
    ->
Business Logic / DSA (Matching, Searching, Ranking)
    ->
DAO Layer
    ->
JDBC
    ->
Local MySQL (campus_lost_found)
```

## Project Structure

```text
CampusLostFound/
├── pom.xml
├── README.md
├── .gitignore
├── database/
│   ├── schema.sql
│   ├── seed.sql
│   └── queries.sql
├── docs/
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── campuslostfound/
    │   └── resources/
    │       └── db.properties.example
    └── test/
        └── java/
            └── com/
                └── campuslostfound/
```

## Technology Stack
- **Language**: Java 26
- **Build Tool**: Maven
- **UI**: JavaFX
- **Database**: Local MySQL Server (`campus_lost_found`)
- **Persistence**: JDBC (via `mysql-connector-j`)
- **Testing**: JUnit 5

## Setup & Running

### 1. Database Configuration
1. Copy `src/main/resources/db.properties.example` to `src/main/resources/db.properties`:
   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```
2. Update `src/main/resources/db.properties` with your local MySQL credentials. (Note: `db.properties` is ignored by Git to protect secrets).

### 2. Build and Test
```bash
mvn clean test
mvn clean package
```

### 3. Run the JavaFX Desktop Application
```bash
mvn javafx:run
```

## Security & Architecture Highlights
- **Layered Architecture**: Strict boundaries between UI (JavaFX), Services, Data Structures & Algorithms, DAO, and JDBC.
- **SQL Injection Prevention**: 100% of SQL queries utilize `java.sql.PreparedStatement` with parameterized placeholders.
- **Password Security**: Passwords hashed with SHA-256 and salt before persistence; sensitive hash fields are omitted from `toString()` representations and logs.
- **Role-Based Access Control**: Student and Admin actions are verified in the Service layer (e.g. claim approval and analytics require Admin privileges).
- **Transactional Integrity**: Claim approvals run inside explicit JDBC transactions (`setAutoCommit(false)`, `SELECT ... FOR UPDATE`, atomic commit/rollback) to prevent double approvals and state corruption.
- **Secrets Management**: Live credentials in `db.properties` are strictly excluded from version control via `.gitignore`.
