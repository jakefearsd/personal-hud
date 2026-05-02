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

## Project Structure
- `hud-backend`: Spring Boot application.
- `hud-frontend`: React application (embedded in backend JAR).
- `Dockerfile`: Multi-stage build for the application.
- `docker-compose.yml`: Orchestration for app and DB.
- `GEMINI.md`: Detailed project architecture and conventions.

## Technical Documentation
- **[Model Configuration (Multi-Brain)](docs/ModelConfig.md)**: Guide on configuring and switching between local (Ollama) and remote (Gemini) LLMs.
