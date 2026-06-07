# G8 Live Demo — Presenter Runbook (Kafka showcase)

---

## What you will show

| Capability | How you prove it |
|---|---|
| **Kafka integration** | Call G3, G7, G9, G2 APIs → they publish → G8 consumes in background |
| **Aggregation & KPIs** | Manual analytics job → dashboard + per-domain stats |
| **Alerting** | Threshold detection → HTTP POST to G5 with JWT |
| **Security** | 401 without token, then signed JWT |
| **Resilience** | Stop G5 → G8 keeps aggregating; circuit breaker drops alerts |
| **Observability** | Prometheus metrics + Grafana dashboard `sgitu_g8.json` |

**Groups showcased:** G2 (abonnements), G3 (utilisateurs), G7 (suivi véhicule), G9 (incidents), G5 (notifications), G8 (analytics).

**Out of scope:** G1 (billetterie) and G6 (paiements) — not integrated / not working in this repo.

---

## 0. Before the presentation (build only — no requests)

### Prerequisites

- Docker Desktop running
- Postman installed
- Repository root: `SGITU-Microservices`
- Root `.env` present (`cp .env.example .env` if needed)

### Postman setup

1. **Import** `G8_LIVE_DEMO.postman_collection.json`
2. Open collection **Variables** and set:
   - `jwtSecret` → **last** `JWT_SECRET` line in root `.env`  
     (Docker Compose uses the last duplicate value; typically `your_jwt_secret_here_min_256_bits`)
3. Leave `g3VerificationCode` empty until Phase 2A.1 completes.

### Build containers (night before or morning of demo)

Run from **repository root**. **Do not run Postman or integration scripts** — this keeps Mongo empty for a clean live demo.

```powershell
# ── 1. Shared broker ──
docker compose up -d kafka

# ── 2. G8 core (our service) ──
docker compose up -d --build g8-mongo g8-ml-service g8-analytics-service

# ── 3. G5 notifications (alert destination) ──
docker compose up -d --build mysql-notification notification-service

# ── 4. Kafka senders ──
docker compose up -d --build g3-users-db redis user-service
docker compose up -d --build db-g7 g7-service
docker compose up -d --build db-g9 g9-service
docker compose up -d --build db-abonnement abonnement-service

# ── 5. Observability ──
docker compose up -d prometheus grafana
```


Verify containers are healthy (~2–3 min after last wave):

