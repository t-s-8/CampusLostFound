# Campus Lost & Found Web App

The JavaFX desktop app remains available. The same Maven project now also runs a Spring Boot web app with a browser UI at `/` and JSON endpoints under `/api`. Both versions reuse the current Java services, JDBC DAOs, MySQL schema, weighted matching algorithm, and transactional claim approval.

## Run locally

1. Start MySQL and create the existing `campus_lost_found` schema using `database/schema.sql` only for a new/local database. That script drops and recreates tables.
2. Set `DB_URL`, `DB_USER`, and `DB_PASSWORD` in the server environment (or use the ignored `src/main/resources/db.properties`). Never put these values in browser code or commit them.
3. Start the web app with `mvn spring-boot:run`; open `http://localhost:8080`. The JavaFX desktop app can still run with `mvn javafx:run`.
4. Create the first administrator using `WEB_BOOTSTRAP_ADMIN_EMAIL`, `WEB_BOOTSTRAP_ADMIN_PASSWORD` (at least 16 characters), and optionally `WEB_BOOTSTRAP_ADMIN_NAME`. These variables are read only by the server. The configured account is made an administrator and its password is securely hashed at startup. Keep those variables private.

## Free demo deployment

This repo includes a Dockerfile for a Render web service and works with an Aiven managed MySQL service. Aiven's free MySQL plan currently lists 1 CPU, 1 GB RAM, 1 GB storage, no SLA, and may power down an unused service. Render's free web services sleep after 15 minutes idle, take about a minute to wake, and may be suspended if service-initiated internet traffic is unusually high. Treat this as a class/demo deployment, not a dependable production service.

1. Create an Aiven MySQL Free service. Choose a nearby region if the console allows it; the Free tier may restrict region choice. Copy the service hostname, port, username, password, and TLS connection details from Aiven.
2. Connect to Aiven's initial database (`defaultdb`) using the Aiven connection details and execute `database/schema-hosted.sql`, followed by `database/hosted-locations.sql`. These hosted scripts do not drop tables. Do not execute `database/schema.sql` or `database/seed.sql` against an existing or hosted database: the local schema drops tables and the demo seed truncates them and inserts predictable demo accounts.
3. Push this repository to a Git provider. In Render, create a **Web Service** from the repo and select the Docker runtime. Render detects the included `Dockerfile`; the app binds to its `PORT` environment variable.
4. Add these Render environment variables. Build the JDBC URL using Aiven's exact hostname and port, selecting `defaultdb` and requiring TLS.

   - `DB_URL` = `jdbc:mysql://<AIVEN_HOST>:<AIVEN_PORT>/defaultdb?sslMode=REQUIRED&serverTimezone=UTC`
   - `DB_USER` = Aiven service username
   - `DB_PASSWORD` = Aiven service password
   - `SESSION_COOKIE_SECURE` = `true`
   - `WEB_BOOTSTRAP_ADMIN_EMAIL` = your private administrator email
   - `WEB_BOOTSTRAP_ADMIN_PASSWORD` = a unique password with at least 16 characters
   - `WEB_BOOTSTRAP_ADMIN_NAME` = optional display name

5. Deploy. Open the Render URL, register student accounts, sign in as the configured administrator, and review reports and claims. A first visit after inactivity may take around a minute while Render wakes.

If the project already contains data you need, do not use the hosted schema reset or demo seed. Export the current MySQL database and import it into Aiven first, then point the app at that database. Back up the source database before migration.

## Environment variables

| Variable | Required | Purpose |
|---|---|---|
| `PORT` | Set by Render | HTTP listener port |
| `DB_URL` | Yes | JDBC URL to the hosted MySQL database, including TLS options |
| `DB_USER` | Yes | Database username, server only |
| `DB_PASSWORD` | Yes | Database password, server only |
| `SESSION_COOKIE_SECURE` | Production | Set `true` behind HTTPS; leave `false` for local HTTP |
| `WEB_BOOTSTRAP_ADMIN_EMAIL` | For a new database | Creates/promotes the first admin account |
| `WEB_BOOTSTRAP_ADMIN_PASSWORD` | For a new database | Private bootstrap password (16+ chars) |
| `WEB_BOOTSTRAP_ADMIN_NAME` | Optional | Admin display name |

## API outline

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout`, `GET /api/auth/me`
- `GET /api/locations`, `GET/POST /api/items`
- `GET /api/matches`
- `GET /api/claims/mine`, `POST /api/claims`, and `POST /api/claims/{id}/approve|reject`
- Admin only: `GET /api/admin/analytics`, `GET /api/claims/admin/pending`, `GET /api/claims/admin/all`

Mutating API requests require the session CSRF token returned by `GET /api/auth/csrf`; the bundled browser app handles it. Session authentication and admin checks run on the server. Public item browsing returns active reports only. Registration always creates a student account; only the server-configured bootstrap account can be promoted to administrator.
