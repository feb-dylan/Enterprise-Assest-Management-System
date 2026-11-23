# EAMS Backend — Enterprise Asset Management System

Spring Boot 4 · Java 21 · MySQL · JWT · AI via OpenRouter (optional, free models)

Manages company assets through their whole lifecycle: registration → request → approval →
assignment → damage → repair → return → retirement. It also includes QR scanning, AI damage
triage, asset health scores, live notifications and stock-take audits.

---

## Quick start

### Option A — try it without MySQL (in-memory demo)

```bash
./mvnw spring-boot:test-run
```

This starts the real backend on an in-memory H2 database with demo data. All data is lost when it stops.

- Swagger UI: http://localhost:8080/swagger-ui.html
- Demo logins (password `Password123!`):
  `admin@eams.demo`, `manager@eams.demo`, `tech@eams.demo`, `alice@eams.demo`, `bob@eams.demo`, `chan@eams.demo`

### Option B — with MySQL

1. Copy `.env.example` to `.env` (same folder as `pom.xml`). `.env` is git-ignored.
2. In `.env`, set at least `DB_PASSWORD` (your MySQL password) and `JWT_SECRET`
   (any random text of 32+ characters).
3. Run:

```bash
./mvnw spring-boot:run
```

The backend reads `.env` automatically. Real environment variables, if set, take priority.
The four roles are created automatically on startup. To load the demo data set once,
set `SEED_DEMO_DATA=true`. It is skipped if `admin@eams.demo` already exists.

### Run the tests

```bash
./mvnw test
```

The tests use H2 and do not need MySQL. There are 34 integration tests covering security, the full workflow and the new features.

---

## Configuration (environment variables)

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_PASSWORD` | yes (MySQL) | – | MySQL password |
| `DB_URL` / `DB_USERNAME` | no | localhost `eams_db` / `root` | Database location |
| `JWT_SECRET` | **yes** | – | At least 32 characters. The app refuses to start without it |
| `CORS_ALLOWED_ORIGINS` | no | `http://localhost:5173` | Frontend origin(s), comma-separated |
| `FRONTEND_URL` | no | `http://localhost:5173` | Used in QR codes and email links |
| `OPENROUTER_API_KEY` | no | – | OpenRouter key (free at openrouter.ai/keys). AI features are *disabled* without it |
| `AI_MODEL` | no | – | OpenRouter model id for "Ask EAMS". Must support **tools**. Free ones end in `:free`. Up to 3 ids separated by commas = fallback list when a free model is busy |
| `AI_VISION_MODEL` | no | = `AI_MODEL` | Model for damage-photo triage. Must accept **images** |
| `MAIL_ENABLED`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` | no | mail off | Email notifications. When mail is off, reset links are printed in the server log |
| `PUBLIC_REGISTRATION` | no | `true` | Set to `false` for invite-only sign-up |
| `SEED_DEMO_DATA` | no | `false` | Load demo users/assets/history |
| `UPLOAD_DIR` | no | `uploads` | Where images are stored |

---

## API changes for the frontend

Everything the current frontend calls **still works**. The changes below are what the next
frontend update should use.

### Identity comes from the login token
- New URLs without ids: `GET /api/requests/my`, `GET /api/requests/assignments/my`,
  `GET /api/damage/my`, `GET /api/dashboard/employee`,
  `PUT /api/requests/assignments/{id}/return-request`,
  `PUT /api/requests/{id}/approve | /reject | /assign`, `PUT /api/requests/assignments/{id}/return`.
- Old URLs with `{employeeId}` still work, but the id must be **your own** (otherwise 403).
  `{managerId}`/`{adminId}` in old URLs are ignored, and the logged-in user is recorded instead.
- `employeeId` in the body of `POST /api/requests` and `POST /api/damage` is ignored.
- `GET /api/auth/me` now also returns `employeeId`.

### Errors
Every error has the same JSON shape: `{timestamp, status, error, message, path}`. Validation errors also
include `messages: {field: text}`. `message` is **always a string**. Status codes are now correct
(400/401/403/404/409/429/503 instead of 500).

### Sessions
- `PUT /api/auth/change-password` returns `{message, token}`. **Store the new token**, because all old tokens
  stop working.
- A disabled user, or an employee set to INACTIVE, is logged out immediately (401).
- Handle `401` globally by redirecting to the login page.

### Richer responses
Requests, assignments and damage reports now include names (`employeeName`, `assetCode`,
`assetName`, `approvedByEmail`, ...). Assets include `warrantyExpiryDate`, `vendor`,
`currentHolderName`. The dashboard includes `warrantyExpiringSoon` and chart data
(`assetsByCategory`, `repairCostByCategory`).

### New endpoints

| Feature | Endpoints |
|---|---|
| Combined asset filter | `GET /api/assets?status=&categoryId=&q=` |
| QR scan page | `GET /api/assets/code/{assetCode}` → asset + `allowedActions` for this user |
| QR label sheet (PDF) | `GET /api/assets/labels/pdf?ids=1,2,3` |
| Timeline | `GET /api/assets/{id}/timeline` |
| Health score / repair-or-replace | `GET /api/assets/{id}/health` |
| Retire with disposal method | `PUT /api/assets/{id}/retire` `{disposalMethod, note}` |
| Warranty list | `GET /api/assets/warranty-expiring?days=30` |
| Category requests | `POST /api/requests {categoryId, reason, priority, neededBy}`; assign with `?assetId=` |
| Damage photos + AI triage | `POST /api/damage/{id}/photos` (multipart `files`), `POST /api/damage/{id}/ai-triage` |
| Technician jobs | `GET /api/maintenance/my`, `GET /api/users/technicians`; `technicianUserId` on create/update |
| Notifications | `GET /api/notifications`, `/unread-count`, `PUT /{id}/read`, `PUT /read-all`, SSE `GET /api/notifications/stream` |
| Insights | `GET /api/insights/summary`, `/replacement-candidates`, `/asset-health`, `/idle-assets?days=90` |
| Ask EAMS (AI) | `GET /api/assistant/status`, `POST /api/assistant/ask {question}` |
| Stock-take audit | `POST /api/audits`, `POST /api/audits/{id}/scans {assetCode, scannedLocation}`, `GET /api/audits/{id}`, `POST /api/audits/{id}/close` |
| Users (admin) | `GET /api/users`, `POST /api/users/invite {email, role}`, `PATCH /api/users/{id} {enabled, role}` |
| Password reset | `POST /api/auth/forgot-password {email}`, `POST /api/auth/reset-password {token, newPassword}` |
| Audit log | `GET /api/audit-log` |

The SSE stream needs the `Authorization` header. Browser `EventSource` cannot send headers, so use a
fetch-based client such as `@microsoft/fetch-event-source`.

---

## How asset status works now

The status is **calculated** from the records, in this priority order:

1. `RETIRED`: stays retired
2. `MAINTENANCE`: an open repair job, or a damage report in REPAIRING
3. `DAMAGED`: an open damage report
4. `ASSIGNED`: an employee holds it
5. `AVAILABLE`: otherwise

Damage, maintenance and assignment can no longer overwrite each other. For example, when a repair
finishes, the asset goes back to the employee who holds it instead of becoming `AVAILABLE`.
