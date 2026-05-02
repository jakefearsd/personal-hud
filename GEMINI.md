# HUD Project Instructions

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
   - `hud-db`: A PostgreSQL 16 database (mapped to **5433** on host).
   - `hud-app`: The Spring Boot application running on **port 8889**.

   3. **Check Logs**:
    ```bash
    docker compose logs -f app
    ```

   ### Configuration
   The application uses environment variables for database connectivity, defaulting to local settings if not provided:
   - `DB_URL`: JDBC connection string (default: `jdbc:postgresql://localhost:5433/hud`)
   - `DB_USER`: Database username (default: `postgres`)
   - `DB_PASS`: Database password (default: `password`)
