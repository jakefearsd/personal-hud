# HUD (Heads-Up Display)

Information aggregator and scraper to support decision making.

## Tech Stack
- **Backend**: Spring Boot 3 (Java 21), Maven, PostgreSQL
- **Frontend**: React (TypeScript), Vite
- **Scraping**: Playwright (Java)
- **Containerization**: Docker & Docker Compose

## Getting Started

### Prerequisites
- Java 21
- Maven 3.9+
- Docker & Docker Compose

---

### Admin Account Setup

On first startup against an empty database the application seeds an `admin` user automatically.

- **Set a known password** by exporting `HUD_ADMIN_PASSWORD` before starting the app:
  ```bash
  export HUD_ADMIN_PASSWORD=your-password-here
  docker compose up --build -d
  ```
- **If the variable is unset**, a random 16-character password is generated and printed once at WARN level in the application log — look for the line containing `Seeded admin user with a GENERATED password`.
- Either way, **the admin account requires a password change on first login**.

---

### Recommended: Build & Run (Containerized)
This is the "no fuss" way to run the application with its dedicated database.

1. **Build the JAR file**:
   From the root directory, build the multi-module project:
   ```bash
   mvn clean install -DskipTests
   ```
   *Note: `-DskipTests` is used here because full integration tests require a running database.*

2. **Start the containers**:
   ```bash
   docker compose up --build -d
   ```
   *Flags explained:*
   - `--build`: Forces a rebuild of the application image (ensures the latest JAR is used).
   - `-d`: Runs containers in the background (detached mode).

3. **Access the application**:
   - Web UI: [http://localhost:8889/](http://localhost:8889/)
   - API News Endpoint: [http://localhost:8889/api/news](http://localhost:8889/api/news)

4. **Monitor Logs**:
   ```bash
   docker compose logs -f app
   ```

5. **Stop everything**:
   ```bash
   docker compose down
   ```

---

### Traditional Build (Manual)
If you prefer to run things outside of Docker:

1. **Build**:
   ```bash
   mvn clean install
   ```
2. **Run Backend**:
   Ensure you have a PostgreSQL instance running on port **5433**.
   ```bash
   mvn spring-boot:run -pl hud-backend
   ```

---

### Testing

Fast unit tests (no external dependencies):
```bash
mvn test
# or equivalently:
./bin/test.sh --unit
```

Full suite including integration and E2E tests (requires Docker, a live Ollama server, and/or a Gemini API key):
```bash
mvn test -Pintegration
# or equivalently:
./bin/test.sh --int
```

The default build excludes tests tagged `integration` via the `surefire.excludedGroups` property. The `-Pintegration` Maven profile clears that exclusion so all tests run.

---

### Database Schema

Schema is managed by [Flyway](https://flywaydb.org/) migrations located in:
```
hud-backend/src/main/resources/db/migration/
```

Migrations run automatically on startup. In production, Hibernate is set to `ddl-auto: validate` — it validates the schema against the entity model but never alters it; all structural changes go through a new migration script.

---

### Data Bootstrap Scripts

`bootstrap_global.sql` and `bootstrap_history.sql` are **not tracked in git**. Regenerate them locally when needed:
```bash
python harvest_global.py    # rebuilds bootstrap_global.sql
python harvest_history.py   # rebuilds bootstrap_history.sql
```

---

## Project Structure
- `hud-backend`: Spring Boot application.
- `hud-frontend`: React application (embedded in backend JAR).
- `Dockerfile`: Multi-stage build for the application.
- `docker-compose.yml`: Orchestration for app and DB.
- `GEMINI.md`: Detailed project architecture and conventions.

## Technical Documentation
- **[Model Configuration (Multi-Brain)](docs/ModelConfig.md)**: Guide on configuring and switching between local (Ollama) and remote (Gemini) LLMs.