```powershell
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### Browser tabs to open before class

| URL | Purpose |
|---|---|
| http://localhost:8088/swagger-ui.html | G8 API docs (backup) |
| http://localhost:9090/targets | Prometheus scrape status |
| http://localhost:3000 | Grafana (`admin` / `GRAFANA_ADMIN_PASSWORD` from `.env`) |
| http://localhost:8085/api/notifications/health | G5 health |

---

## 1. Kafka integration map (talking points)

| Sender | Topic / path | Trigger (Postman) |
|---|---|---|
| **G3** Users | `g8-user-events` | Create user → verify email (code from G3 logs) |
| **G7** Vehicles | `g8.vehicule.status` | Create vehicle → set `EN_SERVICE` → post GPS |
| **G9** Incidents | `incident.analytique.topic` | Signaler incident → **annuler** (supervisor JWT) |
| **G2** Subscriptions | Batch REST (`POST /api/events/batch`) | Create plan → souscrire → relay batch to G8 |
| **G5** Notifications | HTTP (not Kafka) | G8 sends threshold alerts with JWT |

**G5 is not a sender** — it receives alerts **from** G8.

---

## 2. Postman sequence (phase by phase)

Run folders **in order**. Use **Collection Runner** per phase or click manually and narrate responses.

### PHASE 0 — Platform ready (~2 min)

| Request | Say to the teacher |
|---|---|
| 0.1 G8 health | “Our analytics service is up.” |
| 0.2 G5 health | “Notification service is ready to receive alerts.” |
| 0.3 Prometheus | “Central metrics collector is running.” |
| 0.4–0.7 G3, G7, G9, G2 health | “All Kafka sender groups are running.” |

### PHASE 1 — Security (~2 min)

| Request | Say to the teacher |
|---|---|
| 1.1 Dashboard without token | “Unauthenticated access is rejected — 401.” |
| 1.2 Generate JWT | “We sign a JWT with the shared platform secret; all later G8 calls use it.” |

### PHASE 2 — Kafka senders (~12 min)

Run **2A → 2D** in order. Each folder hits another team’s API; G8 consumes Kafka in the background.

#### 2A — G3 Users → `g8-user-events`

| Step | Action |
|---|---|
| 2A.1 | Create user — saves `g3Email` and `g3UserId` |
| **Presenter** | Run Redis command below → copy **only the 6 digits** into `g3VerificationCode` |
| 2A.2 | Verify email — publishes `{ userId, action: active }` to Kafka |

**Get G3 verification code (mandatory pause):**

After **2A.1**, run this in PowerShell. The output is **only the code** — copy it straight into Postman variable `g3VerificationCode`:

```powershell
$key = (docker exec g3-redis redis-cli --raw KEYS "email_verification:demo-g3-*").Trim()
docker exec g3-redis redis-cli --raw GET $key
```

Then send **2A.2**.

#### 2B — G7 Vehicles → `g8.vehicule.status`

| Step | Narration |
|---|---|
| 2B.1 Create vehicle | “G7 registers a bus on line L-DEMO.” |
| 2B.2 Set EN_SERVICE | “Status change publishes to Kafka.” |
| 2B.3 Post GPS | “Position update refreshes speed/line in the same topic.” |

#### 2C — G9 Incidents → `incident.analytique.topic`

| Step | Narration |
|---|---|
| 2C.1 Signaler | “Passenger reports an incident — stored in G9.” |
| 2C.2 Annuler | “Supervisor cancels — **this** publishes the dossier to G8’s Kafka topic.” |

> Signalement alone does **not** publish to G8. Cancellation (or clôture) does.

#### 2D — G2 Subscriptions → G8 batch

| Step | Narration |
|---|---|
| 2D.1 Create plan | “G2 admin creates a subscription plan.” |
| 2D.2 Souscrire | “Passenger subscribes — G2 writes AnalytiqueTrace in MySQL.” |
| 2D.3 Batch to G8 | “Same contract G2’s scheduler POSTs every 30 min — we relay immediately.” |

#### 2E — Pause (~10 s)

Wait ~10 seconds after Phase 2, then optionally confirm Kafka ingestion (2A–2C only — **2D is REST, not Kafka**):

```powershell
docker logs g8-analytics-service --tail=100 | findstr /i "Kafka"
```

You should see lines like:

```text
Kafka [USER] — accepted=1 rejected=0 status=SUCCESS
Kafka [VEHICLE] — accepted=1 rejected=0 status=SUCCESS
Kafka [INCIDENT] — accepted=1 rejected=0 status=SUCCESS
```

G2 subscriptions (2D) won't appear here — they arrived via `POST /api/events/batch` instead.

### PHASE 3 — G8 analytics & KPIs (~5 min)

| Request | Say to the teacher |
|---|---|
| 3.1 Run analytics job | “Scheduler aggregates Kafka events + runs ML + threshold checks.” |
| 3.2 List snapshots | “Stat snapshots stored in MongoDB.” |
| 3.3 Dashboard | “Single API for G10 / frontends — all domains.” |
| 3.4 Users stats | “Data from G3 path.” |
| 3.5 Vehicles activity | “Data from G7 path.” |
| 3.6 Incidents stats | “Data from G9 path.” |
| 3.7 Subscriptions stats | “Data from G2 path.” |

### PHASE 4 — Threshold alerts → G5 (~5 min)

REST ingestion here is **only** to trigger thresholds quickly (not the main Kafka story).

| Request | Say to the teacher |
|---|---|
| 4.1 Delayed vehicles | “We inject poor punctuality (delay > threshold).” |
| 4.2 Critical incidents | “We exceed daily incident volume rules.” |
| 4.3 Run analytics | “G8 detects thresholds and calls G5 over HTTP.” |
| 4.4 Prometheus metrics | “Show `sgitu_alerts_triggered_total` incremented.” |

**Prove G5 persistence (terminal):**

```powershell
docker exec -e MYSQL_PWD=root_sgitu_2026 sgitu-mysql-notification mysql -uroot notifications_db -e "SELECT event_type, COUNT(*) FROM notifications WHERE source_service='G8_ANALYTICS' GROUP BY event_type ORDER BY event_type;"
```

Expected alert types: `PUNCTUALITY_ALERT`, `HIGH_INCIDENT_VOLUME`, `INCIDENT_ZONE_RISK`.

**Show G8 logs (alert sent):**

```powershell
docker logs g8-analytics-service --tail=30 | findstr /i "Alert sent"
```

### PHASE 5 — Resilience (G5 outage) (~5 min)

**Before Phase 5 — stop G5:**

```powershell
docker stop notification-service
```

| Request | Say to the teacher |
|---|---|
| 5.1 Analytics still runs | “G8 does not depend on G5 for aggregation.” |
| 5.2 Dashboard still works | “KPIs remain available.” |
| 5.3 Circuit breaker logs | “Alerts are detected but delivery is skipped safely.” |

**Show circuit breaker in logs:**

```powershell
docker logs g8-analytics-service --tail=50 | findstr /i "circuit breaker dropped"
```

Example:

```text
G5 circuit breaker OPEN — alert dropped [PUNCTUALITY_ALERT]: ...
```

**Restore G5 after demo:**

```powershell
docker start notification-service
Start-Sleep -Seconds 35
```

Re-run **4.3** to prove alerts flow again.

### PHASE 6 — Grafana dashboard (~3 min)

1. Open http://localhost:3000
2. Login: `admin` / `GRAFANA_ADMIN_PASSWORD` from `.env`
3. **Dashboards → SGITU →** provisioned from `monitoring/grafana/dashboards/sgitu_g8.json`
4. Point out panels: `sgitu_freq_total_validations`, `sgitu_veh_active_count`, `sgitu_alerts_triggered_total`
5. Cross-check with Postman **3.3 Dashboard** — same underlying snapshots

Postman **6.1** and **6.2** confirm Prometheus targets and G8 metrics if Grafana is slow to load.

---

## 3. Suggested presentation timeline

| Minute | Action |
|---|---|
| 0–2 | Confirm stack healthy, import Postman, open Grafana/Prometheus tabs |
| 2–4 | Phase 0 + Phase 1 (health + security) |
| 4–16 | Phase 2 Kafka senders (G3 → G7 → G9 → G2) |
| 16–21 | Phase 3 analytics + domain stats |
| 21–26 | Phase 4 alerts + MySQL proof |
| 26–31 | Phase 5 resilience: stop G5, logs, restore G5 |
| 31–35 | Phase 6 Grafana walkthrough |
| 35+ | Questions |

---

## 4. Troubleshooting

| Symptom | Fix |
|---|---|
| All G8 calls return 401 | Set Postman `jwtSecret` to match `.env` (last `JWT_SECRET`) |
| G3 verify-email fails | Re-run the Redis command with `--raw` (see Phase 2A). Code expires in 15 min — run 2A.1 again if needed |
| G9 cancel returns 403 | Supervisor JWT is generated in 2C.2 prerequest — check `jwtSecret` |
| G2 souscrire fails | Run 2A first (needs `g3UserId` / `g3Email`) |
| Kafka events not in dashboard | Wait 10 s after Phase 2, then run 3.1 |
| Prometheus down | `docker compose up -d prometheus` |
| G5 alerts not in MySQL | Rebuild G8 + G5; restart G8 to reset circuit breaker |
| Grafana empty | Run Phase 3.1; confirm Prometheus target `g8-analytics` is UP |
| Circuit breaker stuck OPEN | Wait 30 s after G5 restart, or `docker compose restart g8-analytics-service` |

---

## 5. Reference scripts (after class / validation)

These mirror Phase 2 and are useful for automated proof:

```powershell
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g3-user-events.ps1
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-vehicle-events.ps1
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g7-incident-events.ps1
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g2-subscription-events.ps1
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-g5-alert-integration.ps1
powershell -ExecutionPolicy Bypass -File .\service-analytique\test-root-integration.ps1
```

---

## 6. Files reference

| File | Role |
|---|---|
| `G8_LIVE_DEMO.postman_collection.json` | Live demo — Kafka senders + analytics + resilience |
| `G8_LIVE_DEMO.md` | This runbook |
| `G8_Analytics_Postman_Collection.json` | Full exhaustive test suite |
| `G8_INTEGRATION_TESTING_PLAN.md` | Automated PowerShell integration stages |
| `TEST_SCRIPTS_GUIDE.md` | Script usage and expected results |
| `monitoring/grafana/dashboards/sgitu_g8.json` | Grafana dashboard definition |
| `RAPPORT_AVANCEMENT.md` | Progress report for teachers |

Good luck with the presentation.
