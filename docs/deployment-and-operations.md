# Deployment & Operations Guide

This guide covers building, deploying, monitoring, and operating HUD in development and production containerized environments.

---

## 1. Prerequisites

* **Operating System**: Linux (recommended) or macOS.
* **Java Runtime**: OpenJDK 21 LTS.
* **Build Tools**: Apache Maven 3.9+ and Node.js 22+.
* **Containers**: Docker Engine 24+ & Docker Compose v2.

---

## 2. Containerized Deployment (Recommended)

HUD is designed to run in Docker Compose with a dedicated PostgreSQL database container.

### 2.1. Initial Environment Setup
Set an optional admin password (if unset, a random password is generated and printed in the startup log):
```bash
export HUD_ADMIN_PASSWORD="YourSecurePassword123!"
```

### 2.2. Build Artifacts & Start Containers
Using the helper script:
```bash
./bin/deploy.sh --build
```

Or using standard commands:
```bash
mvn clean install -DskipTests
docker compose up --build -d
```

### 2.3. Port Allocations

| Service | Internal Port | Host Port | Description |
| :--- | :--- | :--- | :--- |
| **HUD Application** | `8888` | **`8889`** | Main Web UI & REST API ([http://localhost:8889](http://localhost:8889)). |
| **PostgreSQL Database** | `5432` | **`6432`** | Dedicated PostgreSQL database (`hud`, user: `huduser`). |

---

## 3. Automation Scripts (`bin/`)

Location-independent wrapper scripts are located in `bin/`:

```bash
# Full clean build with test suite and static analysis (PMD, SpotBugs, JaCoCo)
./bin/build.sh --clean

# Fast build skipping tests and static analysis
./bin/build.sh --fast

# Execute unit tests with JaCoCo code coverage report
./bin/test.sh --unit

# Execute full integration test suite against running containers/Ollama
./bin/test.sh --int

# Create a compressed database backup in backups/
./bin/db-backup.sh

# Restore a specific database snapshot
./bin/db-restore.sh backups/hud_backup_YYYYMMDD_HHMMSS.sql.gz
```

---

## 4. Monitoring & Troubleshooting

### 4.1. View Application Logs
```bash
docker compose logs -f app
```

### 4.2. View Database Logs
```bash
docker compose logs -f hud-db
```

### 4.3. Inspect Observability Dashboard
Navigate to [http://localhost:8889/observability](http://localhost:8889/observability) to review real-time execution times, token counts, and error stack traces for all automated briefing runs.

### 4.4. Complete Teardown & Reset
To wipe the database volume and rebuild from scratch:
```bash
docker compose down -v
mvn clean install -DskipTests
docker compose up --build -d
```
