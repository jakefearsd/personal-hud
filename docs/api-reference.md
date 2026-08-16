# REST API Reference

HUD provides a comprehensive RESTful API for client interaction, automated scheduling, briefing retrieval, macro financial analytics, and system administration.

---

## 1. Authentication & Session Management

Base path: `/api/auth`

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Public | Form login using `username` and `password`. Sets session cookie. |
| `POST` | `/api/auth/logout` | Public | Clears session cookie and invalidates authentication. |
| `GET` | `/api/auth/status` | Public | Returns current authentication state, active username, role, and password change requirement flag. |
| `POST` | `/api/auth/change-password` | Authenticated | Updates user password (`oldPassword`, `newPassword`). |

#### Example: Authentication Status Response
```json
{
  "authenticated": true,
  "username": "admin",
  "role": "ROLE_ADMIN",
  "passwordChangeRequired": false
}
```

---

## 2. Daily Intelligence Briefings

Base path: `/api/briefings`

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/briefings/latest` | Public | Fetches the most recent briefing for a given `category` and optional `modelName`. |
| `GET` | `/api/briefings/history` | Public | Returns historical briefings by category, model, and page limit (`limit=10`). |
| `GET` | `/api/briefings/{id}` | Public | Fetches a single briefing by its unique ID. |

#### Query Parameters for `/api/briefings/latest`:
* `category` *(required)*: Enum string (`WORLD_NEWS`, `US_NEWS`, `TECH_MACRO`, `THEATER_UKRAINE`, `THEATER_MIDDLE_EAST`, `THEATER_INDO_PACIFIC`, `GLOBAL_SITREP`).
* `modelName` *(optional)*: Name of the generating brain (e.g. `gemma4:e4b`, `gemini-2.0-flash`). Defaults to latest generated if omitted.

---

## 3. Investments & Macroeconomic Metrics

Base path: `/api/investments`

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/investments/pods` | Public | Returns all categorized Macro Pods with current prices, 24h deltas, and sparkline arrays. |
| `GET` | `/api/investments/history/{ticker}` | Public | Returns 30/90-day daily historical price series for a specific ticker (e.g., `^GSPC`, `BTC-USD`). |
| `GET` | `/api/investments/predictions/latest` | Public | Returns the latest AI-generated market predictions across monitored asset classes. |
| `GET` | `/api/investments/predictions/history` | Public | Returns historical predictions and accuracy tracking. |
| `GET` | `/api/investments/insights/latest` | Public | Returns the latest weekly cross-asset macroeconomic insights. |

---

## 4. Multi-Brain LLM Configuration

Base path: `/api/config/llm` (Requires `ROLE_ADMIN`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/config/llm` | `ROLE_ADMIN` | Lists all configured Brains. |
| `GET` | `/api/config/llm/active` | Public | Lists currently active Brain names for UI selector. |
| `POST` | `/api/config/llm` | `ROLE_ADMIN` | Creates a new Brain configuration. |
| `PUT` | `/api/config/llm/{id}` | `ROLE_ADMIN` | Updates an existing Brain configuration. |
| `DELETE` | `/api/config/llm/{id}` | `ROLE_ADMIN` | Deletes a Brain configuration. |
| `POST` | `/api/config/llm/{id}/toggle` | `ROLE_ADMIN` | Toggles the active/inactive status of a Brain. |
| `POST` | `/api/config/llm/test` | `ROLE_ADMIN` | Sends a test prompt to verify connectivity to the specified model endpoint. |

#### Brain Configuration Schema (`POST /api/config/llm`):
```json
{
  "name": "Local Gemma 4",
  "provider": "OLLAMA",
  "baseUrl": "http://localhost:11434",
  "modelName": "gemma4:e4b",
  "apiKey": null,
  "contextWindow": 65536,
  "temperature": 0.2,
  "active": true
}
```

---

## 5. Scheduling Configuration

Base path: `/api/config/schedules` (Requires `ROLE_ADMIN`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/config/schedules` | `ROLE_ADMIN` | Lists all scheduled briefing jobs and their cron expressions. |
| `PUT` | `/api/config/schedules/{id}` | `ROLE_ADMIN` | Updates the cron schedule for a specific category job at runtime. |

---

## 6. Pipeline Execution & Observability

Base path: `/api/pipeline` (Requires `ROLE_ADMIN`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/pipeline/trigger` | `ROLE_ADMIN` | Triggers an immediate asynchronous briefing run across all active models and categories. |
| `GET` | `/api/pipeline/runs` | `ROLE_ADMIN` | Returns execution history, durations, token counts, and error cause-chains. |
