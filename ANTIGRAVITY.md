# HUD Project Instructions (Antigravity)

## Architecture & Conventions
- **Multi-module Maven**: The project is split into `hud-backend` (Spring Boot) and `hud-frontend` (React/Vite).
- **Embedded Frontend**: The frontend is built and embedded into the backend JAR at `META-INF/resources`.
- **TDD First**: Always write tests before implementing features.
- **Scraping**: Use Playwright (Java) for dynamic content scraping.

## Containerization & Deployment
We use Docker and Docker Compose for a "no fuss" local development and deployment environment.

### Prerequisites
- Docker and Docker Compose installed.
- Java 21 and Maven 3.9+ (for building the JAR).

### Build & Run Strategy
The application is designed to be built on the host and run in a container to ensure all Playwright system dependencies are met.

1. **Build the JAR**:
   From the root directory:
   ```bash
   mvn clean install -DskipTests
   ```
   *Note: Tests can be run locally using H2, but the final JAR must be built before containerizing.*

2. **Orchestrate with Docker Compose**:
   ```bash
   docker compose up --build -d
   ```
   This will spin up:
   - `hud-db`: A PostgreSQL 16 database (mapped to **6432** on host).
   - `hud-app`: The Spring Boot application running on **port 8889**.

    3. **Check Logs**:
     ```bash
     docker compose logs -f app
     ```

   ### Configuration & Persistence
   - **Database Durability**: We use a named Docker volume (`hud_db_data`) to ensure that user accounts, model configurations, and historical briefings are preserved across restarts.
   - **Wiping Data**: To completely reset the application state, run `docker compose down -v`.
   - **DB Credentials**: Default internal credentials are `huduser` / `hudpass`. The database is mapped to port **6432** on the host for manual inspection or backup.
